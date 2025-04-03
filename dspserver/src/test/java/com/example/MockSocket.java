package com.example;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

public class MockSocket extends CustomSocket{

    public MockSocket(int portNumber) {
            super(portNumber);
            //TODO Auto-generated constructor stub
        }
    
        @Override
    public void send(Map<String, Object> message, UUID requestId, int serviceId, RequestType isRequest,
            InetAddress destinationAddress, int destinationPort) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'send'");
    }

    @Override
    public SenderResult receive() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receive'");
    }
    
}
