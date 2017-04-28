package ru.jxt.traceroutetest.traceroute;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.IDN;
import java.net.InetAddress;
import java.util.ArrayList;

import ru.jxt.traceroutetest.traceroute.asynctasks.GetIpFromHostAsyncTask;
import ru.jxt.traceroutetest.traceroute.asynctasks.TracerAsyncTask;


/* Класс Traceroute
* Выполннеие трассировки с параметрами заданного url
* По окончанию трасировки вызывается колбэк OnTraceFinishedCallback
* в метод onTraceFinished которого возвращается отчет сформированный в ReportFormatter
* При возникновении ошибки вызывается колбэк OnErrorCallback
* в метод onError которого возвращается текст ошибки
* */

public class Traceroute {

    private static final int DEFAULT_HOPS = 15;

    private String mHost;
    private Context mContext;
    private TracerAsyncTask tracerAsyncTask;

    private int pingCount;
    private int responseTimeout;
    private int hopLimit;

    private OnTraceFinishedCallback mOnTraceFinishedCallback;
    private OnErrorCallback mOnErrorCallback;

    public interface OnTraceFinishedCallback {
        void onTraceFinished(Traceroute traceroute, ArrayList<String> report);
    }

    public interface OnErrorCallback {
        void onError(Traceroute traceroute, String error);
    }

    public Traceroute(Context context) {
        mContext = context;
        //заданим дефолтные настройки
        pingCount = 3;
        responseTimeout = 5;
        hopLimit = 30;
    }

    public void cancel() {
        tracerAsyncTask.cancel(true);
    }

    public void start() {
        if(checkAvailabilityPing()) //проверяем поддерживает ли телефон команду ping
            checkHostAndStart(mHost);
        else
            callErrorCallback("Ping isn't available :(");
    }

    private void checkHostAndStart(@NonNull String hostname) {
        new GetIpFromHostAsyncTask(mContext) {
            @Override
            protected void onPostExecute(InetAddress ipAddress) {
                //если получили ip адрес, то запускаем trace иначе выводим ошибку
                if(ipAddress != null)
                    startTrace(ipAddress);
                else
                    callErrorCallback(getError());
            } //конвертируем домены в ascii (чтобы не было проблем с русскими доменами)
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, IDN.toASCII(hostname));
    }

    private void startTrace(@NonNull InetAddress ipAddress) {
        tracerAsyncTask = new TracerAsyncTask(ipAddress, hopLimit, pingCount, responseTimeout) {
            @Override
            protected void onPostExecute(@NonNull ArrayList<String> report) {
                callFinishCallback(report);
            }
        };
        tracerAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //метод проверки доступности команды ping
    private boolean checkAvailabilityPing() {
        boolean pingAvailabe = true;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("ping").getInputStream()), 1024);
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                } else if (line.contains("BusyBox")) {
                    pingAvailabe = false;
                    break;
                }
            }
        } catch (IOException e) {
            pingAvailabe = false;
        }
        return pingAvailabe;
    }

    private void callErrorCallback(String msg) {
        //колбэк для сообщения об ошибке
        if(mOnErrorCallback != null)
            mOnErrorCallback.onError(this, msg);
    }

    private void callFinishCallback(@NonNull ArrayList<String> report) {
        //колбэк для возврата отчета
        if(mOnTraceFinishedCallback != null)
            mOnTraceFinishedCallback.onTraceFinished(this, report);
    }

    public void setPingCount(int pingCount) {
        this.pingCount = pingCount;
    }

    public void setResponseTimeout(int responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public void setHost(String host) {
        mHost = host;
    }

    public void setHopLimit(int hop) {
        if(hop < 1 || 255 < hop)
            hopLimit = DEFAULT_HOPS;
        else
            hopLimit = hop;
    }

    public void setOnTraceFinishedCallback(OnTraceFinishedCallback onTraceFinishedCallback) {
        mOnTraceFinishedCallback = onTraceFinishedCallback;
    }

    public void setOnErrorCallback(OnErrorCallback onErrorCallback) {
        mOnErrorCallback = onErrorCallback;
    }
}
