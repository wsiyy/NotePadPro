package com.example.notepadpro;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 时间戳格式化工具：
 * 今天 -> “今天 HH:mm”，昨天 -> “昨天 HH:mm”，
 * 今年 -> “MM-dd HH:mm”，更早 -> “yyyy-MM-dd HH:mm”。
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static String format(long millis) {
        Date date = new Date(millis);
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);

        Calendar today = Calendar.getInstance();
        if (isSameDay(today, target)) {
            return "今天 " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        }

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(yesterday, target)) {
            return "昨天 " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        }

        if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR)) {
            return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date);
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
