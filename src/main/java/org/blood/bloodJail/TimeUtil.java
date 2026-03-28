package org.blood.bloodJail;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeUtil {

    private static final Pattern PART_PATTERN = Pattern.compile("(\\d+)([smhd])", Pattern.CASE_INSENSITIVE);

    private TimeUtil() {
    }

    public static Duration parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String valueInput = input.trim();
        Matcher matcher = PART_PATTERN.matcher(valueInput);
        long totalMillis = 0L;
        int cursor = 0;

        while (matcher.find()) {
            if (matcher.start() != cursor) {
                return null;
            }
            cursor = matcher.end();

            long value;
            try {
                value = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ex) {
                return null;
            }

            char unit = Character.toLowerCase(matcher.group(2).charAt(0));
            long multiplier;
            switch (unit) {
                case 's':
                    multiplier = 1000L;
                    break;
                case 'm':
                    multiplier = 60_000L;
                    break;
                case 'h':
                    multiplier = 3_600_000L;
                    break;
                case 'd':
                    multiplier = 86_400_000L;
                    break;
                default:
                    return null;
            }

            totalMillis += value * multiplier;
            if (totalMillis <= 0L) {
                return null;
            }
        }

        if (cursor != valueInput.length()) {
            return null;
        }

        return Duration.ofMillis(totalMillis);
    }

    public static String formatCompactDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long days = seconds / 86_400L;
        seconds %= 86_400L;
        long hours = seconds / 3_600L;
        seconds %= 3_600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        if (days > 0L) {
            return days + "д " + hours + "ч " + minutes + "м";
        }
        if (hours > 0L) {
            return hours + "ч " + minutes + "м " + seconds + "с";
        }
        if (minutes > 0L) {
            return minutes + "м " + seconds + "с";
        }
        return seconds + "с";
    }
}
