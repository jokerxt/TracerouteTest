package ru.jxt.traceroutetest.traceroute.helpers;

import static ru.jxt.traceroutetest.ui.MainActivity.reports;

/*
* Помощник в работе с отчетами
* */

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
