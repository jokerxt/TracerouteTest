package ru.jxt.traceroutetest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.URLUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import ru.jxt.traceroutetest.main.Hop;
import ru.jxt.traceroutetest.main.Pinger;
import ru.jxt.traceroutetest.main.Storage;
import ru.jxt.traceroutetest.main.Tracer;

public class Traceroute {

    private Context mContext;
    private OnTraceFinishedCallback mOnTraceFinishedCallback;
    private OnErrorCallback mOnErrorCallback;
    private String mUrl;

    private boolean pingInvalid;

    private static final int DEFAULT_PING_COUNT = 3;
    private static final int DEFAULT_PING_INTERVAL = 1;
    private static final int ONE_SECOND = 1000;

    private static final Pattern VALID_HOST_REGEX;
    private static final String VALID_HOST_STRING = "^(([a-zA-Zа-яА-Я]|[a-zA-Zа-яА-Я0-9][a-zA-Zа-яА-Я0-9\\-]*[a-zA-Zа-яА-Я0-9])\\.)*([a-zA-Zа-яА-Я]|[a-zA-Zа-яА-Я][a-zA-Zа-яА-Я0-9\\-]*[a-zA-Zа-яА-Я0-9])$";
    private static final Pattern VALID_IP_REGEX;
    private static final String VALID_IP_STRING = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

    static {
        VALID_IP_REGEX = Pattern.compile(VALID_IP_STRING);
        VALID_HOST_REGEX = Pattern.compile(VALID_HOST_STRING);
    }

    private int pingCount;
    private int responseTimeout;
    private int hopLimit;
    private Storage mStorage;

    private InetAddress hostIp;
    private TraceAsyncTask tracerTask;
    private PingAsyncTask pingerTask;
    private Object tag;

    public interface OnTraceFinishedCallback {
        void onTraceFinished(Traceroute traceroute, ArrayList<String> report);
    }

    public interface OnErrorCallback {
        void onError(Traceroute traceroute, String error);
    }

    public Traceroute(Context context) {
        mContext = context;
        mStorage = new Storage();
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object getTag() {
        return this.tag;
    }

    public void setPingCount(int pingCount) {
        this.pingCount = pingCount;
    }

    public void setResponseTimeout(int responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    private class PingAsyncTask extends AsyncTask<Void, Void, Void> {
        private Pinger pinger;
        private ArrayList<String> report;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                pinger = new Pinger(mStorage);
                pinger.pingAllNodes(DEFAULT_PING_COUNT, DEFAULT_PING_INTERVAL, responseTimeout);

                long timerMs = responseTimeout * mStorage.getHops().size() * ONE_SECOND;
                while (!pinger.isFinished()) {
                    if (timerMs <= 0) {
                        log("all ping timeout");
                        break;
                    } else {
                        int decreaseMs = 200;
                        TimeUnit.MILLISECONDS.sleep(decreaseMs);
                        timerMs -= decreaseMs;
                    }
                }
            } catch (Exception e) {
                log("Reported PingAsyncTask ex: " + e.getMessage());
            }

            report = new ArrayList<>();
            for(Hop hop : mStorage.getHops())
                report.add(hop.toString());

            mOnTraceFinishedCallback.onTraceFinished(Traceroute.this, report);

            return null;
        }
    }

    private class TraceAsyncTask extends AsyncTask<InetAddress, Integer, Void> {
        private Tracer tracer;

        protected Void doInBackground(InetAddress... host) {
            try {
                tracer = new Tracer(mStorage, host[0], getLocalIpAddress(), pingCount, responseTimeout, hopLimit);
                tracer.trace();

                long timerMs = responseTimeout * 5 * ONE_SECOND;
                while (!tracer.isFinished()) {
                    if (timerMs <= 0) {
                        log("all trace timeout");
                        break;
                    } else {
                        int decreaseMs = 200;
                        TimeUnit.MILLISECONDS.sleep(decreaseMs);
                        timerMs -= decreaseMs;
                    }
                }

                if (tracer.getError() != null)
                    log(tracer.getError());

            } catch (Exception e) {
                log("Reported TraceAsyncTask ex: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //после получения маршрута, нужно проверить ping до каждого найденного узла
            if (pingerTask == null && !mStorage.getHops().isEmpty()) {
                pingerTask = new PingAsyncTask();
                pingerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else
                elog(tracer.getError());
        }
    }

    public void cancelAll() {
        if (tracerTask != null)
            tracerTask.cancel(true);

        if (pingerTask != null)
            pingerTask.cancel(true);

        mStorage.getHops().clear();
        tracerTask = null;
        pingerTask = null;
    }

    public InetAddress getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                Enumeration<InetAddress> enumIpAddr = en.nextElement().getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        log("local address: " + inetAddress.getHostAddress());
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            log(ex.toString());
        }
        return null;
    }

    public void start() {

        checkSystem();

        if(pingInvalid)
            elog("Invalid ping :(");
        else {
            try {
                if(URLUtil.isValidUrl(mUrl) && (URLUtil.isHttpsUrl(mUrl) || URLUtil.isHttpUrl(mUrl)))
                    runTracer(new URL(mUrl).getHost());
                else
                    throw new MalformedURLException("url is not valid");
            }
            catch(MalformedURLException e) {
                //неправильный формат URL
                elog(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void runTracer(final String hostname) {
        if (VALID_IP_REGEX.matcher(hostname).matches() | VALID_HOST_REGEX.matcher(hostname).matches())
            checkHostAndStart(IDN.toASCII(hostname));
        else
            elog("Invalid hostname");
    }

    private void checkHostAndStart(final String host) {
        new AsyncTask<Void, Void, InetAddress[]>() {

            @Override
            protected InetAddress[] doInBackground(Void... params) {
                return getAllIpAddresses(host);
            }

            @Override
            protected void onPostExecute(InetAddress[] ipAddressesArray) {
                if (ipAddressesArray != null && ipAddressesArray.length != 0) {
                    LinkedList<InetAddress> ipAddresses = new LinkedList<>(Arrays.asList(ipAddressesArray));

                    //удаляем все ipv6 адреса
                    for(InetAddress ipa : ipAddresses) {
                        if (ipa instanceof Inet6Address)
                            ipAddresses.remove(ipa);
                    }
                    //если оказалось что массив состоит только из ipv6 адресов
                    if (ipAddresses.isEmpty()) {
                        elog("only ipv6.. can't ping");
                        return;
                    }

                    //берем первый из возможных ip адресов
                    startTrace(ipAddresses.get(0).getHostAddress());
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void startTrace(String ipAddress) {
        try {
            hostIp = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            elog("unknown host");
            cancelAll();
        }

        if (tracerTask == null && hostIp != null) {
            tracerTask = new TraceAsyncTask();
            tracerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, hostIp);
        }
    }

    private InetAddress[] getAllIpAddresses(String hostname) {
        try {
            return InetAddress.getAllByName(hostname);
        } catch (UnknownHostException e) {
            if (isOnline())
                elog("Unknown host");
            else
                elog("Error connection");
            return null;
        }
    }

    public void checkSystem() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("ping").getInputStream()), 1024);
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                } else if (line.contains("BusyBox")) {
                    pingInvalid = true;
                }
            }
        } catch (IOException e) {
            pingInvalid = true;
        }
    }

    private void log(String msg) {
        Log.d(MainActivity.TAG, msg);
    }

    private void elog(String msg) {
        //выводим сообщение об ошибке в итем с результатом в UI
        if(mOnErrorCallback != null)
            mOnErrorCallback.onError(this, msg);
    }

    private boolean isOnline() {
        NetworkInfo netInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return !(netInfo == null || !netInfo.isConnectedOrConnecting());
    }

    public void setUrl(String url) {
        mUrl = url;
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
