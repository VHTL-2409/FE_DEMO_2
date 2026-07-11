# Online Exam & AI Proctoring Platform

Hệ thống thi trực tuyến có giám sát AI, gồm backend Java Spring Boot, frontend Vue và các service Python hỗ trợ phân tích camera, xác thực danh tính, đọc đề thi từ file.

## Chức năng chính

- Đăng ký, đăng nhập, xác thực email, quên mật khẩu, Google OAuth2.
- Phân quyền người dùng theo vai trò admin, giáo viên và học sinh.
- Giáo viên tạo lớp, quản lý học sinh, tạo đề thi, ngân hàng câu hỏi và lịch thi.
- Học sinh tham gia lớp, vào phòng chờ, làm bài, tự lưu câu trả lời và nộp bài.
- Giám sát thời gian thực bằng WebSocket/STOMP.
- Phát hiện dấu hiệu bất thường như mất camera, rời màn hình, nhiều khuôn mặt, nghi ngờ gian lận.
- Tích hợp AI service để phân tích khung hình, xác thực khuôn mặt và chấm điểm rủi ro.
- Import đề thi từ PDF/DOCX/XLSX/CSV thông qua parser service.

## Công nghệ sử dụng

- Backend: Java 17, Spring Boot, Spring Security, Spring Data JPA, WebSocket/STOMP.
- Database: PostgreSQL, MySQL.
- Frontend: Vue 3, Pinia, Vue Router, Tailwind CSS.
- AI service: Python, FastAPI, OpenCV, ONNX Runtime.
- Công cụ: Maven, Docker, Docker Compose, Swagger/OpenAPI.

## Cấu trúc thư mục

```text
BE/              backend spring boot
demo/            frontend vue
ai-service/      service ai xử lý camera và định danh
python_parser/   service đọc và chuẩn hóa đề thi
deploy/          cấu hình triển khai
```

## Chạy nhanh bằng Docker

```bash
docker compose up --build
```

Sau khi chạy, truy cập:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8082`
- AI service: `http://localhost:8090`

## Chạy backend riêng

```bash
cd BE
mvn spring-boot:run
```

Backend đọc cấu hình từ biến môi trường hoặc file `.env`.

## Chạy frontend riêng

```bash
cd demo
npm install
npm run dev
```

