package com.example.demo;

import com.example.demo.config.DotEnvFileSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import jakarta.annotation.PostConstruct;

import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	public static void main(String[] args) {
		// Trước Spring: đưa BE/.env vào System properties — luôn có GOOGLE_* dù EPP không chạy
		Map<String, String> dotenv = DotEnvFileSupport.readMergedDotEnv();
		if (dotenv.isEmpty()) {
			System.err.println("[dotenv] Không đọc được file .env. user.dir=" + System.getProperty("user.dir")
					+ " — đặt BE_DOTENV_FILE=đường_dẫn_tuyệt_đối\\BE\\.env hoặc chạy app từ thư mục BE.");
		}
		DotEnvFileSupport.applyToSystemPropertiesUnlessSet(dotenv);
		SpringApplication.run(DemoApplication.class, args);
	}

	@PostConstruct
	public void init() {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
	}
}
