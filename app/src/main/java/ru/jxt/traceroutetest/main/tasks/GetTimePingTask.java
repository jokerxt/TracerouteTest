package ru.jxt.traceroutetest.main.tasks;

import ru.jxt.traceroutetest.main.NetworkNode;

public class GetTimePingTask extends PingTask {

    public static final int MAX_HOP = 255;
    public static final int DEFAULT_PACKET_SIZE = 56;

    private NetworkNode networkNode;

    public void run() {
        super.run();
    }

    public GetTimePingTask(NetworkNode networkNode, int count, int timeout) {
        super(networkNode, networkNode.getInetAddress(), count, MAX_HOP, timeout, DEFAULT_PACKET_SIZE);
        this.networkNode = networkNode;
    }

    @Override
    public Double getPingTimeFromResponse(String line) {
        Double timeMs = super.getPingTimeFromResponse(line);
        if(networkNode.getIcmpResponse() == NetworkNode.ICMP_RESPONSE.HIT) {
            //если текущий узел пропинговался, то время из ответа добавляем в networkNode
            networkNode.addTime(timeMs);
        }
        return timeMs;
    }

    protected void postRun(NetworkNode networkNode) {
        //если после выполнения команды ping ip адрес не определили, то видимо ответа не пришло
        if (getResponseIp() == null)
            networkNode.setIcmpResponse(NetworkNode.ICMP_RESPONSE.NO_ANSWER);
    }
}
