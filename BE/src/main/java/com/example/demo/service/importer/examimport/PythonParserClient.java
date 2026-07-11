package com.example.demo.service.importer.examimport;

import com.example.demo.common.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@Component
@Slf4j
public class PythonParserClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceBaseUrl;

    public PythonParserClient(
            ObjectMapper objectMapper,
            @Value("${exam-import.python-parser-legacy-url:http://python-parser:8000}") String serviceBaseUrl,
            @Value("${exam-import.python-parser-connect-timeout-ms:120000}") int connectTimeoutMs,
            @Value("${exam-import.python-parser-read-timeout-ms:120000}") int readTimeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.serviceBaseUrl = serviceBaseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restTemplate = new RestTemplate(factory);

        log.info("[PythonParserClient] Init with baseUrl={}, connectTimeout={}ms, readTimeout={}ms",
                serviceBaseUrl, connectTimeoutMs, readTimeoutMs);
    }

    

    public Map<String, Object> parseExam(MultipartFile file, String sessionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            byte[] bytes = file.getBytes();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "exam.pdf";

            body.add("file", new MultipartInputStreamFileResource(bytes, filename));

            if (sessionId != null && !sessionId.isBlank()) {
                body.add("sessionId", sessionId);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    serviceBaseUrl + "/parse-exam",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
                throw new ApiException(HttpStatus.valueOf(response.getStatusCode().value()),
                        "Python parser error: " + response.getBody());
            }

            return objectMapper.readValue(response.getBody(),
                    new TypeReference<Map<String, Object>>() {});

        } catch (ApiException ae) {
            throw ae;
        } catch (Exception ex) {
            log.warn("[PythonParserClient] Failed to call Python service at {}: {}",
                    serviceBaseUrl, ex.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Python parser service unavailable: " + ex.getMessage());
        }
    }

    

    public Map<String, Object> profilePdf(String filePath) {
        try {
            String url = serviceBaseUrl + "/parse-exam/profile?filePath=" + filePath;
            String raw = restTemplate.getForObject(url, String.class);
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            log.warn("[PythonParserClient] profilePdf failed: {}", ex.getMessage());
            return Map.of("error", ex.getMessage());
        }
    }

    

    public boolean isHealthy() {
        try {
            String raw = restTemplate.getForObject(serviceBaseUrl + "/health", String.class);
            return raw != null && raw.contains("ok");
        } catch (Exception ex) {
            log.debug("[PythonParserClient] health check failed: {}", ex.getMessage());
            return false;
        }
    }

    

    public void deleteSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            restTemplate.exchange(
                    serviceBaseUrl + "/parse-exam/session/" + sessionId,
                    HttpMethod.DELETE,
                    HttpEntity.EMPTY,
                    String.class
            );
        } catch (Exception ex) {
            log.debug("[PythonParserClient] delete session {} failed: {}", sessionId, ex.getMessage());
        }
    }

    

    @SuppressWarnings("unchecked")
    public Map<String, Object> fallbackResponse(String sessionId) {
        return Map.of(
                "available", false,
                "sessionId", sessionId,
                "message", "Python parser service unavailable. Use Java fallback parser."
        );
    }

    

    private static class MultipartInputStreamFileResource implements org.springframework.core.io.Resource {
        private final byte[] bytes;
        private final String filename;

        public MultipartInputStreamFileResource(byte[] bytes, String filename) {
            this.bytes = bytes;
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(bytes);
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public long contentLength() {
            return bytes.length;
        }

        @Override
        public org.springframework.core.io.Resource createRelative(String relativePath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDescription() {
            return "MultipartInputStreamFileResource[" + filename + "]";
        }

        @Override
        public java.net.URI getURI() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public java.io.File getFile() throws java.io.IOException {
            throw new java.io.IOException("In-memory resource, not a file");
        }

        @Override
        public java.net.URL getURL() throws java.io.IOException {
            throw new java.io.IOException("In-memory resource has no URL");
        }

        @Override
        public long lastModified() {
            return 0;
        }
    }
}
