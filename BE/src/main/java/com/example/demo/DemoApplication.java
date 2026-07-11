package com.example.demo;

import com.example.demo.config.DotEnvFileSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	public static void main(String[] args) {
		configureWritableTempDir();
		
		Map<String, String> dotenv = DotEnvFileSupport.readMergedDotEnv();
		if (dotenv.isEmpty()) {
			System.err.println("[dotenv] Không đọc được file .env. user.dir=" + System.getProperty("user.dir")
					+ " — đặt BE_DOTENV_FILE=đường_dẫn_tuyệt_đối\\BE\\.env hoặc chạy app từ thư mục BE.");
		}
		DotEnvFileSupport.applyToSystemPropertiesUnlessSet(dotenv);
		SpringApplication.run(DemoApplication.class, args);
	}

	private static void configureWritableTempDir() {
		String configuredTmp = System.getProperty("java.io.tmpdir");
		if (canCreateTempDirectory(configuredTmp)) {
			return;
		}

		Path fallbackTmp = Paths.get(System.getProperty("user.dir"), "tmp").toAbsolutePath().normalize();
		try {
			Files.createDirectories(fallbackTmp);
			System.setProperty("java.io.tmpdir", fallbackTmp.toString());
			System.err.println("[tmpdir] java.io.tmpdir is not writable; using " + fallbackTmp);
		} catch (IOException ex) {
			System.err.println("[tmpdir] Cannot create fallback temp dir " + fallbackTmp + ": " + ex.getMessage());
		}
	}

	private static boolean canCreateTempDirectory(String tmpDir) {
		if (tmpDir == null || tmpDir.isBlank()) {
			return false;
		}

		Path probe = null;
		try {
			probe = Files.createTempDirectory(Paths.get(tmpDir), "exam-smoke-");
			return true;
		} catch (Exception ex) {
			return false;
		} finally {
			if (probe != null) {
				try {
					Files.deleteIfExists(probe);
				} catch (IOException ignored) {
					
				}
			}
		}
	}

	@PostConstruct
	public void init() {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
	}
}
