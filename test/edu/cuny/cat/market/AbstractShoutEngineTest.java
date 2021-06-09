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

package edu.cuny.cat.market;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.apache.log4j.Logger;

import edu.cuny.MyTestCase;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.market.matching.LazyMaxVolumeShoutEngine;
import edu.cuny.cat.market.matching.ShoutEngine;

/**
 * @author Jinzhong Niu
 * @version $Revision: 1.5 $
 */

public abstract class AbstractShoutEngineTest extends MyTestCase {

	protected static Logger logger = Logger
			.getLogger(AbstractShoutEngineTest.class);

	protected Random randGenerator;

	public AbstractShoutEngineTest(String name) {
		super(name);
	}

	@Override
	public void setUp() {
		super.setUp();
		randGenerator = new Random();
	}

	public Shout randomShout() {
		final int quantity = 1 + randGenerator.nextInt(50);
		final double price = (int) (randGenerator.nextDouble() * 100 + 1);
		final boolean isBid = randGenerator.nextBoolean();
		return new Shout(quantity, price, isBid);
	}

	public abstract ShoutEngine getShoutEngine();

	public void checkIterator(Iterator<Shout> itor, Comparator<Shout> comparator,
			String errMsg) {
		Shout prev = null;
		Shout current = null;
		while (itor.hasNext()) {
			current = itor.next();
			if (prev != null && comparator.compare(prev, current) > 0) {
				fail(errMsg + "\n" + prev.toPrettyString() + " vs. "
						+ current.toPrettyString());
				break;
			} else {
				prev = current;
			}
		}
	}

	protected void checkIterators() {
		System.out.println("\n>>>>>>>>>\t " + "checkIterators() \n");

		for (int num = 0; num < 100; num++) {
			try {
				getShoutEngine().newShout(randomShout());
			} catch (DuplicateShoutException e) {
				e.printStackTrace();
			}
		}

		checkIterator(getShoutEngine().ascendingAskIterator(),
				ShoutEngine.AscendingOrder, "ascending ask iterator not ascending!");
		checkIterator(getShoutEngine().descendingBidIterator(),
				ShoutEngine.DescendingOrder, "descending bid iterator not descending!");
	}

	/**
	 * a generic method to check if the matched bids and asks follow the specified
	 * orders.
	 * 
	 * @param ascendingBids
	 *          <code>true</code> if bids are expected to have ascending prices,
	 *          or <code>false</code> if descending.
	 * @param ascendingAsks
	 *          <code>true</code> if asks are expected to have ascending prices,
	 *          or <code>false</code> if descending.
	 */
	protected void checkOrderedMatches(boolean ascendingBids,
			boolean ascendingAsks) {
		System.out.println("\n>>>>>>>>>\t " + "checkOrderedMatches() \n");

		try {

			for (int shout = 0; shout < 200; shout++) {
				getShoutEngine().newShout(randomShout());
			}

			// check if bids and asks are in the specified order
			List<Shout> shouts = getShoutEngine().matchShouts();

			Shout preBid = null;
			Shout preAsk = null;
			Comparator<Shout> bidComparator = null;
			Comparator<Shout> askComparator = null;
			if (ascendingBids) {
				preBid = new Shout(1, Double.NEGATIVE_INFINITY, true);
				bidComparator = ShoutEngine.AscendingOrder;
			} else {
				preBid = new Shout(1, Double.POSITIVE_INFINITY, true);
				bidComparator = ShoutEngine.DescendingOrder;
			}
			if (ascendingAsks) {
				preAsk = new Shout(1, Double.NEGATIVE_INFINITY, false);
				askComparator = ShoutEngine.AscendingOrder;
			} else {
				preAsk = new Shout(1, Double.POSITIVE_INFINITY, false);
				askComparator = ShoutEngine.DescendingOrder;
			}

			Shout bid = null;
			Shout ask = null;
			for (int i = 0; i < shouts.size(); i += 2) {
				bid = shouts.get(i);
				if (bid.isBid()) {
					if (bidComparator.compare(preBid, bid) <= 0) {
						preBid.copyFrom(bid);
					} else {
						logger.warn(preBid.toPrettyString() + " vs. "
								+ bid.toPrettyString());
						logger.warn(bidComparator);
						fail("Matched bids are not in "
								+ (ascendingBids ? "ascending" : "descending")
								+ " order in list of matched shouts from "
								+ getShoutEngine().getClass().getSimpleName() + "!");
					}
				} else {
					fail("Bid expected in list of matched shouts from "
							+ getShoutEngine().getClass().getSimpleName() + "!");
				}

				ask = shouts.get(i + 1);
				if (ask.isAsk()) {
					if (askComparator.compare(preAsk, ask) <= 0) {
						preAsk.copyFrom(ask);
					} else {
						logger.warn(preAsk.toPrettyString() + " vs. "
								+ ask.toPrettyString());
						logger.warn(askComparator);
						fail("Matched asks are not in "
								+ (ascendingAsks ? "ascending" : "descending")
								+ " order in list of matched shouts from "
								+ getShoutEngine().getClass().getSimpleName() + "!");
					}
				} else {
					fail("Ask expected in list of matched shouts from "
							+ getShoutEngine().getClass().getSimpleName() + "!");
				}

				if (bid.getPrice() < ask.getPrice()) {
					fail("Invalid matched bid-ask pair in list of matched shouts from "
							+ LazyMaxVolumeShoutEngine.class.getSimpleName() + "!");
				}
			}
		} catch (final Exception e) {
			getShoutEngine().printState();
			e.printStackTrace();
			Assert.fail();
		}
	}

}
