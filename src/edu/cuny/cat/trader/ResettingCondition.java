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

package edu.cuny.cat.trader;

import java.util.Observable;

import org.apache.log4j.Logger;

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.AuctionEventListener;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.config.param.Parameterizable;
import edu.cuny.obj.Prototypeable;
import edu.cuny.obj.Resetable;

/**
 * specifies in which condition a trader should be reset to simulate fresh air
 * in market.
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.5 $
 */

public abstract class ResettingCondition extends Observable implements
		AuctionEventListener, Parameterizable, Prototypeable, Cloneable, Resetable {

	static Logger logger = Logger.getLogger(ResettingCondition.class);

	protected AbstractTradingAgent agent;

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		// do nothing
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		// do nothing
	}

	public void initialize() {
		// do nothing
	}

	@Override
	public void reset() {
		// do nothing
	}

	public AbstractTradingAgent getAgent() {
		return agent;
	}

	public void setAgent(final AbstractTradingAgent agent) {
		this.agent = agent;
	}

	@Override
	public Object protoClone() {
		return null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}