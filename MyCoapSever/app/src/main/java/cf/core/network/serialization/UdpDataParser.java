/*******************************************************************************
 * Copyright (c) 2015, 2016 Institute for Pervasive Computing, ETH Zurich and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * <p>
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.html.
 * <p>
 * Contributors:
 * Matthias Kovatsch - creator and main architect
 * Martin Lanter - architect and re-implementation
 * Dominique Im Obersteg - parsers and initial implementation
 * Daniel Pauli - parsers and initial implementation
 * Kai Hudalla - logging
 * Bosch Software Innovations GmbH - introduce dedicated MessageFormatException
 * Joe Magerramov (Amazon Web Services) - CoAP over TCP support.
 * Achim Kraus (Bosch Software Innovations GmbH) - replace byte array token by Token
 ******************************************************************************/
package cf.core.network.serialization;

import cf.core.coap.CoAP;
import cf.core.coap.CoAPMessageFormatException;
import cf.core.coap.MessageFormatException;
import cf.core.coap.Token;
import cf.elements.util.DatagramReader;

import static cf.core.coap.CoAP.MessageFormat.CODE_BITS;
import static cf.core.coap.CoAP.MessageFormat.MESSAGE_ID_BITS;
import static cf.core.coap.CoAP.MessageFormat.TOKEN_LENGTH_BITS;
import static cf.core.coap.CoAP.MessageFormat.TYPE_BITS;
import static cf.core.coap.CoAP.MessageFormat.VERSION_BITS;

/**
 * A parser for messages encoded following the standard CoAP encoding.
 */
public final class UdpDataParser extends DataParser {

	@Override
	protected MessageHeader parseHeader(final DatagramReader reader) {
		if (!reader.bytesAvailable(4)) {
			throw new MessageFormatException(
					"UDP Message too short! " + (reader.bitsLeft() / Byte.SIZE) + " must be at least 4 bytes!");
		}
		int version = reader.read(VERSION_BITS);
		assertCorrectVersion(version);
		int type = reader.read(TYPE_BITS);
		int tokenLength = reader.read(TOKEN_LENGTH_BITS);
		assertValidTokenLength(tokenLength);
		int code = reader.read(CODE_BITS);
		int mid = reader.read(MESSAGE_ID_BITS);
		if (!reader.bytesAvailable(tokenLength)) {
			throw new CoAPMessageFormatException("UDP Message too short for token! " + (reader.bitsLeft() / Byte.SIZE)
					+ " must be at least " + tokenLength + " bytes!", null, mid, code, CoAP.Type.CON.value == type);
		}
		Token token = Token.fromProvider(reader.readBytes(tokenLength));

		return new MessageHeader(version, CoAP.Type.valueOf(type), token, code, mid, 0);
	}

	private void assertCorrectVersion(int version) {
		if (version != CoAP.VERSION) {
			throw new MessageFormatException("Message has invalid version: " + version);
		}
	}
}
