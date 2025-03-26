package com.example;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

public class ParserTest {
    @Test
    public void testMarshal() {
        MockSocket mockSocket = new MockSocket(0);
        try {
            Map<String, Object> message = new HashMap<String, Object>();
            message.put("facilityName", "a");
            message.put("days", "b");
            UUID requestId = new UUID(0L, 0L);
            Parser.Message parsedMessage = mockSocket.createMessage(message, 1, requestId, RequestType.REQUEST);
            byte[] actual = mockSocket.parser.marshall(parsedMessage);

            ByteBuffer buffer = ByteBuffer.allocate(16 + 2 + 1 + 6); // UUID (16 bytes) + service ID (2 bytes) +
                                                                     // is_request
            // flag (1 byte)
            buffer.position(16); // Skip UUID
            buffer.putShort((short) 1); // Service ID
            buffer.put((byte) 0); // Request flag
            buffer.putShort((short) 1); // String length
            buffer.put("a".getBytes(StandardCharsets.UTF_8)); // String
            buffer.putShort((short) 1); // String length
            buffer.put("b".getBytes(StandardCharsets.UTF_8)); // String
            byte[] expected = buffer.array();

            // For debugging
            // System.out.print("Expected: ");
            // for (byte b : expected)
            // System.out.printf("%02X ", b);
            // System.out.println();

            // System.out.print("Actual: ");
            // for (byte b : actual)
            // System.out.printf("%02X ", b);
            // System.out.println();

            assertArrayEquals(expected, actual, "Byte arrays do not match");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed due to exception: " + e.getMessage());
        }
        mockSocket.close();
    }

    @Test
    public void testUnmarshal() {
        MockSocket mockSocket = new MockSocket(0);
        try {
            ByteBuffer buffer = ByteBuffer.allocate(16 + 2 + 1 + 6); // UUID (16 bytes) + service ID (2 bytes) +
                                                                     // is_request
            // flag (1 byte)
            buffer.position(16); // Skip UUID
            buffer.putShort((short) 1); // Service ID
            buffer.put((byte) 0); // Request flag
            buffer.putShort((short) 1); // String length
            buffer.put("a".getBytes(StandardCharsets.UTF_8)); // String
            buffer.putShort((short) 1); // String length
            buffer.put("b".getBytes(StandardCharsets.UTF_8)); // String
            byte[] data = buffer.array();

            Parser.Message message = mockSocket.parser.unmarshall(data);
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("facilityName", "a");
            expected.put("days", "b");
            Map<String, Object> actual = message.getData();
            assertEquals(expected, actual, "Maps do not match");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed due to exception: " + e.getMessage());
        }
        mockSocket.close();
    }

    @Test
    public void testMarshalError() {
        MockSocket mockSocket = new MockSocket(0);
        try {
            String errorMessage = "Error: Fuck You";
            Map<String, Object> message = new HashMap<String, Object>();
            message.put("errorMessage", errorMessage);
            UUID requestId = new UUID(0L, 0L);
            Parser.Message parsedMessage = mockSocket.createMessage(message, 1, requestId, RequestType.ERROR);
            byte[] actual = mockSocket.parser.marshall(parsedMessage);

            ByteBuffer buffer = ByteBuffer.allocate(16 + 2 + 1 + 15); // UUID (16 bytes) + service ID (2 bytes) +
                                                                      // is_request
            // flag (1 byte)
            buffer.position(16); // Skip UUID
            buffer.putShort((short) 1); // Service ID
            buffer.put((byte) 2); // Request flag
            buffer.put(errorMessage.getBytes(StandardCharsets.UTF_8)); // String

            byte[] expected = buffer.array();
            // For debugging
            // System.out.print("Expected: ");
            // for (byte b : expected)
            // System.out.printf("%02X ", b);
            // System.out.println();

            // System.out.print("Actual: ");
            // for (byte b : actual)
            // System.out.printf("%02X ", b);
            // System.out.println();

            assertArrayEquals(expected, actual, "Byte arrays do not match");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed due to exception: " + e.getMessage());
        }
        mockSocket.close();
    }

    // TODO: Implement this test if server is updated to handle error messages
    // @Test
    // public void testUnmarshalError() {
    //     MockSocket mockSocket = new MockSocket(0);
    //     try {
    //         String errorMessage = "Error: Something went wrong";
    //         ByteBuffer buffer = ByteBuffer.allocate(16 + 2 + 1 + errorMessage.length()); // UUID (16 bytes) + service ID
    //                                                                                      // (2 bytes) + is_request flag
    //                                                                                      // (1 byte)
    //         buffer.position(16); // Skip UUID
    //         buffer.putShort((short) 1); // Service ID
    //         buffer.put((byte) 2); // Error flag
    //         buffer.put(errorMessage.getBytes(StandardCharsets.UTF_8)); // Error message

    //         byte[] data = buffer.array();

    //         Parser.Message message = mockSocket.parser.unmarshall(data);
    //         assertEquals(RequestType.ERROR, message.getRequestType(), "Request type should be ERROR");
    //         assertEquals(errorMessage, message.getData().get("errorMessage"), "Error message does not match");
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         fail("Test failed due to exception: " + e.getMessage());
    //     }
    //     mockSocket.close();
    // }

}
