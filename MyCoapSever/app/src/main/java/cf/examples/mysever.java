/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 ******************************************************************************/

package cf.examples;

import android.app.Activity;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import cf.core.CoapResource;
import cf.core.CoapServer;
import cf.core.coap.CoAP.ResponseCode;
import cf.core.network.CoapEndpoint;
import cf.core.server.resources.CoapExchange;
import cf.elements.Connector;
import cf.elements.RawData;
import cf.elements.RawDataChannel;
import cf.elements.util.SslContextUtil;
import cf.scandium.DTLSConnect;
import cf.scandium.ScandiumLogger;
import cf.scandium.config.DtlsConnectorConfig;
import cf.scandium.dtls.cipher.CipherSuite;
import cf.scandium.dtls.pskstore.AdvancedMultiPskStore;
import cf.scandium.dtls.CertificateType;
import cf.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import cf.udp.UDPConnect;

import cf.elements.util.SslContextUtil;
import cf.scandium.config.DtlsConnectorConfig;
import cf.scandium.dtls.CertificateType;
import cf.scandium.dtls.pskstore.StaticPskStore;

import org.slf4j.LoggerFactory;

public class mysever {

    static {
        ScandiumLogger.initialize();
        ScandiumLogger.setLevel(Level.FINER);
    }


    private CoapServer server;
    private Activity activity;
    private String filterPort;
    private String phoneNumber;


    private static final Logger LOGGER = Logger.getLogger(mysever.class.getCanonicalName());
    // allows configuration via Californium.properties
    private static final int DEFAULT_PORT = 5684;
    private static final org.slf4j.Logger LOG = (org.slf4j.Logger) LoggerFactory
            .getLogger(mysever.class.getName());


    private static final boolean PSK_MODE = false;
    private static final boolean CERTIFICATE_MODE = true;
    private static final boolean RPK_MODE = false;
    public static final String PSK_IDENTITY = "password";
    public static final byte[] PSK_SECRET = "sesame".getBytes();
    private static final String TRUST_NAME = "root"; // loads all the certificates
    private static final char[] TRUST_STORE_PASSWORD = "rootPass".toCharArray();
    private final static char[] KEY_STORE_PASSWORD = "endPass".toCharArray();
    private static final String KEY_STORE_LOCATION = "assets/certs/server.p12";
    private static final String TRUST_STORE_LOCATION = "assets/certs/trustStore.p12";

    public mysever(Activity activity,String phoneNumber, String port){
        this.activity = activity;
        this.phoneNumber = phoneNumber;
        this.filterPort = port;
    }
    public void startsever(){
        server = new CoapServer();
        server.add(new CoapResource("coapsmssever") {
            @Override
            public void handleGET(CoapExchange exchange) {
                exchange.respond(ResponseCode.CONTENT, "core,elements");
            }
        });
        server.add(new CoapResource("hi") {
            @Override
            public void handleGET(CoapExchange exchange) {
                exchange.respond(ResponseCode.CONTENT, "hello");
            }
        });
        server.add(new CoapResource("hello,world") {
            @Override
            public void handleGET(CoapExchange exchange) {
                exchange.respond(ResponseCode.CONTENT, "hi,world");
            }
        });
        SslContextUtil.Credentials endpointCredentials = null;
        Certificate[] trustedCertificates = null;
        try {
            endpointCredentials = SslContextUtil.loadCredentials(
                    SslContextUtil.CLASSPATH_SCHEME + KEY_STORE_LOCATION, "server", KEY_STORE_PASSWORD,
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
            

            DTLSConnect dtlsConnect = new DTLSConnect(dtlsConfig.build(), activity, filterPort, phoneNumber);
            dtlsConnect.setRawDataReceiver(new RawDataChannelImpl(dtlsConnect));
            //UDPConnect connector = new UDPConnect(activity, filterPort, phoneNumber);
            CoapEndpoint.Builder coapBuilder = new CoapEndpoint.Builder();
            coapBuilder.setConnector(dtlsConnect);
            server.addEndpoint(coapBuilder.build());
            server.start();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        server.stop();
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
