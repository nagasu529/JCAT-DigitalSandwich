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

package edu.cuny.cat.market.charging;

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.AuctionEventListener;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.config.param.Parameterizable;

/**
 * <p>
 * detects whether traders are still exploring markets or not.
 * </p>
 * 
 * <p>
 * <b>Default Base</b>
 * </p>
 * 
 * <table>
 * <tr>
 * <td valign=top><tt>trader_exploring_monitor</tt></td>
 * </tr>
 * </table>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.6 $
 * 
 */

public abstract class TraderExploringMonitor implements AuctionEventListener,
		Parameterizable {

	public static final String P_DEF_BASE = "trader_exploring_monitor";

	@Override
	public abstract void setup(ParameterDatabase parameters, Parameter base);

	/**
	 * tells whether traders are estimated to be exploring or not.
	 * 
	 * @return true if traders are exploring; false otherwise
	 */
	public abstract boolean isExploring();

	/**
	 * @return a numeric value telling the level of traders' exploration.
	 */
	public abstract double getExploringFactor();

	@Override
	public abstract void eventOccurred(AuctionEvent event);
}