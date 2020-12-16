package com.example.cfsudpsms;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import cf.examples.mysever;

public class MainActivity extends AppCompatActivity {

    private boolean isStarted = false;
    private EditText editPhoneNumber;
    private EditText editPort;
    private mysever coapsever;
    private  final int REQUEST_EXTERNAL_STORAGE = 1;
    private  String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    public  void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        verifyStoragePermissions(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS} , 1);

        editPhoneNumber = (EditText)findViewById(R.id.PhoneNumber);
        editPort = (EditText)findViewById(R.id.port);
        Button btnCoapServer = (Button)findViewById(R.id.CoapServer);
        btnCoapServer.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (!isStarted) {
                    coapsever = new mysever(MainActivity.this,editPhoneNumber.getText().toString(),editPort.getText().toString());
                    coapsever.startsever();
                    isStarted = true;
                    v.setEnabled(false);

                }
            }
        });
    }
    private abstract class Worker extends Thread {
        @Override
        public void run() {
            doWork();
        }
        protected abstract void doWork();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        coapsever.stopServer();
    }
}
