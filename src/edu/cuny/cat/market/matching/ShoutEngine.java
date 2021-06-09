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

package edu.cuny.cat.market.matching;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections15.buffer.PriorityBuffer;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.market.AuctioneerPolicy;
import edu.cuny.cat.market.DuplicateShoutException;

/**
 * a framework of a shout processing unit for a specialist.
 * 
 * @author Steve Phelps
 * @version $Revision: 1.13 $
 */

public abstract class ShoutEngine extends AuctioneerPolicy {

	/**
	 * comparator for ascending shouts
	 */
	public static AscendingShoutComparator AscendingOrder = new AscendingShoutComparator();

	/**
	 * comparator for descending shouts
	 */
	public static DescendingShoutComparator DescendingOrder = new DescendingShoutComparator();

	public abstract void newShout(Shout shout) throws DuplicateShoutException;

	public abstract void removeShout(Shout shout);

	/**
	 * Log the current status of the auction.
	 */
	public abstract void printState();

	/**
	 * @deprecated replaced by {@link #matchShouts()}.
	 */
	@Deprecated
	public List<Shout> getMatchedShouts() {
		return matchShouts();
	}

	/**
	 * <p>
	 * Destructively fetch the list of matched bids and asks. The list is of the
	 * form
	 * </p>
	 * <br>
	 * ( b0, a0, b1, a1 .. bn, an )<br>
	 * <p>
	 * where bi is the ith bid and a0 is the ith ask. A typical auctioneer would
	 * clear by matching bi with ai for all i at some price.
	 * </p>
	 * <p>
	 * Note that the engine's set of matched shouts will become empty as a result
	 * of invoking this method.
	 * </p>
	 */
	public abstract List<Shout> matchShouts();

	/**
	 * Get the highest unmatched bid in the auction.
	 */
	public abstract Shout getHighestUnmatchedBid();

	/**
	 * Get the lowest matched bid in the auction.
	 */
	public abstract Shout getLowestMatchedBid();

	/**
	 * Get the lowest unmatched ask.
	 */
	public abstract Shout getLowestUnmatchedAsk();

	/**
	 * Get the highest matched ask.
	 */
	public abstract Shout getHighestMatchedAsk();

	/**
	 * @return an iterator that non-destructively iterates over every ask in the
	 *         auction (both matched and unmatched).
	 */
	public abstract Iterator<Shout> askIterator();

	/**
	 * 
	 * @return an iterator that non-destructively iterates over every ask in the
	 *         auction (both matched and unmatched) in ascending order
	 */
	public abstract Iterator<Shout> ascendingAskIterator();

	/**
	 * @return an iterator that non-destructively iterates over every matched ask
	 *         in the auction in ascending order.
	 */
	public abstract Iterator<Shout> matchedAskIterator();

	/**
	 * @return an iterator that non-destructively iterates over every bid in the
	 *         auction (both matched and unmatched).
	 */
	public abstract Iterator<Shout> bidIterator();

	/**
	 * @return an iterator that non-destructively iterates over every bid in the
	 *         auction (both matched and unmatched) in descending order.
	 */
	public abstract Iterator<Shout> descendingBidIterator();

	/**
	 * @return an iterator that non-destructively iterates over every matched bid
	 *         in the auction in descending order.
	 */
	public abstract Iterator<Shout> matchedBidIterator();

	public abstract int getMatchedVolume();

	public abstract int getUnmatchedSupply();

	public abstract int getUnmatchedDemand();

	public abstract int getNumOfUnmatchedBids();

	public abstract int getNumOfUnmatchedAsks();

	public abstract int getNumOfMatchedBids();

	public abstract int getNumOfMatchedAsks();

	public abstract PriorityBuffer<Shout> getMatchedBids();

	public abstract PriorityBuffer<Shout> getUnmatchedBids();

	public abstract PriorityBuffer<Shout> getMatchedAsks();

	public abstract PriorityBuffer<Shout> getUnmatchedAsks();
}
