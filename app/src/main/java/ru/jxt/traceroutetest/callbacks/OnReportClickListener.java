package ru.jxt.traceroutetest.callbacks;

import android.view.View;

import ru.jxt.traceroutetest.ReportDialog;
import ru.jxt.traceroutetest.helpers.ReportsHelper;

public class OnReportClickListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
        int key = (int) ((View) v.getParent()).getTag(); //получаем ключ
        String report = ReportsHelper.getReport(key); //получаем отчет
        if(report != null)
            new ReportDialog(v.getContext(), report).show(); //показываем отчет в окне ReportDialog
    }
}
