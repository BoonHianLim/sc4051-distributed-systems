package com.example;

import org.junit.jupiter.api.Test;

public class ParserTest {
    @Test
    public void testMarshal() {
        MockSocket mockSocket = new MockSocket(0);
        try {
            Parser.Message parsedMessage = mockSocket.createMessage(null, 0, RequestType.REQUEST);
            byte[] message = mockSocket.parser.marshall(parsedMessage);
            

        } catch (Exception e) {
            e.printStackTrace();
        }
        mockSocket.close();
    }
}
