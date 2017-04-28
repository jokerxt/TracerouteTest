package ru.jxt.traceroutetest.traceroute;

import java.net.InetAddress;
import java.util.ArrayList;

/*
* Класс NetworkNode
* Содержит в себе всю необходимую информацию от узле сети и его доступности
* А так же время прохождения ping команды, расстояние в хопах и ip адрес
*/

public class NetworkNode {
    private int hop;
    private ArrayList<Double> times; //list, в который записываются времена прохождения пинга
    private ICMP_RESPONSE icmpResponse;
    private InetAddress inetAddress;

    //Internet Control Message Protocol (ICMP) — протокол межсетевых управляющих сообщений
    //типы ICMP ответов от пингуемых узлов
    public enum ICMP_RESPONSE {
        HIT,              //ответ удовлетворяет
        TTL_EXCEEDED,     //time to live (ttl) время жизни превышено
        NO_TRACE,        //нет ответа при трассировке
        NO_PING,         //нет ответа при пинге
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
    }

    public NetworkNode(int hopNumber, InetAddress address) {
        this(hopNumber);
        this.inetAddress = address;
        this.icmpResponse = ICMP_RESPONSE.OTHER;
    }

    public int getHop() {
        return this.hop;
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

        return hop + ": " + inetAddress.getCanonicalHostName() + " (" + inetAddress.getHostAddress() + ")\n       " + mstimes.trim();
    }
}
