package com.example.demo.api;

import com.example.demo.api.dto.ApiResponse;
import com.example.demo.api.dto.azota.*;
import com.example.demo.domain.entity.User;
import com.example.demo.service.AzotaService;
import com.example.demo.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/azota")
@RequiredArgsConstructor
public class AzotaController {

    private final AzotaService azotaService;
    private final CurrentUserService currentUserService;

    @PostMapping("/login")
    public ApiResponse<AzotaLoginResponse> login(@Valid @RequestBody AzotaLoginRequest request) {
        User actor = currentUserService.requireCurrentUser();
        currentUserService.requireTeacherOrAdmin(actor);

        AzotaLoginResponse response = azotaService.login(request.getPhone(), request.getPassword());
        return ApiResponse.success(response);
    }

    @GetMapping("/question-banks")
    public ApiResponse<List<AzotaQuestionBankDto>> getQuestionBanks(
            @RequestHeader(value = "X-Azota-Token", required = false) String azotaToken
    ) {
        currentUserService.requireCurrentUser();

        if (azotaToken == null || azotaToken.isBlank()) {
            return ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Thiếu X-Azota-Token header. Vui lòng đăng nhập Azota trước.");
        }

        List<AzotaQuestionBankDto> banks = azotaService.getQuestionBanks(azotaToken);
        return ApiResponse.success(banks);
    }

    @GetMapping("/question-banks/{examId}/questions")
    public ApiResponse<AzotaExamDetailResponse> getExamQuestions(
            @PathVariable String examId,
            @RequestHeader(value = "X-Azota-Token", required = false) String azotaToken
    ) {
        currentUserService.requireCurrentUser();

        if (azotaToken == null || azotaToken.isBlank()) {
            return ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Thiếu X-Azota-Token header.");
        }

        AzotaExamDetailResponse response = azotaService.getExamQuestions(azotaToken, examId);
        return ApiResponse.success(response);
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        currentUserService.requireCurrentUser();
        return ApiResponse.success(Map.of(
                "enabled", azotaService.isEnabled(),
                "service", "azota"
        ));
    }
}
