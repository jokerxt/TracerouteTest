package ru.jxt.traceroutetest.ui.callbacks;

import android.support.annotation.NonNull;
import android.view.View;

import ru.jxt.traceroutetest.ui.ReportDialog;
import ru.jxt.traceroutetest.traceroute.helpers.ReportsHelper;

/*
* Этот слушатель вызывается при нажатии на кнопку "Отчет"
* Создает диалоговое окно с отчетом (если отчет есть)
* Отчет получает по ключу, установленному как tag у соответствующего view
* через ReportsHelper
*/
public class OnReportClickListener implements View.OnClickListener {

    @Override
    public void onClick(@NonNull View v) {
        int key = (int) ((View) v.getParent()).getTag(); //получаем ключ
        String report = ReportsHelper.getReport(key); //получаем отчет
        if(report != null)
            new ReportDialog(v.getContext(), report).show(); //показываем отчет в окне ReportDialog
    }
}
