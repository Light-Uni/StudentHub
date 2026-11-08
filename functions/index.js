const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const v = require('./validation');
initializeApp();
const db = getFirestore();
const auth = getAuth();
const options = { region: 'asia-southeast1', maxInstances: 5, timeoutSeconds: 120 };

async function actor(request, roles = ['admin', 'manager', 'employee']) {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Vui lòng đăng nhập.');
  const snapshot = await db.doc(`users/${request.auth.uid}`).get();
  const profile = snapshot.data();
  if (!profile || profile.status !== 'normal' || !roles.includes(profile.role)) throw new HttpsError('permission-denied', 'Tài khoản bị khóa hoặc không có quyền thực hiện.');
  return { ...profile, uid: request.auth.uid };
}
function endpoint(handler) {
  return onCall(options, async request => {
    try { return await handler(request); }
    catch (error) {
      if (error instanceof HttpsError) throw error;
      if (error.code === 'auth/email-already-exists') throw new HttpsError('already-exists', 'Email đã có tài khoản.');
      console.error('Thao tác thất bại:', error.code || error.name);
      throw new HttpsError('internal', 'Không thể hoàn tất thao tác. Vui lòng thử lại.');
    }
  });
}
function valid(fn, data) {
  try { return fn(data); } catch (e) { throw new HttpsError('invalid-argument', e.message); }
}
function documentId(value) { return valid(v.id, value); }
async function recheck(tx, uid, roles) {
  const snap = await tx.get(db.doc(`users/${uid}`));
  if (!snap.exists || snap.data().status !== 'normal' || !roles.includes(snap.data().role)) throw new HttpsError('permission-denied', 'Quyền tài khoản đã thay đổi.');
}

exports.recordLogin = endpoint(async request => {
  const profile = await actor(request);
  const authTime = request.auth.token.auth_time;
  if (!Number.isInteger(authTime)) throw new HttpsError('unauthenticated', 'Phiên đăng nhập không hợp lệ.');
  const ref = db.doc(`login_history/${profile.uid}_${authTime}`);
  await db.runTransaction(async tx => {
    const current = await tx.get(ref);
    if (!current.exists) tx.create(ref, { userId: profile.uid, loginAt: FieldValue.serverTimestamp(), authTime,
      device: v.text(String(request.data?.device || 'Android'), 'Thiết bị', 120) });
  });
  return { ok: true };
});

exports.saveUser = endpoint(async request => {
  const me = await actor(request, ['admin']);
  const data = request.data || {};
  const creating = !data.uid;
  const fields = valid(d => v.user(d, creating), data);
  if (creating) {
    const account = await auth.createUser({ email: fields.email, password: data.password, displayName: fields.fullName, disabled: true });
    const ref = db.doc(`users/${account.uid}`);
    try {
      await db.runTransaction(async tx => {
        await recheck(tx, me.uid, ['admin']);
        tx.create(ref, { ...fields, status: 'locked', avatarBase64: '', createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(), bootstrap: false });
      });
      if (fields.status === 'normal') {
        await auth.updateUser(account.uid, { disabled: false });
        await ref.update({ status: 'normal' });
      }
      return { uid: account.uid };
    } catch (error) {
      await auth.deleteUser(account.uid).catch(() => {});
      await ref.delete().catch(() => {});
      throw error;
    }
  }
  const uid = valid(d => v.text(d, 'Mã tài khoản', 128), data.uid);
  if (uid.includes('/')) throw new HttpsError('invalid-argument', 'Mã tài khoản không hợp lệ.');
  const ref = db.collection('users').doc(uid);
  await db.runTransaction(async tx => {
    await recheck(tx, me.uid, ['admin']);
    const old = await tx.get(ref);
    if (!old.exists) throw new HttpsError('not-found', 'Không tìm thấy tài khoản.');
    if (old.data().deleting) throw new HttpsError('failed-precondition', 'Tài khoản đang được xóa.');
    if ((uid === me.uid || old.data().bootstrap) && (fields.role !== 'admin' || fields.status !== 'normal')) throw new HttpsError('failed-precondition', 'Không thể khóa hoặc hạ quyền tài khoản này.');
    tx.update(ref, { ...fields, updatedAt: FieldValue.serverTimestamp() });
  });
  await auth.updateUser(uid, { displayName: fields.fullName, disabled: fields.status === 'locked' });
  if (fields.status === 'locked') await auth.revokeRefreshTokens(uid);
  return { uid };
});

exports.deleteUser = endpoint(async request => {
  const me = await actor(request, ['admin']);
  const uid = valid(d => v.text(d, 'Mã tài khoản', 128), request.data?.uid);
  if (uid.includes('/')) throw new HttpsError('invalid-argument', 'Mã tài khoản không hợp lệ.');
  const ref = db.doc(`users/${uid}`);
  await db.runTransaction(async tx => {
    await recheck(tx, me.uid, ['admin']);
    const old = await tx.get(ref);
    if (!old.exists) throw new HttpsError('not-found', 'Không tìm thấy tài khoản.');
    if (uid === me.uid || old.data().bootstrap) throw new HttpsError('failed-precondition', 'Không thể xóa tài khoản này.');
    tx.update(ref, { status: 'locked', deleting: true });
  });
  await auth.deleteUser(uid).catch(e => { if (e.code !== 'auth/user-not-found') throw e; });
  await ref.delete();
  return { ok: true };
});

exports.updateAvatar = endpoint(async request => {
  const me = await actor(request);
  const image = request.data?.avatarBase64;
  if (typeof image !== 'string' || image.length > 180000 || !/^[A-Za-z0-9+/]+={0,2}$/.test(image)) throw new HttpsError('invalid-argument', 'Ảnh không hợp lệ hoặc quá lớn.');
  const bytes = Buffer.from(image, 'base64');
  if (bytes[0] !== 0xff || bytes[1] !== 0xd8 || bytes.length > 135000) throw new HttpsError('invalid-argument', 'Ảnh cần ở dạng JPEG.');
  await db.runTransaction(async tx => {
    await recheck(tx, me.uid, ['admin', 'manager', 'employee']);
    tx.update(db.doc(`users/${me.uid}`), { avatarBase64: image, updatedAt: FieldValue.serverTimestamp() });
  });
  return { ok: true };
});

exports.saveStudent = endpoint(async request => {
  const me = await actor(request, ['admin', 'manager']);
  const fields = valid(v.student, request.data?.record || {});
  const ref = db.doc(`students/${fields.studentId}`);
  await db.runTransaction(async tx => {
    await recheck(tx, me.uid, ['admin', 'manager']);
    const old = await tx.get(ref);
    if (request.data.create && old.exists) throw new HttpsError('already-exists', 'Mã sinh viên đã tồn tại.');
    if (!request.data.create && !old.exists) throw new HttpsError('not-found', 'Sinh viên đã bị xóa.');
    if (old.data()?.deleting) throw new HttpsError('failed-precondition', 'Hồ sơ đang được xóa.');
    tx.set(ref, { ...fields, createdAt: old.data()?.createdAt || FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
  });
  return { ok: true };
});

exports.deleteStudent = endpoint(async request => {
  const me = await actor(request, ['admin', 'manager']);
  const ref = db.doc(`students/${documentId(request.data?.studentId)}`);
  await db.runTransaction(async tx => {
    await recheck(tx, me.uid, ['admin', 'manager']);
    const old = await tx.get(ref);
    if (old.exists) tx.update(ref, { deleting: true });
  });
  try {
    await db.recursiveDelete(ref.collection('certificates'));
    await ref.delete();
  } catch (error) {
    await ref.update({ deleting: false }).catch(() => {});
    throw error;
  }
  return { ok: true };
});

exports.saveCertificate = endpoint(async request => {
  const me = await actor(request, ['admin', 'manager']);
  const studentId = documentId(request.data?.studentId);
  const fields = valid(v.certificate, request.data?.record || {});
  const parent = db.doc(`students/${studentId}`);
  const ref = parent.collection('certificates').doc(fields.certificateId);
  await db.runTransaction(async tx => {
    await recheck(tx, me.uid, ['admin', 'manager']);
    const student = await tx.get(parent);
    const old = await tx.get(ref);
    if (!student.exists || student.data().deleting) throw new HttpsError('not-found', 'Sinh viên không tồn tại hoặc đang được xóa.');
    if (request.data.create && old.exists) throw new HttpsError('already-exists', 'Mã chứng chỉ đã tồn tại.');
    if (!request.data.create && !old.exists) throw new HttpsError('not-found', 'Chứng chỉ đã bị xóa.');
    tx.set(ref, { ...fields, studentId, createdAt: old.data()?.createdAt || FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
  });
  return { ok: true };
});

exports.deleteCertificate = endpoint(async request => {
  const me = await actor(request, ['admin', 'manager']);
  const ref = db.doc(`students/${documentId(request.data?.studentId)}/certificates/${documentId(request.data?.certificateId)}`);
  await db.runTransaction(async tx => {
    await recheck(tx, me.uid, ['admin', 'manager']);
    tx.delete(ref);
  });
  return { ok: true };
});

exports.importRecords = endpoint(async request => {
  const me = await actor(request, ['admin', 'manager']);
  const { kind, records } = request.data || {};
  if (!['students', 'certificates'].includes(kind) || !Array.isArray(records) || records.length < 1 || records.length > 200) throw new HttpsError('invalid-argument', 'Cần từ 1 đến 200 dòng dữ liệu.');
  const studentId = kind === 'certificates' ? documentId(request.data.studentId) : '';
  const collection = kind === 'students' ? db.collection('students') : db.collection(`students/${studentId}/certificates`);
  const validator = kind === 'students' ? v.student : v.certificate;
  const key = kind === 'students' ? 'studentId' : 'certificateId';
  const errors = [], candidates = [], seen = new Set();
  records.forEach((record, index) => {
    try {
      const parsed = validator(record);
      if (seen.has(parsed[key])) throw new Error('Mã trùng trong tệp.');
      seen.add(parsed[key]); candidates.push({ index, parsed });
    } catch (error) { errors.push({ row: index + 2, message: error.message }); }
  });
  const result = await db.runTransaction(async tx => {
    await recheck(tx, me.uid, ['admin', 'manager']);
    if (studentId) {
      const parent = await tx.get(db.doc(`students/${studentId}`));
      if (!parent.exists || parent.data().deleting) throw new HttpsError('not-found', 'Sinh viên không tồn tại.');
    }
    const refs = candidates.map(c => collection.doc(c.parsed[key]));
    const existing = refs.length ? await tx.getAll(...refs) : [];
    let imported = 0;
    const duplicates = [];
    candidates.forEach((candidate, index) => {
      if (existing[index].exists) { duplicates.push(candidate.parsed[key]); return; }
      tx.create(refs[index], { ...candidate.parsed, ...(studentId ? { studentId } : {}), createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
      imported++;
    });
    return { imported, duplicates };
  });
  return { ...result, errors };
});

exports.exportRecords = endpoint(async request => {
  await actor(request, ['admin', 'manager']);
  const kind = request.data?.kind;
  if (!['students', 'certificates'].includes(kind)) throw new HttpsError('invalid-argument', 'Loại dữ liệu không hợp lệ.');
  const col = kind === 'students' ? db.collection('students') : db.collection(`students/${documentId(request.data.studentId)}/certificates`);
  let query = col.orderBy('__name__').limit(200);
  if (request.data.after) query = query.startAfter(documentId(request.data.after));
  const snapshot = await query.get();
  const keys = kind === 'students' ? v.STUDENT_FIELDS : v.CERTIFICATE_FIELDS;
  const records = snapshot.docs.filter(doc => !doc.data().deleting).map(doc => Object.fromEntries(keys.map(key => [key, doc.data()[key] || ''])));
  return { records, next: snapshot.size === 200 ? snapshot.docs.at(-1).id : '' };
});
