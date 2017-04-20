package ru.jxt.traceroutetest.main;

import android.util.Log;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.Future;

import ru.jxt.traceroutetest.MainActivity;
import ru.jxt.traceroutetest.main.tasks.TracerTask;

public class Tracer extends PingerRoot {
    public static final int DEFAULT_HOPS = 30;
    private static final int FIRST_HOP = 1;

    private SortedMap<NetworkNode, Future<TracerTask>> futures;
    private SortedSet<NetworkNode> networkNodes;
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

    public Tracer(Storage storage, InetAddress host, int pingcount, int timeout, int hoplimit) {
        this.mStorage = storage;
        this.networkNodes = storage.getNetworkNodes();
        this.targetHost = host;
        this.pingcount = pingcount;
        this.timeout = timeout;
        this.localAddress = getLocalIpAddress();
        this.maxHops = hoplimit;
        this.futures = new TreeMap<>(storage.getHopComparator());
    }

    //получение локального ip устройства
    public InetAddress getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                Enumeration<InetAddress> enumIpAddr = en.nextElement().getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                        return inetAddress;
                }
            }
        } catch (SocketException ignored) { }
        return null;
    }

    public synchronized boolean isFinished() {
        if (futures.isEmpty()) { //когда futures пустой, значит "трассировка" закончена
            boolean networkNodesIsEmpty = networkNodes.isEmpty();
            boolean lastIsTargetHost = networkNodes.last().getInetAddress().equals(targetHost);

            if (!networkNodesIsEmpty && !lastIsTargetHost) {
                //если у нас превышен лимит хопов (по умолчанию 30)
                //то в конец добавляем хост, которому выполнили traceroute
                //и выставляем ему NO_ANSWER
                NetworkNode lastNetworkNode = new NetworkNode(networkNodes.size() + 1, targetHost, NetworkNode.ICMP_RESPONSE.NO_ANSWER);
                if (networkNodes.add(lastNetworkNode)) {
                    lastNetworkNode.setTarget(true);
                } else {
                    log("Add last NetworkNode failed!");
                }
            } else if (networkNodesIsEmpty) {
                //если за трассировку не нашлось узлов, значит маршрут не построен
                setError("No route to host");
            } else {
                //если последний добавленный ip networkNodes совпадает с targetHost - значит это и есть наш хост
                networkNodes.last().setTarget(true);
            }
            return true;
        }
        return false;
    }

    public void trace() {
        traceHops(FIRST_HOP, maxHops);
    }

    private synchronized void traceHops(int from, int to) {
        //по хопам начиная от from до to (хоп(hop) - это расстояние до узла, количественно идентичен времени жизни пакета - ttl)
        //первый узел - это узел, до которого расстояние 1 хоп (например через wi-fi - первый узел - это роутер 192.168.0.1)
        //тут сформируется лист всех нужных networkNodе для дальнейшего пинга каждого узла по отдельности
        for (int i = from; i <= to; i ++) {
            NetworkNode networkNode = new NetworkNode(i, targetHost);
            //получаем ip адреса узлов в TracerTask
            TracerTask task = new TracerTask(networkNode, localAddress, pingcount, timeout) {
                @Override
                protected void postRun(NetworkNode networkNode) {
                    super.postRun(networkNode);
                    try {
                        //по завершению трассировки определенного хопа обработаем полученный networkNode
                        setTraceElement(networkNode);
                    } catch (TraceException e) {
                        setError(e.getMessage());
                    }
                }
            };
            try { //запускаем задачу и кладем future в мап для отслеживания в isFinished
                if (futures.put(networkNode, mStorage.getExecutor().submit(task, task)) != null)
                    log("Failed to insert future for networkNode " + networkNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	private synchronized void setTraceElement(NetworkNode networkNode) throws TraceException {
		if(networkNode.getInetAddress().equals(localAddress)) {
            //если вдруг мы пингуем локальный адрес
			if(networkNode.getIcmpResponse().equals(NetworkNode.ICMP_RESPONSE.DEST_UNREACHABLE) ||
				networkNode.getIcmpResponse().equals(NetworkNode.ICMP_RESPONSE.NO_ANSWER)) {
                //и по каким-то причинам нет ответа или хост недостижим
				
				if(futures.isEmpty())
					return;

                //очищаем futures, потому что цель недостижима и бросаем исключение
				futures.clear();
				throw new TraceException("Target is not reachable");
			}
		}
		
		NetworkNode lastNetworkNode = null;
		
		if(!networkNodes.isEmpty())
			lastNetworkNode = networkNodes.last();
		
		boolean doNotAdd = false;
		Future thisTask = (Future) futures.get(networkNode);
        if(thisTask != null)
            futures.remove(networkNode); //тк пинг выполнен, удаляем future из мап

        //проверяем что нам принесло выполнение ping команды
		switch(networkNode.getIcmpResponse()) {
		case HIT:
			if(lastNetworkNode != null) {
				if(lastNetworkNode.equals(networkNode)) { //узел с адресом уже есть
					if(lastNetworkNode.getHop() > networkNode.getHop()) { //если текущий узел имеет хоп меньше последнего
						//передобавляем networkNode с меньшим хопом
                        //тк получается что до текущего узла мы дошли за меньше ttl
						networkNodes.remove(lastNetworkNode);
						networkNodes.add(networkNode);
					}
					break;
				}
			}
			//добавляем новый узел
			networkNodes.add(networkNode);
			break;
			
		case TTL_EXCEEDED: //просто добавляем
			networkNodes.add(networkNode);
			break;
			
		case NO_ANSWER:
			doNotAdd = true;
			if(lastNetworkNode != null) {
				if(lastNetworkNode.getIcmpResponse() == NetworkNode.ICMP_RESPONSE.HIT) {
					if(!lastNetworkNode.getInetAddress().equals(targetHost) | networkNode.getHop() < lastNetworkNode.getHop())
					    networkNodes.add(networkNode); //добавляем узел хоть до него не проходит пинг, тк хопов до него меньше
					break;
				}
			}
			break;
			
		case DEST_UNREACHABLE:
            log("Dest unreachable " + networkNode);
			break;
			
		case FILTERED:
		case OTHER:
            log("Filtered or other " + networkNode);
			doNotAdd = true;
			networkNodes.add(networkNode);
			break;
		}
		
		if(!doNotAdd) {
            //проверяем максимум ли хопов до текущего узела
			if(networkNode.getHop() == maxHops) {
                //если во время traceroute узел не был пропингован
				if(networkNode.getIcmpResponse() != NetworkNode.ICMP_RESPONSE.HIT) {
					throw new TraceException("NetworkNode limit reached"); //бросаем, что лимит хопов исчерпан
				}
			}
		}
	}

    private void log(String msg) {
        Log.w(MainActivity.TAG, msg);
    }
}
