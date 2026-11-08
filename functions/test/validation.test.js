const { test } = require('node:test');
const assert = require('node:assert/strict');
const v = require('../validation');
const student = { studentId: 'SV001', fullName: 'Nguyễn An', birthDate: '2004-02-29', gender: 'female', email: 'an@example.test', phone: '0901234567', address: '', className: 'CNTT01', major: 'CNTT', enrollmentDate: '2022-09-01', status: 'studying' };
test('Chấp nhận ngày nhuận và tên tiếng Việt', () => assert.equal(v.student(student).fullName, 'Nguyễn An'));
test('Từ chối ngày không có thật', () => assert.throws(() => v.student({ ...student, birthDate: '2003-02-29' })));
test('Từ chối mã làm thay đổi đường dẫn Firestore', () => assert.throws(() => v.student({ ...student, studentId: '../users' })));
test('Loại trường lạ khi lưu sinh viên', () => assert.equal(v.student({ ...student, role: 'admin' }).role, undefined));
test('Không chấp nhận quyền không xác định', () => assert.throws(() => v.user({ fullName: 'An', age: 20, phone: '', role: 'root', status: 'normal' }, false)));
test('Từ chối chứng chỉ hết hạn trước ngày cấp', () => assert.throws(() => v.certificate({ certificateId: 'C1', name: 'MOS', issuer: 'Certiport', issueDate: '2025-01-02', expirationDate: '2025-01-01', description: '' })));
test('Không cho ngày nhập học trước ngày sinh', () => assert.throws(() => v.student({ ...student, enrollmentDate: '2000-01-01' })));
test('Mật khẩu không nằm trong tài liệu người dùng', () => {
  const result = v.user({ fullName: 'An', age: 20, phone: '', email: 'an@example.test', password: 'StrongPass!2026', role: 'employee', status: 'normal' }, true);
  assert.equal(result.password, undefined);
});
