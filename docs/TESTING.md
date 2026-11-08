# Checklist triển khai và kiểm thử

Tệp này được cập nhật theo kết quả chạy thực tế, không coi mã nguồn có mặt là đã kiểm chứng đầy đủ.

## Các nhóm chức năng đã có mã

- [x] Đăng nhập, khôi phục phiên, đăng xuất; không có màn hình đăng ký.
- [x] Ảnh cá nhân; CRUD tài khoản; khóa/mở khóa; lịch sử đăng nhập phân trang.
- [x] Admin bootstrap an toàn; bảo vệ Admin gốc; kiểm tra quyền Rules/backend.
- [x] Sinh viên: danh sách realtime, CRUD, hồ sơ, tìm nhiều từ khóa không dấu, lọc và 6 kiểu sắp xếp.
- [x] Chứng chỉ riêng từng sinh viên: danh sách, CRUD, kiểm tra ngày.
- [x] Nhập sinh viên/chứng chỉ bằng CSV và XLSX; preview, báo lỗi, xử lý mã trùng.
- [x] Xuất sinh viên/chứng chỉ bằng CSV và XLSX qua Storage Access Framework.
- [x] Dashboard, điều hướng, loading, empty, lỗi và xác nhận xóa.
- [x] Hướng dẫn Firebase thật, Emulator, bootstrap, báo cáo và video.

## Kết quả chạy

Ngày kiểm tra: 01/11/2026.

Cập nhật lỗi đăng nhập Emulator: giá trị API key giả ban đầu không đạt kiểm tra định dạng của Firebase Android SDK, gây thông báo `Please set a valid API key`. Đã đổi giá trị chỉ dành cho Emulator sang định dạng SDK chấp nhận; cấu hình production vẫn phải dùng `google-services.json` thật. Cần thử lại luồng đăng nhập trên thiết bị; kiểm thử backend qua HTTP không thay thế được kiểm thử SDK Android.

| Kiểm tra | Kết quả đã xác nhận |
|---|---|
| Build `assembleEmulatorDebug` | Đạt, tạo APK |
| Java JUnit cho CSV/XLSX và validation | 10/10 đạt |
| Node validation backend | 8/8 đạt |
| Firebase Auth/Firestore/Functions Emulator qua HTTP callable | 12/12 đạt; gồm kiểm tra chặn token cũ ngay khi khóa và nhập/xuất chứng chỉ |
| Android Lint | 0 lỗi, 65 cảnh báo; phần lớn về chuỗi giao diện/khả năng dịch và thư viện mới hơn |
| Chạy giao diện Android | Chưa xác minh đạt: máy ảo gặp ANR/DeadSystemException của System UI/launcher và treo khi đọc/chụp giao diện, kể cả sau khi đổi sang đồ họa phần mềm. Không dùng ảnh lỗi làm bằng chứng kiểm thử |

Lượt tích hợp đã xác minh: Rules chặn người vô danh/tài khoản khóa, Employee không ghi hoặc tự nâng quyền, Admin/Manager đúng phạm vi, CRUD tài khoản, khóa/mở khóa, CRUD sinh viên/chứng chỉ, xóa chứng chỉ cùng sinh viên, nhập hỗn hợp lỗi/trùng, lịch sử không nhân đôi và ảnh chỉ cập nhật cho chính người gọi.

Chưa kiểm thử trên Firebase production hoặc điện thoại thật. Chưa kiểm tra đầy đủ TalkBack, tablet, file picker từ mọi nhà cung cấp và mọi tình huống mất mạng. Các bước đó vẫn nằm trong checklist bên dưới.

## Kiểm tra thủ công trước khi nộp

1. Khởi động Firebase Emulator, đợi `All emulators ready`, chạy seed và xác nhận tạo mẫu thành công; cài APK `emulatorDebug` trên Android Emulator. Giữ backend chạy trong lúc trình diễn. Triển khai Firebase thật và thử trên điện thoại thật là phần mở rộng tùy chọn, không phải yêu cầu thiết bị trong PDF.
2. Đăng nhập cả ba vai trò. Employee không thấy quản trị/ghi/xuất; Manager không xem tài khoản. Thử gọi API không đủ quyền để chứng minh backend chặn.
3. Đăng nhập tài khoản ở máy thứ hai rồi khóa từ Admin; kiểm tra phiên bị chặn. Thử sau khi mở khóa. Tự khóa/xóa và hạ quyền Admin gốc phải bị từ chối.
4. Tạo sinh viên có đủ dữ liệu, sửa, thử mã trùng, tên tiếng Việt, tìm không dấu, kết hợp tên+lớp, các bộ lọc/sắp xếp. Xóa hồ sơ phải xóa cả chứng chỉ.
5. Nhập các tệp mẫu, nhập lại để kiểm tra idempotency. Thử dòng sai email, ngày, cột thiếu, mã trùng; chỉ dòng hợp lệ được nhập.
6. Xuất CSV và XLSX cho sinh viên/chứng chỉ, mở bằng Excel hoặc LibreOffice; thử nhập lại và đối chiếu tiếng Việt, dấu phẩy, xuống dòng, số điện thoại có số 0 đầu.
7. Kiểm tra file picker khi hủy, tệp chỉ đọc, tệp quá lớn, `.xls`, công thức, XML/ZIP không hợp lệ. Kiểm tra mất kết nối giữa lúc lưu và thao tác thử lại.
8. Đổi ảnh từ JPG/PNG và xoay màn hình. Thử cỡ chữ lớn, màn hình nhỏ, tablet, TalkBack và bàn phím không che nút.
9. Sau thay đổi quyền của người dùng đang mở app, kiểm tra UI và các màn hình con. Khi xóa tài khoản, không truy cập lại được.
10. Ghi lại kết quả thật, thiết bị/API và ảnh chụp, không đánh dấu đã kiểm thử những bước chưa chạy.
