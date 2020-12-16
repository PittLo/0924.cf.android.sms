package cf.udp;

import java.net.InetSocketAddress;

import cf.elements.EndpointContext;
import cf.sms.SmsSocketAddress;

public class UserDatagramPacket {

    private byte[] buf;//udp包的字节流
    private UdpPacket udpPacket;
    private short port;//待发送
    private SmsSocketAddress smsSocketAddress;//待发送
    //数据长度
    private int length;
    private InetSocketAddress inetSocketAddress;

    /**
     * Endpoint context of the remote peer.
     */
    private EndpointContext peerEndpointContext;

    //接受使用
    public UserDatagramPacket(byte[] data){
        setUdpPacket(data);
        setBuf(data);
        setLength();
        this.port = getUdpPacket().getLocalport(); //待回复的端点
        this.smsSocketAddress = null; // 待回复的地址
        this.inetSocketAddress = null;
    }

    //发送使用
    public UserDatagramPacket(byte[] buf, SmsSocketAddress endAddress, SmsSocketAddress localAddress){
        setUdpPacket(buf,endAddress,localAddress);
        setBuf(this.getUdpPacket().tobyte());
        setLength();
        this.port = endAddress.getPort();
        this.smsSocketAddress = endAddress;
        this.inetSocketAddress = null;
    }
    //获得包含协议头的UDP报文字节流
    public synchronized byte[] getBuf(){
        return this.buf;
    }
    public synchronized SmsSocketAddress getSmsSocketAddress(){
        return this.smsSocketAddress;
    }
    public synchronized  short getPort(){
        return this.port;
    }
    //获得实际数据，不包含udp的数据头
    public synchronized byte[] getData(){
        return this.getUdpPacket().getBUf();
    }
    //获得实际数据长度
    public synchronized int getLength(){return this.length;}

    public EndpointContext getEndpointContext() {
        return peerEndpointContext;
    }

    public synchronized void setEndpointContext(EndpointContext newPeerEndpointContext) {
        this.peerEndpointContext = newPeerEndpointContext;
    }

    //接受
    private synchronized void setUdpPacket(byte[] data){
        this.udpPacket = new UdpPacket(data);
    }
    //发送
    private synchronized void setUdpPacket(byte[] buf, SmsSocketAddress endAddress, SmsSocketAddress localAddress){
        this.udpPacket = new UdpPacket(buf,endAddress,localAddress);
    }

    public synchronized void setLength(){this.length = getData().length;}


    public synchronized void setBuf(byte[] buf){
        this.buf = buf;
    }
    public synchronized void setPort(short port){ this.port = port;}

    public synchronized void setSmsSocketAddress(SmsSocketAddress smsSocketAddress){ this.smsSocketAddress = smsSocketAddress;}

    private synchronized UdpPacket getUdpPacket(){
        return this.udpPacket;
    }

    public synchronized void setInetSocketAddress(InetSocketAddress inetSocketAddress){this.inetSocketAddress = inetSocketAddress;}

    public InetSocketAddress getInetSocketAddress() {
        return inetSocketAddress;
    }
}
