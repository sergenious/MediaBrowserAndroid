package com.sergenious.mediabrowser.media;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RadioButton;

import com.sergenious.mediabrowser.R;

@SuppressLint("ViewConstructor")
public class HistogramView extends View {
    private static final int WAVEFORM_WIDTH = 256;
    private static final int HISTOGRAM_LINE_WIDTH = 2;

    private HistogramDisplayMode displayMode = HistogramDisplayMode.HISTOGRAM;
    private float[][] histogram;
    private Bitmap waveformBitmap;

    private enum HistogramDisplayMode {
        HISTOGRAM,
        WAVEFORM,
    }

    public static void openDialog(Activity activity, Bitmap bitmap) {
        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.MediaBrowserTheme_AlertDialog)
            .setTitle(R.string.histogram)
            .setView(R.layout.histogram)
            .show();

        HistogramView histogramView = new HistogramView(activity, bitmap);
        ((FrameLayout) dialog.findViewById(R.id.histogramContainer)).addView(histogramView);

        ((RadioButton) dialog.findViewById(R.id.btnHistogram)).setChecked(true);
        dialog.findViewById(R.id.btnHistogram).setOnClickListener(e -> {
            histogramView.setDisplayMode(HistogramDisplayMode.HISTOGRAM);
            dialog.setTitle(R.string.histogram);
        });
        dialog.findViewById(R.id.btnWaveform).setOnClickListener(e -> {
            histogramView.setDisplayMode(HistogramDisplayMode.WAVEFORM);
            dialog.setTitle(R.string.waveform);
        });

        int width = activity.getWindow().getDecorView().getWidth();
        int height = activity.getWindow().getDecorView().getHeight();
        int size = Math.min(width, height);
        size -= ((ViewGroup) activity.findViewById(android.R.id.content)).getTop();
        dialog.getWindow().setLayout(size, size);
    }

    public HistogramView(Context context, Bitmap bitmap) {
        super(context);
        if (bitmap != null) {
            computeHistogramAndWaveform(bitmap);
        }
    }

    public void setDisplayMode(HistogramDisplayMode displayMode) {
        this.displayMode = displayMode;
        invalidate();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        if ((displayMode == HistogramDisplayMode.HISTOGRAM) && (histogram != null)) {
            drawHistogram(canvas, histogram);
        }
        if ((displayMode == HistogramDisplayMode.WAVEFORM) && (waveformBitmap != null)) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            canvas.drawBitmap(waveformBitmap,
                new Rect(0, 0, waveformBitmap.getWidth(), waveformBitmap.getHeight()),
                new Rect(0, 0, getWidth(), getHeight()), paint);
        }
    }

    private void drawHistogram(Canvas canvas, float[][] histogram) {
        for (int channel = 0; channel < 3; channel++) {
            Paint paint = new Paint();
            paint.setColor(0xFF000000 | (255 << (8 * channel))); // proper color of the channel
            paint.setStrokeWidth(HISTOGRAM_LINE_WIDTH);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));

            float prevX = 0;
            float prevY = (1 - histogram[channel][0]) * getHeight();
            for (int i = 1; i < 256; i++) {
                float x = (i * getWidth()) / 256.0f;
                float y = (1 - histogram[channel][i]) * getHeight();
                canvas.drawLine(prevX, prevY, x, y, paint);
                prevX = x;
                prevY = y;
            }
        }
    }

    private void computeHistogramAndWaveform(Bitmap bitmap) {
        histogram = new float[3][256]; // 3 color channels with 256 values each
        float[][][] waveformData = new float[3][WAVEFORM_WIDTH][256];
        waveformBitmap = Bitmap.createBitmap(WAVEFORM_WIDTH, 256, Bitmap.Config.ARGB_8888);

        // count color values for each color channel
        int[] pixels = new int[bitmap.getWidth()];
        for (int y = 0; y < bitmap.getHeight(); y++) {
            bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, y, bitmap.getWidth(), 1);

            for (int x = 0; x < bitmap.getWidth(); x++) {
                int pixel = pixels[x];
                for (int channel = 0; channel < 3; channel++) {
                    int waveformX = (x * WAVEFORM_WIDTH) / bitmap.getWidth();
                    int value = (pixel >> (8 * channel)) & 255;
                    waveformData[channel][waveformX][value]++;
                    histogram[channel][value]++;
                }
            }
        }

        // normalization into [0..1] range
        int totalPixels = bitmap.getWidth() * bitmap.getHeight();
        for (int channel = 0; channel < 3; channel++) {
            float max = 0;
            for (int i = 1; i < 255; i++) { // ignore edges
                max = Math.max(max, histogram[channel][i]);
            }
            for (int i = 0; i < 256; i++) {
                histogram[channel][i] = (max > 0) ? Math.min(1, histogram[channel][i] / max) : 0;
            }
            for (int x = 0; x < WAVEFORM_WIDTH; x++) {
                max = 0;
                for (int i = 1; i < 255; i++) { // ignore edges
                    max = Math.max(max, waveformData[channel][x][i]);
                }
                for (int i = 0; i < 256; i++) {
                    waveformData[channel][x][i] = (max > 0) ? Math.min(1, waveformData[channel][x][i] / max) : 0;
                }
            }
        }

        // convert waveform to bitmap
        for (int i = 0; i < 256; i++) {
            for (int x = 0; x < WAVEFORM_WIDTH; x++) {
                int b = (int) (255 * waveformData[0][x][i]);
                int g = (int) (255 * waveformData[1][x][i]);
                int r = (int) (255 * waveformData[2][x][i]);
                waveformBitmap.setPixel(x, 255 - i, 0xFF000000 | (r << 16) | (g << 8) | b);
            }
        }
    }
}
