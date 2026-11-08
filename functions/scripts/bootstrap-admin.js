const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { user } = require('../validation');

async function main() {
  const projectId = process.env.GCLOUD_PROJECT;
  if (!projectId || projectId.startsWith('demo-')) throw new Error('Đặt GCLOUD_PROJECT bằng mã dự án Firebase thật. Dùng seed-emulator.js cho môi trường thử nghiệm.');
  if (process.env.FIRESTORE_EMULATOR_HOST || process.env.FIREBASE_AUTH_EMULATOR_HOST) throw new Error('Bỏ biến môi trường Emulator trước khi khởi tạo dự án thật.');
  const password = process.env.ADMIN_PASSWORD;
  const profile = user({ fullName: process.env.ADMIN_NAME || 'Quản trị viên', email: process.env.ADMIN_EMAIL,
    password, phone: process.env.ADMIN_PHONE || '', age: process.env.ADMIN_AGE || 25, role: 'admin', status: 'normal' }, true);
  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore(), auth = getAuth(), marker = db.doc('system/bootstrap');
  await marker.create({ state: 'initializing', createdAt: FieldValue.serverTimestamp() });
  let account;
  try {
    account = await auth.createUser({ email: profile.email, password, displayName: profile.fullName, disabled: true });
    await db.doc(`users/${account.uid}`).create({ ...profile, bootstrap: true, avatarBase64: '', createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp() });
    await auth.updateUser(account.uid, { disabled: false });
    await marker.update({ state: 'ready', uid: account.uid });
    console.log('Đã tạo Admin đầu tiên. UID:', account.uid);
    console.log('Gửi email và mật khẩu cho giảng viên qua kênh riêng; không đưa mật khẩu vào mã nguồn.');
  } catch (error) {
    if (account) {
      await auth.deleteUser(account.uid).catch(() => {});
      await db.doc(`users/${account.uid}`).delete().catch(() => {});
    }
    await marker.delete(); throw error;
  }
}
main().catch(error => { console.error('Khởi tạo thất bại:', error.message); process.exitCode = 1; });
