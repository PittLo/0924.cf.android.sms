package cf.sms;

public interface SmsHander {
    void smsHandle(byte[] message, String phoneNumber);
}
