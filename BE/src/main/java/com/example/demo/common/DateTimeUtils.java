package com.example.demo.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Chuyển đổi thời gian theo timezone.
 * Dùng timezone của giáo viên tạo đề thi (exam.timezone), fallback Asia/Ho_Chi_Minh.
 */
public final class DateTimeUtils {

    public static final ZoneId ZONE_HCM = ZoneId.of("Asia/Ho_Chi_Minh");

    private DateTimeUtils() {}

    public static OffsetDateTime toOffsetHcm(LocalDateTime ldt) {
        return toOffset(ldt, ZONE_HCM);
    }

    /** Chuyển LocalDateTime sang OffsetDateTime theo timezone (ví dụ "Asia/Ho_Chi_Minh"). */
    public static OffsetDateTime toOffset(LocalDateTime ldt, String timezone) {
        if (ldt == null) return null;
        ZoneId zone = toZoneId(timezone);
        return ldt.atZone(zone).toOffsetDateTime();
    }

    public static OffsetDateTime toOffset(LocalDateTime ldt, ZoneId zoneId) {
        return ldt == null ? null : ldt.atZone(zoneId).toOffsetDateTime();
    }

    public static ZoneId toZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) return ZONE_HCM;
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZONE_HCM;
        }
    }
}
