package com.example;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A system for serializing and deserializing structured data according to a
 * schema.
 */
public class Parser {
    private final Map<String, DataFormat> dataFormats;
    private final Map<Integer, ServiceInfo> services;

    /**
     * Initializes the Parser with the given schemas.
     * 
     * @param dataSchema     The schema for data objects
     * @param servicesSchema The schema for services
     */
    public Parser(List<Map<String, Object>> dataSchema, List<Map<String, Object>> servicesSchema) {
        this.dataFormats = initializeDataFormats(dataSchema);
        this.services = initializeServices(servicesSchema);
    }

    /**
     * Converts data schemas into internal format.
     */
    private Map<String, DataFormat> initializeDataFormats(List<Map<String, Object>> schema) {
        Map<String, DataFormat> result = new HashMap<>();

        for (Map<String, Object> obj : schema) {
            String name = (String) obj.get("name");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> fields = (List<Map<String, String>>) obj.get("fields");

            List<FieldType> fieldTypes = new ArrayList<>();
            for (Map<String, String> field : fields) {
                String fieldName = field.keySet().iterator().next();
                String fieldType = field.get(fieldName);
                fieldTypes.add(new FieldType(fieldName, fieldType));
            }

            result.put(name, new DataFormat(name, fieldTypes));
        }

        return result;
    }

    /**
     * Converts service schemas into internal format.
     */
    private Map<Integer, ServiceInfo> initializeServices(List<Map<String, Object>> servicesSchema) {
        Map<Integer, ServiceInfo> result = new HashMap<>();

        for (Map<String, Object> obj : servicesSchema) {
            Integer id = (Integer) obj.get("id");
            String name = (String) obj.get("name");
            String request = (String) obj.get("request");
            String response = (String) obj.get("response");

            result.put(id, new ServiceInfo(name, request, response));
        }

        return result;
    }

    /**
     * Marshals a UUID according to RFC 4122 format.
     */
    public byte[] marshalUUID(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.order(ByteOrder.BIG_ENDIAN);

        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();

        // Extract fields in RFC 4122 order
        buffer.putInt((int) (mostSigBits >> 32));
        buffer.putShort((short) (mostSigBits >> 16));
        buffer.putShort((short) mostSigBits);
        buffer.putShort((short) (leastSigBits >> 48));

        for (int i = 5; i >= 0; i--) {
            buffer.put((byte) (leastSigBits >>> (8 * i)));
        }
        return buffer.array();
    }

    /**
     * Unmarshals a UUID from RFC 4122 format bytes.
     */
    public UUID unmarshalUUID(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Invalid UUID bytes: expected 16 bytes but got " + bytes.length);
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Extract fields in RFC 4122 order
        long timeLow = buffer.getInt() & 0xFFFFFFFFL;
        long timeMid = buffer.getShort() & 0xFFFFL;
        long timeHiAndVersion = buffer.getShort() & 0xFFFFL;

        long mostSigBits = (timeLow << 32) | (timeMid << 16) | timeHiAndVersion;

        long clockSeq = buffer.getShort() & 0xFFFFL;
        long node = 0;
        for (int i = 0; i < 6; i++) {
            node = (node << 8) | (buffer.get() & 0xFF);
        }

        long leastSigBits = (clockSeq << 48) | node;

        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Unmarshals the received bytes into a Map object.
     * 
     * @param bytes The received bytes to be unmarshalled
     * @return A Map containing the unmarshalled data
     * @throws IllegalArgumentException if the service ID is not found or the format
     *                                  is invalid
     */
    public Message unmarshall(byte[] bytes) {
        Map<String, Object> data = new HashMap<>();

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Extract header information
        byte[] uuidBytes = new byte[16];
        buffer.get(uuidBytes);
        UUID requestId = this.unmarshalUUID(uuidBytes);

        int serviceId = buffer.getShort() & 0xFFFF;
        RequestType isRequest = RequestType.fromCode(buffer.get());

        // Get the data format for unmarshalling
        ServiceInfo serviceInfo = services.get(serviceId);
        if (serviceInfo == null) {
            throw new IllegalArgumentException("Unknown service ID: " + serviceId);
        }

        if (isRequest == RequestType.ERROR || isRequest == RequestType.LOST) {
            throw new IllegalArgumentException("Received error response");
        }
        String formatName = isRequest == RequestType.REQUEST ? serviceInfo.request : serviceInfo.response;
        DataFormat dataFormat = dataFormats.get(formatName);
        if (dataFormat == null) {
            throw new IllegalArgumentException("Unknown data format: " + formatName);
        }

        // Extract fields based on their types
        for (FieldType field : dataFormat.fields) {
            data.put(field.name, readField(buffer, field.type));
        }

        return new Message(requestId, serviceId, isRequest, formatName, data);
    }

    /**
     * Reads a field of the specified type from the buffer.
     */
    private Object readField(ByteBuffer buffer, String fieldType) {
        switch (fieldType) {
            case "int":
                return buffer.getInt();
            case "str":
                int strLen = buffer.getShort() & 0xFFFF;
                byte[] strBytes = new byte[strLen];
                buffer.get(strBytes);
                return new String(strBytes, StandardCharsets.UTF_8);
            case "float":
                return buffer.getFloat();
            case "bool":
                return buffer.get() == 1;
            default:
                throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
    }

    /**
     * Marshalls the given message into a byte array.
     * 
     * @param message The message to be marshalled
     * @return The marshalled byte array
     * @throws IllegalArgumentException if the format name is not found
     */
    public byte[] marshall(Message message) {
        if (message.getRequestType() == RequestType.REQUEST || message.getRequestType() == RequestType.LOST) {
            return marshalNormal(message);
        } else if (message.getRequestType() == RequestType.ERROR) {
            return marshalError(message);
        } else {
            throw new IllegalArgumentException("Unsupported request type: " + message.getRequestType());
        }
    }

    private byte[] marshalNormal(Message message) {
        DataFormat dataFormat = dataFormats.get(message.getFormatName());
        if (dataFormat == null) {
            throw new IllegalArgumentException("Unknown data format: " + message.getFormatName());
        }

        // Calculate buffer size
        int size = calculateBufferSize(dataFormat, message.getData());

        // Allocate buffer
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Write header
        buffer.put(marshalUUID(message.getRequestId()));
        buffer.putShort((short) message.getServiceId());
        buffer.put((byte) message.getRequestType().getCode());
        for (FieldType field : dataFormat.fields) {
            writeField(buffer, field.name, field.type, message.getData());
        }

        return buffer.array();
    }

    private byte[] marshalError(Message message) {
        int size = 16 + 2 + 1
                + message.getData().get("errorMessage").toString().getBytes(StandardCharsets.UTF_8).length;
        System.out.println(size);
        // Allocate buffer
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Write header
        buffer.put(marshalUUID(message.getRequestId()));
        buffer.putShort((short) message.getServiceId());
        buffer.put((byte) message.getRequestType().getCode());

        String errorMessage = (String) message.getData().get("errorMessage");
        byte[] strBytes = errorMessage.getBytes(StandardCharsets.UTF_8);
        buffer.put(strBytes);

        return buffer.array();
    }

    /**
     * Calculates the required buffer size for marshalling.
     */
    private int calculateBufferSize(DataFormat dataFormat, Map<String, Object> data) {
        int size = 16 + 2 + 1; // UUID (16 bytes) + service ID (2 bytes) + is_request flag (1 byte)

        for (FieldType field : dataFormat.fields) {
            String fieldName = field.name;
            String fieldType = field.type;

            switch (fieldType) {
                case "int":
                    size += 4;
                    break;
                case "str":
                    String strValue = (String) data.get(fieldName);
                    size += 2 + strValue.getBytes(StandardCharsets.UTF_8).length;
                    break;
                case "float":
                    size += 4;
                    break;
                case "bool":
                    size += 1;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported field type: " + fieldType);
            }
        }

        return size;
    }

    /**
     * Writes a field to the buffer.
     */
    private void writeField(ByteBuffer buffer, String fieldName, String fieldType, Map<String, Object> data) {
        switch (fieldType) {
            case "int":
                buffer.putInt((Integer) data.get(fieldName));
                break;
            case "str":
                String strValue = (String) data.get(fieldName);
                byte[] strBytes = strValue.getBytes(StandardCharsets.UTF_8);
                buffer.putShort((short) strBytes.length);
                buffer.put(strBytes);
                break;
            case "float":
                buffer.putFloat((Float) data.get(fieldName));
                break;
            case "bool":
                Boolean boolValue = (Boolean) data.get(fieldName);
                buffer.put((byte) (boolValue != null && boolValue ? 1 : 0));
                break;
            default:
                throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
    }

    /**
     * Represents a field type in a data format.
     */
    static class FieldType {
        private final String name;
        private final String type;

        public FieldType(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    /**
     * Represents a data format with a name and a list of fields.
     */
    static class DataFormat {
        private final String name;
        private final List<FieldType> fields;

        public DataFormat(String name, List<FieldType> fields) {
            this.name = name;
            this.fields = fields;
        }
    }

    /**
     * Represents information about a service.
     */
    static class ServiceInfo {
        private final String name;
        private final String request;
        private final String response;

        public ServiceInfo(String name, String request, String response) {
            this.name = name;
            this.request = request;
            this.response = response;
        }
    }

    /**
     * Represents a message to be marshalled or unmarshalled.
     */
    public static class Message {
        private final UUID requestId;
        private final int serviceId;
        private final RequestType requestType;
        private final String formatName;
        private final Map<String, Object> data;

        public Message(UUID requestId, int serviceId, RequestType requestType, String formatName,
                Map<String, Object> data) {
            this.requestId = requestId;
            this.serviceId = serviceId;
            this.requestType = requestType;
            this.formatName = formatName;
            this.data = data;
        }

        public UUID getRequestId() {
            return requestId;
        }

        public int getServiceId() {
            return serviceId;
        }

        public RequestType getRequestType() {
            return requestType;
        }

        public String getFormatName() {
            return formatName;
        }

        public Map<String, Object> getData() {
            return data;
        }
    }
}