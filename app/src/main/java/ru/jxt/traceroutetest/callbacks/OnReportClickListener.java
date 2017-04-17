package ru.jxt.traceroutetest.callbacks;

import android.app.Dialog;
import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import ru.jxt.traceroutetest.MainActivity;
import ru.jxt.traceroutetest.R;

import static ru.jxt.traceroutetest.MainActivity.reports;

public class OnReportClickListener implements View.OnClickListener {

    private Context mContext;
    @Override
    public void onClick(View v) {
        mContext = v.getContext();
        if(reports != null) {
            int key = (int) ((View) v.getParent()).getTag();
            String report = reports.get(key);
            if(report != null)
                showReportDialog(report);
        }
    }

    private void showReportDialog(String report) {
        Dialog d = new Dialog(mContext);
        d.setContentView(createReportView(report));
        setupDialogSize(d);
        d.show();
    }

    private void setupDialogSize(Dialog d) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window w = d.getWindow();
        if(w != null) {
            lp.copyFrom(w.getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            w.setAttributes(lp);
        }
    }

    private View createReportView(String report) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View main = inflater.inflate(R.layout.report_layout, null);
        TextView tv = (TextView) main.findViewById(R.id.hostTextView);
        tv.setText(report.trim());
        return main;
    }
}
