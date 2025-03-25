package com.sergenious.mediabrowser.io.exif;

import android.graphics.PointF;
import android.util.Log;

import com.sergenious.mediabrowser.Constants;
import com.sergenious.mediabrowser.io.JpegAppExtractor;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class ExifReader {
	private static final String EXIF = "Exif";
	private static final byte[] UNDEFINED_TEXT = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
	private static final byte[] ASCII = {0x41, 0x53, 0x43, 0x49, 0x49, 0x00, 0x00, 0x00};
	private static final byte[] JIS = {0x4A, 0x49, 0x53, 0x00, 0x00, 0x00, 0x00, 0x00};
	private static final byte[] UNICODE = {0x55, 0x4E, 0x49, 0x43, 0x4F, 0x44, 0x45, 0x00};

	public static Map<ExifTag, Object> extract(File file, Collection<ExifTag> filter) throws IOException {
		Map<ExifTag, Object> exifMetadata = new TreeMap<>(Comparator.comparingInt(ExifTag::getSortOrder));

		boolean isJpeg = JpegAppExtractor.extract(file, (appName, appFileOfs, appStream, appStreamSize) -> {
			if (appName.equals(EXIF)) {
				parseExif(exifMetadata, file, appFileOfs, appStream, appStreamSize, filter);
				return JpegAppExtractor.JpegAppReadState.READ_COMPLETED;
			}
			return JpegAppExtractor.JpegAppReadState.UNREAD;
		});

		return isJpeg ? exifMetadata : Collections.emptyMap();
	}

	public static PointF getGpsPositionLonLat(Map<ExifTag, Object> exifMetadata) {
		ExifDegree longitudeDegree = (ExifDegree) exifMetadata.get(ExifTag.GPS_LONGITUDE);
		ExifDegree latitudeDegree = (ExifDegree) exifMetadata.get(ExifTag.GPS_LATITUDE);

		if ((longitudeDegree != null) && (latitudeDegree != null)) {
			double longitude = longitudeDegree.getDMS();
			double latitude = latitudeDegree.getDMS();
			Object longitudeRef = exifMetadata.get(ExifTag.GPS_LONGITUDE_REF);
			if ("W".equals(longitudeRef)) {
				longitude = -longitude;
			}
			Object latitudeRef = exifMetadata.get(ExifTag.GPS_LATITUDE_REF);
			if ("S".equals(latitudeRef)) {
				latitude = -latitude;
			}
			return new PointF((float) longitude, (float) latitude);
		}
		return null;
	}

	private static void parseExif(Map<ExifTag, Object> metadata, File file, long fileOfs,
		InputStream is, long length, Collection<ExifTag> filter) throws IOException {

		int ignored = is.read(); // padding

		ExifByteOrder byteOrder = ExifByteOrder.fromCode(read16bit(ExifByteOrder.UNKNOWN, is));
		int version = read16bit(byteOrder, is);
		long ifdOffset = read32bit(byteOrder, is);

		if (byteOrder == ExifByteOrder.UNKNOWN) {
			throw new IOException("Invalid EXIF byte order");
		}
		if (version < 0x2a) {
			throw new IOException("Invalid EXIF version");
		}

		long toSkip = length - 9;
		if (is.skip(toSkip) != toSkip) {
			return;
		}

		// we cannot assume we can freely mark and skip the original input stream,
		// so we rather use a random access file, where we can jump and read anything anywhere.
		try (RandomAccessFile fileInput = new RandomAccessFile(file, "r")) {
			parseExifIFD(metadata, fileInput, fileOfs + 1, ifdOffset, ExifIfdType.STANDARD, byteOrder, filter);
		}
	}

	private static void parseExifIFD(Map<ExifTag, Object> data, RandomAccessFile fileInput, long tiffHeaderOffset,
		long ifdOffset, ExifIfdType ifdType, ExifByteOrder byteOrder, Collection<ExifTag> filter) throws IOException {

		byte[] tagBuffer = new byte[4];

		long saveFileOffset = fileInput.getFilePointer();
		fileInput.seek(tiffHeaderOffset + ifdOffset);
		int entryCount = read16bit(byteOrder, fileInput);

		for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
			int tagCode = read16bit(byteOrder, fileInput);
			ExifTag tag = ExifTag.fromCode(ifdType, tagCode);
			ExifFieldType fieldType = ExifFieldType.fromCode(read16bit(byteOrder, fileInput));
			int length = (int) read32bit(byteOrder, fileInput);
			int valueLength = length * fieldType.size();

			fileInput.read(tagBuffer, 0, 4);
			long intValue = read32bit(byteOrder, tagBuffer, 0);
			byte[] buffer = tagBuffer;

			if ((filter != null) && !filter.isEmpty() && !filter.contains(tag) && !ExifTag.isIfdIndex(tag)) {
				continue; // only process the data, if the tag is in the filter, or the tag is an IFD index
			}

			if ((tag == ExifTag.EXIF_OFFSET) || (tag == ExifTag.INTEROP_OFFSET)) {
				parseExifIFD(data, fileInput, tiffHeaderOffset, intValue, ExifIfdType.STANDARD, byteOrder, filter);
			}
			else if (tag == ExifTag.EXIF_GPS) {
				parseExifIFD(data, fileInput, tiffHeaderOffset, intValue, ExifIfdType.GPS, byteOrder, filter);
			}
			else {
				if (valueLength > 4) { // read indirect value somewhere else in the file
					buffer = readBuffer(fileInput, tiffHeaderOffset + intValue, valueLength);
				}

				Object value = (tag == ExifTag.MAKER_NOTE)
					? buffer // direct raw buffer, as we don't know how to interpret it
					: convertValue(fieldType, byteOrder, buffer, length, intValue);

				if ((value instanceof byte[])
					&& ((tag == ExifTag.EXIF_VERSION) || (tag == ExifTag.FLASHPIX_VERSION) || (tag == ExifTag.INTEROP_VERSION))) {

					value = createString(buffer, length);
				}

				if ((fieldType == ExifFieldType.UNDEFINED) && (value instanceof byte[])) {
					value = decodeUndefinedValue(value);
				}

				// interpreting some specific tags
				if ((tag == ExifTag.USER_COMMENT) && !(value instanceof String)) { // corrupted comment?
					value = "<bin>"; // probably binary data
				}
				else if ((tag == ExifTag.SHUTTER_SPEED) && (value instanceof Double)) {
					value = Math.pow(2, - (Double) value); // APEX value
				}
				else if (((tag == ExifTag.APERTURE) || (tag == ExifTag.MAX_APERTURE)) && (value instanceof Double)) {
					value = Math.pow(2, (Double) value / 2.0); // APEX value
				}
				else if (((tag == ExifTag.GPS_LATITUDE) || (tag == ExifTag.GPS_LONGITUDE)) && (value instanceof double[])) {
					value = ExifDegree.fromValues((double[]) value);
				}
				else if ((tag == ExifTag.GPS_TIMESTAMP) && (value instanceof double[]) && (((double[]) value).length >= 3)) {
					double[] timestamp = (double[]) value;
					value = (int) timestamp[0] + ":" + (int) timestamp[1] + ":" + (int) timestamp[2];
				}

				if ((value != null) && (tag.getLabelId() != 0)) {
					data.put(tag, value);
				}
				if (tag == ExifTag.UNKNOWN) {
					Log.i(Constants.appNameInternal, "Unknown EXIF tag: " + Integer.toHexString(tagCode)
						+ ", value = " + value);
				}
			}
		}

		fileInput.seek(saveFileOffset);
	}

	private static Object decodeUndefinedValue(Object value) {
		byte[] byteArrayValue = (byte[]) value;
		if (byteArrayValue.length >= 8) {
			byte[] prefix = Arrays.copyOfRange(byteArrayValue, 0, 8);
			if (Arrays.equals(prefix, UNDEFINED_TEXT) || Arrays.equals(prefix, ASCII)) {
				// NOTE: ASCII is a subset of UTF-8, so it will work properly
				// "Undefined text" is left for the interpretation, but the safest is to read is as UTF-8
				return new String(byteArrayValue, 8, byteArrayValue.length - 8, StandardCharsets.UTF_8);
			}
			else if (Arrays.equals(prefix, UNICODE)) {
				return new String(byteArrayValue, 8, byteArrayValue.length - 8, StandardCharsets.UTF_16BE);
			}
			// TODO!!! ISO-2022-JP / JIS X 0208
		}
		return value;
	}

	private static Object convertValue(ExifFieldType fieldType, ExifByteOrder byteOrder,
		byte[] buffer, int length, Object defaultValue) {

		if (fieldType == ExifFieldType.UNDEFINED) {
			return Arrays.copyOf(buffer, length);
		}
		if (fieldType == ExifFieldType.ASCII) {
			return createString(buffer, length);
		}
		if ((fieldType == ExifFieldType.RATIONAL) || (fieldType == ExifFieldType.SRATIONAL)) {
			double[] valueArray = new double[length];
			for (int i = 0; i < length; i++) {
				long numerator = read32bit(byteOrder, buffer, i * 8);
				long denominator = read32bit(byteOrder, buffer, i * 8 + 4);
				if (fieldType == ExifFieldType.SRATIONAL) {
					if (numerator >= 0x80000000L) {
						numerator -= 0x100000000L;
					}
					if (denominator >= 0x80000000L) {
						denominator -= 0x100000000L;
					}
				}
				valueArray[i] = (numerator >= -0x7FFFFFFFL) && (numerator <= 0x7FFFFFFFL) && (denominator != 0)
					? (double) numerator / denominator
					: 0;
			}
			return (valueArray.length == 1) ? valueArray[0] : valueArray;
		}
		if ((fieldType == ExifFieldType.BYTE) || (fieldType == ExifFieldType.SHORT)) {
			int[] valueArray = new int[length];
			for (int i = 0; i < length; i++) {
				valueArray[i] = (fieldType == ExifFieldType.BYTE)
					? unsigned(buffer[i])
					: read16bit(byteOrder, buffer, i * 2);
			}
			return (valueArray.length == 1) ? valueArray[0] : valueArray;
		}
		if ((fieldType == ExifFieldType.LONG) || (fieldType == ExifFieldType.SLONG)) {
			long[] valueArray = new long[length];
			for (int i = 0; i < length; i++) {
				valueArray[i] = read32bit(byteOrder, buffer, i * 4);
			}
			return (valueArray.length == 1) ? valueArray[0] : valueArray;
		}
		return defaultValue;
	}

	private static Object createString(byte[] buffer, int length) {
		while ((length > 0) && (buffer[length - 1] == 0)) {
			length--; // strip out the null terminator char
		}
		try {
			// try first with UTF8
			return new String(buffer, 0, length, StandardCharsets.UTF_8);
		}
		catch (Exception e) {
			return new String(buffer, 0, length, StandardCharsets.ISO_8859_1);
		}
	}

	private static byte[] readBuffer(RandomAccessFile fileInput, long offset, int valueLength) throws IOException {
		long saveOffset = fileInput.getFilePointer();
		fileInput.seek(offset);
		byte[] buffer = new byte[valueLength];
		fileInput.read(buffer);
		fileInput.seek(saveOffset);
		return buffer;
	}

	private static long read32bit(ExifByteOrder byteOrder, byte[] buffer, int ofs) {
		return fix32bit(byteOrder,
			(long) unsigned(buffer[ofs])
			+ ((long) unsigned(buffer[ofs + 1]) << 8)
			+ ((long) unsigned(buffer[ofs + 2]) << 16)
			+ ((long) unsigned(buffer[ofs + 3]) << 24));
	}

	private static long read32bit(ExifByteOrder byteOrder, InputStream is) throws IOException {
		return fix32bit(byteOrder,
			(long) is.read()
			+ ((long) is.read() << 8)
			+ ((long) is.read() << 16)
			+ ((long) is.read() << 24));
	}

	private static long read32bit(ExifByteOrder byteOrder, DataInput input) throws IOException {
		return fix32bit(byteOrder, read32bit(input));
	}

	private static long read32bit(DataInput input) throws IOException {
		return (long) input.readUnsignedByte()
			+ ((long) input.readUnsignedByte() << 8)
			+ ((long) input.readUnsignedByte() << 16)
			+ ((long) input.readUnsignedByte() << 24);
	}

	private static int read16bit(ExifByteOrder byteOrder, byte[] buffer, int ofs) {
		return fix16bit(byteOrder, unsigned(buffer[ofs]) + (unsigned(buffer[ofs + 1]) << 8));
	}

	private static int read16bit(ExifByteOrder byteOrder, InputStream is) throws IOException {
		return fix16bit(byteOrder, is.read() + (is.read() << 8));
	}

	private static int read16bit(ExifByteOrder byteOrder, DataInput input) throws IOException {
		return fix16bit(byteOrder, read16bit(input));
	}

	private static int read16bit(DataInput input) throws IOException {
		return input.readUnsignedByte() + (input.readUnsignedByte() << 8);
	}

	private static long fix32bit(ExifByteOrder byteOrder, long value) {
		if (byteOrder == ExifByteOrder.MOTOROLA) {
			return ((value & 0xFF) << 24)
				+ ((value & 0xFF00) << 8)
				+ ((value & 0xFF0000) >> 8)
				+ ((value & 0xFF000000) >> 24);
		}
		return value;
	}

	private static int fix16bit(ExifByteOrder byteOrder, int value) {
		if (byteOrder == ExifByteOrder.MOTOROLA) {
			return ((value & 0xFF) << 8) + ((value & 0xFF00) >> 8);
		}
		return value;
	}

	private static int unsigned(int value) {
		return (value < 0) ? value + 256 : value;
	}
}
