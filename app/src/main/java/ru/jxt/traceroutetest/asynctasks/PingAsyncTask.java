package ru.jxt.traceroutetest.asynctasks;

import java.util.ArrayList;

import ru.jxt.traceroutetest.main.NetworkNode;
import ru.jxt.traceroutetest.main.Pinger;
import ru.jxt.traceroutetest.main.Storage;

public class PingAsyncTask extends WaitFinishAsyncTask<Void, Void, ArrayList<String>> {
    private static final int DEFAULT_PING_COUNT = 3;
    private static final int DEFAULT_PING_INTERVAL = 1;

    private int responseTimeout;
    private Storage mStorage;

    public PingAsyncTask(Storage mStorage, int responseTimeout) {
        super(responseTimeout);
        this.mStorage = mStorage;
        this.responseTimeout = responseTimeout;
    }

    @Override
    protected ArrayList<String> doInBackground(Void... params) {
        Pinger pinger = new Pinger(mStorage);
        pinger.pingAllNodes(DEFAULT_PING_COUNT, DEFAULT_PING_INTERVAL, responseTimeout);
        mPingerRoot = pinger;
        super.doInBackground(params);  //ожидаем выполнения Pinger
        return reportFormation();
    }

    //формируем отчет
    private ArrayList<String> reportFormation() {
        ArrayList<String> report = new ArrayList<>();
        for(NetworkNode networkNode : mStorage.getNetworkNodes())
            report.add(networkNode.toString());
        return report;
    }
}
