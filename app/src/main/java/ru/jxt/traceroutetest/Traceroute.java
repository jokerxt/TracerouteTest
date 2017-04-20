package ru.jxt.traceroutetest;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.IDN;
import java.net.InetAddress;
import java.util.ArrayList;

import ru.jxt.traceroutetest.asynctasks.PingAsyncTask;
import ru.jxt.traceroutetest.asynctasks.TraceAsyncTask;
import ru.jxt.traceroutetest.asynctasks.СheckHostAndGetIpAsyncTask;
import ru.jxt.traceroutetest.main.Storage;
import ru.jxt.traceroutetest.main.Tracer;

public class Traceroute {

    private String mHost;
    private Context mContext;
    private Storage mStorage;

    private int pingCount;
    private int responseTimeout;
    private int hopLimit;

    private TraceAsyncTask tracerTask;
    private PingAsyncTask pingerTask;

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
        mStorage = new Storage();
        //заданим дефолтные настройки
        pingCount = 1;
        responseTimeout = 5;
        hopLimit = 30;
    }

    public void cancel() {
        if (tracerTask != null)
            tracerTask.cancel(true);

        if (pingerTask != null)
            pingerTask.cancel(true);

        mStorage.getNetworkNodes().clear();
        tracerTask = null;
        pingerTask = null;
    }

    public void start() {
        if(checkAvailabilityPing()) //проверяем поддерживает ли телефон команду ping
            checkHostAndStart(mHost);
        else
            callErrorCallback("Ping isn't available :(");
    }

    private void checkHostAndStart(@NonNull String hostname) {
        new СheckHostAndGetIpAsyncTask(mContext) {
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
        if (tracerTask == null) {
            tracerTask = new TraceAsyncTask(mStorage, pingCount, responseTimeout, hopLimit) {
                @Override
                protected void onPostExecute(Void aVoid) {
                    //проверим если у нас узлы в списке
                    if(!mStorage.getNetworkNodes().isEmpty())
                        startPing(); //проверяем ping до каждого найденного узла
                    else //если узлов нет, то что-то было не так, покажем что (какая была ошибка)
                        callErrorCallback(getError());
                }
            };
            tracerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ipAddress);
        }
    }

    private void startPing() {
        if (pingerTask == null) {
            pingerTask = new PingAsyncTask(mStorage, responseTimeout) {
                @Override
                protected void onPostExecute(ArrayList<String> report) {
                    callFinishCallback(report);
                }
            };
            pingerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
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

    private void callFinishCallback(ArrayList<String> report) {
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
            hopLimit = Tracer.DEFAULT_HOPS;
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
