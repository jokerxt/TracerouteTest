package ru.jxt.traceroutetest.traceroute.asynctasks;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import ru.jxt.traceroutetest.traceroute.NetworkNode;
import ru.jxt.traceroutetest.traceroute.Ping;
import ru.jxt.traceroutetest.traceroute.ReportFormatter;

/* Класс TracerAsyncTask
* AsyncTask в котором выполнется Ping с разными параметрами
* имитриуя трассировку, затем пинг для подсчета времени.
* В onPostExecute возвращается отчет по всем узлам сформированный в ReportFormatter
* в котором содержатся ip адреса, названия хостов и времена пингов
* */

public class TracerAsyncTask extends AsyncTask<Void, Void, ArrayList<String>> {

    private int hopLimit;
    private int pingCount;
    private InetAddress ipAddress;
    private int responseTimeout;
    private SortedSet<NetworkNode> networkNodes;

    public TracerAsyncTask(@NonNull InetAddress ipAddress, int hopLimit, int pingCount, int responseTimeout) {
        this.hopLimit = hopLimit;
        this.pingCount = pingCount;
        this.ipAddress = ipAddress;
        this.responseTimeout = responseTimeout;
        this.networkNodes = Collections.synchronizedSortedSet(new TreeSet<>(new HopComparator()));
    }

    private class HopComparator implements Comparator<NetworkNode> {
        public int compare(NetworkNode nn1, NetworkNode nn2) {
            return nn1.getHop() - nn2.getHop();
        }
    }

    @Override
    protected ArrayList<String> doInBackground(Void... params) {
        try {
            Ping ping = new Ping(1, responseTimeout);

            for (int i = 1; i <= hopLimit; i ++) {
                if(isCancelled()) {
                    networkNodes.clear();
                    break;
                }
                NetworkNode networkNode = new NetworkNode(i, ipAddress);
                networkNodes.add(networkNode);
                ping.setNetworkNode(networkNode);
                ping.setTtl(i);
                ping.start();
                //является ли узел узлом, который мы трассируем, если да, то трассировка закончена
                if (networkNode.getIcmpResponse() == NetworkNode.ICMP_RESPONSE.HIT)
                    break;
            }

            //проверим если у нас узлы в списке и выполняем ping
            if(!networkNodes.isEmpty()) {
                ping.setCount(pingCount);
                ping.setTtl(Ping.MAX_TTL);
                for (NetworkNode networkNode : networkNodes) {
                    if(isCancelled()) {
                        networkNodes.clear();
                        break;
                    }
                    if(networkNode.getIcmpResponse() != NetworkNode.ICMP_RESPONSE.NO_TRACE &&
                       networkNode.getIcmpResponse() != NetworkNode.ICMP_RESPONSE.DEST_UNREACHABLE) {
                        if (networkNode.getIcmpResponse() == NetworkNode.ICMP_RESPONSE.HIT)
                            ping.setCount(pingCount - 1);
                        ping.setNetworkNode(networkNode);
                        ping.start();
                    }
                }
            }
        }
        catch(Exception ignore) {}
        return ReportFormatter.getReport(networkNodes);
    }
}
