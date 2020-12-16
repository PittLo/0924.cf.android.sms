package cf.udp;

import android.app.Activity;

import cf.sms.SmsHander;
import cf.sms.SmsSocket;
import cf.sms.SmsSocketAddress;

public class UserDatagramSocket {

    private Activity activity;
    private String filterPort;
    private SmsSocket smsSocket;
    private UdpHander udpHander;

    public UserDatagramSocket(Activity activity,String filterPort){
        this.activity = activity;
        this.filterPort = filterPort;
        this.smsSocket = new SmsSocket(this.activity,this.filterPort);
        System.out.println("短信注册成功");
    }

    public void start(){
        smsSocket.init();
        System.out.println("短信初始成功");
        smsSocket.setReceiverHander(new SmsHander() {
            @Override
            public void smsHandle(byte[] message, String phoneNumber) {
                UserDatagramPacket msg = new UserDatagramPacket(message);
                short port =msg.getPort();//需要回复的端点
                SmsSocketAddress add = new SmsSocketAddress(phoneNumber,port);//需要回复的地址
                msg.setSmsSocketAddress(add);
               // System.out.printl("不含UDP头数据："+new String(msg.getData())+"\n包含UDP头数据"+new String(msg.getBuf()));

                udpHander.udpHandle(msg);
            }
        });
    }

    public void send(UserDatagramPacket msg){
        smsSocket.sendBinary(msg.getSmsSocketAddress().getPhoneNumber(),msg.getPort(),msg.getBuf());
    }

    public void setUdpHander(UdpHander udpHander){
        this.udpHander = udpHander;
    }
    public void stop(){
        smsSocket.unInit();
    }
}
