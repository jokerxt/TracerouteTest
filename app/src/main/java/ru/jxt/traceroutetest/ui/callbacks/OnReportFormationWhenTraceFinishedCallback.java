package ru.jxt.traceroutetest.ui.callbacks;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import ru.jxt.traceroutetest.traceroute.Traceroute;
import ru.jxt.traceroutetest.traceroute.helpers.ReportsHelper;

/*
* Когда закончили трассировку (вместе с пингом всех узлов)
* вызывается этот колбэк, где формируется отчет
* и добавляется в список всех отчетов
* через ReportsHelper
*/
public class OnReportFormationWhenTraceFinishedCallback implements Traceroute.OnTraceFinishedCallback {
    @Override
    public void onTraceFinished(@NonNull Traceroute traceroute, @NonNull ArrayList<String> report) {
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
