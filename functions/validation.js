const STUDENT_FIELDS = ['studentId', 'fullName', 'birthDate', 'gender', 'email', 'phone', 'address', 'className', 'major', 'enrollmentDate', 'status'];
const CERTIFICATE_FIELDS = ['certificateId', 'name', 'issuer', 'issueDate', 'expirationDate', 'description'];

function text(value, label, max = 160, required = true) {
  if (typeof value !== 'string' || (required && !value.trim()) || value.length > max) {
    throw new Error(`${label} không hợp lệ (tối đa ${max} ký tự).`);
  }
  return value.trim();
}
function id(value) {
  if (typeof value !== 'string' || !/^[A-Za-z0-9_-]{1,40}$/.test(value)) throw new Error('Mã chỉ gồm chữ, số, dấu gạch ngang hoặc gạch dưới (1–40 ký tự).');
  return value;
}
function date(value, label, required = true) {
  const s = text(value, label, 10, required);
  if (!s && !required) return '';
  const d = new Date(`${s}T00:00:00Z`);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s) || !Number.isFinite(d.getTime()) || d.toISOString().slice(0, 10) !== s || s < '1900-01-01' || s > '2200-12-31') {
    throw new Error(`${label} cần là ngày có thật, dạng yyyy-MM-dd.`);
  }
  return s;
}
function email(value) {
  const s = text(value, 'Email', 160);
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(s)) throw new Error('Email không hợp lệ.');
  return s;
}
function phone(value) {
  const s = text(value, 'Số điện thoại', 20, false);
  if (s && !/^\+?[0-9 ()-]{7,20}$/.test(s)) throw new Error('Số điện thoại không hợp lệ.');
  return s;
}
function choice(value, values, label) {
  if (!values.includes(value)) throw new Error(`${label} không hợp lệ.`);
  return value;
}
function student(data) {
  const result = {
    studentId: id(data.studentId), fullName: text(data.fullName, 'Họ tên', 120),
    birthDate: date(data.birthDate, 'Ngày sinh'), gender: choice(data.gender, ['male', 'female', 'other'], 'Giới tính'),
    email: email(data.email), phone: phone(data.phone), address: text(data.address, 'Địa chỉ', 300, false),
    className: text(data.className, 'Lớp', 60), major: text(data.major, 'Ngành học', 120),
    enrollmentDate: date(data.enrollmentDate, 'Ngày nhập học'), status: choice(data.status, ['studying', 'graduated', 'paused'], 'Trạng thái')
  };
  if (result.birthDate > new Date().toISOString().slice(0, 10)) throw new Error('Ngày sinh không được ở tương lai.');
  if (result.enrollmentDate < result.birthDate) throw new Error('Ngày nhập học phải sau ngày sinh.');
  return result;
}
function certificate(data) {
  const result = { certificateId: id(data.certificateId), name: text(data.name, 'Tên chứng chỉ', 160),
    issuer: text(data.issuer, 'Đơn vị cấp', 160), issueDate: date(data.issueDate, 'Ngày cấp'),
    expirationDate: date(data.expirationDate, 'Ngày hết hạn', false), description: text(data.description, 'Mô tả', 1000, false) };
  if (result.expirationDate && result.expirationDate < result.issueDate) throw new Error('Ngày hết hạn phải từ ngày cấp trở đi.');
  return result;
}
function user(data, creating) {
  const age = Number(data.age);
  if (!Number.isInteger(age) || age < 16 || age > 100) throw new Error('Tuổi phải từ 16 đến 100.');
  const result = { fullName: text(data.fullName, 'Họ tên', 120), age, phone: phone(data.phone),
    role: choice(data.role, ['admin', 'manager', 'employee'], 'Vai trò'), status: choice(data.status, ['normal', 'locked'], 'Trạng thái') };
  if (creating) {
    result.email = email(data.email);
    if (typeof data.password !== 'string' || data.password.length < 12 || data.password.length > 128) throw new Error('Mật khẩu cần từ 12 đến 128 ký tự.');
  }
  return result;
}
module.exports = { STUDENT_FIELDS, CERTIFICATE_FIELDS, text, id, date, student, certificate, user };
