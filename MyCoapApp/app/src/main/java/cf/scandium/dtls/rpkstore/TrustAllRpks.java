/*******************************************************************************
 * Copyright (c) 2017 RISE SICS.
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
 *    Ludwig Seitz (RISE SICS) - initial creation
 ******************************************************************************/
package cf.scandium.dtls.rpkstore;

import cf.elements.auth.RawPublicKeyIdentity;
import cf.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;

/**
 * A basic raw public key trust store that trusts all raw public keys.
 * 
 * @author Ludwig Seitz
 *
 * @deprecated use {@link StaticNewAdvancedCertificateVerifier} instead
 */
@Deprecated
public class TrustAllRpks implements TrustedRpkStore {

	@Override
	public boolean isTrusted(RawPublicKeyIdentity id) {
		return true;
	}

}
