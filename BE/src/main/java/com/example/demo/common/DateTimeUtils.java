package com.example.demo.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Tiện ích xử lý thời gian — luôn dùng timezone Việt Nam để đảm bảo nhất quán.
 */
public final class DateTimeUtils {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private DateTimeUtils() {}

    /**
     * Chuyển LocalDateTime sang OffsetDateTime theo timezone Việt Nam.
     */
    public static OffsetDateTime toOffset(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(VIETNAM_ZONE).toOffsetDateTime();
    }

    /**
     * Chuyển LocalDateTime sang OffsetDateTime theo ZoneId cho trước.
     */
    public static OffsetDateTime toOffset(LocalDateTime ldt, ZoneId zoneId) {
        return ldt == null ? null : ldt.atZone(zoneId).toOffsetDateTime();
    }
}
