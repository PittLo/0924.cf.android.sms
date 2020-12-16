package cf.examples;

import android.app.Activity;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.cert.Certificate;
import java.util.logging.Level;

import cf.core.CoapClient;
import cf.core.CoapResponse;
import cf.core.Utils;
import cf.core.network.CoapEndpoint;
import cf.elements.Connector;
import cf.elements.RawData;
import cf.elements.RawDataChannel;
import cf.elements.exception.ConnectorException;
import cf.elements.util.SslContextUtil;
import cf.scandium.DTLSConnect;
import cf.scandium.ScandiumLogger;
import cf.scandium.config.DtlsConnectorConfig;
import cf.scandium.dtls.CertificateType;
import cf.scandium.dtls.pskstore.AdvancedMultiPskStore;
import cf.scandium.dtls.pskstore.StaticPskStore;
import cf.udp.UDPConnect;

public class myclient {
    static {
        ScandiumLogger.initialize();
        ScandiumLogger.setLevel(Level.FINE);
    }

    private CoapClient client;
    private UDPConnect udpconnect;
    private Activity activity;
    private String localPhoneNumber;
    private String localPort = "8094";


    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(myclient.class.getCanonicalName());
    // allows configuration via Californium.properties
    private static final int DEFAULT_PORT = 5684;
    private static final org.slf4j.Logger LOG = (org.slf4j.Logger) LoggerFactory
            .getLogger(myclient.class.getName());


    private static final boolean PSK_MODE = false;
    private static final boolean CERTIFICATE_MODE = true;
    private static final boolean RPK_MODE = false;
    public static final String PSK_IDENTITY = "password";
    public static final byte[] PSK_SECRET = "sesame".getBytes();
    private static final String TRUST_NAME = "root"; // null = loads all the certificates / root = pcs12
    private static final char[] TRUST_STORE_PASSWORD = "rootPass".toCharArray();
    private final static char[] KEY_STORE_PASSWORD = "endPass".toCharArray();
    private static final String KEY_STORE_LOCATION = "assets/certs/client.p12";
    private static final String TRUST_STORE_LOCATION = "assets/certs/trustStore.p12";


    public myclient(Activity activity, String localphoneNumber) {
        this.activity = activity;
        this.localPhoneNumber = localphoneNumber;
    }

    public void register() {
        client = new CoapClient();

        AdvancedMultiPskStore pskStore = new AdvancedMultiPskStore();
        pskStore.setKey("Client_identity", "secretPSK".getBytes());
        try {
            SslContextUtil.Credentials endpointCredentials = null;
            Certificate[] trustedCertificates = null;

            endpointCredentials = SslContextUtil.loadCredentials(
                    SslContextUtil.CLASSPATH_SCHEME + KEY_STORE_LOCATION, "client", KEY_STORE_PASSWORD,
                    KEY_STORE_PASSWORD);
            trustedCertificates = SslContextUtil.loadTrustedCertificates(
                    SslContextUtil.CLASSPATH_SCHEME + TRUST_STORE_LOCATION, TRUST_NAME, TRUST_STORE_PASSWORD);
            CoapEndpoint.Builder dtlsEndpointBuilder = new CoapEndpoint.Builder();
            // setup coap EndpointManager to dtls connector
            DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
            if (PSK_MODE) {
                dtlsConfig.setPskStore(new StaticPskStore(PSK_IDENTITY, PSK_SECRET));
            } else if (CERTIFICATE_MODE) {
                dtlsConfig.setTrustStore(trustedCertificates);
                dtlsConfig.setIdentity(endpointCredentials.getPrivateKey(),
                        endpointCredentials.getCertificateChain(), CertificateType.X_509);
            } else if (RPK_MODE) {
                dtlsConfig.setIdentity(endpointCredentials.getPrivateKey(),
                        endpointCredentials.getCertificateChain(), CertificateType.RAW_PUBLIC_KEY);
            }


            //udpconnect = new UDPConnect(activity, localPort, localPhoneNumber);

            DTLSConnect dtlsConnect = new DTLSConnect(dtlsConfig.build(), activity, localPort, localPhoneNumber);
            dtlsConnect.setRawDataReceiver(new RawDataChannelImpl(dtlsConnect));

            CoapEndpoint.Builder udpEndpointBuilder = new CoapEndpoint.Builder();
            udpEndpointBuilder.setConnector(dtlsConnect);
            client.setEndpoint(udpEndpointBuilder.build());
            //EndpointManager.getEndpointManager().setDefaultEndpoint(udpEndpointBuilder.build());
            client.setTimeout((long) 300000);
            System.out.println("timeout:"+client.getTimeout());
        } catch ( Exception e) {
            System.err.println("Could not start coapclient");
            e.printStackTrace();
        }
    }

    public void send(String u) throws ConnectorException, IOException {
       // u= "coap://[3fff:0086:1881:1795:336f:fff0::]:8097/coapsmssever";
        CoapResponse response = null;
        //EndpointManager.getEndpointManager().setDefaultEndpoint(udpEndpointBuilder.build());
        client.setURI(u);
        response = client.get();

        if (response != null) {

            System.out.println(response.getCode());
            System.out.println(response.getOptions());
            System.out.println(response.getResponseText());

            System.out.println("\nADVANCED\n");
            System.out.println(Utils.prettyPrint(response));

        } else {
            System.out.println("No response received.");
        }
//        boolean p  = client.ping();
//        System.out.println(p);

    }

    private class RawDataChannelImpl implements RawDataChannel {

        private Connector connector;

        public RawDataChannelImpl(Connector con) {
            this.connector = con;
        }

        @Override
        public void receiveData(final RawData raw) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Received request: {}", new String(raw.getBytes()));
            }
            RawData response = RawData.outbound("ACK".getBytes(),
                    raw.getEndpointContext(), null, false);
            connector.send(response);
        }
    }
}
