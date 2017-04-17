package ru.jxt.traceroutetest.main;

import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Storage {
    private ExecutorService executor;
    private SortedSet<Hop> hops;

    public Storage() {
        hops = Collections.synchronizedSortedSet(new TreeSet<>(getHopComparator()));
        executor = Executors.newCachedThreadPool();
    }

    class HopComparator implements Comparator<Hop> {
        public int compare(Hop o1, Hop o2) {
            return o1.getHop() - o2.getHop();
        }
    }

    public HopComparator getHopComparator() {
        return new HopComparator();
    }

    public synchronized SortedSet<Hop> getHops() {
        return hops;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
