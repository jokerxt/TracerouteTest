package ru.jxt.traceroutetest.callbacks;

import java.util.ArrayList;

import ru.jxt.traceroutetest.helpers.ReportsHelper;
import ru.jxt.traceroutetest.Traceroute;


public class OnReportFormationWhenTraceFinishedCallback implements Traceroute.OnTraceFinishedCallback {
    @Override
    public void onTraceFinished(Traceroute traceroute, ArrayList<String> report) {
        if(!report.isEmpty()) {
            //формируем отчет
            String fReport = "";
            for(String t : report)
                fReport += (t + "\r\n");

            //добавляем отчет в список отчетов с ключом по hashCode
            ReportsHelper.addReport(traceroute.hashCode(), fReport);
        }
    }
}
