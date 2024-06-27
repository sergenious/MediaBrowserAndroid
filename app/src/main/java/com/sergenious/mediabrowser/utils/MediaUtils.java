package com.sergenious.mediabrowser.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.TypedValue;

import com.sergenious.mediabrowser.Constants;
import com.sergenious.mediabrowser.R;
import com.sergenious.mediabrowser.io.exif.ExifReader;
import com.sergenious.mediabrowser.io.exif.ExifTag;
import com.sergenious.mediabrowser.io.xmp.XmpReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MediaUtils {
	public static final String MAPS_URL = "http://www.google.com/maps/place/";
	private static final DateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

	private static final Map<String, String> IMAGE_EXTENSIONS = new HashMap<String, String>() {{
		put("bmp", "image/bmp");
		put("gif", "image/gif");
		put("jpg", "image/jpeg");
		put("jpeg", "image/jpeg");
		put("png", "image/png");
		put("webp", "image/webp");
		put("heic", "image/heic");
		put("heif", "image/heif");
		put("avif", "image/avif");
	}};

	private static final Map<String, String> VIDEO_EXTENSIONS = new HashMap<String, String>() {{
		put("mp4", "video/mp4");
		put("mkv", "video/x-matroska");
		put("3gp", "video/3gpp");
	}};

	private static final Map<Integer, float[]> EXIF_ORIENTATION_COEFFICIENTS = new HashMap<Integer, float[]>() {{
		// scaleX, scaleY, rotateAngle, translateX, translateY
		put(2, new float[] {-1, 1, 0, 1, 0});
		put(3, new float[] {-1, -1, 0, 1, 1});
		put(4, new float[] {1, -1, 0, 0, 1});
		put(5, new float[] {-1, 1, 270, 0, 0});
		put(6, new float[] {1, 1, 90, 1, 0});
		put(7, new float[] {-1, 1, 90, 1, 1});
		put(8, new float[] {1, 1, 270, 0, 1});
	}};

	private static Bitmap videoFrameOverlay;

	public static List<String> getAllMediaExtensions() {
		List<String> extensions = new ArrayList<>();
		extensions.addAll(IMAGE_EXTENSIONS.keySet());
		extensions.addAll(VIDEO_EXTENSIONS.keySet());
		return extensions;
	}

	public static boolean isImageExtension(String extension) {
		return IMAGE_EXTENSIONS.containsKey(extension);
	}

	public static boolean isVideoExtension(String extension) {
		return VIDEO_EXTENSIONS.containsKey(extension);
	}

	public static String getMimeType(File file) {
		String extension = FileUtils.getFileExtension(file);
		String mime = VIDEO_EXTENSIONS.get(extension);
		if (mime != null) {
			return mime;
		}
		return IMAGE_EXTENSIONS.get(extension);
	}

	public static Pair<RectF, Bitmap> loadImageWithPano(Context ctx, File file,
		int maxWidth, int maxHeight, boolean maxSizeAsArea, boolean useExifOrientation) {

		if (!file.exists()) {
			return null;
		}

		Pair<Pair<Size, Integer>, Bitmap> image = loadImage(file, maxWidth, maxHeight, maxSizeAsArea, useExifOrientation);
		if ((image == null) || (image.second == null)) {
			return null;
		}

		return new Pair<>(getImagePanoRect(file, image.first.first.getWidth(), image.first.first.getHeight()), image.second);
	}

	public static Bitmap loadImage(byte[] content) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		return BitmapFactory.decodeByteArray(content, 0, content.length, options);
	}

	public static Bitmap loadImage(File file) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
	}

	public static Size getImageDimensions(File file) {
		String extension = FileUtils.getFileExtension(file);

		if (IMAGE_EXTENSIONS.containsKey(extension)) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			return new Size(options.outWidth, options.outHeight);
		}

		return new Size(0, 0);
	}

	public static Pair<Pair<Size, Integer>, Bitmap> loadThumbnailImage(Context context, File file, boolean useExifOrientation) {
		Pair<Pair<Size, Integer>, Bitmap> imageInfo = loadImage(file,
			Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE, false, useExifOrientation);

		String extension = FileUtils.getFileExtension(file);
		if ((imageInfo != null) && (imageInfo.second != null) && VIDEO_EXTENSIONS.containsKey(extension)) {
			if (videoFrameOverlay == null) {
				videoFrameOverlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.video_frame);
			}
			if (videoFrameOverlay != null) {
				Canvas canvas = new Canvas(imageInfo.second);
				Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
				int targetWidth = imageInfo.second.getWidth();
				int targetHeight = imageInfo.second.getHeight();
				int targetSize = Math.min(targetWidth, targetHeight);
				canvas.drawBitmap(videoFrameOverlay, null,
					new Rect((targetWidth - targetSize) / 2, (targetHeight - targetSize) / 2,
						(targetWidth + targetSize) / 2 + 1, (targetHeight + targetSize) / 2 + 1), null);
			}
		}

		return imageInfo;
	}

	@SuppressWarnings("deprecation")
	public static Pair<Pair<Size, Integer>, Bitmap> loadImage(File file, int maxWidth, int maxHeight,
		boolean maxSizeAsArea, boolean useExifOrientation) {

		String extension = FileUtils.getFileExtension(file);

		if (VIDEO_EXTENSIONS.containsKey(extension)) {
			return new Pair<>(new Pair<>(new Size(0, 0), 0),
				ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND));
		}

		if (IMAGE_EXTENSIONS.containsKey(extension)) {
			Size actualSize = getImageDimensions(file);
			Pair<Size, Integer> sizeAndOrientation = getImageExifOrientationAndSize(file);
			if ((sizeAndOrientation == null) || !actualSize.equals(sizeAndOrientation.first)) {
				// Android used the EXIF for the rotation
				sizeAndOrientation = new Pair<>(actualSize, 0);
			}

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = false;
			int imgWidth = sizeAndOrientation.first.getWidth();
			int imgHeight = sizeAndOrientation.first.getHeight();

			options.inSampleSize = 1;
			while ((!maxSizeAsArea && ((imgWidth > maxWidth) || (imgHeight > maxHeight)))
				|| (maxSizeAsArea && ((long) imgWidth * imgHeight > (long) maxWidth * maxHeight))) {

				options.inSampleSize <<= 1;
				imgWidth >>= 1;
				imgHeight >>= 1;
			}

			Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

			if (useExifOrientation) {
				bitmap = fixImageByExifOrientation(bitmap, sizeAndOrientation.second);
			}

			return new Pair<>(sizeAndOrientation, bitmap);
		}

		return null;
	}

	public static byte[] writeImage(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			bitmap.compress(format, quality, os);
			return os.toByteArray();
		}
		catch (IOException e) {
			return null;
		}
	}

	public static boolean isImagePano(File file) {
		try {
			Document xmpDoc = XmpReader.extract(file);
			if (xmpDoc != null) {
				Element rdfElem = XmpReader.findRDF(xmpDoc);
				Element descElem = XmpReader.findDescription(rdfElem, XmpReader.NS_GPANO);
				if (descElem == null) {
					return false;
				}
				String usePanoramaViewer = XmpReader.readString(descElem,
					XmpReader.NS_GPANO, "UsePanoramaViewer");
				String width = XmpReader.readString(descElem,
					XmpReader.NS_GPANO, "FullPanoWidthPixels");
				String height = XmpReader.readString(descElem,
					XmpReader.NS_GPANO, "FullPanoHeightPixels");
				String projection = XmpReader.readString(descElem,
					XmpReader.NS_GPANO, "ProjectionType");
				return ((width != null) || (height != null) || (projection != null))
					&& ((usePanoramaViewer == null) || !usePanoramaViewer.equalsIgnoreCase("false"))
					&& ((projection == null) || projection.equals("equirectangular"));
			}
		}
		catch (Exception e) {
			Log.e(Constants.appNameInternal, "Error parsing JPEG " + file.getAbsolutePath(), e);
		}
		return false;
	}

	public static RectF getImagePanoRect(File file, int imageWidth, int imageHeight) {
		try {
			Document xmpDoc = XmpReader.extract(file);
			if (xmpDoc != null) {
				Element rdfElem = XmpReader.findRDF(xmpDoc);
				Element descElem = XmpReader.findDescription(rdfElem, XmpReader.NS_GPANO);
				if (descElem != null) {
					Float fullWidth = XmpReader.readFloat(descElem, XmpReader.NS_GPANO,
						"FullPanoWidthPixels", (float) imageWidth);
					Float fullHeight = XmpReader.readFloat(descElem, XmpReader.NS_GPANO,
						"FullPanoHeightPixels", null);
					Float xLeft = XmpReader.readFloat(descElem, XmpReader.NS_GPANO,
						"CroppedAreaLeftPixels", 0.0f);
					Float yTop = XmpReader.readFloat(descElem, XmpReader.NS_GPANO,
						"CroppedAreaTopPixels", 0.0f);

					if (fullHeight == null) {
						yTop += (fullWidth / 2 - imageHeight) / 2;
						fullHeight = fullWidth / 2; // equirectangular
					}
					else if (fullHeight < fullWidth / 2) { // not 2:1
						yTop += (fullWidth / 2 - fullHeight) / 2;
						fullHeight = fullWidth / 2;
					}

					return new RectF(
						Math.max(-180, Math.min(-1, 360 * (xLeft / fullWidth) - 180)),
						Math.max(-90, Math.min(-1, 180 * (yTop / fullHeight) - 90)),
						Math.min(180, Math.max(1, 360 * ((xLeft + imageWidth) / fullWidth) - 180)),
						Math.min(90, Math.max(1, 180 * ((yTop + imageHeight) / fullHeight) - 90)));
				}
			}
		}
		catch (Exception e) {
			Log.e(Constants.appNameInternal, "Error parsing JPEG " + file.getAbsolutePath(), e);
		}

		return new RectF(
			-180.0f,
			Math.max(-90, -(180.0f * imageHeight) / imageWidth),
			180.0f,
			Math.min(90, (180.0f * imageHeight) / imageWidth));
	}

	public static Pair<Size, Integer> getImageExifOrientationAndSize(File file) {
		try {
			Map<ExifTag, Object> exifMetadata = ExifReader.extract(file,
				Arrays.asList(ExifTag.ORIENTATION, ExifTag.EXIF_IMAGE_WIDTH, ExifTag.EXIF_IMAGE_HEIGHT));

			int width = getExifTagIntValue(exifMetadata, ExifTag.EXIF_IMAGE_WIDTH, 0);
			int height = getExifTagIntValue(exifMetadata, ExifTag.EXIF_IMAGE_HEIGHT, 0);

			return new Pair<>(new Size(width, height),
				getExifTagIntValue(exifMetadata, ExifTag.ORIENTATION, 0));
		}
		catch (Exception e) {
			Log.e(Constants.appNameInternal, "Error parsing EXIF for " + file.getAbsolutePath(), e);
			return null;
		}
	}

	public static Bitmap fixImageByExifOrientation(Bitmap bitmap, Integer exifOrientation) {
		if (EXIF_ORIENTATION_COEFFICIENTS.containsKey(exifOrientation)) {
			Matrix matrix = new Matrix();
			Size sourceImageSize = new Size(bitmap.getWidth(), bitmap.getHeight());
			Size targetImageSize = fixImageSizeByExifOrientation(sourceImageSize, exifOrientation);
			fixImageMatrixByExifOrientation(matrix, targetImageSize, exifOrientation);
			Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
			bitmap.recycle(); // not needed anymore
			return newBitmap;
		}
		return bitmap;
	}

	public static Size fixImageSizeByExifOrientation(Size size, Integer exifOrientation) {
		if ((size != null) && (exifOrientation != null) && (exifOrientation >= 5)) {
			return new Size(size.getHeight(), size.getWidth());
		}
		return size;
	}

	public static void fixImageMatrixByExifOrientation(Matrix matrix, Size imageSize, Integer exifOrientation) {
		float[] coefficients = EXIF_ORIENTATION_COEFFICIENTS.get(exifOrientation);
		if (coefficients != null) {
			matrix.postScale(coefficients[0], coefficients[1]);
			matrix.postRotate(coefficients[2], 0, 0);
			matrix.postTranslate(coefficients[3] * imageSize.getWidth(), coefficients[4] * imageSize.getHeight());
		}
	}

	public static Uri getMediaContentUri(Context context, File file) {
		String filePath = file.getAbsolutePath();
		try (Cursor cursor = context.getContentResolver().query(
			MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
			new String[] {MediaStore.Images.Media._ID},
			MediaStore.Images.Media.DATA + "=? ",
			new String[] {filePath}, null)) {

			if ((cursor != null) && cursor.moveToFirst()) {
				@SuppressLint("Range")
				int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
				Uri baseUri = Uri.parse("content://media/external/images/media");
				return Uri.withAppendedPath(baseUri, Integer.toString(id));
			}
			else if (file.exists()) {
				ContentValues values = new ContentValues();
				values.put(MediaStore.Images.Media.DATA, filePath);
				return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
			}
			return null;
		}
	}

	public static void startShareIntent(Context context, File file, String title) {
		try {
			Uri uri = getMediaContentUri(context, file);
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
			shareIntent.setType(getMimeType(file));
			context.startActivity(Intent.createChooser(shareIntent, title));
		}
		catch (Exception e) {
			Log.e(Constants.appNameInternal, "Error during share", e);
		}
	}

	public static void startShareIntent(Context context, Collection<File> files, String title) {
		try {
			ArrayList<Uri> uriList = new ArrayList<>();
			Set<String> mimeTypes = new HashSet<>();
			for (File file : files) {
				uriList.add(getMediaContentUri(context, file));
				mimeTypes.add(getMimeType(file));
			}

			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
			shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
			shareIntent.setType(String.join(",", mimeTypes));
			context.startActivity(Intent.createChooser(shareIntent, title));
		}
		catch (Exception e) {
			Log.e(Constants.appNameInternal, "Error during share", e);
		}
	}

	public static Map<String, Object> getMetadata(Context context, File file) throws IOException {
		Map<String, Object> metadata = new LinkedHashMap<>();
		String extension = FileUtils.getFileExtension(file);
		long fileSize = file.length();

		metadata.put(context.getString(R.string.file_name) + ":", file.getName());
		metadata.put(context.getString(R.string.file_path) + ":",
			(file.getParentFile() != null) ? file.getParentFile().getAbsolutePath() : "");
		metadata.put(context.getString(R.string.file_created_time) + ":",
			TIME_FORMATTER.format(FileUtils.getFileCreatedTime(file)));
		metadata.put(context.getString(R.string.file_modified_time) + ":",
			TIME_FORMATTER.format(FileUtils.getFileLastModifiedTime(file)));
		metadata.put(context.getString(R.string.file_size) + ":", FileUtils.fileSizeToString(fileSize, true));

		if (IMAGE_EXTENSIONS.containsKey(extension)) {
			Size imageSize = getImageDimensions(file);
			String imageSizeStr = imageSize.getWidth() + " x " + imageSize.getHeight();
			int numPixels = imageSize.getWidth() * imageSize.getHeight();
			if (numPixels > 1000 * 1000) {
				imageSizeStr += " (" + (numPixels / (1000 * 1000)) + "MP)";
			}
			metadata.put(context.getString(R.string.resolution) + ":", imageSizeStr);

			appendExifMetadata(context, file, metadata);
		}

		if (VIDEO_EXTENSIONS.containsKey(extension)) {
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			retriever.setDataSource(file.getAbsolutePath());

			int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
			int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
			metadata.put(context.getString(R.string.resolution) + ":", width + " x " + height);

			double duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000.0;
			metadata.put(context.getString(R.string.duration) + ":", UiUtils.durationToString(duration) + " s");
			retriever.release();
		}

		return metadata;
	}

	private static void appendExifMetadata(Context context, File file, Map<String, Object> metadata) {
		try {
			Map<ExifTag, Object> exifMetadata = ExifReader.extract(file, null);

			PointF gpsLocation = ExifReader.getGpsPositionLonLat(exifMetadata);
			if (gpsLocation != null) {
				metadata.put(context.getString(R.string.gps_location) + ":",
					createMapsLink(context, gpsLocation.x, gpsLocation.y));
			}

			for (Map.Entry<ExifTag, Object> exifEntry : exifMetadata.entrySet()) {
				ExifTag tag = exifEntry.getKey();
				Object value = tag.translateValue(context, exifEntry.getValue());

				if (((tag == ExifTag.EXPOSURE_TIME) || (tag == ExifTag.SHUTTER_SPEED)) && (value instanceof Number)) {
					double time = ((Number) value).doubleValue();
					if (time <= 0.5) {
						time = (time != 0) ? 1 / time : 0;
						value = "1 / " + (int) time;
					}
					else {
						value = UiUtils.VALUE_FORMATTER.format(time);
					}
				}
				else {
					value = UiUtils.valueToString(value);
				}

				if (tag.getPrefix() != null) {
					value = tag.getPrefix() + value;
				}
				if (tag.getSuffix() != null) {
					value += tag.getSuffix();
				}

				metadata.put(context.getString(exifEntry.getKey().getLabelId()) + ":", value.toString());
			}
		}
		catch (Exception e) {
			Log.e(Constants.appNameInternal, "Error parsing file " + file.getAbsolutePath(), e);

			metadata.put(context.getString(R.string.error) + ":",
				context.getString(R.string.error_parsing_file) + ": " + e.getMessage());
		}
	}

	private static Spanned createMapsLink(Context context, double longitude, double latitude) {
		String url = MAPS_URL
			+ UiUtils.VALUE_FORMATTER.format(latitude) + ","
			+ UiUtils.VALUE_FORMATTER.format(longitude);

		String html = "<img src=\"" + R.drawable.ic_maps + "\" width=\"24\" height=\"24\" /> " +
			"<a href=\"" + url + "\">" + context.getString(R.string.open_in_maps_app) + "</a>";

		final int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24,
			context.getResources().getDisplayMetrics());

		Html.TagHandler tagHandler = (opening, tag, output, xmlReader) -> {};
		Html.ImageGetter imageGetter = source -> {
			Drawable d = context.getDrawable(Integer.parseInt(source));
			d.setBounds(0, 0, iconSize, iconSize);
			return d;
		};

		return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, imageGetter, tagHandler);
	}

	private static int getExifTagIntValue(Map<ExifTag, Object> exifMetadata, ExifTag tag, int defaultValue) {
		Number value = (Number) exifMetadata.get(tag);
		return (value != null) ? value.intValue() : defaultValue;
	}
}

