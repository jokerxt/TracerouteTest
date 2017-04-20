package ru.jxt.traceroutetest.main.tasks;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;

import ru.jxt.traceroutetest.main.NetworkNode;

abstract class PingTask implements Runnable {
    private int count;
    private NetworkNode networkNode;
    private InetAddress ownAddress;
    private int packetsize;
	private String responseIp;
    private int timeout;
    private int ttl;

    protected abstract void postRun(NetworkNode networkNode) throws Exception;

    public PingTask(NetworkNode networkNode, InetAddress ownAddress, int count, int ttl, int timeout, int packetsize) {
        this.networkNode = networkNode;
        this.count = count;
        this.packetsize = packetsize;
        this.ttl = ttl;
        this.timeout = timeout;
        this.ownAddress = ownAddress;
    }

	public String getResponseIp() {
		return responseIp;
	}

	//парсер в ICMP_RESPONSE пришедших ответов на ping
    private NetworkNode.ICMP_RESPONSE parseErrorString(String error) {
        if (error.contains("Time to live exceeded")) {
            return NetworkNode.ICMP_RESPONSE.TTL_EXCEEDED;
        }
        if (error.contains("filtered")) {
            return NetworkNode.ICMP_RESPONSE.FILTERED;
        }
        if (error.contains("nreachable")) {
            return NetworkNode.ICMP_RESPONSE.DEST_UNREACHABLE;
        }
        return NetworkNode.ICMP_RESPONSE.OTHER;
    }

    @Override
    public void run() {
		try {
			//формируем команду ping
			String pingCommand = "";
			InetAddress address = networkNode.getInetAddress();
			boolean ipv6 = false;

			//в зависимости от адреса выбираем тип команды ping
			if(address instanceof java.net.Inet6Address) {
				ipv6 = true;
				pingCommand += "ping6";
			}
			else 
				pingCommand += "ping";
			
			if(!ipv6) {
				pingCommand += (" -t" + ttl);     //задаем время ttl (аналогично расстоянию жо узла - хопу)
				pingCommand += (" -W" + timeout); //задаем таймаут ожидания
			}
			else
				pingCommand += (" -h" + ttl);
			
			pingCommand += (" -c" + count);       //задаем количество раз будет послан пинг
			pingCommand += (" -s" + packetsize);  //задаем размер пакета пинга
			pingCommand += (" " + address.getHostAddress()); //и сам хост для пинга

			if(ipv6) {
				if(address.isLinkLocalAddress()) {
					//если мы пингуем локальный адрес ipv6
					NetworkInterface iface = NetworkInterface.getByInetAddress(this.ownAddress);
					//то в конец добавляем название интерфейса
					pingCommand += ("%" + iface.getName());
				}
			}

			//запускаем выполнение команды ping
			Process process;
			if(!ipv6)
				process = Runtime.getRuntime().exec(pingCommand);
			else
				process = Runtime.getRuntime().exec(new String[] {"su", "-c", pingCommand});

			//получам входные потоки символов - обычный и ошибок
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
			BufferedReader errreader = new BufferedReader(new InputStreamReader(process.getErrorStream()), 1024);

			responseIp = null;
			String errorMsg = null;
			String line;

			while((line = errreader.readLine()) != null) {
				errorMsg = line;
				//если вдруг пишла ошибка, то парсим ее и устанавливаем ICMP_RESPONSE тип
				networkNode.setIcmpResponse(parseErrorString(errorMsg));
			}

			int _count = count;
			while((line = reader.readLine()) != null) {
				if(errorMsg == null) {
					//если не было ошибок, то парсим пришедший ответ на ping
					if(line.startsWith("From ")) {
						//example: "From 88.206.32.1: icmp_seq=1 Time to live exceeded"
						//выделяем ip адрес
						responseIp = getIpFromResponse(line);
						networkNode.setInetAddress(InetAddress.getByName(responseIp));
						networkNode.setIcmpResponse(parseErrorString(line)); //и парсим ответ
						if(--_count == 0) //проверяем сколько ответов нужно получить, столько раз и крутимся в цикле
							break;
					}
					else if(line.contains("from")) {
						//example: "64 bytes from 87.250.250.242: icmp_seq=1 ttl=57 time=32.2 ms"
						//выделяем ip адрес
						responseIp = getIpFromResponse(line);

						//устанавливаем ICMP_RESPONSE HIT если наш узел пропинговался
						//только во время пинга каждого узла по отдельности, пакет до которого дошел, те ttl не вышло
						if(InetAddress.getByName(responseIp).equals(networkNode.getInetAddress()))
							networkNode.setIcmpResponse(NetworkNode.ICMP_RESPONSE.HIT);

                        //нужно получить время пинга
                        getPingTimeFromResponse(line);

						if(--_count == 0) //проверяем сколько ответов нужно получить
							break;
					}
				}
			}
			//ожидаем завершения процесса и закрываем все потоки и буферы
			process.waitFor();
			reader.close();
			errreader.close();
			process.getInputStream().close();
			process.getOutputStream().close();
			process.getErrorStream().close();

			//запускаем оконечный метод
			postRun(networkNode);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

    protected void timeParsed(Double timeMs) {}

	public Double getPingTimeFromResponse(String line) {
		int beg = line.lastIndexOf('=') + 1;
		int end = line.lastIndexOf(' ');
		String time = line.substring(beg, end);
		return Double.parseDouble(time);
	}

	public String getIpFromResponse(String line) {
		int beg = line.indexOf("rom ") + 4;
		int end = line.indexOf("icmp") - 1;
		if(line.charAt(end-1) == ':')
			--end;
		if(line.indexOf('%') != -1)
			end = line.indexOf('%');
		return line.substring(beg, end);
	}
}
