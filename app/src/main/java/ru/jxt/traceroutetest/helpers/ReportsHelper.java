package ru.jxt.traceroutetest.helpers;

import android.util.Log;

import static ru.jxt.traceroutetest.MainActivity.reports;

public class ReportsHelper {
    public static String getReport(int key) {
        if(isReportsAvailable())
            return reports.get(key);
        return null;
    }

    public static void removeReport(int key) {
        if(isReportsAvailable())
            reports.remove(key);
    }

    public static void addReport(int key, String report) {
        if(reports != null)
            reports.put(key, report);
    }

    private static boolean isReportsAvailable() {
        return (reports != null && !reports.isEmpty());
    }
}
