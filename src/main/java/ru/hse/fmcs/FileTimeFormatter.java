package ru.hse.fmcs;

import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileTimeFormatter {
    private static final DateTimeFormatter DATE_FORMATTER_WITH_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd/HH:mm:ss.SSSSSSSSS");
    public static String fileTimeToString(FileTime fileTime) {
        return parseToString(
                fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    private static String parseToString(LocalDateTime localDateTime) {
        return localDateTime.format(DATE_FORMATTER_WITH_TIME);
    }

    public static FileTime fileTimeFromString(String dateTimeString) {
        LocalDateTime localDateTime = parseFromString(dateTimeString);
        return FileTime.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDateTime parseFromString(String date) {
        return LocalDateTime.parse(date, DATE_FORMATTER_WITH_TIME);
    }
}
