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
/*
 * JASA Java Auction Simulator API
 * Copyright (C) 2001-2005 Steve Phelps
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

package edu.cuny.cat.event;

import edu.cuny.cat.core.Transaction;

/**
 * An event that is fired to notify subscribers of a transaction. In contrast,
 * {@link TransactionExecutedEvent} is used to tell a market that a transaction
 * is made in the market.
 * 
 * @see TransactionExecutedEvent
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.2 $
 */

public class TransactionPostedEvent extends AuctionEvent implements Cloneable {

	protected Transaction transaction;

	public TransactionPostedEvent(final Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		TransactionPostedEvent event = null;
		event = (TransactionPostedEvent) super.clone();

		if (event.transaction != null) {
			event.transaction = (Transaction) event.transaction.clone();
		}

		return event;
	}

	public Transaction getTransaction() {
		return transaction;
	}
}
