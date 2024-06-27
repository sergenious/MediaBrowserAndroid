package com.sergenious.mediabrowser.io.exif;

import java.text.DecimalFormat;

public class ExifDegree {
    private static final DecimalFormat DEGREE_FORMATTER = new DecimalFormat("0.#####");
    private static final DecimalFormat SECONDS_FORMATTER = new DecimalFormat("0.##");

    private final double dms;

    public ExifDegree(double degree, double minute, double second) {
        this.dms = degree + minute / 60.0 + second / 3600.0;
    }

    public static ExifDegree fromValues(double[] values) {
        return new ExifDegree(
            (values.length > 0) ? values[0] : 0,
            (values.length > 1) ? values[1] : 0,
            (values.length > 2) ? values[2] : 0);
    }

    public double getDMS() {
        return dms;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        int d = (int) Math.floor(dms);
        int m = (int) Math.floor(60.0 * (dms - d));
        double s = 60.0 * ((dms - d) * 60 - m);

        return d + "\u00B0 " + m + "' " + SECONDS_FORMATTER.format(s) + "\""
            + " / " + DEGREE_FORMATTER.format(dms) + "\u00B0";
    }
}
