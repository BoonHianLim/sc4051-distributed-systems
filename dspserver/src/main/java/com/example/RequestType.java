package com.example;

public enum RequestType {
    REQUEST(0),
    RESPONSE(1),
    ERROR(2),
    LOST(3);

    private final int code;

    RequestType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    // Optional: reverse lookup from int to enum
    public static RequestType fromCode(int code) {
        for (RequestType s : RequestType.values()) {
            if (s.code == code)
                return s;
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
