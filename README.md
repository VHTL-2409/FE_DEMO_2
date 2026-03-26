# FE_DEMO — Hướng dẫn từ đầu

Monorepo gồm **backend Spring Boot** (`BE/`), **frontend Vue + Vite** (`demo/`), **Python AI service** (`ai-service/`) và **PostgreSQL**. Bạn có thể chạy bằng **Docker** (đơn giản nhất) hoặc cài **JDK / Node / Postgres / Python** trên máy.

---

## Bước 0 — Chuẩn bị

| Công cụ | Phiên bản gợi ý | Dùng khi |
|---------|-----------------|----------|
| **Git** | bất kỳ | Clone repo |
| **Docker Desktop** (Windows/macOS) hoặc **Docker Engine** (Linux) | mới nhất | Chạy full stack bằng Compose |
| **JDK 17** + **Maven** | 17 | Chạy backend không Docker |
| **Node.js** | 20 LTS | Chạy frontend dev server |
| **PostgreSQL** | 15+ | Backend cần DB `datn` khi không dùng Docker |

---

## Bước 1 — Lấy mã nguồn

```bash
git clone <URL-repo-của-bạn>.git
cd FE_DEMO
```

---

## Bước 2 — Chạy bằng Docker (khuyến nghị)

### 2.1 Khởi động

Tại thư mục gốc `FE_DEMO`:

```bash
docker compose up -d --build
```

Lần đầu sẽ build image (vài phút).

### 2.2 Kiểm tra

| Địa chỉ | Mô tả |
|---------|--------|
| http://localhost:8080 | Giao diện web (Nginx phục vụ bản build Vue) |
| http://localhost:8082/api/health | API health (JSON) |
| http://localhost:8090/health | AI service health (OCR/proctoring) |

### 2.3 Dừng

```bash
docker compose down
```

Dữ liệu Postgres nằm trong volume `pgdata` — `down` không xóa volume; chỉ xóa DB khi `docker compose down -v` (cẩn thận).

---

## Bước 3 — Chạy local (không Docker)

Dùng khi bạn muốn sửa code với hot-reload (Vite) và debug backend.

### 3.1 PostgreSQL

Tạo database tên **`datn`**, user/password khớp file cấu hình (mặc định trong `BE` thường là user `postgres`, mật khẩu trong `BE/.env` hoặc `application.properties`).

Ví dụ tạo nhanh (Linux/macOS, đã cài `psql`):

```bash
createdb datn
# hoặc: psql -U postgres -c "CREATE DATABASE datn;"
```

### 3.2 Backend

```bash
cd BE
# Tùy chọn: copy BE/.env.example thành .env rồi chỉnh Postgres
mvn spring-boot:run
```

Backend mặc định: **http://localhost:8082**

### 3.3 Frontend

Terminal mới:

```bash
cd demo
npm ci
npm run dev
```

Frontend dev: thường **http://localhost:5173** (Vite).  
Đảm bảo backend cho phép CORS origin dev (đã cấu trong `application.properties` / `WebCorsConfig` cho `localhost:5173`).

### 3.4 AI service (OCR + proctoring)

Terminal mới:

```bash
cd ai-service
python -m venv .venv
. .venv/bin/activate  # Windows PowerShell: .venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8090
```

AI service mặc định: **http://localhost:8090**

---

## Bước 4 — Tài khoản demo (seed tự tạo khi backend chạy)

Sau khi backend kết nối DB thành công, `DataInitializer` tạo sẵn (nếu chưa tồn tại):

| Username | Password | Vai trò |
|----------|----------|---------|
| admin | 123456 | ADMIN |
| teacher1 | 123456 | TEACHER |
| student1 | 123456 | STUDENT |

Đổi mật khẩu ngay trên môi trường thật (production).

---

## Bước 5 — Deploy lên VPS (production)

Hướng dẫn chi tiết: **[DEPLOY_VPS.md](DEPLOY_VPS.md)**  
File compose production: **`docker-compose.prod.yml`** + file **`.env`** ở thư mục gốc.

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

**Reverse proxy:** [deploy/REVERSE-PROXY.txt](deploy/REVERSE-PROXY.txt) — **HTTP :80, không 443, không ghi cổng:** [deploy/nginx-host-80-http-only.conf.example](deploy/nginx-host-80-http-only.conf.example). **HTTPS không ghi cổng:** [deploy/nginx-host-80-443.conf.example](deploy/nginx-host-80-443.conf.example). **HTTPS :444:** [deploy/nginx-host-81-444.conf.example](deploy/nginx-host-81-444.conf.example). [DEPLOY_VPS.md](DEPLOY_VPS.md).

---

## Cấu trúc thư mục (rút gọn)

```
FE_DEMO/
├── BE/                 # Spring Boot API
├── demo/               # Vue 3 + Vite
├── ai-service/         # FastAPI OCR + proctoring sidecar
├── docker-compose.yml       # Dev / local Docker
├── docker-compose.prod.yml  # Production
└── DEPLOY_VPS.md            # Deploy VPS từng bước
```

---

## Tài liệu thêm

- **GitNexus:** [docs/GITNEXUS_SETUP.md](docs/GITNEXUS_SETUP.md)
- **Scaling:** [docs/SCALING_GUIDE.md](docs/SCALING_GUIDE.md)
