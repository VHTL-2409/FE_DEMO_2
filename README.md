Docker Desktop.

docker compose up --build

-http://localhost:8080`
-http://localhost:8082`
-http://localhost:8082/actuator/health`

Run local(without Docker)
Backend
PostgreSQL chạy ở `localhost:5432`, DB `datn`

cd BE
./mvnw spring-boot:run


Frontend

cd demo
npm ci
npm run dev


Seed accounts
- `admin / 123456`
- `teacher1 / 123456`
- `student1 / 123456`


