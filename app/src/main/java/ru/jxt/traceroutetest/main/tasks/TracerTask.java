package ru.jxt.traceroutetest.main.tasks;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ru.jxt.traceroutetest.main.NetworkNode;

public class TracerTask extends PingTask {

    public static final int DEFAULT_PACKET_SIZE = 56;

    public TracerTask(NetworkNode networkNode, InetAddress localAddress, int pingcount, int timeout) {
        super(networkNode, localAddress, pingcount, networkNode.getHop(), timeout, DEFAULT_PACKET_SIZE);
    }

    public void run() {
        super.run();
    }

    protected void postRun(NetworkNode networkNode) {
        try {
            //если по каким-то причинам ip-адрес не получен
            if (getResponseIp() == null) {
                //то устанавливаем networkNode адрес равный 0.0.0.0 и тип ICMP_RESPONSE = NO_ANSWER
                networkNode.setInetAddress(InetAddress.getByAddress(new byte[4]));
                networkNode.setIcmpResponse(NetworkNode.ICMP_RESPONSE.NO_ANSWER);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}