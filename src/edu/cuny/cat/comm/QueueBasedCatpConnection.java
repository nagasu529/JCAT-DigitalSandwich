/*
 * JCAT - TAC Market Design Competition Platform
 * Copyright (C) 2006-2013 Jinzhong Niu, Kai Cai
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package edu.cuny.cat.comm;

import org.apache.commons.collections15.Buffer;
import org.apache.commons.collections15.BufferUtils;
import org.apache.commons.collections15.buffer.UnboundedFifoBuffer;
import org.apache.log4j.Logger;

/**
 * <p>
 * The implementation of {@link Connection} when
 * {@link QueueBasedInfrastructureImpl} is used.
 * </p>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.12 $
 */

public class QueueBasedCatpConnection implements
		ProactiveConnection<CatpMessage> {

	static Logger logger = Logger.getLogger(QueueBasedCatpConnection.class);

	/**
	 * the {@link Connector} that requested to create this connection
	 */
	protected Object connector;

	/**
	 * the other side of the connection that may call
	 * {@link #relayMessage(CatpMessage)} to transmit messages.
	 */
	protected QueueBasedCatpConnection peer;

	/**
	 * the queue buffering incoming messages
	 */
	protected Buffer<CatpMessage> messages;

	/**
	 * an identifier for this connection
	 */
	protected String id;

	public QueueBasedCatpConnection(final Object connector, final String id) {
		this.connector = connector;
		this.id = id;
	}

	public Object getConnector() {
		return connector;
	}

	public void setConnector(final Object connector) {
		this.connector = connector;
	}

	public QueueBasedCatpConnection getPeer() {
		return peer;
	}

	public void setPeer(final QueueBasedCatpConnection peer) {
		this.peer = peer;
	}

	@Override
	public void sendMessage(final CatpMessage msg) throws CatException {
		if (isClosed()) {
			throw new ConnectionException(getClass().getSimpleName()
					+ " is closed or not open yet for writing !");
		}

		peer.relayMessage(msg);
	}

	public synchronized void relayMessage(final CatpMessage msg)
			throws CatException {
		if (messages == null) {
			try {
				wait();
			} catch (final InterruptedException e) {
				e.printStackTrace();
				throw new ConnectionException(getClass().getSimpleName()
						+ " is closed or not open yet for relaying !");
			}
		}

		synchronized (messages) {
			messages.add(msg);
			messages.notify();
		}
	}

	@Override
	public CatpMessage getMessage() throws CatException {
		if (isClosed()) {
			throw new ConnectionException(getClass().getSimpleName()
					+ " is closed or not open yet for reading !");
		}

		synchronized (messages) {
			if (messages.isEmpty()) {
				try {
					messages.wait();
				} catch (final InterruptedException e) {
					// thread waiting for messages interrupted, which is normal.
					return null;
				}
			}
			return messages.remove();
		}
	}

	@Override
	public synchronized void open() throws ConnectionException {
		messages = BufferUtils
				.synchronizedBuffer(new UnboundedFifoBuffer<CatpMessage>());
		notifyAll();
	}

	@Override
	public void close() throws ConnectionException {
		messages = null;
	}

	@Override
	public synchronized boolean isClosed() {
		return messages == null;
	}

	@Override
	public String getLocalAddressInfo() {
		return id + "_local";
	}

	@Override
	public String getRemoteAddressInfo() {
		return id + "_remote";
	}
}