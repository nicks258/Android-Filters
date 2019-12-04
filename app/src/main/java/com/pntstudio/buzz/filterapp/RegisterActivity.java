package com.pntstudio.buzz.filterapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class RegisterActivity extends AppCompatActivity {
    String nameString,emailString,phoneNumberString;
    public static final String MyPREFERENCES = "MyPrefs" ;
    SharedPreferences sharedpreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_register);
        final EditText name = findViewById(R.id.name);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        ImageView openIpScreen = findViewById(R.id.logo);
        openIpScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent ipActivityIntent = new Intent(RegisterActivity.this,IPActivity.class);
                startActivity(ipActivityIntent);
            }
        });
        final EditText email = findViewById(R.id.email);
        final EditText phone = findViewById(R.id.phone_number);
        Button submit = findViewById(R.id.submit);
        final GetterClass getterClass = new GetterClass();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        , 2);
            }


        }
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameString = name.getText().toString();
                emailString = email.getText().toString();
                phoneNumberString = phone.getText().toString();
                getterClass.setEmail(emailString);
                getterClass.setName(nameString);
                getterClass.setPhone(phoneNumberString);
                Intent cameraActivityIntent = new Intent(RegisterActivity.this,CameraActivity.class);
                cameraActivityIntent.putExtra("name", nameString);
                cameraActivityIntent.putExtra("phone", phoneNumberString);
                cameraActivityIntent.putExtra("email", emailString);
                cameraActivityIntent.putExtra("data", getterClass);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("name", nameString);
                editor.putString("phone", phoneNumberString);
                editor.putString("email", emailString);
                editor.apply();
                startActivity(cameraActivityIntent);
                RegisterActivity.this.finish();
            }
        });
    }
}
