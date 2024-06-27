package com.sergenious.mediabrowser.io.exif;

public enum ExifFieldType {
    UNKNOWN(0, 0, false),
    BYTE(1, 1, true),
    ASCII(2, 1, false),
    SHORT(3, 2, true),
    LONG(4, 4, true),
    RATIONAL(5, 8, true),
    UNDEFINED(7, 1, false),
    SLONG(9, 4, true),
    SRATIONAL(10, 8, true);

    private final int code, size;
    private final boolean integral;

    ExifFieldType(int code, int size, boolean integral) {
        this.code = code;
        this.size = size;
        this.integral = integral;
    }

    public int size() {
        return size;
    }

    public boolean isIntegral() {
        return integral;
    }

    public static ExifFieldType fromCode(int code) {
        for (ExifFieldType fieldType : values()) {
            if (fieldType.code == code) {
                return fieldType;
            }
        }
        return UNKNOWN;
    }
}
