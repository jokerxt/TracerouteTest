package ru.jxt.traceroutetest.main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;

abstract class PingTask implements Runnable {
    private int count;
    private Hop hop;
    private InetAddress ownAddress;
    private int packetsize;
    protected String responseIp;
    private int timeout;
    private int ttl;

    protected abstract void postRun(Hop hop) throws Exception;

    public PingTask(Hop hop, InetAddress ownAddress, int count, int ttl, int timeout, int packetsize) {
        this.hop = hop;
        this.count = count;
        this.packetsize = packetsize;
        this.ttl = ttl;
        this.timeout = timeout;
        this.ownAddress = ownAddress;
    }

    private Hop.ICMP_RESPONSE parseErrorString(String error) {
        if (error.contains("Time to live exceeded")) {
            return Hop.ICMP_RESPONSE.TTL_EXCEEDED;
        }
        if (error.contains("filtered")) {
            return Hop.ICMP_RESPONSE.FILTERED;
        }
        if (error.contains("nreachable")) {
            return Hop.ICMP_RESPONSE.DEST_UNREACHABLE;
        }
        return Hop.ICMP_RESPONSE.OTHER;
    }

    @Override
    public void run() {
		try {
			String pingCommand = "";
			InetAddress address = hop.getInetAddress();
			boolean ipv6 = false;

			if(address instanceof java.net.Inet6Address) {
				ipv6 = true;
				pingCommand += "ping6";
			}
			else 
				pingCommand += "ping";
			
			if(!ipv6) {
				pingCommand += (" -t" + ttl);
				pingCommand += (" -W" + timeout);
			}
			else
				pingCommand += (" -h" + ttl);
			
			pingCommand += (" -c" + count);
			pingCommand += (" -s" + packetsize);
			pingCommand += (" " + address.getHostAddress());

			if(ipv6) {
				if(address.isLinkLocalAddress()) {
					NetworkInterface iface = NetworkInterface.getByInetAddress(this.ownAddress);
					pingCommand += ("%" + iface.getName());
				}
			}

			Process process;
			if(!ipv6)
				process = Runtime.getRuntime().exec(pingCommand);
			else
				process = Runtime.getRuntime().exec(new String[] {"su", "-c", pingCommand});

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1024);
			BufferedReader errreader = new BufferedReader(new InputStreamReader(process.getErrorStream()), 1024);

			responseIp = null;
			String errorMsg = null;
			String line;

			while((line = errreader.readLine()) != null) {
				errorMsg = line;
				//Log.w(MainActivity.TAG, "ERROR LINE: " + line);
				hop.setIcmpResponse(parseErrorString(errorMsg));
			}

			int _count = count;

			while((line = reader.readLine()) != null) {
				if(errorMsg == null) {
					//Log.w(MainActivity.TAG, hop.getHop() + " LINE: " + line);

					if(line.startsWith("From ")) {
						int beg = line.indexOf(' ') + 1;
						int end = line.indexOf("icmp") - 1;

						if(line.charAt(end-1) == ':')
							--end;

						if(line.indexOf('%') != -1)
							end = line.indexOf('%');
						
						responseIp = line.substring(beg, end);
						
						hop.setInetAddress(InetAddress.getByName(responseIp));
						hop.setIcmpResponse(parseErrorString(line));
						if(--_count == 0)
							break;
					}
					else if(line.contains("from")) {
						int beg = line.indexOf('m') + 0x2;
						int end = line.indexOf("icmp") - 1;

						if(line.charAt(end-1) == ':')
							--end;

						if(line.indexOf('%') != -1)
							end = line.indexOf('%');
						
						responseIp = line.substring(beg, end);
						
						if(InetAddress.getByName(responseIp).equals(hop.getInetAddress()))
							hop.setIcmpResponse(Hop.ICMP_RESPONSE.HIT);

						if(this instanceof Pinger.UpdatePingTask) {
							if(hop.getIcmpResponse() == Hop.ICMP_RESPONSE.HIT) {
								double currentTime = Double.parseDouble(line.substring(line.lastIndexOf('=') + 1, line.lastIndexOf(' ')));
								hop.addTime(currentTime);
							}
						}

						if(--_count == 0)
							break;
					}
				}
			}
			process.waitFor();
			reader.close();
			errreader.close();
			process.getInputStream().close();
			process.getOutputStream().close();
			process.getErrorStream().close();
			
			postRun(hop);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}
