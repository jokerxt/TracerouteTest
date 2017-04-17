package ru.jxt.traceroutetest.main;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Hop {
    public static final double MAX_TIME = 200000.0d;
    public static InetAddress NULL_ADDRESS;
    private static final byte[] bytes;
    private ArrayList<Double> times;
    private int hop;
    private ICMP_RESPONSE icmpResponse;
    private InetAddress inetAddress;
    private boolean isTarget;


    public enum ICMP_RESPONSE {
        HIT,
        TTL_EXCEEDED,
        NO_ANSWER,
        DEST_UNREACHABLE,
        FILTERED,
        OTHER
    }

    static {
        NULL_ADDRESS = null;
        bytes = new byte[4];
    }

    public Hop() {
        this.times = new ArrayList<>();

        if (NULL_ADDRESS == null) {
            try {
                NULL_ADDRESS = InetAddress.getByAddress(bytes);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    public Hop(int hopNumber) {
        this();
        this.hop = hopNumber;
        this.isTarget = false;
    }

    public Hop(int hopNumber, InetAddress address) {
        this(hopNumber);
        this.inetAddress = address;
        this.icmpResponse = ICMP_RESPONSE.TTL_EXCEEDED;
    }

    public Hop(int hopNumber, InetAddress address, ICMP_RESPONSE icmpErr) {
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
        Hop other = (Hop) obj;
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
