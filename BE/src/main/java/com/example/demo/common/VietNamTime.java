package com.example.demo.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Lấy thời gian hiện tại theo timezone Việt Nam (Asia/Ho_Chi_Minh).
 * Luôn dùng timezone cố định để đảm bảo nhất quán giữa server và database.
 */
public final class VietNamTime {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private VietNamTime() {}

    /**
     * Trả về LocalDateTime hiện tại theo giờ Việt Nam.
     */
    public static LocalDateTime now() {
        return ZonedDateTime.now(VIETNAM_ZONE).toLocalDateTime();
    }

    /**
     * Trả về ZoneId cố định của Việt Nam.
     */
    public static ZoneId zone() {
        return VIETNAM_ZONE;
    }
}
