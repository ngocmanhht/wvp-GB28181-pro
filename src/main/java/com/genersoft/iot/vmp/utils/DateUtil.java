package com.genersoft.iot.vmp.utils;


import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.ObjectUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * Global time tool class
 * @author lin
 */
public class DateUtil {

    /**
     * Compatible with non-standard iso8601 time format
     */
	private static final String ISO8601_COMPATIBLE_PATTERN = "yyyy-M-d'T'H:m:s";

    /**
     * Used to output the standard iso8601 time format
     */
	private static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * iso8601Time format with time zone, for example：2024-02-21T11:10:36+08:00
     */
    private static final String ISO8601_ZONE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

    /**
     * Compatible time format iso8601 time format with milliseconds
     */
    private static final String ISO8601_MILLISECOND_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * wvpInternal unified time format
     */
    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * wvpInternal unified time format
     */
    public static final String URL_PATTERN = "yyyyMMddHHmmss";
    public static final String PATTERN1078 = "yyMMddHHmmss";
    public static final String PATTERN1078Date = "yyyyMMdd";

    /**
     * date format
     */
    public static final String date_PATTERN = "yyyy-MM-dd";

    public static final String zoneStr = "Asia/Shanghai";

    public static final DateTimeFormatter formatterCompatibleISO8601 = DateTimeFormatter.ofPattern(ISO8601_COMPATIBLE_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter formatterISO8601 = DateTimeFormatter.ofPattern(ISO8601_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter formatterZoneISO8601 = DateTimeFormatter.ofPattern(ISO8601_ZONE_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter formatterMillisecondISO8601 = DateTimeFormatter.ofPattern(ISO8601_MILLISECOND_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter DateFormatter = DateTimeFormatter.ofPattern(date_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter urlFormatter = DateTimeFormatter.ofPattern(URL_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter formatter1078 = DateTimeFormatter.ofPattern(PATTERN1078, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter formatter1078date = DateTimeFormatter.ofPattern(PATTERN1078Date, Locale.getDefault()).withZone(ZoneId.of(zoneStr));

    public static String yyyy_MM_dd_HH_mm_ssToISO8601(@NotNull String formatTime) {
        return formatterISO8601.format(formatter.parse(formatTime));
    }

	public static String yyyy_MM_dd_HH_mm_ssToUrl(@NotNull String formatTime) {
        return urlFormatter.format(formatter.parse(formatTime));
    }

	public static String ISO8601Toyyyy_MM_dd_HH_mm_ss(String formatTime) {
        // Tried all three date formats in order to be compatible with date formats from different manufacturers.
        if (verification(formatTime, formatterCompatibleISO8601)) {
            return formatter.format(formatterCompatibleISO8601.parse(formatTime));
        } else if (verification(formatTime, formatterZoneISO8601)) {
            return formatter.format(formatterZoneISO8601.parse(formatTime));
        } else if (verification(formatTime, formatterMillisecondISO8601)) {
            return formatter.format(formatterMillisecondISO8601.parse(formatTime));
        }
        return formatter.format(formatterISO8601.parse(formatTime));
    }

	public static String urlToyyyy_MM_dd_HH_mm_ss(String formatTime) {
        return formatter.format(urlFormatter.parse(formatTime));
    }
    public static String yyyy_MM_dd_HH_mm_ssTo1078(String formatTime) {
        return formatter1078.format(formatter.parse(formatTime));
    }
    public static String jt1078Toyyyy_MM_dd_HH_mm_ss(String formatTime) {
        return formatter.format(formatter1078.parse(formatTime));
    }
    public static String jt1078dateToyyyy_MM_dd(String formatTime) {
        return DateFormatter.format(formatter1078date.parse(formatTime));
    }

    /**
     * yyyy_MM_dd_HH_mm_ss transfer timestamp
     * @param formatTime
     * @return
     */
	public static long yyyy_MM_dd_HH_mm_ssToTimestamp(String formatTime) {
        TemporalAccessor temporalAccessor = formatter.parse(formatTime);
        Instant instant = Instant.from(temporalAccessor);
        return instant.getEpochSecond();
	}

    /**
     * timestamp turn yyyy_MM_dd_HH_mm_ss
     */
	public static String timestampTo_yyyy_MM_dd_HH_mm_ss(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        return formatter.format(LocalDateTime.ofInstant(instant, ZoneId.of(zoneStr)));
	}

    /**
     * timestamp turn yyyy_MM_dd_HH_mm_ss
     */
	public static String timestampMsToUrlToyyyy_MM_dd_HH_mm_ss(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return urlFormatter.format(LocalDateTime.ofInstant(instant, ZoneId.of(zoneStr)));
	}

    /**
     * yyyy_MM_dd_HH_mm_ss Turn timestamp (milliseconds)）
     *
     * @param formatTime
     * @return
     */
    public static long yyyy_MM_dd_HH_mm_ssToTimestampMs(String formatTime) {
        TemporalAccessor temporalAccessor = formatter.parse(formatTime);
        Instant instant = Instant.from(temporalAccessor);
        return instant.toEpochMilli();
    }

    /**
     * timestamp (milliseconds) turn yyyy_MM_dd_HH_mm_ss
     */
    public static String timestampMsTo_yyyy_MM_dd_HH_mm_ss(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return formatter.format(LocalDateTime.ofInstant(instant, ZoneId.of(zoneStr)));
    }

    /**
     * yyyy_MM_dd_HH_mm_ss Turn timestamp (milliseconds)）
     *
     * @param formatTime
     * @return
     */
    public static long urlToTimestampMs(String formatTime) {
        TemporalAccessor temporalAccessor = urlFormatter.parse(formatTime);
        Instant instant = Instant.from(temporalAccessor);
        return instant.toEpochMilli();
    }

    /**
     * timestamp turn yyyy_MM_dd
     */
    public static String timestampTo_yyyy_MM_dd(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return DateFormatter.format(LocalDateTime.ofInstant(instant, ZoneId.of(zoneStr)));
    }

    /**
     * Get current time
     * @return
     */
    public static String getNow() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        return formatter.format(nowDateTime);
    }

    /**
     * Get current time
     * @return
     */
    public static String getNowForUrl() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        return urlFormatter.format(nowDateTime);
    }


    /**
     * Format check
     * @param timeStr time string
     * @param dateTimeFormatter Format to be verified
     * @return
     */
    public static boolean verification(String timeStr, DateTimeFormatter dateTimeFormatter) {
        try {
            LocalDate.parse(timeStr, dateTimeFormatter);
            return true;
        }catch (DateTimeParseException exception) {
            return false;
        }
    }

    public static String getNowForISO8601() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        return formatterISO8601.format(nowDateTime);
    }

    public static long getDifferenceForNow(String keepaliveTime) {
        if (ObjectUtils.isEmpty(keepaliveTime)) {
            return 0;
        }
        Instant beforeInstant = Instant.from(formatter.parse(keepaliveTime));
        return ChronoUnit.MILLIS.between(beforeInstant, Instant.now());
    }

    public static long getDifference(String startTime, String endTime) {
        if (ObjectUtils.isEmpty(startTime) || ObjectUtils.isEmpty(endTime)) {
            return 0;
        }
        Instant startInstant = Instant.from(formatter.parse(startTime));
        Instant endInstant = Instant.from(formatter.parse(endTime));
        return ChronoUnit.MILLIS.between(startInstant, endInstant);
    }

}
