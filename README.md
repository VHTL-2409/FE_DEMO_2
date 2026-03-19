Docker Desktop.

docker compose up --build

-http://localhost:8080`
-http://localhost:8082`
-http://localhost:8082/actuator/health`

Run local(without Docker)
Backend
PostgreSQL chạy ở `localhost:5432`, DB `datn`

cd BE
mvn spring-boot:run


Frontend

cd demo
npm ci
npm run dev


Seed accounts
- `admin / 123456`
- `teacher1 / 123456`
- `student1 / 123456`

---

GitNexus (Code Intelligence cho AI)
- `npx gitnexus analyze` — index codebase, tạo knowledge graph
- `npx gitnexus setup` — cấu hình MCP cho Cursor/Claude Code
- Xem [docs/GITNEXUS_SETUP.md](docs/GITNEXUS_SETUP.md)

Triển khai quy mô lớn (nhiều bài thi × 1000 thí sinh)
- Xem [docs/SCALING_GUIDE.md](docs/SCALING_GUIDE.md)
- Chạy với profile `scale`: `mvn spring-boot:run -Dspring.profiles.active=scale`


