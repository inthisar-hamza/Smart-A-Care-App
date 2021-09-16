package com.example.vmac.WatBot;


import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.xml.transform.Source;

public class Register_Activity extends AppCompatActivity {

    private EditText Email;
    private EditText Password;
    private EditText CfmPassword;
    public EditText Date;

    private Button registerBtn;

    DatePickerDialog datePickerDialog;

    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase db;


    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().hide();

        openHelper = new DatabaseHelper(this);

        Email = findViewById(R.id.rEmail);
        Password = findViewById(R.id.rPassword);
        CfmPassword = findViewById(R.id.rCpassword);
        Date = findViewById(R.id.rDuedate);
        Date.setFocusable(false);


        registerBtn = findViewById(R.id.Register_btn);
        registerBtn.setOnClickListener(v -> {
            db = openHelper.getWritableDatabase();


            String getEmail = Email.getText().toString();
            String getPassword = Password.getText().toString();
            String CPassword = CfmPassword.getText().toString();
            String getDate = Date.getText().toString();

            if(getEmail.isEmpty() ||  getPassword.isEmpty() ||  CPassword.isEmpty() ||  getDate.isEmpty()){
                Toast.makeText(Register_Activity.this, "Some Fields are Missing" , Toast.LENGTH_SHORT).show();
            }else{

                if(getPassword.equals(CPassword) ){

                    if(isValid(getEmail)){
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("email", getEmail);
                        contentValues.put("password",getPassword);
                        contentValues.put("date", getDate);

                        long id = db.insert(DatabaseHelper.TABLE_NAME,null,contentValues);
                        Toast.makeText(Register_Activity.this, "Registration Success" , Toast.LENGTH_SHORT).show();

                        Email.setText("");
                        Password.setText("");
                        CfmPassword.setText("");
                        Date.setText("");
                        Intent i = new Intent(this, ActivityHome.class);
                        startActivity(i);

                    } else{
                        Toast.makeText(Register_Activity.this, "Invalid Email" , Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(Register_Activity.this, "Please Check your Password" , Toast.LENGTH_SHORT).show();

                }



            }



        });


    }

    static boolean isValid(String email) {
        String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        return email.matches(regex);
    }

    private static String formatDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.YEAR, year);
        Date myDate = cal.getTime();

        String date=new SimpleDateFormat("dd-MMM-yyyy").format(myDate);


        return date;
    }

    public void DatePick(View v){
        final Calendar c = Calendar.getInstance();
        c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        int mYear = c.get(Calendar.YEAR); // current year
        int mMonth = c.get(Calendar.MONTH); // current month
        int mDay = c.get(Calendar.DAY_OF_MONTH); // current day
        // date picker dialog

        datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    // set day of month , month and year value in the edit text

                    Date.setText(dayOfMonth + "/"
                            + (monthOfYear + 1) + "/" + year);


                    String Dated = formatDate(year,monthOfYear,dayOfMonth);
                    Date.setText(Dated);

                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }







    public void openLoginPage(View view) {
        Intent intent = new Intent(this, ActivityHome.class);
        startActivity(intent);
    }
}