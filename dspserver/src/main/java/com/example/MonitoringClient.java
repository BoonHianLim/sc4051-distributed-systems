package com.example;
import java.util.List;

public class MonitoringClient {
    private String clientAddress;
    private int port;
    private int expirationTime;
    private String facilityName;

    public MonitoringClient (String clientAddress, int port, int expirationTime, String facilityName)
    {
        this.clientAddress = clientAddress;
        this.port = port;
        this.expirationTime = expirationTime;
        this.facilityName = facilityName;
    }

    public void sendCallBack(List<String> availabilityPeriod)
    {
        // TO DO
    }

}
