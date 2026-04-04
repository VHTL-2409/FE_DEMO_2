"""
validator.py — Validate parsed exam results and generate reports.
"""

from __future__ import annotations

from typing import Optional

from ..schemas import ParseReport, ParsedQuestion, QuestionType, TemplateType


class ExamValidator:
    """Validate a parsed exam and produce a ParseReport."""

    def __init__(self, questions: list[ParsedQuestion], selected_template: TemplateType):
        self.questions = questions
        self.selected_template = selected_template
        self._report: Optional[ParseReport] = None

    def validate(self) -> ParseReport:
        """Run all validations and return a report."""
        if self._report is not None:
            return self._report

        warnings: list[str] = []
        errors: list[str] = []
        invalid_questions: list[int] = []

        # Count by type
        mcq_count = sum(1 for q in self.questions if q.type == QuestionType.MULTIPLE_CHOICE)
        essay_count = sum(1 for q in self.questions if q.type == QuestionType.ESSAY)

        # 1. Check question numbers are continuous
        numbers = sorted(q.number for q in self.questions if q.number > 0)
        if numbers:
            expected = list(range(numbers[0], numbers[-1] + 1))
            missing = set(expected) - set(numbers)
            if missing:
                warnings.append(
                    f"Missing question numbers: {sorted(missing)}. "
                    f"Expected {numbers[0]}-{numbers[-1]}"
                )
                for q in self.questions:
                    if q.number in missing:
                        invalid_questions.append(q.number)

        # 2. Check answer coverage for MCQ
        answered_count = sum(
            1 for q in self.questions
            if q.type == QuestionType.MULTIPLE_CHOICE and q.answer is not None
        )
        if mcq_count > 0 and answered_count < mcq_count * 0.5:
            missing = mcq_count - answered_count
            warnings.append(
                f"Only {answered_count}/{mcq_count} MCQ questions have answers. "
                f"Missing: {missing}."
            )

        # 3. Check each question for issues
        for q in self.questions:
            q_issues = self._validate_question(q)
            for issue in q_issues:
                warnings.append(f"Question {q.number}: {issue}")
                if q.number not in invalid_questions:
                    invalid_questions.append(q.number)

        # 4. Check for duplicate question numbers
        seen: dict[int, int] = {}
        for q in self.questions:
            if q.number > 0:
                if q.number in seen:
                    warnings.append(
                        f"Duplicate question number {q.number} "
                        f"(at indices {seen[q.number]} and "
                        f"{self.questions.index(q)})"
                    )
                seen[q.number] = self.questions.index(q)

        # 5. Calculate overall confidence
        avg_confidence = (
            sum(q.confidence for q in self.questions) / max(len(self.questions), 1)
        )
        # Gentle bonus for answer coverage (not a harsh penalty).
        # Inline-answer formats (DOCX, clean MCQ) have answers embedded in text,
        # so the ratio can be low while still being structurally correct.
        if mcq_count > 0:
            answer_ratio = answered_count / mcq_count
            # Use soft adjustment: full bonus when ratio >= 0.5, linear below
            coverage_bonus = 0.2 * answer_ratio
            avg_confidence = min(avg_confidence + coverage_bonus, 1.0)

        self._report = ParseReport(
            selectedTemplate=self.selected_template,
            questionCount=len(self.questions),
            answerCount=answered_count,
            multipleChoiceCount=mcq_count,
            essayCount=essay_count,
            invalidQuestions=sorted(invalid_questions),
            warnings=warnings,
            errors=errors,
            confidence=round(avg_confidence, 3),
        )
        return self._report

    def _validate_question(self, q: ParsedQuestion) -> list[str]:
        """Check a single question for issues."""
        issues = []

        if q.type == QuestionType.MULTIPLE_CHOICE:
            # Check for enough options
            valid_options = {k: v for k, v in q.options.items() if v.strip()}
            if len(valid_options) < 2:
                issues.append(
                    f"Multiple choice with only {len(valid_options)} valid options. "
                    "Consider converting to essay."
                )

            # Check for missing answer
            if q.answer is None:
                issues.append("Multiple choice without an extracted answer.")

            # Check confidence
            if q.confidence < 0.5:
                issues.append(f"Low confidence score: {q.confidence}.")

            # Check for very short content
            if len(q.text.strip()) < 10:
                issues.append("Question text is suspiciously short.")

        elif q.type == QuestionType.ESSAY:
            if len(q.text.strip()) < 10:
                issues.append("Essay question text is too short.")

        return issues


def validate_parsed(
    questions: list[ParsedQuestion],
    selected_template: TemplateType,
) -> ParseReport:
    """Convenience function."""
    validator = ExamValidator(questions, selected_template)
    return validator.validate()
