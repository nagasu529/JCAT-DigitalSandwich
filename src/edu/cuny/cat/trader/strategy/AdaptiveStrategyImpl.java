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

package edu.cuny.cat.trader.strategy;

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.RoundClosedEvent;
import edu.cuny.cat.trader.AbstractTradingAgent;

/**
 * <p>
 * An abstract implementation of {@link AdaptiveStrategy} and
 * {@link FixedQuantityStrategy}.
 * </p>
 * 
 * @author Steve Phelps
 * @version $Revision: 1.12 $
 */

public abstract class AdaptiveStrategyImpl extends FixedQuantityStrategyImpl
		implements AdaptiveStrategy {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AdaptiveStrategyImpl(final AbstractTradingAgent agent) {
		super(agent);
	}

	public AdaptiveStrategyImpl() {
		super();
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		if (event instanceof RoundClosedEvent) {
			getLearner().monitor();
		}
	}
}