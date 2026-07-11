package com.example.demo.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;


public final class DateTimeUtils {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private DateTimeUtils() {}

    

    public static OffsetDateTime toOffset(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(VIETNAM_ZONE).toOffsetDateTime();
    }

    

    public static OffsetDateTime toOffset(LocalDateTime ldt, ZoneId zoneId) {
        return ldt == null ? null : ldt.atZone(zoneId).toOffsetDateTime();
    }
}
