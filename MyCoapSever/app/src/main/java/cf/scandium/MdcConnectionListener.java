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
 *    Bosch IO.GmbH - initial creation
 ******************************************************************************/
package cf.scandium;

import org.slf4j.MDC;

import java.net.InetSocketAddress;

import cf.elements.util.StringUtil;
import cf.scandium.dtls.Connection;
import cf.scandium.dtls.ConnectionId;
import cf.scandium.dtls.DTLSSession;
import cf.scandium.dtls.SessionId;

/**
 * Setup the logging MDC with the connection state.
 * 
 * Set:
 * 
 * {@code "PEER"}
 * {@code "CONNECTION_ID"}
 * {@code "WRITE_CONNECTION_ID"}
 * {@code "SESSION_ID"}
 * 
 * @since 2.4
 */
public class MdcConnectionListener implements ConnectionExecutionListener, ConnectionListener {

	@Override
	public void onConnectionEstablished(Connection connection) {

	}

	@Override
	public void onConnectionRemoved(Connection connection) {

	}

	@Override
	public void beforeExecution(Connection connection) {
		InetSocketAddress peerAddress = connection.getPeerAddress();
		if (peerAddress != null) {
			MDC.put("PEER", StringUtil.toString(peerAddress));
		}
		ConnectionId cid = connection.getConnectionId();
		if (cid != null) {
			MDC.put("CONNECTION_ID", cid.getAsString());
		}
		SessionId sid = connection.getSessionIdentity();
		DTLSSession session = connection.getSession();
		if (session != null) {
			sid = session.getSessionIdentifier();
			ConnectionId writeConnectionId = session.getWriteConnectionId();
			if (writeConnectionId != null && !writeConnectionId.isEmpty()) {
				MDC.put("WRITE_CONNECTION_ID", writeConnectionId.getAsString());
			}
		}
		if (sid != null) {
			MDC.put("SESSION_ID", sid.toString());
		}
	}

	@Override
	public void updateExecution(Connection connection) {
		beforeExecution(connection);
	}

	@Override
	public void afterExecution(Connection connection) {
		MDC.clear();
	}

}
