package ru.jxt.traceroutetest.ui;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import ru.jxt.traceroutetest.R;

/*
* Класс ReportDialog
* Специфический диалог для показа отчетов трассировки
*/

public class ReportDialog extends Dialog {

    public ReportDialog(@NonNull Context context, @NonNull String report) {
        super(context);
        setContentView(createReportView(report));
        setupDialogSize();
    }

    private void setupDialogSize() { //настроим размер окна
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window w = getWindow();
        if(w != null) {
            lp.copyFrom(w.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
        }
    }

    private View createReportView(String report) { //создадим кастомный View для отчета
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View main = inflater.inflate(R.layout.report_layout, null);
        TextView tv = (TextView) main.findViewById(R.id.hostTextView);
        tv.setText(report.trim());
        return main;
    }
}
