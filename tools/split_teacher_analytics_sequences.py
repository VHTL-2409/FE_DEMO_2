from __future__ import annotations

from pathlib import Path
from textwrap import dedent


ROOT = Path(__file__).resolve().parents[1]
ANALYTICS_DIR = ROOT / "docs" / "uml-diagrams" / "plantuml_2" / "02-teacher" / "analytics"
IMAGES_DIR = ROOT / "docs" / "uml-diagrams" / "plantuml_2" / "_images" / "02-teacher" / "analytics"
OVERVIEW_PATH = ANALYTICS_DIR / "sequence-analytics.puml"


def normalize(text: str) -> str:
    return dedent(text).strip("\n") + "\n"


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(normalize(text), encoding="utf-8", newline="\n")


def build(start_id: str, title: str, participants: str, body: str) -> str:
    return normalize(
        f"""@startuml {start_id}
!theme plain

skinparam defaultFontName "DejaVu Sans"
skinparam actorBackgroundColor #F9E79F
skinparam participantBackgroundColor #FEF9E7
skinparam participantBorderColor #F39C12
skinparam actorFontColor black
skinparam participantFontColor black
skinparam arrowFontColor black
skinparam maxMessageSize 60

title {title}
autoactivate on

{participants}

{body}
@enduml
"""
    )


PARTICIPANTS_FULL = """
actor "Giáo viên" as Teacher
boundary "DASHBOARD" as DASHBOARD
participant "ANALYTICS SVC" as ANALYTICS_SVC
control "ANALYTICS CTRL" as ANALYTICS_CTRL
participant "ANALYTICS BE" as ANALYTICS_BE
participant "SUBMIT REPO" as SUBMIT_REPO
database "PostgreSQL" as DB
"""

PARTICIPANTS_FULL_WITH_Q = """
actor "Giáo viên" as Teacher
boundary "DASHBOARD" as DASHBOARD
participant "ANALYTICS SVC" as ANALYTICS_SVC
control "ANALYTICS CTRL" as ANALYTICS_CTRL
participant "ANALYTICS BE" as ANALYTICS_BE
participant "Q REPO" as Q_REPO
participant "SUBMIT REPO" as SUBMIT_REPO
database "PostgreSQL" as DB
"""

PARTICIPANTS_FULL_WITH_REPORT = """
actor "Giáo viên" as Teacher
boundary "DASHBOARD" as DASHBOARD
participant "ANALYTICS SVC" as ANALYTICS_SVC
control "ANALYTICS CTRL" as ANALYTICS_CTRL
participant "ANALYTICS BE" as ANALYTICS_BE
participant "REPORT GEN" as REPORT_GEN
"""

PARTICIPANTS_DASHBOARD_CHART = """
actor "Giáo viên" as Teacher
boundary "DASHBOARD" as DASHBOARD
participant "CHART" as CHART
"""

PARTICIPANTS_DASHBOARD_KPI = """
actor "Giáo viên" as Teacher
boundary "DASHBOARD" as DASHBOARD
participant "KPI CARDS" as KPI_CARDS
"""

PARTICIPANTS_DASHBOARD_ONLY = """
actor "Giáo viên" as Teacher
boundary "DASHBOARD" as DASHBOARD
"""


DIAGRAMS = [
    (
        "sequence-analytics-part-01a.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-01A",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 01A",
        PARTICIPANTS_FULL,
        """
== 1. Tai KPI tong quan ==

Teacher -> DASHBOARD : 1. Mo "Phan tich du lieu"
DASHBOARD -> ANALYTICS_SVC : 2. getKPISummary(teacherId)
ANALYTICS_SVC -> ANALYTICS_CTRL : 3. GET /api/analytics/kpi
ANALYTICS_CTRL -> ANALYTICS_BE : 4. getKPISummary(teacherId)

ANALYTICS_BE -> SUBMIT_REPO : 5. Aggregate KPI metrics
SUBMIT_REPO -> DB : 6. Count exams with submissions
DB --> SUBMIT_REPO : examCount
SUBMIT_REPO -> DB : 7. Count total submissions
DB --> SUBMIT_REPO : submissionCount
SUBMIT_REPO -> DB : 8. Compute pass rate and average score
DB --> SUBMIT_REPO : passRate + avgScore

SUBMIT_REPO --> ANALYTICS_BE : KPI data
ANALYTICS_BE --> ANALYTICS_CTRL : KPIDTO
ANALYTICS_CTRL --> ANALYTICS_SVC : 200 OK
ANALYTICS_SVC --> DASHBOARD : KPI data

note right of DASHBOARD
  Phan 01B hien thi KPI cards.
end note
""",
    ),
    (
        "sequence-analytics-part-01b.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-01B",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 01B",
        PARTICIPANTS_DASHBOARD_KPI,
        """
== 1. Hien thi KPI cards ==

note over DASHBOARD
  Du lieu KPI da san sang
  tu phan 01A.
end note

DASHBOARD -> KPI_CARDS : 1. Bind KPIDTO
KPI_CARDS -> KPI_CARDS : 2. Render count, pass rate, average score
KPI_CARDS --> DASHBOARD : KPI cards rendered
DASHBOARD --> Teacher : 3. Display summary cards
""",
    ),
    (
        "sequence-analytics-part-02a.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-02A",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 02A",
        PARTICIPANTS_FULL,
        """
== 2. Tai bieu do phan bo diem ==

Teacher -> DASHBOARD : 1. Select exam from filter
DASHBOARD -> DASHBOARD : 2. Apply filter
DASHBOARD -> ANALYTICS_SVC : 3. getScoreDistribution(examId)
ANALYTICS_SVC -> ANALYTICS_CTRL : 4. GET /api/analytics/score-distribution
ANALYTICS_CTRL -> ANALYTICS_BE : 5. getScoreDistribution(examId)

ANALYTICS_BE -> SUBMIT_REPO : 6. Load submitted scores
SUBMIT_REPO -> DB : 7. Query score rows for exam
DB --> SUBMIT_REPO : Score rows
SUBMIT_REPO --> ANALYTICS_BE : Score rows

ANALYTICS_BE -> ANALYTICS_BE : 8. Group by 10-point ranges
ANALYTICS_BE -> ANALYTICS_BE : 9. Compute mean and median

ANALYTICS_BE --> ANALYTICS_CTRL : ScoreDistributionDTO
ANALYTICS_CTRL --> ANALYTICS_SVC : 200 OK
ANALYTICS_SVC --> DASHBOARD : Distribution data

note right of DASHBOARD
  Phan 02B ve histogram.
end note
""",
    ),
    (
        "sequence-analytics-part-02b.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-02B",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 02B",
        PARTICIPANTS_DASHBOARD_CHART,
        """
== 2. Ve histogram ==

note over DASHBOARD
  ScoreDistributionDTO da co
  tu phan 02A.
end note

DASHBOARD -> CHART : 1. Render histogram
CHART -> CHART : 2. Draw buckets and labels
CHART --> DASHBOARD : Chart rendered
DASHBOARD --> Teacher : 3. Display score distribution chart
""",
    ),
    (
        "sequence-analytics-part-03a.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-03A",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 03A",
        PARTICIPANTS_FULL,
        """
== 3. Tai xu huong diem ==

Teacher -> DASHBOARD : 1. Click "Xu huong diem"
DASHBOARD -> ANALYTICS_SVC : 2. getScoreTrend(teacherId, filters)
ANALYTICS_SVC -> ANALYTICS_CTRL : 3. GET /api/analytics/score-trend
ANALYTICS_CTRL -> ANALYTICS_BE : 4. getScoreTrend(teacherId, filters)

ANALYTICS_BE -> SUBMIT_REPO : 5. Load historical submissions
SUBMIT_REPO -> DB : 6. Query average score by exam date
DB --> SUBMIT_REPO : Trend rows
SUBMIT_REPO --> ANALYTICS_BE : Trend rows

ANALYTICS_BE -> ANALYTICS_BE : 7. Order by exam date
ANALYTICS_BE --> ANALYTICS_CTRL : TrendDTO
ANALYTICS_CTRL --> ANALYTICS_SVC : 200 OK
ANALYTICS_SVC --> DASHBOARD : Trend data

note right of DASHBOARD
  Phan 03B ve line chart.
end note
""",
    ),
    (
        "sequence-analytics-part-03b.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-03B",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 03B",
        PARTICIPANTS_DASHBOARD_CHART,
        """
== 3. Ve line chart ==

note over DASHBOARD
  TrendDTO da co tu phan 03A.
end note

DASHBOARD -> CHART : 1. Render line chart
CHART -> CHART : 2. Draw series, axis, tooltip
CHART --> DASHBOARD : Chart rendered
DASHBOARD --> Teacher : 3. Display trend chart
""",
    ),
    (
        "sequence-analytics-part-04a.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-04A",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 04A",
        PARTICIPANTS_FULL_WITH_Q,
        """
== 4. Phan tich cau hoi kho ==

Teacher -> DASHBOARD : 1. Click "Phan tich cau hoi"
DASHBOARD -> ANALYTICS_SVC : 2. getQuestionAnalysis(examId)
ANALYTICS_SVC -> ANALYTICS_CTRL : 3. GET /api/analytics/question-analysis
ANALYTICS_CTRL -> ANALYTICS_BE : 4. getQuestionAnalysis(examId)

ANALYTICS_BE -> Q_REPO : 5. Load questions for exam
Q_REPO -> DB : 6. Query questions by exam
DB --> Q_REPO : Questions
Q_REPO --> ANALYTICS_BE : Questions

ANALYTICS_BE -> SUBMIT_REPO : 7. Load answer stats
loop For each question
  SUBMIT_REPO -> DB : 8. Count correct answers and total attempts
  DB --> SUBMIT_REPO : Correct rate
end
SUBMIT_REPO --> ANALYTICS_BE : Question stats

note right of DASHBOARD
  Phan 04B se xep hang va danh dau cau kho.
end note
""",
    ),
    (
        "sequence-analytics-part-04b.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-04B",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 04B",
        """
actor "Giáo viên" as Teacher
boundary "DASHBOARD" as DASHBOARD
participant "ANALYTICS SVC" as ANALYTICS_SVC
control "ANALYTICS CTRL" as ANALYTICS_CTRL
participant "ANALYTICS BE" as ANALYTICS_BE
""",
        """
== 4. Xep hang cau hoi ==

note over ANALYTICS_BE, DASHBOARD
  Question stats da co
  tu phan 04A.
end note

ANALYTICS_BE -> ANALYTICS_BE : 1. Flag difficult questions (correct_rate < 30%)
ANALYTICS_BE -> ANALYTICS_BE : 2. Sort questions by difficulty
ANALYTICS_BE --> ANALYTICS_CTRL : QuestionAnalysisDTO
ANALYTICS_CTRL --> ANALYTICS_SVC : 200 OK
ANALYTICS_SVC --> DASHBOARD : Analysis data
DASHBOARD --> Teacher : 3. Display sorted questions and highlights
""",
    ),
    (
        "sequence-analytics-part-05a.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-05A",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 05A",
        PARTICIPANTS_FULL,
        """
== 5. Xem danh sach HS co nguy co ==

Teacher -> DASHBOARD : 1. Click "HS co nguy co"
DASHBOARD -> ANALYTICS_SVC : 2. getAtRiskStudents(teacherId)
ANALYTICS_SVC -> ANALYTICS_CTRL : 3. GET /api/analytics/at-risk-students
ANALYTICS_CTRL -> ANALYTICS_BE : 4. getAtRiskStudents(teacherId)

ANALYTICS_BE -> SUBMIT_REPO : 5. Load recent submissions
SUBMIT_REPO -> DB : 6. Aggregate score by student
DB --> SUBMIT_REPO : Student stats
SUBMIT_REPO --> ANALYTICS_BE : Student stats

ANALYTICS_BE -> ANALYTICS_BE : 7. Detect risk by avg score and attempts
ANALYTICS_BE --> ANALYTICS_CTRL : AtRiskStudentDTO[]

note right of DASHBOARD
  Phan 05B ve bang ket qua.
end note
""",
    ),
    (
        "sequence-analytics-part-05b.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-05B",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 05B",
        PARTICIPANTS_DASHBOARD_ONLY,
        """
== 5. Hien thi bang nguy co ==

note over DASHBOARD
  AtRiskStudentDTO[] da co
  tu phan 05A.
end note

DASHBOARD -> DASHBOARD : 1. Sort by avg score
DASHBOARD -> DASHBOARD : 2. Highlight rows below threshold
DASHBOARD --> Teacher : 3. Display at-risk students table
""",
    ),
    (
        "sequence-analytics-part-06a.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-06A",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 06A",
        PARTICIPANTS_FULL_WITH_REPORT,
        """
== 6. Xuat bao cao PDF/Excel ==

Teacher -> DASHBOARD : 1. Click "Xuat bao cao"
DASHBOARD -> DASHBOARD : 2. Show export dialog
Teacher -> DASHBOARD : 3. Select format (PDF/Excel)
Teacher -> DASHBOARD : 4. Click "Tai xuong"
DASHBOARD -> ANALYTICS_SVC : 5. exportReport(format)
ANALYTICS_SVC -> ANALYTICS_CTRL : 6. GET /api/analytics/report
ANALYTICS_CTRL -> ANALYTICS_BE : 7. exportReport(format, filters)
ANALYTICS_BE -> ANALYTICS_BE : 8. Gather KPI, scores, trends, at-risk students
ANALYTICS_BE -> REPORT_GEN : 9. Generate report
REPORT_GEN -> REPORT_GEN : 10. Build PDF/Excel file
REPORT_GEN --> ANALYTICS_BE : Report file

note right of ANALYTICS_BE
  Phan 06B se stream file
  va bat download.
end note
""",
    ),
    (
        "sequence-analytics-part-06b.puml",
        "Hinh-Teacher-Analytics-Sequence-Part-06B",
        "Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Phần 06B",
        PARTICIPANTS_DASHBOARD_ONLY.replace(
            'boundary "DASHBOARD" as DASHBOARD',
            'boundary "DASHBOARD" as DASHBOARD\nparticipant "ANALYTICS SVC" as ANALYTICS_SVC\ncontrol "ANALYTICS CTRL" as ANALYTICS_CTRL\nparticipant "ANALYTICS BE" as ANALYTICS_BE',
        ),
        """
== 6. Tai file bao cao ==

note over ANALYTICS_BE, DASHBOARD
  Report file da co
  tu phan 06A.
end note

ANALYTICS_BE --> ANALYTICS_CTRL : File stream + Content-Type
ANALYTICS_CTRL --> ANALYTICS_SVC : 200 OK + binary stream
ANALYTICS_SVC --> DASHBOARD : Report file
DASHBOARD -> DASHBOARD : Create blob URL
DASHBOARD -> DASHBOARD : Trigger download via anchor click
DASHBOARD --> Teacher : Download started
""",
    ),
]


def build_overview() -> str:
    items = "\n".join(f"  - {filename}" for filename, *_ in DIAGRAMS)
    return normalize(
        f"""@startuml Hinh-Teacher-Analytics-Sequence-Overview
!theme plain

skinparam defaultFontName "DejaVu Sans"
skinparam actorBackgroundColor #F9E79F
skinparam participantBackgroundColor #FEF9E7
skinparam participantBorderColor #F39C12
skinparam actorFontColor black
skinparam participantFontColor black
skinparam arrowFontColor black

title Hình - Giảng viên - Thống kê - Biểu đồ tuần tự - Tổng quan

|#FFFFFF| Tong quan |
start
:Mo cac file part cua sequence-analytics;
legend right
{items}
endlegend
stop
@enduml
"""
    )


def cleanup() -> None:
    for path in ANALYTICS_DIR.glob("sequence-analytics-part-*.puml"):
        path.unlink()

    for path in IMAGES_DIR.glob("Hinh-Teacher-Analytics-Sequence-Part-*.png"):
        path.unlink()


def main() -> None:
    cleanup()
    for filename, start_id, title, participants, body in DIAGRAMS:
        write(ANALYTICS_DIR / filename, build(start_id, title, participants, body))

    write(OVERVIEW_PATH, build_overview())
    print(f"wrote {len(DIAGRAMS)} split diagrams")
    print(f"updated {OVERVIEW_PATH}")


if __name__ == "__main__":
    main()
