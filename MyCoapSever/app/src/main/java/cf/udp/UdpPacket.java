package cf.udp;

import cf.sms.SmsSocketAddress;


public class UdpPacket {

    //接收方端口
    private short endport;
    //发送方端口
    private short localport;
    //udp报文长度 包含头部8子节和数据长度
    private short length;
    //校验和
    private short check;
    //数据
    private byte[] buf;

    //目的地址包含端口和手机号
    private SmsSocketAddress endAddress;
    //本地地址包含端口和手机号
    private SmsSocketAddress localAddress;

    //是否执行校验和
    private boolean ischeck ;


    public UdpPacket(byte[] buf, SmsSocketAddress endAddress, SmsSocketAddress localAddress, boolean isCheck){
        setEndport(endAddress.getPort());
        setLocalport(localAddress.getPort());
        setBuf(buf);
        setLength((short) buf.length);
        setEndAddress(endAddress);
        setLocalAddress(localAddress);
        setIscheck(isCheck);
        setCheck(countUdpcheck(this.ischeck));
    }

    public UdpPacket(byte[] buf, SmsSocketAddress endAddress, SmsSocketAddress localAddress){
        this(buf,endAddress,localAddress,false);
    }

    public UdpPacket(byte[] data){
        toUdp(data);
    }

    //返回字节流的数据报
    public byte[] tobyte(){
        byte[] res = new byte[(int) this.length];

        System.arraycopy(short2byte(endport), 0, res, 0, 2);
        System.arraycopy(short2byte(localport), 0, res, 2, 2);
        System.arraycopy(short2byte(length), 0, res, 4, 2);
        System.arraycopy(short2byte(check), 0, res, 6, 2);
        System.arraycopy(buf, 0, res, 8, buf.length);
        return res;
    }

    private void toUdp(byte[] data){
        byte[] tmpbyte1 = new byte[2];
        byte[] tmpbyte2 = new byte[2];
        byte[] tmpbyte3 = new byte[2];
        byte[] tmpbyte4 = new byte[2];
        byte[] tmpdata = new byte[data.length-8];
        System.arraycopy(data,0,tmpbyte1,0,2);
        setEndport(byte2short(tmpbyte1));
        System.arraycopy(data,2,tmpbyte2,0,2);
        setLocalport(byte2short(tmpbyte2));
        System.arraycopy(data,4,tmpbyte3,0,2);
        setLength(byte2short(tmpbyte3));
        System.arraycopy(data,6,tmpbyte4,0,2);
        setCheck(byte2short(tmpbyte4));
        if(data.length>8){
            System.arraycopy(data,8,tmpdata,0,data.length-8);
            setBuf(tmpdata);
        }
        else setBuf(new byte[0]);


    }
    //计算校验和
    //目前还有问题？？？？？？？？？？？？？？？？？？？？？？手机号 和 ipv6地址转换问题
    private short countUdpcheck(boolean ischeck){

        if(!ischeck) return (short) 0;

        short low  = (short) (getendIPNumber(getEndAddress()) & 0xffff);
        short high = (short) ((getendIPNumber(getEndAddress()) >> 16) & 0xffff);
        int tmphead = (short) (high & low);
        low  = (short) (getendIPNumber(getLocalAddress()) & 0xffff);
        high = (short) ((getendIPNumber(getLocalAddress()) >> 16) & 0xffff);
        tmphead =  tmphead &  high & low & 17 & getLength();

        int udphead = getEndport() & getLocalport() & getLength() ;

        int tmplength = getBufLength();
        byte[] tmpbuf= getBUf();
        int tmpres = 0;

        if(tmplength%2==1){
            tmpres += ((tmpbuf[tmplength-1]) & 0x000000ff) << 8;
            tmplength--;
        }

        while(tmplength>0){
            tmpres += ((tmpbuf[tmplength-2]) & 0x000000ff) << 8;
            tmpres += tmpbuf[tmplength-1];
            tmplength-=2;
        }

        return (short) (~(short) (((short)tmphead + (short) udphead + (short) tmpres)));

    }

    //将String类型的11位手机号转换为int类型的16字节ipv6数据
    private int getendIPNumber(SmsSocketAddress endAddress){

        //尚未实现
        int res = 0;
        return res;
    }



    private synchronized void setEndport(short endport) {
        this.endport =endport;
    }

    private synchronized void setLocalport(short localport) {this.localport = localport; }

    private synchronized void setBuf(byte[] buf) {this.buf = buf;}

    private synchronized void setLength(short length){ this.length = (short) (8+length); }

    private synchronized void setCheck(short check ) { this.check = check;}

    private synchronized void setIscheck(boolean ischeck) {
        this.ischeck = ischeck;
    }

    private synchronized void setEndAddress(SmsSocketAddress endAddress) { this.endAddress = endAddress; }

    private synchronized void setLocalAddress(SmsSocketAddress localAddress) { this.localAddress = localAddress; }

    //udp报文长度
    public synchronized short getLength() {
        return this.length;
    }

    private synchronized short getCheck() {
        return this.check;
    }

    public synchronized short getEndport() {
        return this.endport;
    }

    public synchronized short getLocalport(){
        return this.localport;
    }

    private synchronized int getBufLength() { return this.buf.length; }

    //不含udp头的数据
    public synchronized byte[] getBUf() { return this.buf; }

    private synchronized SmsSocketAddress getEndAddress() {
        return this.endAddress;
    }

    private synchronized SmsSocketAddress getLocalAddress() {
        return this.localAddress;
    }

    private byte[] short2byte(short s){
        byte[] b = new byte[2];
        for(int i = 0; i < 2; i++){
            int offset = 16 - (i+1)*8; //因为byte占1个字节，所以要计算偏移量
            b[i] = (byte)((s >> offset)&0xff); //把16位分为2个8位进行分别存储
        }
        return b;
    }

    private short byte2short(byte[] b){
        short l = 0;
        for (int i = 0; i < 2; i++) {
            l<<=8; //<<=和我们的 +=是一样的，意思就是 l = l << 8
            l |= (b[i] & 0xff); //和上面也是一样的  l = l | (b[i]&0xff)
        }
        return l;
    }

}
