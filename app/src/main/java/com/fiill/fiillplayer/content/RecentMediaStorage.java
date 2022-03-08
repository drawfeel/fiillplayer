/*
This is class for UI to load/save recent media items from storage.

 */

package com.fiill.fiillplayer.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import androidx.loader.content.AsyncTaskLoader;

public class RecentMediaStorage {
    public static void saveUrlAsync(Context context, String url) {
        saveUrlInThread(context, url);
    }

    public static void saveUrlInThread(Context context, String url) {
        new Thread() {
            @Override
            public void run() {
                saveUrl(context, url);
            }
        }.start();
    }

    public static void saveUrl(Context context, String url) {
        ContentValues cv = new ContentValues();
        cv.putNull(Entry.COLUMN_NAME_ID);
        cv.put(Entry.COLUMN_NAME_URL, url);
        cv.put(Entry.COLUMN_NAME_LAST_ACCESS, System.currentTimeMillis());
        cv.put(Entry.COLUMN_NAME_NAME, getNameOfUrl(url));
        save(context,cv);
    }

    public static void save(Context context, ContentValues contentValue) {
        OpenHelper openHelper = new OpenHelper(context);
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.replace(Entry.TABLE_NAME, null, contentValue);
        db.close();
    }

    public static int removeAsync(Context context, long[] ids) throws InterruptedException {
        return removeInThread(context, ids);
    }

    public static int removeInThread(Context context, long[] ids) throws InterruptedException {
        Thread t = new Thread() {
            public void run() {
                removeIds(context, ids);
            }
        };
        t.start();
        t.join(); // this may block UI
        return 0;
    }

    public static void removeIds(Context context, long[] ids) {
        OpenHelper openHelper = new OpenHelper(context);
        SQLiteDatabase db = openHelper.getWritableDatabase();
        StringBuilder whereClause = new StringBuilder(Entry.COLUMN_NAME_ID);
        if(null == ids) {
            whereClause.append(" >= 0;");
        } else {
            whereClause.append(" IN (");
            for (int i = 0; i < ids.length; i++) {
                if (i > 0) whereClause.append(",");
                whereClause.append("'").append(ids[i]).append("'");
            }
            whereClause.append(");");
        }
        String whereClauseStr = whereClause.toString();
        db.delete(Entry.TABLE_NAME, whereClauseStr,  null);
        db.close();
    }

    public static String getNameOfUrl(String url) {
        return getNameOfUrl(url, "");
    }

    public static String getNameOfUrl(String url, String defaultName) {
        String name = null;
        int pos = url.lastIndexOf('/');
        if (pos >= 0)
            name = url.substring(pos + 1);

        if (TextUtils.isEmpty(name))
            name = defaultName;

        return name;
    }

    public static class Entry {
        public static final String TABLE_NAME = "RecentMedia";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_LAST_ACCESS = "last_access";
    }

    public static final String[] ALL_COLUMNS = new String[]{
            Entry.COLUMN_NAME_ID + " as _id",
            Entry.COLUMN_NAME_ID,
            Entry.COLUMN_NAME_URL,
            Entry.COLUMN_NAME_NAME,
            Entry.COLUMN_NAME_LAST_ACCESS};

    public static class OpenHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;
        private static final String DATABASE_NAME = "RecentMedia.db";
        private static final String SQL_CREATE_ENTRIES =
                " CREATE TABLE IF NOT EXISTS " + Entry.TABLE_NAME + " (" +
                        Entry.COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        Entry.COLUMN_NAME_URL + " VARCHAR UNIQUE, " +
                        Entry.COLUMN_NAME_NAME + " VARCHAR, " +
                        Entry.COLUMN_NAME_LAST_ACCESS + " INTEGER) ";

        public OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public static class CursorLoader extends AsyncTaskLoader<Cursor> {
        final OpenHelper openHelper;
        final SQLiteDatabase db;
        public CursorLoader(Context context) {
            super(context);
            openHelper = new OpenHelper(context);
            db = openHelper.getReadableDatabase();
        }

        public void destroy() {
            db.close();
        }
        @Override
        public Cursor loadInBackground() {
            return db.query(Entry.TABLE_NAME, ALL_COLUMNS, null, null, null, null,
                    Entry.COLUMN_NAME_LAST_ACCESS + " DESC",
                    "100");
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }
}
