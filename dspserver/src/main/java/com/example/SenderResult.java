package com.example;

import java.net.InetAddress;
import java.util.Map;

public class SenderResult {
    private InetAddress senderIpAddress;
    private int senderPort;
    private Map<String, Object> result;

    public SenderResult(InetAddress senderIpAddress, int senderPort, Map<String, Object> result) {
        this.senderIpAddress = senderIpAddress;
        this.senderPort = senderPort;
        this.result = result;
    }

    public InetAddress getSenderIpAddress() {
        return senderIpAddress;
    }

    public void setSenderIpAddress(InetAddress senderIpAddress) {
        this.senderIpAddress = senderIpAddress;
    }

    public int getSenderPort() {
        return senderPort;
    }

    public void setSenderPort(int senderPort) {
        this.senderPort = senderPort;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

}
