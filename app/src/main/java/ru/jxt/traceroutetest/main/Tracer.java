package ru.jxt.traceroutetest.main;

import android.util.Log;
import java.net.InetAddress;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.Future;

import ru.jxt.traceroutetest.MainActivity;

public class Tracer {
    public static final int DEFAULT_HOPS = 15;
    public static final int DEFAULT_PACKET_SIZE = 56;
    private static final int FIRST_HOP = 1;

    private String error;
    private SortedMap<Hop, Future<TracerTask>> futures;
    private int hoplimit;
    private SortedSet<Hop> hops;
    private int maxHops;
    private InetAddress localAddress;
    private int pingcount;
    private InetAddress targetHost;
    private int timeout;
    private Storage mStorage;

    public class TraceException extends Exception {
        public TraceException(String message) {
            super(message);
        }
    }

    public Tracer(Storage storage, InetAddress host, InetAddress localAddress, int pingcount, int timeout, int hoplimit) {
        this.mStorage = storage;
        this.hops = storage.getHops();
        this.error = null;
        this.targetHost = host;
        this.pingcount = pingcount;
        this.timeout = timeout;
        this.hoplimit = hoplimit;
        this.localAddress = localAddress;
        this.maxHops = hoplimit;
        this.futures = new TreeMap<>(storage.getHopComparator());
    }

    public class TracerTask extends PingTask {
        public void run() {
            super.run();
        }

        public TracerTask(Hop hop) {
            super(hop, localAddress, pingcount, hop.getHop(), timeout, DEFAULT_PACKET_SIZE);
        }

        protected void postRun(Hop hop) {
            if (responseIp == null) {
                hop.setInetAddress(Hop.NULL_ADDRESS);
                hop.setIcmpResponse(Hop.ICMP_RESPONSE.NO_ANSWER);
            }
            try {
                setTraceElement(hop);
            } catch (TraceException e) {
                e.printStackTrace();
                setError(e.getMessage());
            }
        }
    }

    public void setError(String err) {
        error = err;
    }

    public String getError() {
        return error;
    }

    public void trace() {
        traceHops(FIRST_HOP, maxHops);
    }

    public synchronized boolean isFinished() {
        if (futures.isEmpty()) {
            log("TRACE FINISHED hops number - " + hops.size());

            if (!hops.isEmpty() && !hops.last().getInetAddress().equals(targetHost)) {
                Hop lastHop = new Hop(hops.size() + 1, targetHost, Hop.ICMP_RESPONSE.NO_ANSWER);
                if (hops.add(lastHop)) {
                    log("ADD LAST HOP SUCCEDED " + lastHop.toString());
                    lastHop.setTarget(true);
                } else {
                    log("ADD LAST HOP FAILED");
                }
            } else if (hops.isEmpty()) {
                log("HOPS IS EMPTY");
                setError("No route to host");
            } else if (hops.last().getInetAddress().equals(targetHost)) {
                log("LAST IS TARGET!");
                hops.last().setTarget(true);
            }
            return true;
        }
        return false;
    }

    private synchronized void traceHops(int from, int to) {
        for (int i = from; i <= to; i ++) {
            Hop hop = new Hop(i, targetHost);
            TracerTask task = new TracerTask(hop);
            try {
                if (futures.put(hop, mStorage.getExecutor().submit(task, task)) != null)
                    log("Failed to insert future for hop " + hop);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	private synchronized void setTraceElement(Hop hop) throws TraceException {
		if(hop.getInetAddress().equals(localAddress)) {
			if(hop.getIcmpResponse().equals(Hop.ICMP_RESPONSE.DEST_UNREACHABLE) ||
				hop.getIcmpResponse().equals(Hop.ICMP_RESPONSE.NO_ANSWER)) {
				
				if(futures.isEmpty())
					return;
				
				futures.clear();
				throw new TraceException("Target is not reachable");
			}
		}
		
		Hop lastHop = null;
		
		if(!hops.isEmpty())
			lastHop = hops.last();
		
		boolean doNotAdd = false;
		Future thisTask = (Future) futures.get(hop);
        if(thisTask != null)
            futures.remove(hop);
		
		switch(hop.getIcmpResponse()) {
		case HIT:
			if(lastHop != null) {
				if(lastHop.equals(hop)) { //узел с адресом уже есть
                    //log
					if(lastHop.getHop() > hop.getHop()) { //если hop меньше последнего
						//передобавляем с меньшим hop
						hops.remove(lastHop);
						hops.add(hop);
					}
					break;
				}
			}
			//добавляем новый узел
			hops.add(hop);
			break;
			
		case TTL_EXCEEDED:
			hops.add(hop);
			break;
			
		case NO_ANSWER:
			doNotAdd = true;
			if(lastHop != null) {
				if(lastHop.getIcmpResponse() == Hop.ICMP_RESPONSE.HIT) {
					//нет ответа от текущего узла
					if(!lastHop.getInetAddress().equals(targetHost) | hop.getHop() < lastHop.getHop())
					    hops.add(hop);
					break;
				}
			}
            log("no answer last " + hop);
			break;
			
		case DEST_UNREACHABLE:
            log("Dest unreachable " + hop);
			break;
			
		case FILTERED:
		case OTHER:
            log("Filtered or other " + hop);
			doNotAdd = true;
			hops.add(hop);
			break;
		}
		
		if(!doNotAdd) {
			if(hop.getHop() == maxHops) {
				if(hop.getIcmpResponse() != Hop.ICMP_RESPONSE.HIT) {
					throw new TraceException("Hop limit reached");
				}
			}
		}
	}

    private void log(String msg) {
        Log.w(MainActivity.TAG, msg);
    }

}
