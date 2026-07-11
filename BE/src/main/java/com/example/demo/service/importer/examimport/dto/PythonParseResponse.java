package com.example.demo.service.importer.examimport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonParseResponse {

    private ExamMetaDto meta;
    private ParseReportDto report;
    private List<ParsedQuestionDto> questions;
    private String rawText;

    @SuppressWarnings("unchecked")
    public static ParseReportDto toReportDto(Map<String, Object> raw) {
        if (raw == null) return ParseReportDto.builder().build();

        ParseReportDto dto = new ParseReportDto();
        dto.setSelectedTemplate((String) raw.get("selectedTemplate"));
        dto.setQuestionCount(toInt(raw.get("questionCount")));
        dto.setAnswerCount(toInt(raw.get("answerCount")));
        dto.setMultipleChoiceCount(toInt(raw.get("multipleChoiceCount")));
        dto.setEssayCount(toInt(raw.get("essayCount")));
        dto.setInvalidQuestions((List<Integer>) raw.get("invalidQuestions"));
        dto.setWarnings((List<String>) raw.get("warnings"));
        dto.setErrors((List<String>) raw.get("errors"));
        dto.setParseTimeMs(toInt(raw.get("parseTimeMs")));
        dto.setConfidence(toDouble(raw.get("confidence")));
        return dto;
    }

    @SuppressWarnings("unchecked")
    public static ExamMetaDto toMetaDto(Map<String, Object> raw) {
        if (raw == null) return ExamMetaDto.builder().build();

        ExamMetaDto dto = new ExamMetaDto();
        dto.setTitle((String) raw.get("title"));
        dto.setSubject((String) raw.get("subject"));
        dto.setDuration((String) raw.get("duration"));
        dto.setGrade((String) raw.get("grade"));
        dto.setExamType((String) raw.get("examType"));
        dto.setTotalQuestions(toInt(raw.get("totalQuestions")));
        dto.setTemplate((String) raw.get("template"));
        return dto;
    }

    @SuppressWarnings("unchecked")
    public static ParsedQuestionDto toQuestionDto(Map<String, Object> raw) {
        if (raw == null) return null;

        ParsedQuestionDto dto = new ParsedQuestionDto();
        dto.setNumber(toInt(raw.get("number")));
        dto.setType((String) raw.get("type"));
        dto.setPage(toInt(raw.get("page")));
        dto.setText((String) raw.get("text"));

        
        dto.setLatexContent((String) raw.get("latexContent"));
        dto.setLatexOptions((Map<String, String>) raw.get("latexOptions"));
        dto.setContentType((String) raw.get("contentType"));

        dto.setOptions((Map<String, String>) raw.get("options"));
        dto.setSubQuestions((List<ParsedQuestionDto>) raw.get("subQuestions"));
        dto.setAnswer((String) raw.get("answer"));
        dto.setExplanation((String) raw.get("explanation"));
        dto.setConfidence(toDouble(raw.get("confidence")));
        dto.setIssues((List<String>) raw.get("issues"));

        
        dto.setFormulaHints((Map<String, Object>) raw.get("formulaHints"));

        Object renderRaw = raw.get("render");
        if (renderRaw instanceof Map) {
            Map<String, Object> renderMap = (Map<String, Object>) renderRaw;
            ParsedQuestionDto.RenderInfoDto render = new ParsedQuestionDto.RenderInfoDto();
            render.setMode((String) renderMap.get("mode"));
            render.setImagePath((String) renderMap.get("imagePath"));
            render.setBbox((List<Double>) renderMap.get("bbox"));
            dto.setRender(render);
        }

        return dto;
    }

    private static Integer toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return 0; }
    }

    private static Double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Double) return (Double) val;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0.0; }
    }
}
