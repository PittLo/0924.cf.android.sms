package cf.sms;

public class SmsSocketAddress {
    private String phoneNumber;
    private short port;
    public SmsSocketAddress(String phoneNumber, short port) {
        this.phoneNumber = phoneNumber;
        this.port = port;
    }
    public String getPhoneNumber() { return phoneNumber; }

    public short getPort() { return port; }
}