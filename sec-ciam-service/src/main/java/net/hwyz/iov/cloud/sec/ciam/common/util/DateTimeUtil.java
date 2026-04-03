package net.hwyz.iov.cloud.sec.ciam.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类。
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private DateTimeUtil() {
    }

    /**
     * 获取当前时间（默认时区）
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /**
     * 格式化为 yyyy-MM-dd HH:mm:ss
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DEFAULT_FORMATTER);
    }

    /**
     * 解析 yyyy-MM-dd HH:mm:ss 格式字符串
     */
    public static LocalDateTime parse(String text) {
        return text == null ? null : LocalDateTime.parse(text, DEFAULT_FORMATTER);
    }

    /**
     * 毫秒时间戳转 LocalDateTime
     */
    public static LocalDateTime fromEpochMilli(long epochMilli) {
        return Instant.ofEpochMilli(epochMilli).atZone(DEFAULT_ZONE).toLocalDateTime();
    }

    /**
     * LocalDateTime 转毫秒时间戳
     */
    public static long toEpochMilli(LocalDateTime dateTime) {
        return dateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }
}
