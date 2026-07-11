package com.example.demo.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;


public final class VietNamTime {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private VietNamTime() {}

    

    public static LocalDateTime now() {
        return ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();
    }

    

    public static ZoneId zone() {
        return VIETNAM_ZONE;
    }
}
