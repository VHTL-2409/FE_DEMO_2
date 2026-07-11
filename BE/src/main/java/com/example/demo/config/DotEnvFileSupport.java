package com.example.demo.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class DotEnvFileSupport {

    private DotEnvFileSupport() {
    }

    public static Map<String, String> readMergedDotEnv() {
        Path userDir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        String explicit = System.getenv("BE_DOTENV_FILE");
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv("DOTENV_PATH");
        }
        if (explicit != null && !explicit.isBlank()) {
            candidates.add(Path.of(explicit.trim()).toAbsolutePath().normalize());
        }
        candidates.add(userDir.resolve("BE").resolve(".env"));
        candidates.add(userDir.resolve(".env"));
        Path parent = userDir.getParent();
        if (parent != null) {
            candidates.add(parent.resolve("BE").resolve(".env"));
        }

        Set<Path> seen = new LinkedHashSet<>();
        Map<String, String> merged = new LinkedHashMap<>();
        for (Path p : candidates) {
            Path abs = p.toAbsolutePath().normalize();
            if (!seen.add(abs) || !Files.isRegularFile(abs)) {
                continue;
            }
            mergeFile(abs, merged);
        }
        return merged;
    }

    

    public static void applyToSystemPropertiesUnlessSet(Map<String, String> merged) {
        if (merged.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> e : merged.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (k == null || k.isBlank() || v == null || v.isBlank()) {
                continue;
            }
            String envVal = System.getenv(k);
            if (envVal != null && !envVal.isBlank()) {
                continue;
            }
            String propVal = System.getProperty(k);
            if (propVal != null && !propVal.isBlank()) {
                continue;
            }
            System.setProperty(k, v);
        }
    }

    static void mergeFile(Path file, Map<String, String> target) {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (!t.isEmpty() && t.charAt(0) == '\uFEFF') {
                    t = t.substring(1).trim();
                }
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq < 1) {
                    continue;
                }
                String key = t.substring(0, eq).trim().replace("\r", "").trim();
                String val = t.substring(eq + 1).trim().replace("\r", "").trim();
                if (key.isEmpty()) {
                    continue;
                }
                if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                    val = val.substring(1, val.length() - 1);
                }
                target.put(key, val);
            }
        } catch (IOException ignored) {
            
        }
    }
}
