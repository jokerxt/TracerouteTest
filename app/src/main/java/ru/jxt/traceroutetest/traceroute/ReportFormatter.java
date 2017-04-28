package ru.jxt.traceroutetest.traceroute;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.SortedSet;

/* Класс ReportFormatter
* Формирование отчет
* */

public class ReportFormatter {
    public static ArrayList<String> getReport(@NonNull SortedSet<NetworkNode> networkNodes) {
        ArrayList<String> report = new ArrayList<>();
        for(NetworkNode networkNode : networkNodes) {
            String str = networkNode.toString();
            if(networkNode.getIcmpResponse() == NetworkNode.ICMP_RESPONSE.NO_TRACE)
                str += "Not traced";
            else if(networkNode.getIcmpResponse() == NetworkNode.ICMP_RESPONSE.NO_PING)
                str += "Not pinged";
            else if(networkNode.getIcmpResponse() == NetworkNode.ICMP_RESPONSE.DEST_UNREACHABLE)
                str += "Distance unreachable";
            report.add(str);
        }
        return report;
    }
}
