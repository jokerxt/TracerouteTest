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
import android.widget.Toast;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ru.jxt.traceroutetest.callbacks.OnItemCreateContextMenuListener;
import ru.jxt.traceroutetest.callbacks.OnReportClickListener;
import ru.jxt.traceroutetest.callbacks.OnReportFormationWhenTraceFinishedCallback;
import ru.jxt.traceroutetest.helpers.URLHelper;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "TracerouteTest";

    public static List<Traceroute> mTracerouteList;
    public static ConcurrentHashMap<Integer, String> reports;

    private OnReportClickListener mOnReportClickListener;
    private OnItemCreateContextMenuListener mOnItemCreateContextMenuListener;

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
        Button mTestButton = (Button) findViewById(R.id.reportButton);
        EditText mEditText = (EditText) findViewById(R.id.editText);
        OnClickToStartTrace mOnClickToStartTrace = new OnClickToStartTrace();
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
    }

    private void startTraceAndAddTracedItemView(View v) {
        String strurl = getUrlAndResetSearchLine(v);
        if(strurl != null) {
            try {
                Traceroute mTraceroute = startTrace(strurl);
                //добавим соответствующий item (TracedItemView) в UI для отследивания результата
                LinearLayout main = (LinearLayout) findViewById(R.id.mainLayout);
                ScrollView scroll = (ScrollView) main.getParent();
                //если список содержит много TracedItemView и мы пролиснули его
                // то при добавлении нового TracedItemView список пролистнется наверх
                scroll.smoothScrollTo(0, 0);
                main.addView(createTracedItemView(strurl, mTraceroute.hashCode()), 0);
            }
            catch(URLHelper.UrlNotValidException e) { //покажем что есть ошибка во введенном url
                Toast.makeText(MainActivity.this, "Url isn't valid!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //метод для получения url из EditText
    private String getUrlAndResetSearchLine(View v) {
        EditText mEditText = (EditText) ((View) v.getParent()).findViewById(R.id.editText);
        String host = mEditText.getText().toString();
        mEditText.setText("");
        if(host.isEmpty())
            return null;
        return host;
    }

    //создание TracedItem для соответствующего экземпляра Traceroute, чтобы отслеживать его работу
    private View createTracedItemView(String host, int hash) {
        View item = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_layout, null);
        item.setTag(hash);
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

    private Traceroute startTrace(String strurl) throws URLHelper.UrlNotValidException {
        //получаем валидный объект URL из пришедшей строки, иначе кинем исключение
        URL url = URLHelper.getValidURLFromString(strurl);

        Traceroute mTraceroute = new Traceroute(getApplicationContext());
        //добавляем в список, чтобы можно было заершить все запущенные Traceroute при выходе из приложения
        mTracerouteList.add(mTraceroute);
        //объекту отдаем host, тк из командной строки работает именно с хостом
        mTraceroute.setHost(url.getHost());
        //устанавливаем за максимум сколько hop'ов должен быть достигнуть нужный host
        //по умолчанию в windows это значение 30
        mTraceroute.setHopLimit(30);
        //тк traceroute сделан из команды ping, то устанавливаем сколько раз будет пинговаться каждый узел
        //traceroute в Android без root-прав не доступен
        mTraceroute.setPingCount(1);
        //устанавливаем за сколькосекунд узел должен дать ответ, о том что он доступен
        mTraceroute.setResponseTimeout(5);
        //устанавливаем колбэк, чтобы вывести юзеру сообщение об ошибке
        mTraceroute.setOnErrorCallback(mOnErrorCallback);
        //устанавливаем колбэк, чтобы обновить UI и "вывести кнопку с результатом"
        mTraceroute.setOnTraceFinishedCallback(mOnTraceFinishedCallback);
        mTraceroute.start();
        return mTraceroute;
    }

    OnReportFormationWhenTraceFinishedCallback mOnTraceFinishedCallback = new OnReportFormationWhenTraceFinishedCallback() {
        @Override
        public void onTraceFinished(Traceroute traceroute, ArrayList<String> report) {
            super.onTraceFinished(traceroute, report); //сформировали и добавили отчет
            //убираем Traceroute из списка, тк он уже отработал
            mTracerouteList.remove(traceroute);
            //обновляем состяние соответствующего TracedItemView в UI
            tracedItemChangeState(traceroute.hashCode(), report.isEmpty(), false, null);
        }
    };

    Traceroute.OnErrorCallback mOnErrorCallback = new Traceroute.OnErrorCallback() {
        @Override
        public void onError(Traceroute traceroute, String error) {
            //убираем Traceroute из списка, тк он уже отработал
            mTracerouteList.remove(traceroute);
            //обновляем состяние соответствующего TracedItemView в UI, в котором отобразим текст ошибки
            tracedItemChangeState(traceroute.hashCode(), true, true, error);
        }
    };

    private void tracedItemChangeState(Object tag, final boolean emptyReport, final boolean error, final String errorText) {
        View item = getViewByTag(tag);
        final Button mReportButton = (Button) item.findViewById(R.id.reportButton);
        final ProgressBar mProgressBar = (ProgressBar) item.findViewById(R.id.progressBar);
        final TextView mErrorView = (TextView) item.findViewById(R.id.errorTextView);

        //обновим видимости элементов TracedItemView
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
            //завершим все traceroute, если вышли из приложения
            for(Traceroute traceroute : mTracerouteList)
                traceroute.cancel();
        }
        super.onDestroy();
    }

}
