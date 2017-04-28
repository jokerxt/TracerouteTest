package ru.jxt.traceroutetest.traceroute;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static ru.jxt.traceroutetest.traceroute.utils.PingResponseUtil.getIpAddress;
import static ru.jxt.traceroutetest.traceroute.utils.PingResponseUtil.getPingTime;
import static ru.jxt.traceroutetest.traceroute.utils.PingResponseUtil.getTypeIcmp;

/* Класс Ping
* Формирование, выполнение и обработка команды ping
* С помощью метода start() запускаем выполнение команды ping и последующую обработку
* с занесением необходимой инфрмации (ip адреса, времени и типа icmp) в заданный networkNode
* Если networkNode не задан, то будет исключение NetworkNodeNotSetException
* Аналогично и с другими важными параметрами, такие как:
* * ttl и count - исключение InvalidArgumentException
* */

public class Ping {
    public static final int MAX_TTL = 255;
    public static final int DEFAULT_PACKET_SIZE = 56;

    private NetworkNode networkNode;
	private int packetsize;
    private int timeout;
	private int count;
    private int ttl;

    public class InvalidArgumentException extends Exception {
        public InvalidArgumentException(String message) {
            super(message);
        }
    }

    public class NetworkNodeNotSetException extends Exception {
        public NetworkNodeNotSetException(String message) {
            super(message);
        }
    }

    public Ping(int count, int timeout) {
        this.count = count;
        this.packetsize = DEFAULT_PACKET_SIZE;
        this.timeout = timeout;
        this.networkNode = null;
        this.ttl = MAX_TTL;
    }

	public void setNetworkNode(NetworkNode networkNode) {
		this.networkNode = networkNode;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public void setCount(int count) {
		this.count = count;
	}

	private String buildingPingCommand() {
		String pingCommand = "ping";
		pingCommand += (" -t" + ttl);     //задаем время ttl (аналогично расстоянию до узла - хопу)
		pingCommand += (" -W" + timeout); //задаем таймаут ожидания
		pingCommand += (" -c" + count);       //задаем сколько раз будет послан пинг
		pingCommand += (" -s" + packetsize);  //задаем размер пакета пинга
		pingCommand += (" " + networkNode.getInetAddress().getHostAddress()); //и сам хост для пинга
		return pingCommand;
	}

	private String errorReader(@NonNull Process process) throws IOException {
		BufferedReader errreader = new BufferedReader(new InputStreamReader(process.getErrorStream()), 1024);
		String line;
		String errorMsg = null;
		while((line = errreader.readLine()) != null) {
			errorMsg = line;
		}
		errreader.close();
		return errorMsg;
	}

	private void fillNetworkNode(String ip, NetworkNode.ICMP_RESPONSE icmp, double time) throws UnknownHostException {
		if(time != -1)
			networkNode.addTime(time);
		if(ip != null)
			networkNode.setInetAddress(InetAddress.getByName(ip));
		networkNode.setIcmpResponse(icmp);
	}

	private void fillNotResponsedNetworkNode() throws UnknownHostException {
		switch (networkNode.getIcmpResponse()) {
			case OTHER:
				fillNetworkNode("0.0.0.0", NetworkNode.ICMP_RESPONSE.NO_TRACE, -1);
				break;
			case TTL_EXCEEDED:
				fillNetworkNode(null, NetworkNode.ICMP_RESPONSE.NO_PING, -1);
				break;
		}
	}

	private String responseHandler(@NonNull String response) throws UnknownHostException {
        NetworkNode.ICMP_RESPONSE icmp = NetworkNode.ICMP_RESPONSE.OTHER;
        String responseIp = null;
		double time = -1;

		boolean startFrom = response.startsWith("From ");
		boolean containsFrom = response.contains("from");

		if(startFrom || containsFrom) {
			responseIp = getIpAddress(response);
			if(startFrom) {
				icmp = getTypeIcmp(response);
			}
			else {
				time = getPingTime(response);
				if(InetAddress.getByName(responseIp).equals(networkNode.getInetAddress()))
                    icmp = NetworkNode.ICMP_RESPONSE.HIT;
			}

            fillNetworkNode(responseIp, icmp, time);
		}
		return responseIp;
	}

	private void responseReader(@NonNull Process process) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
		String line;
		String responseIp = null;
		while((line = reader.readLine()) != null) {
			if(line.isEmpty())
				break;
			responseIp = responseHandler(line);
		}

		if (responseIp == null)
			fillNotResponsedNetworkNode();

		reader.close();
	}

    public void start() throws NetworkNodeNotSetException, InvalidArgumentException {
		if(networkNode != null) {
            if(ttl != 0) {
                if(count != 0) {
                    try {
                        //запускаем выполнение команды ping
                        Process process = Runtime.getRuntime().exec(buildingPingCommand());
						if(process != null) {
							String errorMsg = errorReader(process);
							if (errorMsg != null)
								networkNode.setIcmpResponse(getTypeIcmp(errorMsg));
							else
								responseReader(process);

							process.waitFor();
							process.getInputStream().close();
							process.getOutputStream().close();
							process.getErrorStream().close();
						}
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else
                    throw new InvalidArgumentException("Can't set COUNT to 0");
            }
            else
                throw new InvalidArgumentException("Can't set TTL to 0");
		}
		else
		    throw new NetworkNodeNotSetException("NetworkNode not set!");
	}
}
