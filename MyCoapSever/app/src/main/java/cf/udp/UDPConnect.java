package cf.udp;

import android.app.Activity;
import android.provider.Telephony;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import cf.elements.Connector;
import cf.elements.EndpointContext;
import cf.elements.EndpointContextMatcher;
import cf.elements.RawData;
import cf.elements.RawDataChannel;
import cf.elements.UdpEndpointContext;
import cf.elements.util.ClockUtil;
import cf.scandium.dtls.HandshakeException;
import cf.sms.SmsSocketAddress;

public class UDPConnect implements Connector {

    public static final Logger LOGGER = LoggerFactory.getLogger(UDPConnect.class);

    private UserDatagramSocket userDatagramSocket;

    /** A queue for buffering outgoing messages */
    private final BlockingQueue<UserDatagramPacket> outboundMessages = new LinkedBlockingQueue<>();

    private final BlockingQueue<UserDatagramPacket> inboundMessages  = new LinkedBlockingQueue<>();

    /** The thread that receives messages */
    private Worker receiver;

    /** The thread that sends messages */
    private Worker sender;

    protected boolean multicast;

    private boolean running;

    private volatile EndpointContextMatcher endpointContextMatcher;


    private RawDataChannel messageHandler;

    private InetSocketAddress inetSocketAddress;

    private String filePort;

    private String PhoneNumber;

    private boolean closed = false;

    private Object closeLock = new Object();


    public UDPConnect(Activity activity, String filterPort,String phoneNumber){

        inetSocketAddress = new InetSocketAddress("127.0.0.1", Integer.parseInt(filterPort));
        this.userDatagramSocket = new UserDatagramSocket(activity,filterPort);
        this.filePort = filterPort;
        this.PhoneNumber = phoneNumber;
            userDatagramSocket.setUdpHander(new UdpHander() {
                @Override
                public void udpHandle(UserDatagramPacket msg) {
                    boolean queueFull = !inboundMessages.offer(msg);
                    if (queueFull) {
                        LOGGER.warn( "Inbound message queue is full! Dropping outbound message to peer [{0}]",
                                msg.getSmsSocketAddress());
                    }
                }
            });
    }


    /**
     * Starts the connector.
     *
     * The connector might bind to a socket for instance.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public final synchronized void start() throws IOException {
        userDatagramSocket.start();
        running = true;
        sender = new Worker("UDP-Sender" ) {
            @Override
            public void doWork() throws Exception {
                sendNextMessageOverNetwork();
            }
        };

        receiver = new Worker("UDP-Receiver") {
            @Override
            public void doWork() throws Exception {
                receiveNextDatagramFromNetwork();
            }
        };
        receiver.start();
        sender.start();
        LOGGER.info(
                "UDP connector listening on [{0}] with MTU [{1}] using (inbound) datagram buffer size [{2} bytes]");
    }

    /**
     * Sends a raw message to a client via the network.
     *
     * This should be a non-blocking operation.
     *
     * @param msg the message to be sent
     */
    public void send(RawData msg){
        String tmpIP = msg.getEndpointContext().toString();
        System.out.println("EndIP:"+tmpIP);
        short port = (short) msg.getPort();
        //此处应该将IP号转换为手机号
        String tmpNumber = IPv62number(tmpIP);
        System.out.println("EndNumber:"+tmpNumber);
        SmsSocketAddress endadd = new SmsSocketAddress(tmpNumber,port);
        SmsSocketAddress localadd = new SmsSocketAddress(PhoneNumber,(short)String2int(filePort));
        send(msg,endadd,localadd);
    }

    public void send(RawData msg, SmsSocketAddress endaddress, SmsSocketAddress localaddress){
        if (msg == null) {
            throw new NullPointerException("Message must not be null");
        } else {
            UserDatagramPacket message = new UserDatagramPacket(msg.getBytes(),endaddress,localaddress);

            // 新版本需要实现EndpointContext
            EndpointContext destination = msg.getEndpointContext();
            InetSocketAddress destinationAddress = destination.getPeerAddress();
            EndpointContextMatcher endpointMatcher = UDPConnect.this.endpointContextMatcher;
            EndpointContext connectionContext = new UdpEndpointContext(destinationAddress);
            if (endpointMatcher != null && !endpointMatcher.isToBeSent(destination, connectionContext)) {
                LOGGER.warn("UDPSMSConnector ({}) drops {} bytes to {}:{}", inetSocketAddress, msg.getSize(),
                        destinationAddress.getAddress(), destinationAddress.getPort());
                return;
            }
            msg.onContextEstablished(connectionContext);

            boolean queueFull = !outboundMessages.offer(message);
            if (queueFull) {
                LOGGER.warn( "Outbound message queue is full! Dropping outbound message to peer [{0}]",
                        msg.getAddress());
            }
        }
    }

    private void receiveNextDatagramFromNetwork(){
        try {
            UserDatagramPacket message  = inboundMessages.take(); // Blocking
            System.out.println("对方手机号码："+message.getSmsSocketAddress().getPhoneNumber());
            System.out.println("对方端口："+message.getPort());
            System.out.println("UDP数据长度:"+message.getData().length);

            //此处应该将手机号转化为IP号
            String tmpIPv6 = Number2IPv6(message.getSmsSocketAddress().getPhoneNumber());
            final InetSocketAddress peer = new InetSocketAddress(tmpIPv6, message.getPort());
            long timestamp = ClockUtil.nanoRealtime();
            RawData msg = RawData.inbound(message.getData(),
                    new UdpEndpointContext(peer),
                    multicast, timestamp);
            System.out.println("Coap数据："+new String(msg.getBytes()));
            messageHandler.receiveData(msg);
        } catch (InterruptedException e) {
            // this means that the worker thread for sending
            // outbound messages has been interrupted, most
            // probably because the connector is shutting down
            Thread.currentThread().interrupt();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (HandshakeException e) {
            e.printStackTrace();
        }
    }

    //手机号转IP
    public  String Number2IPv6 (String Number){
        char[] NumberChar = Number.toCharArray();
        StringBuilder res = new StringBuilder("2fff");
        if(NumberChar.length == 0) return "2FFF:0:0:0:0:0:0:0";
        int l = 0;
        if(NumberChar[0] == '+'){
            res.append("00");
            l++;
        }
        for (int i = l;i<NumberChar.length;i++){
            if(NumberChar[i] != ' '){
                res.append(NumberChar[i]);
            }
        }
        res.append("ffff");
        int index = 32-res.length();
        while(index > 0){
            res.append("0");
            index--;
        }
        int h = 0;
        for(int k = 0;k<res.length();k++){
            if(k%5 == 4) res.insert(k,":");
            h++;
        }

        return res.toString();
    }

    //IP转手机号
    public  String IPv62number (String IPv6Address){
        char[] IPv6AddressChar = IPv6Address.toCharArray();
        StringBuilder res = new StringBuilder("");
        int index = IPv6Address.indexOf("2fff");
        if(index != -1){
            index = index+5;
            if(IPv6AddressChar[index]=='0' && IPv6AddressChar[index+1]=='0')  {
                res.append("+");
                index = index+2;
            }
            for(int i = index;i<IPv6Address.length();i++){
                if(IPv6AddressChar[i] == ':') continue;
                res.append(IPv6AddressChar[i]);
            }
            int end = res.indexOf("ffff");
            return res.substring(0,end);
        }
        return res.toString();
    }


    private int String2int(String num){
        int res;
        res = Integer.parseInt(num);
        return res;
    }

    private void sendNextMessageOverNetwork() {

        try {
            UserDatagramPacket msg = outboundMessages.take(); // Blocking
            sendMessage(msg);
        } catch (InterruptedException e) {
            // this means that the worker thread for sending
            // outbound messages has been interrupted, most
            // probably because the connector is shutting down
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage(final UserDatagramPacket message){

        if(message.getSmsSocketAddress() != null){
            userDatagramSocket.send(message);
            System.out.println("发送信息:"+message.getData()+"目的:"+message.getSmsSocketAddress().getPhoneNumber()+"端口:"+message.getPort());
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                LOGGER.info( "sleeping is interrupted...");
            }
        }
    }
    /**
     * Stops the sender and receiver threads and closes the socket
     * used for sending and receiving datagrams.
     */
    final synchronized void releaseSocket() {
        running = false;
        sender.interrupt();
        outboundMessages.clear();
        inboundMessages.clear();
    }

    /**
     * Stops the connector.
     *
     * All resources such as threads or bound sockets on network
     * interfaces should be stopped and released. A connector that has
     * been stopped using this method can be started again using the
     * {@link #start()} method.
     */
    public void stop(){
        if (!running) return;
        this.running = false;
        // stop all threads
        LOGGER.info( "Stopping UDP connector on [{0}]");
        userDatagramSocket.stop();
        releaseSocket();

    }


    /**
     * Stops the connector and cleans up any leftovers.
     *
     * A destroyed connector cannot be expected to be able to start again.
     */
    public void destroy(){
        stop();
    }



    /**
     * Sets the handler for incoming messages.
     *
     * The handler's {@link RawDataChannel#receiveData(RawData)} method
     * will be called whenever a new message from a client has been received
     * via the network.
     *
     * @param messageHandler the message handler
     */
    @Override
    public void setRawDataReceiver(RawDataChannel messageHandler){
        this.messageHandler = messageHandler;
    }

    @Override
    public void setEndpointContextMatcher(EndpointContextMatcher matcher) {

    }


    /**
     * Gets the address of the socket this connector is bound to.
     *
     * @return the address
     */
    public InetSocketAddress getAddress(){
        return this.inetSocketAddress;
    }

    @Override
    public String getProtocol() {
        return "UDP";
    }

    public String getPhoneNumber(){
        return this.PhoneNumber;
    }

    public String getFilePort(){
        return this.filePort;
    }

    public void setInetSocketAddress(InetSocketAddress inetSocketAddress){ this.inetSocketAddress = inetSocketAddress;}

    private abstract class Worker extends Thread {

        /**
         * Instantiates a new worker.
         *
         * @param name the name, e.g., of the transport protocol
         */
        private Worker(String name) {
            super(name);
        }

        @Override
        public void run() {
            try {
                LOGGER.info("Starting worker thread [{0}]", getName());
                while (running) {
                    try {
                        doWork();
                    }  catch (Exception e) {
                        if (running) {
                            LOGGER.info("Exception thrown by worker thread [" + getName() + "]", e);
                        }
                    }
                }
            } finally {
                LOGGER.info( "Worker thread [{0}] has terminated", getName());
            }
        }

        /**
         * Does the actual work.
         *
         * Subclasses should do the repetitive work here.
         *
         * @throws Exception if something goes wrong
         */
        protected abstract void doWork() throws Exception;
    }

    public boolean isClosed() {
        synchronized(closeLock) {
            return closed;
        }
    }
}
