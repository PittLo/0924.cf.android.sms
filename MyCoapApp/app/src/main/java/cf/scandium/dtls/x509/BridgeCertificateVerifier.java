/*******************************************************************************
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Bosch.IO GmbH - initial creation
 ******************************************************************************/
package cf.scandium.dtls.x509;

import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cf.elements.auth.RawPublicKeyIdentity;
import cf.elements.util.CertPathUtil;
import cf.elements.util.SslContextUtil;
import cf.scandium.dtls.AlertMessage;
import cf.scandium.dtls.AlertMessage.AlertDescription;
import cf.scandium.dtls.AlertMessage.AlertLevel;
import cf.scandium.dtls.CertificateMessage;
import cf.scandium.dtls.CertificateType;
import cf.scandium.dtls.CertificateVerificationResult;
import cf.scandium.dtls.ConnectionId;
import cf.scandium.dtls.DTLSSession;
import cf.scandium.dtls.HandshakeException;
import cf.scandium.dtls.HandshakeResultHandler;
import cf.scandium.dtls.rpkstore.InMemoryRpkTrustStore;
import cf.scandium.dtls.rpkstore.TrustAllRpks;
import cf.scandium.dtls.rpkstore.TrustedRpkStore;
import cf.scandium.util.ServerNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to use custom implementations of the deprecated certificate verifier
 * until having them migrated.
 * 
 * Delegates certificate verification to provided {@link CertificateVerifier}
 * and {@link TrustedRpkStore}.
 * 
 * @since 2.5
 */
@SuppressWarnings("deprecation")
public class BridgeCertificateVerifier implements NewAdvancedCertificateVerifier {

	protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

	/**
	 * x509 certificate verifier to delegate verification.
	 */
	private final CertificateVerifier x509verifier;

	/**
	 * RPK certificate verifier to delegate verification.
	 */
	private final TrustedRpkStore rpkVerifier;

	/**
	 * List of supported certificate type in order of preference.
	 */
	private final List<CertificateType> supportedCertificateTypes;

	/**
	 * Create delegating certificate verifier for x509 and RPK.
	 * 
	 * @param x509verifier x509 certificate verifier to delegate verification.
	 * @param rpkVerifier RPK certificate verifier to delegate verification.
	 * @param supportedCertificateTypes list of supported certificate type in
	 *            order of preference.
	 * @throws IllegalArgumentException if both verifier are {@code null}.
	 * @throws NullPointerException if the list of supported certificate types
	 *             is {@code null}
	 */
	protected BridgeCertificateVerifier(CertificateVerifier x509verifier, TrustedRpkStore rpkVerifier,
			List<CertificateType> supportedCertificateTypes) {
		if (x509verifier == null && rpkVerifier == null) {
			throw new IllegalArgumentException("no verifier provided!");
		}
		if (supportedCertificateTypes == null) {
			throw new NullPointerException("list of supported certificate types must not be null!");
		}
		this.x509verifier = x509verifier;
		this.rpkVerifier = rpkVerifier;
		this.supportedCertificateTypes = supportedCertificateTypes;
	}

	@Override
	public List<CertificateType> getSupportedCertificateType() {
		return supportedCertificateTypes;
	}

	@Override
	public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName,
			Boolean clientUsage, boolean truncateCertificatePath, CertificateMessage message, DTLSSession session) {
		try {
			CertPath certChain = message.getCertificateChain();
			if (certChain == null) {
				if (rpkVerifier == null) {
					AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.INTERNAL_ERROR,
							session.getPeer());
					throw new HandshakeException("RPK verification not enabled!", alert);
				}
				PublicKey publicKey = message.getPublicKey();
				RawPublicKeyIdentity rpk = new RawPublicKeyIdentity(publicKey);
				if (!rpkVerifier.isTrusted(rpk)) {
					LOGGER.debug("Certificate validation failed: Raw public key is not trusted");
					AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
							session.getPeer());
					throw new HandshakeException("Raw public key is not trusted!", alert);
				}
				return new CertificateVerificationResult(cid, publicKey, null);
			} else {
				if (x509verifier == null) {
					AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.INTERNAL_ERROR,
							session.getPeer());
					throw new HandshakeException("x509 verification not enabled!", alert);
				}
				if (x509verifier instanceof AdvancedCertificateVerifier) {
					certChain = ((AdvancedCertificateVerifier) x509verifier).verifyCertificate(clientUsage,
							truncateCertificatePath, message, session);
				} else {
					if (clientUsage != null && !message.isEmpty()) {
						Certificate certificate = certChain.getCertificates().get(0);
						if (certificate instanceof X509Certificate) {
							if (!CertPathUtil.canBeUsedForAuthentication((X509Certificate) certificate, clientUsage)) {
								AlertMessage alert = new AlertMessage(AlertLevel.FATAL,
										AlertDescription.BAD_CERTIFICATE, session.getPeer());
								throw new HandshakeException("Key Usage doesn't match!", alert);
							}
						}
					}
					x509verifier.verifyCertificate(message, session);
				}
				return new CertificateVerificationResult(cid, certChain, null);
			}
		} catch (HandshakeException e) {
			LOGGER.debug("Certificate validation failed!", e);
			return new CertificateVerificationResult(cid, e, null);
		}
	}

	@Override
	public List<X509Certificate> getAcceptedIssuers() {
		return Arrays.asList(x509verifier.getAcceptedIssuers());
	}

	@Override
	public void setResultHandler(HandshakeResultHandler resultHandler) {
		// empty implementation
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		/**
		 * x509 certificate verifier to delegate verification.
		 */
		protected CertificateVerifier x509verifier;
		/**
		 * RPK certificate verifier to delegate verification.
		 */
		protected TrustedRpkStore rpkVerifier;
		/**
		 * List of supported certificate type in order of preference.
		 */
		protected List<CertificateType> supportedCertificateTypes;

		public Builder setCertificateVerifier(CertificateVerifier x509verifier) {
			this.x509verifier = x509verifier;
			return this;
		}

		public Builder setTrustedCertificates(Certificate[] trustedCertificates) {
			if (trustedCertificates == null) {
				this.x509verifier = null;
			} else if (trustedCertificates.length == 0) {
				this.x509verifier = new StaticCertificateVerifier(new X509Certificate[0]);
			} else {
				X509Certificate[] certificates = SslContextUtil.asX509Certificates(trustedCertificates);
				SslContextUtil.ensureUniqueCertificates(certificates);
				this.x509verifier = new StaticCertificateVerifier(certificates);
			}
			return this;
		}

		public Builder setTrustAllCertificates() {
			this.x509verifier = new StaticCertificateVerifier(new X509Certificate[0]);
			return this;
		}

		public Builder setTrustedRPKs(TrustedRpkStore rpkVerifier) {
			this.rpkVerifier = rpkVerifier;
			return this;
		}

		public Builder setTrustedRPKs(Set<RawPublicKeyIdentity> trustedRPKs) {
			this.rpkVerifier = new InMemoryRpkTrustStore(trustedRPKs);
			return this;
		}

		public Builder setTrustAllRPKs() {
			this.rpkVerifier = new TrustAllRpks();
			return this;
		}

		public NewAdvancedCertificateVerifier build() {
			init();
			return new BridgeCertificateVerifier(x509verifier, rpkVerifier, supportedCertificateTypes);
		}

		protected void init() {
			List<CertificateType> supported = new ArrayList<>();
			if (rpkVerifier != null) {
				supported.add(CertificateType.RAW_PUBLIC_KEY);
			}
			if (x509verifier != null) {
				supported.add(CertificateType.X_509);
			}
			supportedCertificateTypes = Collections.unmodifiableList(supported);
		}
	}
}
