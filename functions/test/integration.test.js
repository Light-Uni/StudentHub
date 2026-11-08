const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, getDoc, setDoc, collection, getDocs, collectionGroup } = require('firebase/firestore');
const { initializeApp, deleteApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const projectId = 'demo-student-manager';
let env, adminApp, db, auth;
const tokens = {};
const student = { studentId: 'TEST001', fullName: 'Nguyễn Kiểm Thử', birthDate: '2004-02-29', gender: 'other', email: 'test@example.test', phone: '', address: '', className: 'CNTT01', major: 'CNTT', enrollmentDate: '2022-09-01', status: 'studying' };
async function call(name, role, data) {
  const response = await fetch(`http://127.0.0.1:5001/${projectId}/asia-southeast1/${name}`, {
    method: 'POST', signal: AbortSignal.timeout(60000), headers: { 'Content-Type': 'application/json', ...(role ? { Authorization: `Bearer ${tokens[role]}` } : {}) }, body: JSON.stringify({ data })
  });
  return response.json();
}
before(async () => {
  if (!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST) throw new Error('Chạy kiểm thử thông qua firebase emulators:exec.');
  env = await initializeTestEnvironment({ projectId, firestore: { host: '127.0.0.1', port: 8080, rules: fs.readFileSync(path.join(__dirname, '../../firestore.rules'), 'utf8') } });
  await env.clearFirestore();
  adminApp = initializeApp({ projectId }, 'integration'); db = getFirestore(adminApp); auth = getAuth(adminApp);
  for (const role of ['admin', 'manager', 'employee', 'locked']) {
    const uid = `test-${role}`, email = `${role}@integration.test`;
    await auth.deleteUser(uid).catch(() => {});
    await auth.createUser({ uid, email, password: 'TestPassword!2026' });
    await db.doc(`users/${uid}`).set({ fullName: role, email, role: role === 'locked' ? 'employee' : role, status: role === 'locked' ? 'locked' : 'normal', age: 24, phone: '', avatarBase64: '', bootstrap: role === 'admin', createdAt: FieldValue.serverTimestamp() });
    const response = await fetch('http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-key', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password: 'TestPassword!2026', returnSecureToken: true }) });
    tokens[role] = (await response.json()).idToken;
    assert.ok(tokens[role]);
  }
  await db.doc('students/TEST001').set({ ...student, createdAt: FieldValue.serverTimestamp() });
});
after(async () => { if (env) { await env.clearFirestore(); await env.cleanup(); } if (adminApp) await deleteApp(adminApp); });
test('Chặn người chưa đăng nhập và tài khoản khóa đọc sinh viên', async () => {
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'students/TEST001')));
  await assertFails(getDoc(doc(env.authenticatedContext('test-locked').firestore(), 'students/TEST001')));
});
test('Employee đọc sinh viên nhưng không ghi, nâng quyền hay xem người dùng khác', async () => {
  const client = env.authenticatedContext('test-employee').firestore();
  await assertSucceeds(getDoc(doc(client, 'students/TEST001')));
  await assertFails(setDoc(doc(client, 'students/TEST002'), student));
  await assertFails(setDoc(doc(client, 'users/test-employee'), { role: 'admin' }, { merge: true }));
  await assertFails(getDocs(collection(client, 'users')));
  await assertFails(getDocs(collection(client, 'login_history')));
  await assertSucceeds(getDoc(doc(client, 'users/test-employee')));
});
test('Admin đọc danh sách tài khoản; Manager không được đọc', async () => {
  await assertSucceeds(getDocs(collection(env.authenticatedContext('test-admin').firestore(), 'users')));
  await assertFails(getDocs(collection(env.authenticatedContext('test-manager').firestore(), 'users')));
});
test('Callable từ chối Employee ghi, nhập và xuất dữ liệu', async () => {
  for (const [name, data] of [['saveStudent', { record: student, create: false }], ['importRecords', { kind: 'students', records: [student] }], ['exportRecords', { kind: 'students' }], ['deleteStudent', { studentId: 'TEST001' }]]) {
    assert.equal((await call(name, 'employee', data)).error.status, 'PERMISSION_DENIED');
  }
});
test('Callable từ chối người vô danh, tài khoản khóa và Manager tạo tài khoản', async () => {
  assert.equal((await call('saveStudent', null, { record: student })).error.status, 'UNAUTHENTICATED');
  assert.equal((await call('saveStudent', 'locked', { record: student })).error.status, 'PERMISSION_DENIED');
  assert.equal((await call('saveUser', 'manager', {})).error.status, 'PERMISSION_DENIED');
});
test('CRUD sinh viên và chứng chỉ; xóa sinh viên xóa cả chứng chỉ', async () => {
  const record = { ...student, studentId: 'TESTCRUD' };
  assert.ok((await call('saveStudent', 'manager', { record, create: true })).result.ok);
  assert.equal((await call('saveStudent', 'manager', { record, create: true })).error.status, 'ALREADY_EXISTS');
  assert.ok((await call('saveStudent', 'manager', { record: { ...record, fullName: 'Đã sửa' }, create: false })).result.ok);
  const certificate = { certificateId: 'C01', name: 'MOS', issuer: 'Certiport', issueDate: '2025-01-01', expirationDate: '', description: '' };
  assert.ok((await call('saveCertificate', 'manager', { studentId: record.studentId, record: certificate, create: true })).result.ok);
  assert.ok((await call('saveCertificate', 'manager', { studentId: record.studentId, record: { ...certificate, name: 'MOS Excel' }, create: false })).result.ok);
  assert.equal((await call('exportRecords', 'manager', { kind: 'certificates', studentId: record.studentId })).result.records[0].name, 'MOS Excel');
  await assertSucceeds(getDocs(collectionGroup(env.authenticatedContext('test-employee').firestore(), 'certificates')));
  assert.ok((await call('deleteStudent', 'manager', { studentId: record.studentId })).result.ok);
  assert.equal((await db.doc(`students/${record.studentId}`).get()).exists, false);
  assert.equal((await db.collection(`students/${record.studentId}/certificates`).get()).size, 0);
});
test('Nhập hỗn hợp: bỏ mã có sẵn, phát hiện trùng trong tệp và ngày sai', async () => {
  const result = await call('importRecords', 'manager', { kind: 'students', records: [student, { ...student, studentId: 'IMPORT01' }, { ...student, studentId: 'IMPORT01' }, { ...student, studentId: 'INVALID01', birthDate: '2003-02-29' }] });
  assert.equal(result.result.imported, 1); assert.equal(result.result.duplicates.length, 1); assert.equal(result.result.errors.length, 2);
  const retry = await call('importRecords', 'manager', { kind: 'students', records: [{ ...student, studentId: 'IMPORT01' }] });
  assert.equal(retry.result.imported, 0);
});
test('Lịch sử đăng nhập do backend ghi, không nhân đôi một phiên', async () => {
  await call('recordLogin', 'employee', { device: 'Android test' }); await call('recordLogin', 'employee', { device: 'Android test' });
  const history = await db.collection('login_history').where('userId', '==', 'test-employee').get(); assert.equal(history.size, 1);
  await assertFails(setDoc(doc(env.authenticatedContext('test-admin').firestore(), 'login_history/forged'), { userId: 'test-employee' }));
});
test('Employee chỉ đổi ảnh của chính mình', async () => {
  const avatarBase64 = Buffer.from([0xff, 0xd8, 0xff, 0xd9]).toString('base64');
  assert.ok((await call('updateAvatar', 'employee', { avatarBase64, uid: 'test-admin' })).result.ok);
  assert.equal((await db.doc('users/test-employee').get()).data().avatarBase64, avatarBase64);
  assert.equal((await db.doc('users/test-admin').get()).data().avatarBase64, '');
});
test('Admin tạo/sửa/khóa/xóa tài khoản; không lưu mật khẩu Firestore', async () => {
  const fields = { fullName: 'Tài khoản kiểm thử', email: 'new@integration.test', age: 22, phone: '0901234567', role: 'employee', status: 'normal', password: 'TestPassword!2026' };
  const old = await auth.getUserByEmail(fields.email).catch(() => null); if (old) await auth.deleteUser(old.uid);
  const created = await call('saveUser', 'admin', fields); assert.ok(created.result?.uid, JSON.stringify(created)); const uid = created.result.uid;
  assert.equal((await db.doc(`users/${uid}`).get()).data().password, undefined);
  assert.ok((await call('saveUser', 'admin', { ...fields, uid, status: 'locked', role: 'manager' })).result.uid);
  assert.equal((await auth.getUser(uid)).disabled, true);
  assert.ok((await call('saveUser', 'admin', { ...fields, uid, status: 'normal' })).result.uid);
  assert.equal((await auth.getUser(uid)).disabled, false);
  assert.ok((await call('deleteUser', 'admin', { uid })).result.ok);
  assert.equal((await db.doc(`users/${uid}`).get()).exists, false);
  assert.equal((await call('deleteUser', 'admin', { uid: 'test-admin' })).error.status, 'FAILED_PRECONDITION');
});
test('Nhập/xuất và xóa chứng chỉ, giữ quan hệ đúng sinh viên', async () => {
  const certificate = { certificateId: 'IMPORTCERT', name: 'IELTS', issuer: 'British Council', issueDate: '2025-05-01', expirationDate: '2027-05-01', description: 'Kiểm thử nhập' };
  const data = { kind: 'certificates', studentId: 'TEST001', records: [certificate, certificate, { ...certificate, certificateId: 'INVALIDCERT', expirationDate: '2024-01-01' }] };
  const result = await call('importRecords', 'manager', data);
  assert.equal(result.result.imported, 1); assert.equal(result.result.errors.length, 2);
  assert.equal((await db.doc('students/TEST001/certificates/IMPORTCERT').get()).data().studentId, 'TEST001');
  const exported = await call('exportRecords', 'manager', { kind: 'certificates', studentId: 'TEST001' });
  assert.equal(exported.result.records[0].name, 'IELTS');
  assert.ok((await call('deleteCertificate', 'manager', { studentId: 'TEST001', certificateId: 'IMPORTCERT' })).result.ok);
  assert.equal((await db.doc('students/TEST001/certificates/IMPORTCERT').get()).exists, false);
  assert.equal((await call('saveCertificate', 'manager', { studentId: 'MISSING', record: certificate, create: true })).error.status, 'NOT_FOUND');
});
test('Khóa tài khoản chặn ngay token cũ ở Rules và callable', async () => {
  const data = { uid: 'test-manager', fullName: 'Quản lý', age: 24, phone: '', role: 'manager', status: 'locked' };
  assert.ok((await call('saveUser', 'admin', data)).result.uid);
  assert.equal((await call('saveStudent', 'manager', { record: student, create: false })).error.status, 'PERMISSION_DENIED');
  await assertFails(getDoc(doc(env.authenticatedContext('test-manager').firestore(), 'students/TEST001')));
  assert.ok((await call('saveUser', 'admin', { ...data, status: 'normal' })).result.uid);
});
