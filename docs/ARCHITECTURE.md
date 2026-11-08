# Phân tích và kiến trúc

## Phạm vi theo Mid-term.pdf

Đề gồm quản lý tài khoản (đăng nhập, ảnh đại diện, CRUD, lịch sử), quản lý sinh viên (CRUD, tìm kiếm, sắp xếp, chi tiết), quản lý chứng chỉ theo từng sinh viên và nhập/xuất danh sách sinh viên, chứng chỉ. Ba vai trò: Admin toàn quyền; Manager quản lý sinh viên và chứng chỉ; Employee chỉ đọc và đổi ảnh cá nhân. Không có đăng ký công khai. Đầu ra học phần gồm mã nguồn, báo cáo PDF tiếng Anh và video có thuyết minh tối đa 20 phút.

Các trường sinh viên mở rộng, dashboard và giao diện tiếng Việt xuất phát từ bản mô tả kèm theo; không phải toàn bộ đều được PDF quy định. Firebase Authentication được chọn dù PDF không bắt buộc.

## Cấu trúc

```text
app/src/main/java/vn/edu/studentmanager/
  activities/      Đăng nhập, điều hướng, hồ sơ, biểu mẫu, nhập/xuất
  fragments/       Tổng quan, sinh viên, tài khoản, cá nhân
  adapters/        RecyclerView dùng chung cho các danh sách
  models/          Student, Certificate, UserProfile
  repositories/    Đọc Firestore và gọi backend; quản lý phiên
  utils/           Biểu mẫu, validation, CSV/Excel, ảnh và thông báo
app/src/main/res/   Bố cục XML, theme, màu, menu, vector
functions/         Cloud Functions, validation và bootstrap Admin
firestore.rules    Quyền đọc, chặn ghi trực tiếp từ client
```

Luồng: Activity/Fragment → Repository → Firebase SDK → Firestore / callable Cloud Functions. Mọi ghi dữ liệu được thực hiện ở backend, không lưu quyền quyết định trong UI. Giao diện dùng ViewBinding; tác vụ đọc/ghi tệp và xử lý ảnh chạy ở executor. Listener được tháo khi màn hình dừng. Bộ nhớ đệm Firestore ở RAM, không lưu hồ sơ vào cache bền vững trên máy.

## Điều hướng

Đăng nhập → Tổng quan / Sinh viên / Tài khoản (Admin) / Cá nhân.
Sinh viên → Hồ sơ → Chứng chỉ; mỗi phần có biểu mẫu và xác nhận xóa.
Tổng quan → Nhập/xuất sinh viên. Hồ sơ → Nhập/xuất chứng chỉ của sinh viên đang xem.
Tài khoản → Xem/sửa (bao gồm khóa/mở khóa), lịch sử đăng nhập phân trang 50 lượt, xóa.

## Firestore

| Đường dẫn | Dữ liệu |
|---|---|
| `users/{uid}` | `fullName`, `email`, `age` (số), `phone`, `role`, `status`, `avatarBase64`, `bootstrap`, `createdAt`, `updatedAt` |
| `students/{studentId}` | `studentId`, `fullName`, `birthDate`, `gender`, `email`, `phone`, `address`, `className`, `major`, `enrollmentDate`, `status`, `createdAt`, `updatedAt` |
| `students/{studentId}/certificates/{certificateId}` | `certificateId`, `studentId`, `name`, `issuer`, `issueDate`, `expirationDate`, `description`, `createdAt`, `updatedAt` |
| `login_history/{uid_authTime}` | `userId`, `authTime`, `loginAt`, `device` |
| `system/bootstrap` | Dấu mốc khởi tạo Admin đầu tiên; client không được đọc/ghi |

Các thời điểm tạo/cập nhật dùng server timestamp. Ngày sinh/ngày cấp là chuỗi ISO `yyyy-MM-dd` vì đây là ngày lịch không có múi giờ. Không có mật khẩu trong Firestore. UID từ Firebase Auth, mã sinh viên/chứng chỉ là định danh duy nhất không đổi. Ảnh cá nhân được cắt vuông, giảm còn 256×256 JPEG; tối đa 135 KB trước base64. Không cần Firebase Storage. Sinh viên dùng avatar chữ cái từ tên; không có chức năng tải ảnh sinh viên riêng.

## Phân quyền

| Chức năng | Admin | Manager | Employee |
|---|---|---|---|
| Đăng nhập, đọc sinh viên/chứng chỉ | Có | Có | Có |
| Thay ảnh của chính mình | Có | Có | Có |
| CRUD sinh viên/chứng chỉ | Có | Có | Không |
| Nhập/xuất | Có | Có | Không |
| Danh sách/CRUD người dùng, nhật ký | Có | Không | Không |

Firestore Rules chỉ cấp đọc cho tài khoản có hồ sơ `normal`. Tất cả ghi trực tiếp bị từ chối, kể cả Admin. Callable đọc vai trò từ hồ sơ máy chủ, không tin vai trò client truyền lên; các transaction ghi dữ liệu kiểm tra lại hồ sơ người gọi. Khóa tài khoản cập nhật hồ sơ và vô hiệu hóa Firebase Auth; listener phiên đưa người bị khóa về đăng nhập. Admin khởi tạo không thể bị xóa, khóa, hạ quyền; Admin không tự xóa/khóa/hạ quyền mình.

Không có nút đăng ký và không có callable đăng ký tự do. Firebase Email/Password có API tạo Auth user; người gọi API đó bên ngoài ứng dụng có thể tạo một danh tính Auth nhưng **không có hồ sơ được cấp quyền**, nên không đọc/ghi được dữ liệu. Nếu triển khai cần cấm cả việc tạo danh tính Auth từ bên ngoài, cấu hình Identity Platform với blocking function trước khi tạo tài khoản; không đưa tùy chọn đó vào bản học phần này.

Nhật ký ghi đăng nhập thành công được ứng dụng xác nhận với backend, một bản ghi mỗi `auth_time`; thời điểm ghi do máy chủ đặt. Không phải nhật ký mọi lần thử sai mật khẩu hay nhật ký đảm bảo ghi cả những lần đăng nhập trực tiếp ngoài ứng dụng. Thông tin thiết bị do client cung cấp, chỉ để tham khảo.

## Tìm kiếm và quy mô

Danh sách sinh viên dùng realtime listener cho toàn bộ tập dữ liệu, tìm nhiều từ khóa không dấu trên mã/tên/lớp/ngành/trạng thái, lọc trạng thái, sắp xếp theo tên tiếng Việt, thời điểm tạo, mã và lớp. Đây là lựa chọn có chủ đích cho dữ liệu học phần; không cắt mất kết quả sau trang đầu. Với hàng chục nghìn hồ sơ cần chuyển sang tìm kiếm có chỉ mục phía máy chủ và phân trang có cursor. Export đọc từng trang 200 bản ghi; lịch sử đọc từng trang 50. Tổng chứng chỉ dùng Firestore aggregate count và làm mới khi mở trang hoặc bấm làm mới.

## Nhập/xuất

- Storage Access Framework, không xin quyền bộ nhớ diện rộng.
- CSV UTF-8, dấu phẩy, hỗ trợ BOM, ngoặc kép, dấu phẩy và xuống dòng trong ô.
- Excel OOXML `.xlsx` đọc trang tính đầu tiên, hỗ trợ shared strings/inline strings, cột thưa và quan hệ workbook. Không hỗ trợ `.xls`, ô công thức hoặc ngày lưu dạng số serial; đặt cột ngày thành Text với `yyyy-MM-dd` trước khi nhập.
- Excel được viết bằng `ZipOutputStream` và đọc bằng `XmlPullParser` có sẵn trên Android; không thêm Apache POI nặng vào APK. JUnit dùng kXML để kiểm tra cùng bộ đọc.
- Tối đa 5 MB nén, 20 MB sau giải nén, 200 bản ghi mỗi lần nhập. Các cột bắt buộc theo đúng tệp mẫu; dòng trắng bỏ qua.
- Xem trước dữ liệu, phát hiện lỗi và mã trùng trong tệp; người dùng xác nhận rồi mới gọi backend. Backend kiểm tra lại, bỏ qua mã đã tồn tại và dùng transaction để chống ghi đè do tranh chấp. Nhập lại cùng tệp không tạo bản sao.
- CSV vô hiệu hóa tiền tố công thức; Excel ghi ô kiểu chuỗi. Tệp ngày/text giữ nguyên định dạng.
- Export nhiều trang không phải snapshot nguyên tử: tránh sửa dữ liệu trong lúc xuất nếu cần một bản chụp nhất quán tuyệt đối. Employee không có chức năng xuất; như mọi ứng dụng cấp quyền đọc, không thể ngăn người dùng tự sao chép dữ liệu họ được phép xem.

## Hướng giao diện

Skill `design-taste-frontend`: Accent ladder, seed `832602031`, không rút lại hướng. Thích ứng sang native Android Material 3: nền sáng, xanh ngọc trên hành động chính và vùng nhấn nhạt, chữ trung tính, viền nhẹ, 16dp bo góc. Hệ màu giữ đúng ảnh người dùng; dùng chữ neutral trên nút primary để dễ đọc. Roboto hệ thống hỗ trợ tiếng Việt theo lựa chọn được phép trong mô tả; chưa nhúng Be Vietnam Pro.

Câu hỏi của từng màn hình: Tổng quan — có bao nhiêu hồ sơ và mở việc gì tiếp? Danh sách — sinh viên cần tìm là ai? Hồ sơ — thông tin học tập và chứng chỉ là gì? Tài khoản — ai có quyền truy cập? Cá nhân — đang dùng tài khoản nào? Nhập/xuất — tệp có những dòng nào hợp lệ?

Điểm nhận diện là biểu tượng mũ tốt nghiệp và tên Sổ sinh viên, kèm dòng mã sinh viên/lớp trên hồ sơ. Số liệu lấy từ Firestore, không có tăng trưởng hay thống kê giả. Dữ liệu mẫu được ghi rõ trong script seed và chỉ chạy trên dự án `demo-student-manager`. Công cụ browser inspector của skill không áp dụng trực tiếp XML Android; kiểm tra trực quan bằng ảnh chụp Emulator thay thế.
