package ru.jxt.traceroutetest.asynctasks;

import java.net.InetAddress;

import ru.jxt.traceroutetest.main.Storage;
import ru.jxt.traceroutetest.main.Tracer;

public class TraceAsyncTask extends WaitFinishAsyncTask<InetAddress, Integer, Void> {
    private int pingCount;
    private int responseTimeout;
    private int hopLimit;
    private Storage mStorage;

    public TraceAsyncTask(Storage mStorage, int pingCount, int responseTimeout, int hopLimit) {
        super(responseTimeout * 5);
        this.pingCount = pingCount;
        this.responseTimeout = responseTimeout;
        this.hopLimit = hopLimit;
        this.mStorage = mStorage;
    }

    protected Void doInBackground(InetAddress... host) {
        Tracer tracer = new Tracer(mStorage, host[0], pingCount, responseTimeout, hopLimit);
        tracer.trace();
        mPingerRoot = tracer;
        super.doInBackground(); //ожидаем выполнения Tracer
        return null;
    }
}
