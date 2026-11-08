# Chuẩn bị nộp bài

## Phạm vi theo đề

PDF trang 3 yêu cầu báo cáo tiếng Anh, video thuyết minh không quá 20 phút (trừ khi giảng viên đổi sang báo cáo trực tiếp), cùng đầy đủ mã nguồn, thư viện, dữ liệu và hướng dẫn để giảng viên chạy trên máy tính. PDF không quy định phải chạy trên điện thoại thật hoặc nộp APK hoạt động độc lập.

Cách trình diễn được chọn là Android Emulator + Firebase Emulator. Gói mã nguồn có Gradle Wrapper, khai báo thư viện Android, khóa phiên bản thư viện backend, script tạo dữ liệu mẫu và tài khoản Admin để tái lập môi trường. Lần thiết lập đầu cần Internet để tải các thư viện và công cụ; trong lúc trình diễn phải giữ Firebase Emulator chạy. Bản APK kèm theo là bản dành cho môi trường này. Không coi việc có APK hoặc mã nguồn là bằng chứng tất cả luồng đã được kiểm thử đạt; xem `TESTING.md`.

## Báo cáo tiếng Anh

PDF đề bài yêu cầu báo cáo tiếng Anh theo mẫu của khoa. Mẫu khoa và thông tin thành viên chưa được cung cấp; không tự điền tên trường, giảng viên hay mã số sinh viên. Tạo báo cáo theo khung sau rồi xuất PDF:

1. **Cover page**: trường, khoa, học phần 503074, tên đề tài, giảng viên, thành viên và mã số đúng thực tế.
2. **Abstract and objectives**: mục tiêu tìm hiểu Firestore, phạm vi chức năng.
3. **Firebase Firestore fundamentals**: document/collection, kiểu dữ liệu, truy vấn, index, realtime listeners, transaction, security rules; phân biệt với Realtime Database, nêu ưu/nhược điểm và chi phí đọc/ghi.
4. **Requirements and authorization**: ma trận ba vai trò, yêu cầu PDF, bổ sung từ thiết kế ứng dụng.
5. **System architecture**: sơ đồ Android → Repository → Firebase Auth / Firestore / Functions; giải thích vì sao Admin SDK chỉ chạy phía máy chủ.
6. **Database design**: bảng schema và quan hệ sinh viên/chứng chỉ; giải thích ID, timestamp, ngày lịch, ảnh JPEG nhỏ.
7. **Implementation**: đăng nhập/khóa phiên, CRUD, tìm kiếm không dấu, sorting, lifecycle listener, validation, nhập/xuất và xử lý trùng.
8. **Security and limitations**: quyền Rules/backend, không lưu mật khẩu, giới hạn đăng ký Auth bên ngoài, giới hạn nhật ký, phân trang và export không nguyên tử; không gọi hệ thống là đã sẵn sàng production khi chưa kiểm thử trên dự án thật.
9. **Testing and results**: chỉ ghi kết quả thực sự chạy được từ `TESTING.md`, ảnh chụp của chính ứng dụng, tình huống lỗi và ba vai trò.
10. **Conclusion and future work**: tìm kiếm quy mô lớn, kiểm soát đồng thời khi sửa hồ sơ, lưu ảnh riêng bằng Storage khi cần, kiểm thử thiết bị rộng hơn.
11. **References and appendices**: liên kết tài liệu Firebase/Android chính thức, cách chạy dự án, phân công thực tế, CSV mẫu, ma trận testcase.

Tự viết diễn giải dựa trên hiểu biết của nhóm, trích nguồn sát nội dung, có chú thích hình/bảng. Không chép nguyên văn bài blog. Nhúng ảnh thật từ Emulator/điện thoại, không dùng ảnh mockup như bằng chứng chạy chương trình. `docs/ARCHITECTURE.md` là tài liệu nguồn để xây báo cáo, không thay cho báo cáo tiếng Anh theo mẫu khoa.

## Kịch bản video 18–19 phút

| Thời gian | Nội dung |
|---|---|
| 0:00–1:30 | Giới thiệu nhóm, bài toán, stack và phân quyền |
| 1:30–4:00 | Firestore collection/document, realtime, transaction, Security Rules |
| 4:00–7:00 | Admin đăng nhập, thêm/sửa/khóa/mở khóa người dùng, lịch sử và ảnh cá nhân |
| 7:00–10:30 | Manager thêm/sửa/tìm/lọc/sắp xếp và mở hồ sơ sinh viên |
| 10:30–12:30 | Chứng chỉ: thêm/sửa/xóa, ngày hết hạn |
| 12:30–15:30 | Nhập CSV/Excel, tệp lỗi và trùng mã, xuất hai định dạng rồi mở tệp |
| 15:30–17:30 | Employee chỉ đọc, đổi ảnh; chứng minh backend từ chối ghi |
| 17:30–19:00 | Kết quả kiểm thử, giới hạn và hướng phát triển |

Video có giọng thuyết minh, tối thiểu 720p, tổng thời lượng không quá 20 phút. Dùng dữ liệu thử nghiệm, che mật khẩu thật, không quay service-account key. Có thể tăng tốc đoạn chờ build/cài đặt.

## Gói mã nguồn

Giữ `app/src`, Gradle Wrapper, cấu hình Gradle, `functions` (trừ `node_modules`), `package-lock.json`, Rules/indexes, tài liệu và samples. Bỏ `.gradle`, `.idea`, cache, log, APK trung gian, service-account keys, keystore, `.env`, thông tin nhạy cảm. Giữ hướng dẫn lấy `google-services.json` và cách bootstrap Admin. Nếu giảng viên cần bản chạy ngay trên Firebase thật, chuẩn bị APK được cấu hình thật và gửi tài khoản qua kênh riêng.

Ứng dụng và các hướng dẫn đã có trong dự án. Báo cáo PDF theo mẫu khoa và video có thuyết minh cần nhóm bổ sung thông tin, quay và hoàn thiện; chúng chưa được tạo tự động.
