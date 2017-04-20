package ru.jxt.traceroutetest.asynctasks;


import android.os.AsyncTask;

import java.util.concurrent.TimeUnit;

import ru.jxt.traceroutetest.main.PingerRoot;

public abstract class WaitFinishAsyncTask<T, N, M> extends AsyncTask<T, N, M> {
    private static final int ONE_SECOND = 1000;

    protected PingerRoot mPingerRoot;
    private int fullRresponseTimeout;

    public WaitFinishAsyncTask(int allRresponseTimeout) {
        this.fullRresponseTimeout = allRresponseTimeout;
    }

    @Override
    protected M doInBackground(T... params) {
        try {
            long timerMs = fullRresponseTimeout * ONE_SECOND;
            while (!mPingerRoot.isFinished()) {
                if (timerMs <= 0) {
                    mPingerRoot.setError("timeout");
                    break;
                } else {
                    int decreaseMs = 200;
                    TimeUnit.MILLISECONDS.sleep(decreaseMs);
                    timerMs -= decreaseMs;
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    protected String getError() {
        return mPingerRoot.getError();
    }
}
