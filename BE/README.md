# Exam Service Demo

## Run
1. Ensure PostgreSQL is running on `localhost:5432` and database `datn` exists.
2. Update credentials in [application.properties](src/main/resources/application.properties) if needed.
3. Run:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Open:
   - Login: `http://localhost:8082/login`
   - Teacher UI: `http://localhost:8082/web/teacher`
   - Student UI: `http://localhost:8082/web/student`

## Seed accounts
- `admin / 123456`
- `teacher1 / 123456`
- `student1 / 123456`

## Main features
- JWT auth + role-based APIs
- Exam CRUD, question CRUD
- Student start/submit attempt with auto scoring
- XLSX import endpoint: `POST /api/exams/{examId}/questions/import-xlsx`
- Monitoring events + risk score
- Realtime suspicious alerts via WebSocket (`/topic/teacher-alerts`)

## XLSX format (sheet 1)
- Column A: content
- Column B: option A
- Column C: option B
- Column D: option C
- Column E: option D
- Column F: correctAnswer (A/B/C/D)
- Column G: scoreWeight (number)

You can start from the CSV sample in [questions-template.csv](src/main/resources/questions-template.csv) and save it as `.xlsx`.
