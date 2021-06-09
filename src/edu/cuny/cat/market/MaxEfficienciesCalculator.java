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

package edu.cuny.cat.market;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.market.matching.ThetaShoutEngine;

/**
 * A class calculating the maximal allocative efficiency given any possible
 * trading volume in an auction, based on a given {@link ThetaShoutEngine}
 * instance.
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.10 $
 */

public class MaxEfficienciesCalculator {

	protected ThetaShoutEngine shoutEngine;

	protected List<Double> efficiencies;

	Logger logger = Logger.getLogger(MaxEfficienciesCalculator.class);

	public MaxEfficienciesCalculator(final ThetaShoutEngine shoutEngine) {
		this.shoutEngine = shoutEngine;

		calculateEfficiencies();
	}

	/**
	 * calculates maximal efficiencies at each possible trading volume.
	 */
	public void calculateEfficiencies() {
		int qmv = shoutEngine.calculateMatchingQuantity();
		int qe = shoutEngine.calculateEquilibriumQuantity();

		Iterator<Shout> asks = shoutEngine.ascendingAskIterator();
		Iterator<Shout> bids = shoutEngine.descendingBidIterator();

		ArrayList<Double> profits = new ArrayList<Double>(qmv);

		double profit = 0.0;
		double maxProfit = Double.NEGATIVE_INFINITY;
		Shout ask = new Shout(0, Double.NaN, false);
		Shout bid = new Shout(0, Double.NaN, true);
		for (int i = 1; i <= qmv; i++) {
			if (ask.getQuantity() == 0) {
				ask.copyFrom(asks.next());
				// logger.info("processing ask: " + ask.toPrettyString());
			}

			if (bid.getQuantity() == 0) {
				bid.copyFrom(bids.next());
				// logger.info("processing bid: " + bid.toPrettyString());
			}

			profit += bid.getPrice() - ask.getPrice();
			ask.setQuantity(ask.getQuantity() - 1);
			bid.setQuantity(bid.getQuantity() - 1);

			// logger.info("profit: " + profit);

			if (profit > maxProfit) {
				maxProfit = profit;
			}

			profits.add(profit);
		}

		if (maxProfit != profits.get(qe - 1)) {
			logger
					.error("Theoretical profit unexpected higher than the equilibrium profit at non-equilibrium trading volumes!");
			return;
		}

		// normalize into efficiencies
		if (maxProfit > 0) {
			for (int i = 0; i < profits.size(); i++) {
				profits.set(i, (profits.get(i) / maxProfit) * 100);
			}
		} else {
			// zero profit but positive trading volume, indicating barely touching
			// supply and demand schedules
			for (int i = 0; i < profits.size(); i++) {
				profits.set(i, 100.0d);
			}
		}

		efficiencies = profits;
	}

	public List<Double> getEfficiencies() {
		return efficiencies;
	}
}
