package com.sergenious.mediabrowser.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;

import com.sergenious.mediabrowser.Constants;

public class ThumbnailsDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "thumbs";
    private static final String FILE_PATH = "file";
    private static final String FILE_SIZE = "size";
    private static final String THUMBNAIL = "thumbnail";

    private static ThumbnailsDatabase instance;

    private final String createTableSql;
    private final SQLiteDatabase db;

    public static ThumbnailsDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new ThumbnailsDatabase(context);
        }
        return instance;
    }

    private ThumbnailsDatabase(Context context) {
        super(context, DB_NAME, null, 1);
        createTableSql = "create table " + DB_NAME + "(" + FILE_PATH + " TEXT NOT NULL, "
            + FILE_SIZE + " int NOT NULL, " + THUMBNAIL + " BLOB);";
        db = getWritableDatabase();
    }

    public Bitmap loadThumbnail(String filePath, long fileSize) {
        String condition = FILE_PATH + "=? AND " + FILE_SIZE + "=?";
        try (Cursor cursor = db.query(DB_NAME, new String[] {THUMBNAIL}, condition,
            new String[] {filePath, Long.toString(fileSize)}, null, null, null)) {

            if (cursor != null) {
                if (cursor.moveToNext()) {
                    return MediaUtils.loadImage(cursor.getBlob(0));
                }
            }
        }
        catch (Exception e) {
            Log.e(Constants.appNameInternal, "Error reading from DB", e);
        }
        return null;
    }

    public void saveThumbnail(String filePath, long fileSize, Bitmap bitmap) {
        db.beginTransaction();
        try {
            db.delete(DB_NAME, FILE_PATH + "=?", new String[] {filePath});
            ContentValues contentValue = new ContentValues();
            contentValue.put(FILE_PATH, filePath);
            contentValue.put(FILE_SIZE, fileSize);
            contentValue.put(THUMBNAIL, MediaUtils.writeImage(bitmap, Bitmap.CompressFormat.PNG, 100));
            db.insertOrThrow(DB_NAME, null, contentValue);
            db.setTransactionSuccessful();
        }
        catch (Exception e) {
            Log.e(Constants.appNameInternal, "Error writing to DB", e);
        }
        finally {
            db.endTransaction();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTableSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
