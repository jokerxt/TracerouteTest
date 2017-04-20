package ru.jxt.traceroutetest.main;

import java.net.InetAddress;
import java.util.ArrayList;

public class NetworkNode {
    private int hop;
    private ArrayList<Double> times; //list, в который записываются времена прохождения пинга
    private ICMP_RESPONSE icmpResponse;
    private InetAddress inetAddress;
    private boolean isTarget;

    //Internet Control Message Protocol (ICMP) — протокол межсетевых управляющих сообщений
    //типы ICMP ответов от пингуемых узлов
    public enum ICMP_RESPONSE {
        HIT,              //ответ удовлетворяет
        TTL_EXCEEDED,     //time to live (ttl) время жизни превышено
        NO_ANSWER,        //нет ответа
        DEST_UNREACHABLE, //цель(хост) не достигнута
        FILTERED,         //пинг был отфильтрован и не пропущен дальше
        OTHER             //иное
    }

    public NetworkNode() {
        this.times = new ArrayList<>();
    }

    public NetworkNode(int hopNumber) {
        this();
        this.hop = hopNumber;
        this.isTarget = false;
    }

    public NetworkNode(int hopNumber, InetAddress address) {
        this(hopNumber);
        this.inetAddress = address;
        this.icmpResponse = ICMP_RESPONSE.TTL_EXCEEDED;
    }

    public NetworkNode(int hopNumber, InetAddress address, ICMP_RESPONSE icmpErr) {
        this(hopNumber, address);
        this.icmpResponse = icmpErr;
    }

    public int getHop() {
        return this.hop;
    }

    public void setHop(int hop) {
        this.hop = hop;
    }

    public InetAddress getInetAddress() {
        return this.inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public ICMP_RESPONSE getIcmpResponse() {
        return this.icmpResponse;
    }

    public void setIcmpResponse(ICMP_RESPONSE icmp) {
        this.icmpResponse = icmp;
    }

    public int hashCode() {
        return (this.inetAddress == null ? 0 : this.inetAddress.hashCode()) + 31;
    }

    public void addTime(double t) {
        times.add(t);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NetworkNode other = (NetworkNode) obj;
        if (this.inetAddress == null) {
            if (other.inetAddress != null) {
                return false;
            }
            return true;
        } else if (this.inetAddress.equals(other.inetAddress)) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        String mstimes = "";
        for(Double t : times)
            mstimes += (t + " ms ");

        return hop + ": " + inetAddress.getCanonicalHostName() + " (" +inetAddress.getHostAddress() + ")\n       " + mstimes.trim();
    }

    public boolean isTarget() {
        return this.isTarget;
    }

    public void setTarget(boolean isTarget) {
        this.isTarget = isTarget;
    }
}
