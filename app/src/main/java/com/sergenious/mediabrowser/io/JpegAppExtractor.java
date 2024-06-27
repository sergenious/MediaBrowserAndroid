package com.sergenious.mediabrowser.io;

import com.sergenious.mediabrowser.utils.FileUtils;
import com.sergenious.mediabrowser.utils.PositionInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("IOStreamConstructor")
public class JpegAppExtractor {
	public enum JpegAppReadState {
		UNREAD,
		READ_COMPLETED,
		READ,
	}

	public interface JpegAppListener {
		JpegAppReadState onJpegApp(String appName, long appFileOfs, InputStream appStream, int appStreamSize)
			throws IOException;
	}

	public static boolean extract(File jpegFile, JpegAppListener listener) throws IOException {
		String extension = FileUtils.getFileExtension(jpegFile);
		if (!"jpg".equalsIgnoreCase(extension) && !"jpeg".equalsIgnoreCase(extension)) {
			return false;
		}

		try (PositionInputStream is = new PositionInputStream(new BufferedInputStream(new FileInputStream(jpegFile), 65536))) {
			while (true) {
				int marker = findMarker(is);
				if ((marker < 0) || (marker == 0xD9)) { // eiher marker not found, EOF, or EOI (end of image)
					break;
				}
				else if (marker == 0xDD) { // DRI marker, fixed 4 byte payload
					if (is.skip(4) != 4) {
						return false; // EOF
					}
				}
				else if ((marker < 0xD0) || (marker > 0xD9)) { // ignore markers without payload
					int markerLen = (is.read() << 8) | is.read();
					if (markerLen < 0) {
						break; // EOF
					}
					if (marker == 0xE1) { // APP1
						String appName = readNullTerminatedString(is);

						JpegAppReadState readState = listener.onJpegApp(appName, is.getPosition(),
							is, markerLen - appName.length() - 3);

						if (readState == JpegAppReadState.READ_COMPLETED) {
							break; // completed, does not need anything more
						}
						else if (readState == JpegAppReadState.UNREAD) {
							long toSkip = markerLen - appName.length() - 3;
							if (is.skip(toSkip) != toSkip) {
								return false;
							}
						}
					}
					else if (is.skip(markerLen - 2) != markerLen - 2) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public static String readNullTerminatedString(InputStream is) throws IOException {
		StringBuilder s = new StringBuilder();
		int ch = is.read();
		while (ch > 0) {
			s.append((char) ch);
			ch = is.read();
		}
		return s.toString();
	}
	
	public static String readString(InputStream is, int length) throws IOException {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < length; i++) {
			int ch = is.read();
			s.append((char) ch);
		}
		return s.toString();
	}
	
	private static int findMarker(InputStream is) throws IOException {
		int ch = is.read();
		while (ch >= 0) {
			// ignore FF00 combination, which is there to allow FF char
			// in the content to not be confused by a marker
			if (ch >= 0xFF01) {
				return ch & 0xFF;
			}
			ch = ((ch & 0xFF) << 8) | is.read();
		}
		return -1;
	}
}
