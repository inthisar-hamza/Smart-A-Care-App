package com.example.vmac.WatBot;

import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class ActivityHome extends AppCompatActivity {


    private EditText Email;
    private EditText Password;
    public EditText Date;
    private Button login;
    private Button goRegister;


    private SQLiteDatabase db;
    private SQLiteOpenHelper openHelper;
    private Cursor cursor;

    SharedPreferences sharedpreferences;

    public ActivityHome() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        getSupportActionBar().hide();


        openHelper = new DatabaseHelper(this);
        db = openHelper.getReadableDatabase();


        Email = findViewById(R.id.editTextEmail);
        Password = findViewById(R.id.editTextPassword);

        login = findViewById(R.id.loginbtn);
        goRegister = findViewById(R.id.goregister);



        sharedpreferences = getSharedPreferences("userData", Context.MODE_PRIVATE);
        String UserEmail = sharedpreferences.getString("Email", null);
        String DueD = sharedpreferences.getString("Date", null);
        if (UserEmail != null && DueD != null )
        {
            startActivity( new Intent(this, MainActivity.class));
        }



        login.setOnClickListener(v -> {

            String getEmail = Email.getText().toString();
            String getPassword = Password.getText().toString();

            if (getEmail.isEmpty() || getPassword.isEmpty()) {
                Toast.makeText(ActivityHome.this, "Enter your Email and Password to login", Toast.LENGTH_SHORT).show();
            }else{
                cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_NAME + " WHERE " + DatabaseHelper.COL_1 + "=? AND " +  DatabaseHelper.COL_2 + "=?", new String[]{getEmail, getPassword});
                if (cursor != null){
                    if( cursor.getCount() < 1 ) {
                        Toast.makeText(getApplicationContext(), "Incorrect Email/Password", Toast.LENGTH_SHORT).show();
                    }else{
                        while (cursor.moveToNext()){
                            String email = cursor.getString(1);
                            String password = cursor.getString(2);
                            String date = cursor.getString(3);
                            if(email.equals(getEmail) && password.equals(getPassword)){

                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString("Email", email);
                                editor.putString("Date", date);
                                editor.apply();

                                Toast.makeText(getApplicationContext(), "Login success", Toast.LENGTH_SHORT).show();
                                Email.setText("");
                                Password.setText("");

                                startActivity(new Intent(ActivityHome.this, MainActivity.class));


                            }else{
                                Toast.makeText(getApplicationContext(), "Incorrect Email/Password", Toast.LENGTH_SHORT).show();
                                Email.setText("");
                                Password.setText("");

                            }
                        }

                    }


                }else {
                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "Login failed");
                }

            }

        });




        goRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, Register_Activity.class);
            startActivity(intent);
        });

    }



    public void openRegisterPage(View view) {
        Intent intent = new Intent(this, Register_Activity.class);
        startActivity(intent);
    }


}

