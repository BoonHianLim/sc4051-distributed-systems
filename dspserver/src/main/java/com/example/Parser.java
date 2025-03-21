package com.example;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class FieldType {
    public String name;
    public String type;

    public FieldType(String name, String type) {
        this.name = name;
        this.type = type;
    }
}

class DataFormat {
    public String name;
    public List<FieldType> fields;

    public DataFormat(String name, List<FieldType> fields) {
        this.name = name;
        this.fields = fields;
    }
}

class ServiceInfo {
    public String name;
    public String request;
    public String response;

    public ServiceInfo(String name, String request, String response) {
        this.name = name;
        this.request = request;
        this.response = response;
    }
}

class BaseModel {
    public String obj_name;

    public Object get(String fieldName) {
        // This method should be implemented by subclasses
        // to return the value of a field by its name
        throw new UnsupportedOperationException("Method not implemented");
    }
}

public class Parser {
    private Map<String, DataFormat> data;
    private Map<Integer, ServiceInfo> services;

    /**
     * Initializes the Parser with the given schema.
     * 
     * @param schema         The schema for objects
     * @param servicesSchema The schema for services
     */
    public Parser(List<Map<String, Object>> schema, List<Map<String, Object>> servicesSchema) {
        this.data = new HashMap<>();
        this.services = new HashMap<>();

        for (Map<String, Object> obj : schema) {
            String name = (String) obj.get("name");
            List<Map<String, String>> fields = (List<Map<String, String>>) obj.get("fields");

            List<FieldType> fieldTypes = new ArrayList<>();
            for (Map<String, String> field : fields) {
                String fieldName = field.keySet().iterator().next();
                String fieldType = field.get(fieldName);
                fieldTypes.add(new FieldType(fieldName, fieldType));
            }

            data.put(name, new DataFormat(name, fieldTypes));
        }

        for (Map<String, Object> obj : servicesSchema) {
            Integer id = (Integer) obj.get("id");
            String name = (String) obj.get("name");
            String request = (String) obj.get("request");
            String response = (String) obj.get("response");

            services.put(id, new ServiceInfo(name, request, response));
        }
    }

    public byte[] marshalUUIDInRFC4412(UUID uuid) {
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

    public UUID UnmarshalUUIDInRFC4412(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Invalid UUID bytes");
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
     * Unmarshalls the received bytes into a Map object.
     * 
     * @param recvBytes The received bytes to be unmarshalled
     * @return A Map containing the unmarshalled data
     */
    public Map<String, Object> unmarshall(byte[] recvBytes) {
        Map<String, Object> obj = new HashMap<>();

        ByteBuffer buffer = ByteBuffer.wrap(recvBytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Extract UUID (16 bytes)
        byte[] uuidBytes = new byte[16];
        buffer.get(uuidBytes);
        UUID requestId = this.UnmarshalUUIDInRFC4412(uuidBytes);

        // Extract service ID (2 bytes)
        int serviceId = buffer.getShort() & 0xFFFF;

        // Extract is_request flag (1 byte)
        boolean isRequest = buffer.get() == 0;

        // Get the data format for unmarshalling
        ServiceInfo serviceInfo = services.get(serviceId);
        String formatName = isRequest ? serviceInfo.request : serviceInfo.response;
        DataFormat dataFormat = data.get(formatName);

        // Process fields
        for (FieldType field : dataFormat.fields) {
            String fieldName = field.name;
            String fieldType = field.type;

            switch (fieldType) {
                case "int":
                    obj.put(fieldName, buffer.getInt());
                    break;
                case "str":
                    int strLen = buffer.getShort() & 0xFFFF;
                    byte[] strBytes = new byte[strLen];
                    buffer.get(strBytes);
                    obj.put(fieldName, new String(strBytes, StandardCharsets.UTF_8));
                    break;
                case "float":
                    obj.put(fieldName, buffer.getFloat());
                    break;
                case "bool":
                    obj.put(fieldName, buffer.get() == 1);
                    break;
            }
        }

        obj.put("request_id", requestId);
        obj.put("service_id", serviceId);
        obj.put("is_request", isRequest);

        return obj;
    }

    /**
     * Marshalls the given object into a byte array according to the specified
     * format.
     * 
     * @param requestId The unique identifier for the request
     * @param serviceId The service identifier
     * @param isRequest Whether this is a request or response
     * @param item      The object to be marshalled
     * @return The marshalled byte array
     */
    public byte[] marshall(UUID requestId, int serviceId, boolean isRequest, String objName, Map<String, Object> item) {
        DataFormat dataFormat = data.get(objName);

        // Calculate the size of the resulting byte array
        int size = 16 + 2 + 1; // UUID (16 bytes) + service ID (2 bytes) + is_request flag (1 byte)

        // First pass to calculate the size
        for (FieldType field : dataFormat.fields) {
            String fieldName = field.name;
            String fieldType = field.type;

            switch (fieldType) {
                case "int":
                    size += 4;
                    break;
                case "str":
                    String strValue = (String) item.get(fieldName);
                    size += 2 + strValue.getBytes(StandardCharsets.UTF_8).length;
                    break;
                case "float":
                    size += 4;
                    break;
                case "bool":
                    size += 1;
                    break;
            }
        }

        // Allocate buffer
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Add UUID
        buffer.put(this.marshalUUIDInRFC4412(requestId));

        // Add service ID
        buffer.putShort((short) serviceId);

        // Add is_request flag
        buffer.put((byte) (isRequest ? 0 : 1));

        // Add fields
        for (FieldType field : dataFormat.fields) {
            String fieldName = field.name;
            String fieldType = field.type;

            switch (fieldType) {
                case "int":
                    buffer.putInt((Integer) item.get(fieldName));
                    break;
                case "str":
                    String strValue = (String) item.get(fieldName);
                    byte[] strBytes = strValue.getBytes(StandardCharsets.UTF_8);
                    buffer.putShort((short) strBytes.length);
                    buffer.put(strBytes);
                    break;
                case "float":
                    buffer.putFloat((Float) item.get(fieldName));
                    break;
                case "bool":
                    Boolean boolValue = (Boolean) item.get(fieldName);
                    buffer.put((byte) (boolValue != null && boolValue ? 1 : 0));
                    break;
            }
        }

        return buffer.array();
    }
}
