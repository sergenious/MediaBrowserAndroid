package com.sergenious.mediabrowser.io.exif;

public enum ExifByteOrder {
    INTEL(0x4949),
    MOTOROLA(0x4D4D),
    UNKNOWN(0);

    private final int code;

    ExifByteOrder(int code) {
        this.code = code;
    }

    public static ExifByteOrder fromCode(int code) {
        for (ExifByteOrder byteOrder : values()) {
            if (byteOrder.code == code) {
                return byteOrder;
            }
        }
        return UNKNOWN;
    }
}
