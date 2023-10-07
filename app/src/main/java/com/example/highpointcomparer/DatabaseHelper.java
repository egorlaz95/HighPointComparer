package com.example.highpointcomparer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SearchHistory.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SEARCH_HISTORY = "search_history";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CITY_NAME = "city_name";
    public static final String COLUMN_ELEVATION = "elevation";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_SEARCH_HISTORY + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CITY_NAME + " TEXT, " +
                    COLUMN_ELEVATION + " REAL);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCH_HISTORY);
        onCreate(db);
    }
}
