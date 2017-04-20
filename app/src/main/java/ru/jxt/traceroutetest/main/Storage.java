package ru.jxt.traceroutetest.main;

import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Storage {
    private ExecutorService executor;               //храним исполнитель
    private SortedSet<NetworkNode> networkNodes;    //и список всех узлов

    public Storage() {
        //получаем синхронизированную потокобезопасную коллекцию TreeSet
        networkNodes = Collections.synchronizedSortedSet(new TreeSet<>(getHopComparator()));
        executor = Executors.newCachedThreadPool();
    }

    class HopComparator implements Comparator<NetworkNode> {
        public int compare(NetworkNode nn1, NetworkNode nn2) {
            return nn1.getHop() - nn2.getHop();
        }
    }

    public HopComparator getHopComparator() {
        return new HopComparator();
    }

    public synchronized SortedSet<NetworkNode> getNetworkNodes() {
        return networkNodes;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
