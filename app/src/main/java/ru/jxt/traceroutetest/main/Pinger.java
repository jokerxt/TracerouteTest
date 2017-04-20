package ru.jxt.traceroutetest.main;


import android.util.Log;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ru.jxt.traceroutetest.MainActivity;
import ru.jxt.traceroutetest.main.tasks.GetTimePingTask;

public class Pinger extends PingerRoot {

    private SortedMap<NetworkNode, Future<GetTimePingTask>> futures;
    private Storage mStorage;

    public Pinger(Storage storage) {
        this.mStorage = storage;
        this.futures = new TreeMap<>(storage.getHopComparator());
    }

    public void pingAllNodes(int count, int interval, int timeout) {
        SortedSet<NetworkNode> networkNodes = mStorage.getNetworkNodes();
        if (!networkNodes.isEmpty()) {
            for (NetworkNode networkNode : networkNodes) {
                //проходимся по всем узлам и пингуем их
                GetTimePingTask task = new GetTimePingTask(networkNode, count, timeout) {
                    @Override
                    protected void postRun(NetworkNode networkNode) {
                        super.postRun(networkNode);
                        //после пинга удаляем Future из мап (за мапом следим в isFinished)
                        if(futures.get(networkNode) != null)
                            futures.remove(networkNode);
                    }
                };

                try {
                    //выполняем и складываем Future в мап
                    if (futures.put(networkNode, mStorage.getExecutor().submit(task, task)) != null)
                        Log.w(MainActivity.TAG, "Failed to insert future for networkNode " + networkNode);
                    else
                        TimeUnit.SECONDS.sleep(interval);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    public synchronized boolean isFinished() {
        return futures.isEmpty();
    }

}
