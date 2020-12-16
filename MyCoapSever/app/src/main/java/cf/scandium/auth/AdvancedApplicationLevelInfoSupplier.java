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
package cf.scandium.auth;

import java.security.Principal;

import cf.elements.auth.AdditionalInfo;
import cf.elements.util.PublicAPIExtension;
import cf.scandium.dtls.PskSecretResult;
import cf.scandium.dtls.pskstore.AdvancedPskStore;

/**
 * A strategy for retrieving additional (application level) information about an
 * authenticated peer. Supports an optional custom argument, currently available
 * for {@link AdvancedPskStore} in the {@link PskSecretResult}.
 * 
 * @since 2.3
 */
@PublicAPIExtension(type = ApplicationLevelInfoSupplier.class)
public interface AdvancedApplicationLevelInfoSupplier {

	/**
	 * Gets additional information about an authenticated peer.
	 * 
	 * @param peerIdentity The peer identity.
	 * @param customArgument a custom argument.
	 * @return The additional information about the peer.
	 */
	AdditionalInfo getInfo(Principal peerIdentity, Object customArgument);
}
