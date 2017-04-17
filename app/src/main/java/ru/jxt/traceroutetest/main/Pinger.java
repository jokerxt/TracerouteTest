package ru.jxt.traceroutetest.main;


import android.util.Log;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ru.jxt.traceroutetest.MainActivity;

public class Pinger {

    public static final int MAX_HOP = 255;
    public static final int DEFAULT_PACKET_SIZE = 56;

    private SortedMap<Hop, Future<UpdatePingTask>> futures;
    private Storage mStorage;

    public Pinger(Storage storage) {
        this.mStorage = storage;
        this.futures = new TreeMap<>(storage.getHopComparator());
    }

    public void pingAllNodes(int count, int interval, int timeout) {
        SortedSet<Hop> hops = mStorage.getHops();

        if (!hops.isEmpty()) {
            for (Hop hop : hops) {
                UpdatePingTask task = new UpdatePingTask(hop, count, timeout);
                try {
                    if (futures.put(hop, mStorage.getExecutor().submit(task, task)) != null)
                        log("Failed to insert future for hop " + hop);
                    else
                        TimeUnit.SECONDS.sleep(interval);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    public synchronized boolean isFinished() {
        return futures.isEmpty();
    }

    public class UpdatePingTask extends PingTask {

        public void run() {
            super.run();
        }

        public UpdatePingTask(Hop hop, int count, int timeout) {
            super(hop, hop.getInetAddress(), count, MAX_HOP, timeout, DEFAULT_PACKET_SIZE);
        }

        protected void postRun(Hop hop) {
            if(futures.get(hop) != null)
                futures.remove(hop);

            if (this.responseIp == null)
                hop.setIcmpResponse(Hop.ICMP_RESPONSE.NO_ANSWER);
        }
    }

    private void log(String msg) {
        Log.w(MainActivity.TAG, msg);
    }
}
