package ru.jxt.traceroutetest.traceroute.utils;


import android.support.annotation.NonNull;

import ru.jxt.traceroutetest.traceroute.NetworkNode;

/*
* Класс PingResponseUtil включает в себя методы
* для обработки выводов работы команды ping
* Можно получить тип ICMP, время пинга и Ip адрес
* */

public class PingResponseUtil {

    public static NetworkNode.ICMP_RESPONSE getTypeIcmp(@NonNull String response) {
        if (response.contains("Time to live exceeded")) {
            return NetworkNode.ICMP_RESPONSE.TTL_EXCEEDED;
        }
        if (response.contains("filtered")) {
            return NetworkNode.ICMP_RESPONSE.FILTERED;
        }
        if (response.contains("nreachable")) {
            return NetworkNode.ICMP_RESPONSE.DEST_UNREACHABLE;
        }
        return NetworkNode.ICMP_RESPONSE.OTHER;
    }

    public static Double getPingTime(@NonNull String response) {
        int beg = response.lastIndexOf('=') + 1;
        int end = response.lastIndexOf(' ');
        String time = response.substring(beg, end);
        return Double.parseDouble(time);
    }

    public static String getIpAddress(@NonNull String response) {
        int beg = response.indexOf("rom ") + 4;
        int end = response.indexOf("icmp") - 1;
        if(response.charAt(end-1) == ':')
            --end;
        if(response.indexOf('%') != -1)
            end = response.indexOf('%');
        return response.substring(beg, end);
    }
}
