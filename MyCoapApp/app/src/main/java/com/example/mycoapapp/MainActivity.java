package com.example.mycoapapp;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

import cf.elements.exception.ConnectorException;
import cf.examples.myclient;

public class MainActivity extends AppCompatActivity {


    private EditText URIText = null;
    private EditText serverPhoneText = null;
    private EditText localPhoneText= null;
    private myclient client;
    private boolean isStarted = false;
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

        this.URIText = (EditText)findViewById(R.id.url);
        this.localPhoneText = (EditText)findViewById(R.id.localphone);
        Button registerBtnre = (Button)findViewById(R.id.register);
        registerBtnre.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStarted) {
                client = new myclient(MainActivity.this,localPhoneText.getText().toString());
//                url = "coap://127.0.0.1:8097/coapsmssever";
                client.register();
                isStarted = true;
                v.setEnabled(false);
                }
            }
        });

        Button registerBtn = (Button)findViewById(R.id.send);
        registerBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = URIText.getText().toString();
                new Thread(new SendSecReqThread(url)).start();
            }
        });
    }
    private class SendSecReqThread implements Runnable {
        private String uri;

        public SendSecReqThread(String uri) {
            this.uri = uri;
        }
        @Override
        public void run() {
            try {
                client.send(uri);
            } catch (ConnectorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}




