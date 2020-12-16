package cf.sms;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;


public class SmsSocket {
    private static final String SENT_SMS_ACTION = "com.paad.smssnippets.SENT_SMS_ACTION";
    private static final String DELIVERED_SMS_ACTION = "com.paad.smssnippets.DELIVERED_SMS_ACTION";
    private static final String SMS_BINARY_RECEIVED = "android.intent.action.DATA_SMS_RECEIVED";
    private Activity activity;
    private SmsHander smsHander;
    private String filterPort;

    public SmsSocket(Activity activity, String filterPort) {
        this.activity = activity;
        this.filterPort = filterPort;
    }

    public void init() {
        // 动态注册短信广播接收器，可使用静态注册方式即在AndroidManifest.xml中注明
        activity.registerReceiver(sendReceiver, new IntentFilter(SENT_SMS_ACTION));
        activity.registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED_SMS_ACTION));

        IntentFilter filter = new IntentFilter(SMS_BINARY_RECEIVED);
        filter.addDataAuthority("localhost", filterPort);
        filter.addDataScheme("sms");
        activity.registerReceiver(binarySMSReceiver, filter);
        System.out.println("注册端口："+filterPort);
        ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.RECEIVE_SMS} , 2);


    }
    public void unInit() {
        activity.unregisterReceiver(sendReceiver);
        activity.unregisterReceiver(deliveredReceiver);
        activity.unregisterReceiver(binarySMSReceiver);
    }

    public void setFilterPort(String filterPort) {
        this.filterPort = filterPort;
    }

    public void sendBinary(String phoneNumber, short port, byte[] message) {
        SmsManager manager = SmsManager.getDefault();
        PendingIntent piSend = PendingIntent.getBroadcast(activity, 0, new Intent(SENT_SMS_ACTION), 0);
        PendingIntent piDelivered = PendingIntent.getBroadcast(activity, 0, new Intent(DELIVERED_SMS_ACTION), 0);
        //String msg = "hello,coap.This is a test message.if u can receive,dont repely.";
        System.out.println("短信长度:"+message.length);
        manager.sendDataMessage(phoneNumber, null, port, message ,piSend, piDelivered);
        //manager.sendTextMessage(phoneNumber,null,"This is a test message.",piSend, piDelivered);
    }

    public void setReceiverHander(SmsHander smsHander) {
        this.smsHander = smsHander;
    }

    private BroadcastReceiver sendReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String resultText = "UNKNOWN";

            switch(getResultCode()) {
                case Activity.RESULT_OK:
                    resultText = "Transmission successful"; break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    resultText = "Transmission failed"; break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    resultText = "Transmission failed: Radio is off"; break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    resultText = "Transmission failed: No PDU specified"; break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    resultText = "Transmission failed: No service"; break;
            }

            System.out.println("client:"+resultText);
            Toast.makeText(context, resultText, Toast.LENGTH_SHORT).show();
        }

    };

    private BroadcastReceiver deliveredReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String info = "Delivery information: ";

            switch(getResultCode())
            {
                case Activity.RESULT_OK: info += "delivered"; break;
                case Activity.RESULT_CANCELED: info += "not delivered"; break;
            }

            System.out.println("client:"+info);
            Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
        }
    };

    private BroadcastReceiver binarySMSReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Bundle bundle = intent.getExtras();
            String format = intent.getStringExtra("format");
            SmsMessage[] msgs;
            if(null != bundle) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (null != pdus) {
                    msgs = new SmsMessage[pdus.length];
                    byte[] data;
                    String phonenumber;

                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                        byte[] pdu = msgs[i].getPdu();
                        data = msgs[i].getUserData();
                        phonenumber = msgs[i].getDisplayOriginatingAddress();
                        Toast.makeText(context, new String(data), Toast.LENGTH_SHORT).show();
                        smsHander.smsHandle(data,phonenumber);
                    }
                }
            }
        }
    };
}