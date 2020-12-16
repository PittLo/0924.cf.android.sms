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
package cf.scandium.dtls.pskstore;

import java.net.InetSocketAddress;

import javax.crypto.SecretKey;

import cf.scandium.dtls.ConnectionId;
import cf.scandium.dtls.PskSecretResult;
import cf.scandium.dtls.PskSecretResultHandler;
import cf.scandium.dtls.PskPublicInformation;
import cf.scandium.dtls.cipher.PseudoRandomFunction;
import cf.scandium.dtls.cipher.ThreadLocalCryptoMap;
import cf.scandium.dtls.cipher.ThreadLocalCryptoMap.Factory;
import cf.scandium.dtls.cipher.ThreadLocalMac;
import cf.scandium.util.SecretUtil;
import cf.scandium.util.ServerNames;

/**
 * Simple in-memory example implementation of {@link AdvancedPskStore}.
 * 
 * Delegates calls to {@link PskStore}.
 * 
 * @since 2.3
 * @deprecated use {@link BridgePskStore} until migrated.
 */
@Deprecated
public class AdvancedInMemoryPskStore implements AdvancedPskStore {

	protected static final ThreadLocalCryptoMap<ThreadLocalMac> MAC = new ThreadLocalCryptoMap<>(
			new Factory<ThreadLocalMac>() {

				@Override
				public ThreadLocalMac getInstance(String algorithm) {
					return new ThreadLocalMac(algorithm);
				}
			});

	protected final PskStore pskStore;

	/**
	 * Create an advanced pskstore from {@link PskStore}.
	 * 
	 * @param pskStore psk store
	 * @throws NullPointerException if store is {@code null}
	 */
	public AdvancedInMemoryPskStore(PskStore pskStore) {
		if (pskStore == null) {
			throw new NullPointerException("PSK store must not be null!");
		}
		this.pskStore = pskStore;
	}

	@Override
	public boolean hasEcdhePskSupported() {
		return true;
	}

	@Override
	public PskSecretResult requestPskSecretResult(ConnectionId cid, ServerNames serverNames,
			PskPublicInformation identity, String hmacAlgorithm, SecretKey otherSecret, byte[] seed) {
		SecretKey secret = serverNames != null ? pskStore.getKey(serverNames, identity) : pskStore.getKey(identity);
		if (secret != null) {
			SecretKey masterSecret = generateMasterSecret(hmacAlgorithm, secret, otherSecret, seed);
			SecretUtil.destroy(secret);
			secret = masterSecret;
		}
		return new PskSecretResult(cid, identity, secret);
	}

	@Override
	public PskPublicInformation getIdentity(InetSocketAddress peerAddress, ServerNames virtualHost) {
		return virtualHost != null ? pskStore.getIdentity(peerAddress, virtualHost) : pskStore.getIdentity(peerAddress);
	}

	@Override
	public void setResultHandler(PskSecretResultHandler resultHandler) {
		// empty implementation
	}

	protected SecretKey generateMasterSecret(String hmacAlgorithm, SecretKey pskSecret, SecretKey otherSecret,
			byte[] seed) {
		ThreadLocalMac hmac = MAC.get(hmacAlgorithm);
		SecretKey premasterSecret = PseudoRandomFunction.generatePremasterSecretFromPSK(otherSecret, pskSecret);
		SecretKey masterSecret = PseudoRandomFunction.generateMasterSecret(hmac.current(), premasterSecret, seed);
		SecretUtil.destroy(premasterSecret);
		return masterSecret;
	}

}
