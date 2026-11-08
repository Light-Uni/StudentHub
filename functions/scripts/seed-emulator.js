const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

async function main() {
  if (process.env.GCLOUD_PROJECT && process.env.GCLOUD_PROJECT !== 'demo-student-manager') throw new Error('Chỉ tạo mẫu cho demo-student-manager.');
  process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
  process.env.FIREBASE_AUTH_EMULATOR_HOST = '127.0.0.1:9099';
  initializeApp({ projectId: 'demo-student-manager' });
  const auth = getAuth(), db = getFirestore();
  for (const [role, fullName] of [['admin', 'Nguyễn Minh Anh'], ['manager', 'Trần Hoàng Nam'], ['employee', 'Lê Thu Hà']]) {
    const uid = `demo-${role}`;
    try { await auth.createUser({ uid, email: `${role}@example.test`, password: 'DemoStudent!2026', displayName: fullName }); }
    catch (error) { if (error.code !== 'auth/uid-already-exists' && error.code !== 'auth/email-already-exists') throw error; }
    await db.doc(`users/${uid}`).set({ fullName, email: `${role}@example.test`, age: 28, phone: '0901234567', role, status: 'normal', bootstrap: role === 'admin', avatarBase64: '', createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
  }
  const students = [
    ['SV001', 'Nguyễn Hoài An', 'D22CQCN01', 'Công nghệ thông tin', 'female', 'studying'],
    ['SV002', 'Trần Minh Khang', 'D22CQCN01', 'Công nghệ thông tin', 'male', 'studying'],
    ['SV003', 'Lê Bảo Ngọc', 'D22CQMK02', 'Marketing', 'female', 'studying'],
    ['SV004', 'Phạm Gia Huy', 'D21CQCN02', 'Công nghệ thông tin', 'male', 'graduated'],
    ['SV005', 'Võ Thanh Trúc', 'D22CQKT01', 'Kế toán', 'female', 'paused'],
    ['SV006', 'Đặng Đức Minh', 'D22CQMK02', 'Marketing', 'male', 'studying']
  ];
  for (const [studentId, fullName, className, major, gender, status] of students) {
    await db.doc(`students/${studentId}`).set({ studentId, fullName, className, major, gender, status, birthDate: '2004-05-15', enrollmentDate: '2022-09-05', email: `${studentId.toLowerCase()}@example.test`, phone: '0912345678', address: 'Thành phố Hồ Chí Minh', createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
  }
  for (const [studentId, certificateId, name, issuer] of [['SV001', 'IELTS01', 'IELTS Academic', 'British Council'], ['SV001', 'MOS01', 'MOS Excel Associate', 'Certiport'], ['SV002', 'TOEIC01', 'TOEIC Listening & Reading', 'IIG Việt Nam']]) {
    await db.doc(`students/${studentId}/certificates/${certificateId}`).set({ studentId, certificateId, name, issuer, issueDate: '2025-06-20', expirationDate: '2027-06-20', description: 'Dữ liệu minh họa cho bài giữa kỳ.', createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
  }
  console.log('Đã tạo dữ liệu mẫu cục bộ: 3 tài khoản, 6 sinh viên, 3 chứng chỉ.');
  console.log('Email: admin@example.test / manager@example.test / employee@example.test');
  console.log('Mật khẩu thử nghiệm chung: DemoStudent!2026 (chỉ dùng Emulator).');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
