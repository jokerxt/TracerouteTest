package ru.jxt.traceroutetest;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ru.jxt.traceroutetest.callbacks.OnItemCreateContextMenuListener;
import ru.jxt.traceroutetest.callbacks.OnReportClickListener;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "TracerouteTest";

    public static List<Traceroute> mTracerouteList;
    public static ConcurrentHashMap<Integer, String> reports;

    private OnReportClickListener mOnReportClickListener;
    private OnItemCreateContextMenuListener mOnItemCreateContextMenuListener;

    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        layoutWidgetsSetup();
    }

    private void init() {
        mTracerouteList = Collections.synchronizedList(new ArrayList<Traceroute>());
        reports = new ConcurrentHashMap<>();
        mOnReportClickListener = new OnReportClickListener();
        mOnItemCreateContextMenuListener = new OnItemCreateContextMenuListener();
    }

    private void layoutWidgetsSetup() {
        OnClickToStartTrace mOnClickToStartTrace = new OnClickToStartTrace();

        Button mTestButton = (Button) findViewById(R.id.reportButton);
        EditText mEditText = (EditText) findViewById(R.id.editText);

        mTestButton.setOnClickListener(mOnClickToStartTrace);
        mEditText.setOnEditorActionListener(mOnClickToStartTrace);
    }

    private class OnClickToStartTrace implements View.OnClickListener, TextView.OnEditorActionListener {

        @Override
        public void onClick(View v) {
            startTraceAndAddTracedItemView(v);
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if(actionId == EditorInfo.IME_ACTION_DONE)
                startTraceAndAddTracedItemView(v);
            return false;
        }

        private void startTraceAndAddTracedItemView(View v) {
            String host = getHostAndResetSearchLine(v);

            if(host != null) {
                LinearLayout main = (LinearLayout) findViewById(R.id.mainLayout);
                ScrollView scroll = (ScrollView) main.getParent();
                scroll.smoothScrollTo(0, 0);
                main.addView(createTracedItemView(host), 0);
                startTrace(host);
            }
        }

        private String getHostAndResetSearchLine(View v) {
            EditText mEditText = (EditText) ((View) v.getParent()).findViewById(R.id.editText);
            String host = mEditText.getText().toString();
            mEditText.setText("");
            if(host.isEmpty())
                return null;
            return host;
        }

        private View createTracedItemView(String host) {
            View item = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_layout, null);
            item.setTag(count);
            item.setOnCreateContextMenuListener(mOnItemCreateContextMenuListener);

            TextView mHostTextView = (TextView) item.findViewById(R.id.hostTextView);
            Button mResultButton = (Button) item.findViewById(R.id.reportButton);
            ProgressBar mProgressBar = (ProgressBar) item.findViewById(R.id.progressBar);
            TextView mErrorTextView = (TextView) item.findViewById(R.id.errorTextView);

            mHostTextView.setText(host);
            mResultButton.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mErrorTextView.setVisibility(View.GONE);
            return item;
        }

        private void startTrace(String url) {
            Traceroute mTraceroute = new Traceroute(getApplicationContext());
            mTracerouteList.add(mTraceroute);
            mTraceroute.setUrl(url);
            mTraceroute.setHopLimit(30);
            mTraceroute.setPingCount(1);
            mTraceroute.setResponseTimeout(5);
            mTraceroute.setTag(count++);
            mTraceroute.setOnErrorCallback(mOnErrorCallback);
            mTraceroute.setOnTraceFinishedCallback(mOnTraceFinishedCallback);
            mTraceroute.start();
        }
    }

    Traceroute.OnTraceFinishedCallback mOnTraceFinishedCallback = new Traceroute.OnTraceFinishedCallback() {
        @Override
        public void onTraceFinished(Traceroute traceroute, ArrayList<String> result) {
            boolean emptyReport = result.isEmpty();
            if(!emptyReport) {
                String text = "";
                for(String t : result)
                    text += (t + "\r\n");

                reports.put((int) traceroute.getTag(), text);
            }
            mTracerouteList.remove(traceroute);
            tracedItemChangeState(traceroute, emptyReport, false, null);
        }
    };

    Traceroute.OnErrorCallback mOnErrorCallback = new Traceroute.OnErrorCallback() {
        @Override
        public void onError(Traceroute traceroute, String error) {
            traceroute.cancelAll();
            mTracerouteList.remove(traceroute);
            tracedItemChangeState(traceroute, true, true, error);
        }
    };

    private void tracedItemChangeState(Traceroute traceroute, final boolean emptyReport, final boolean error, final String errorText) {
        View item = getViewByTag(traceroute.getTag());
        final Button mReportButton = (Button) item.findViewById(R.id.reportButton);
        final ProgressBar mProgressBar = (ProgressBar) item.findViewById(R.id.progressBar);
        final TextView mErrorView = (TextView) item.findViewById(R.id.errorTextView);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(View.GONE);
                if(error){
                    mErrorView.setText(errorText);
                    mErrorView.setVisibility(View.VISIBLE);
                    mReportButton.setVisibility(View.GONE);
                }
                else {
                    mReportButton.setVisibility(View.VISIBLE);
                    if(emptyReport) {
                        mReportButton.setText("Ошибка");
                        mReportButton.setTextColor(ContextCompat.getColor(getApplicationContext(),
                                android.R.color.holo_red_light));
                    }
                    else
                        mReportButton.setOnClickListener(mOnReportClickListener);
                }
            }
        });
    }

    private View getViewByTag(Object o) {
        LinearLayout main = (LinearLayout) findViewById(R.id.mainLayout);
        return main.findViewWithTag(o);
    }

    @Override
    protected void onDestroy() {
        if (mTracerouteList != null) {
            for(Traceroute c : mTracerouteList)
                if(c != null)
                    c.cancelAll();
        }
        super.onDestroy();
    }

}
