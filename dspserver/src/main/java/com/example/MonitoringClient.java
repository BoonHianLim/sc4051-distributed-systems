package com.example;

import java.net.InetAddress;

public class MonitoringClient {
    private InetAddress clientAddress;
    private int port;
    private int expirationTime;
    private String facilityName;
    private long registrationTime;

    /**
     * Creates a new MonitoringClient.
     * 
     * @param clientAddress the client's IP address as a string
     * @param port the client's port number
     * @param expirationTime the monitoring duration in seconds
     * @param facilityName the name of the facility being monitored
     */
    public MonitoringClient(InetAddress clientAddress, int port, int expirationTime, String facilityName) {
        this.clientAddress = clientAddress;
        this.port = port;
        this.expirationTime = expirationTime;
        this.facilityName = facilityName;
        this.registrationTime = System.currentTimeMillis();
    }

    /**
     * Gets the client's IP address.
     * 
     * @return the client's IP address as a string
     */
    public InetAddress getClientAddress() {
        return clientAddress;
    }

    /**
     * Gets the client's port number.
     * 
     * @return the client's port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the monitoring expiration time in seconds.
     * 
     * @return the expiration time in seconds
     */
    public int getExpirationTime() {
        return expirationTime;
    }

    /**
     * Gets the name of the facility being monitored.
     * 
     * @return the facility name
     */
    public String getFacilityName() {
        return facilityName;
    }

    /**
     * Gets the timestamp when the client was registered.
     * 
     * @return the registration timestamp in milliseconds
     */
    public long getRegistrationTime() {
        return registrationTime;
    }

    /**
     * Checks if the monitoring has expired.
     * 
     * @return true if the monitoring period has expired
     */
    public boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        // Convert minutes to milliseconds (60 seconds * 1000 milliseconds)
        long expirationTimeMillis = registrationTime + (expirationTime * 60 * 1000L);
        return currentTime > expirationTimeMillis;
    }

    /**
     * Gets the remaining monitoring time in seconds.
     * 
     * @return the remaining time in seconds, or 0 if expired
     */
    public int getRemainingTime() {
        if (isExpired()) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis();
        long expirationTimeMillis = registrationTime + (expirationTime * 1000L);
        long remainingMillis = expirationTimeMillis - currentTime;
        
        return (int)(remainingMillis / 1000);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        MonitoringClient that = (MonitoringClient) o;
        return port == that.port && 
               clientAddress.equals(that.clientAddress) && 
               facilityName.equals(that.facilityName);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * clientAddress.hashCode() + port) + facilityName.hashCode();
    }

}
