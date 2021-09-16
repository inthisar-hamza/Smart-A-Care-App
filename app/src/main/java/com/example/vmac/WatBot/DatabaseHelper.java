package com.example.vmac.WatBot;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import android.content.Intent;
import android.view.View;

import javax.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    public static final String DATABASE_NAME="new.db";
    public static final String TABLE_NAME="User";
    public static final String COL_1="email";
    public static final String COL_2="password";

    private Context context;

    public  DatabaseHelper(@Nullable Context context ){
        super(context, "new.db", null, 21 );

    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists User (id integer primary key autoincrement, email text, password text, date text)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("drop table if exists User");
        onCreate(db);
    }




}
