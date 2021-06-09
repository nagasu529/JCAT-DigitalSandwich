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

package edu.cuny.cat.market.matching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.util.Utils;

/**
 * <p>
 * This class represents a continuum of shout matching policies, including
 * {@link FourHeapShoutEngine} and {@link LazyMaxVolumeShoutEngine}. It uses a
 * matching quantity coefficient, {@link #theta}, to determine a matching
 * quantity that falls between 0 and the maximal quantity calculated in
 * {@link LazyMaxVolumeShoutEngine}. When {@link #theta} is 1, the quantity is
 * the maximal; when {@link #theta} is 0, the quantity is the equilibrium
 * quantity; and when {@link #theta} is -1, no matches will be made.
 * </p>
 * 
 * <p>
 * <b>Parameters </b>
 * 
 * <table>
 * <tr>
 * <td valign=top><i>base </i> <tt>.theta</tt><br>
 * <font size=-1>-1 <= double <= 1 (0 by default)</font></td>
 * <td valign=top>(the matching quantity coefficient controlling the matching
 * quantity relative to the equilibrium quantity and the maximial quantity.)</td>
 * </tr>
 * 
 * <tr>
 * <td valign=top><i>base </i> <tt>.orderly</tt><br>
 * <font size=-1> boolean (<code>true</code> by default)</font></td>
 * <td valign=top>(whether to pair up matched shouts in the same order of price,
 * or favor most competitive shouts by pairing them up respectively from both
 * sides with the condition that the underlying shout set of the matching set is
 * guaranteed.)</td>
 * </tr>
 * 
 * </table>
 * 
 * <p>
 * <b>Default Base</b>
 * </p>
 * <table>
 * <tr>
 * <td valign=top><tt>theta_matching</tt><br>
 * </td>
 * </tr>
 * </table>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.4 $
 */

public class ThetaShoutEngine extends LazyMaxVolumeShoutEngine {

	static Logger logger = Logger.getLogger(ThetaShoutEngine.class);

	public static final String P_THETA = "theta";

	public static final String P_ORDERLY = "orderly";

	public static final String P_DEF_BASE = "theta_matching";

	/**
	 * by default, do maximal volume
	 */
	public static final double DEFAULT_THETA = 0;

	/**
	 * matching quantity coefficient
	 */
	protected double theta;

	/**
	 * whether to match bids and asks in totally ascending order, or favor the
	 * most competitive shouts to pair them up as in {@link FourHeapShoutEngine}.
	 */
	protected boolean orderly = true;

	public ThetaShoutEngine() {
		this(DEFAULT_THETA);
	}

	public ThetaShoutEngine(double theta) {
		this.theta = theta;

		validateTheta();
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		final Parameter defBase = new Parameter(ThetaShoutEngine.P_DEF_BASE);

		theta = parameters.getDoubleWithDefault(
				base.push(ThetaShoutEngine.P_THETA),
				defBase.push(ThetaShoutEngine.P_THETA), ThetaShoutEngine.DEFAULT_THETA);

		validateTheta();

		orderly = parameters.getBoolean(base.push(P_ORDERLY),
				defBase.push(P_ORDERLY), orderly);

	}

	protected void validateTheta() {
		if (theta < -1) {
			theta = -1;
		} else if (theta > 1) {
			theta = 1;
		}
	}

	/**
	 * 
	 * @return {@link #theta}
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * 
	 * @param theta
	 */
	public void setTheta(double theta) {
		this.theta = theta;
		validateTheta();

		updateMatchedShouts();
	}

	/**
	 * 
	 * @return {@link #orderly}
	 */
	public boolean getOrderly() {
		return orderly;
	}

	/**
	 * 
	 * @param orderly
	 */
	public void setOrderly(boolean orderly) {
		this.orderly = orderly;

		updateMatchedShouts();
	}

	/**
	 * @return the demand at the price of the lowest ask
	 */
	protected int getDemandAboveLowestAsk() {
		int q = 0;

		final ListIterator<Shout> askItor = asks.listIterator();
		final ListIterator<Shout> bidItor = bids.listIterator();

		Shout ask = null, bid = null;

		// 1. find lowest ask
		if (askItor.hasNext()) {
			ask = askItor.next();

			// 2. find lowest bid above the lowest ask
			while (true) {
				if (bidItor.hasNext()) {
					bid = bidItor.next();
					if (bid.getPrice() >= ask.getPrice()) {
						break;
					}
				} else {
					bid = null;
					break;
				}
			}

			while (bid != null) {
				q += bid.getQuantity();
				bid = bidItor.hasNext() ? bidItor.next() : null;
			}
		}

		return q;
	}

	/**
	 * @return the equilibrium quantity
	 */
	public int calculateEquilibriumQuantity() {
		int demand = getDemandAboveLowestAsk();
		int supply = 0;
		int qe = 0;

		final Iterator<Shout> askItor = asks.iterator();
		final Iterator<Shout> bidItor = bids.iterator();

		Shout ask = null, bid = null;

		// 1. find lowest ask
		if (askItor.hasNext()) {
			ask = askItor.next();

			// 2. find lowest bid above the lowest ask
			while (true) {
				if (bidItor.hasNext()) {
					bid = bidItor.next();
					if (bid.getPrice() >= ask.getPrice()) {
						break;
					}
				} else {
					bid = null;
					break;
				}
			}

			// 3. find the equilibrium quantity
			while (bid != null) {
				if ((ask != null) && (ask.getPrice() <= bid.getPrice())) {
					supply += ask.getQuantity();
					ask = askItor.hasNext() ? askItor.next() : null;
				} else {
					demand -= bid.getQuantity();
					bid = bidItor.hasNext() ? bidItor.next() : null;
				}

				qe = Math.max(qe, Math.min(demand, supply));
				if (qe > demand) {
					// already find the equilibrium point, break out.
					break;
				}
			}
		}

		return qe;
	}

	@Override
	public int calculateMatchingQuantity() {
		final int qmv = super.calculateMatchingQuantity();
		final int qe = calculateEquilibriumQuantity();

		if (qe > qmv) {
			ThetaShoutEngine.logger
					.error("The equilibrium quantity should NOT surpass the maxvolume-matching quantity !");
		}

		// calculate matching quantity based on theta
		int q = 0;
		if (theta <= 0) {
			q = (int) Math.round((1 + theta) * qe);

			if (q < 0) {
				q = 0;
			} else if (q > qe) {
				q = qe;
			}
		} else {
			q = (int) Math.round((1 - theta) * qe + theta * qmv);

			if (q < qe) {
				q = qe;
			} else if (q > qmv) {
				q = qmv;
			}
		}

		// check for rounding errors
		if (q < 0) {
			ThetaShoutEngine.logger.error("The quantity should NOT be negative !");
			q = 0;
		}

		if (q > qmv) {
			ThetaShoutEngine.logger
					.error("The quantity should NOT be larger than the maxvolume-matching quantity !");
			q = qmv;
		}

		return q;
	}

	/**
	 * 
	 * @return the quantity of goods that can be traded between the most
	 *         competitive bids and asks while guaranteeing the required trading
	 *         volume is met.
	 */
	protected int calculateUnorderlyQuantity() {

		if (matchingVolume == 0
				|| sIn.get(sIn.size() - 1).getPrice() <= bIn.get(0).getPrice()) {
			// a shortcut, avoiding traversing shouts all the way again
			return matchingVolume;
		}

		int ibIn = bIn.size() - 1;
		int isIn = sIn.size() - 1;
		int surplus = 0;
		int minSurplus = -1; // negative denotes still traversing bids higher than
													// highest ask

		while (ibIn >= 0) { // do not need to check isIn, as it should always get to
												// the end of bIn first
			if (bIn.get(ibIn).getPrice() >= sIn.get(isIn).getPrice()) {
				surplus += bIn.get(ibIn).getQuantity();
				ibIn--;
			} else {
				if (minSurplus < 0) {
					minSurplus = surplus;
				} else {
					surplus -= sIn.get(isIn).getQuantity();
					minSurplus = Math.min(minSurplus, surplus);
					isIn--;
				}
			}
		}

		return minSurplus;
	}

	/**
	 * overrides the way of pairing up matched bids and asks in the order of
	 * monotonically increasing price on both sides, and if {@link #orderly} is
	 * <code>false</code>, try best to pair up most competitive shouts on both
	 * sides so that they maintain their profit margins as in the traditional
	 * equilibrium matching.
	 */
	@Override
	public List<Shout> matchShouts() {
		if (orderly) {
			return super.matchShouts();
		}

		final ArrayList<Shout> result = new ArrayList<Shout>(sIn.size()
				+ bIn.size());
		int unorderlyQuantity = calculateUnorderlyQuantity();

		// match unorderly shouts
		int ibIn = bIn.size() - 1;
		int isIn = 0;
		int num = 0;
		while (unorderlyQuantity > 0) {
			num = Math.min(bIn.get(ibIn).getQuantity(), sIn.get(isIn).getQuantity());
			unorderlyQuantity -= num;

			if (bIn.get(ibIn).getQuantity() > num) {
				result.add(bIn.get(ibIn).split(num));
			} else {
				result.add(bIn.get(ibIn));
				bids.remove(bIn.get(ibIn));
				ibIn--;
			}

			if (sIn.get(isIn).getQuantity() > num) {
				result.add(sIn.get(isIn).split(num));
			} else {
				result.add(sIn.get(isIn));
				asks.remove(sIn.get(isIn));
				isIn++;
			}
		}

		// match the rest shouts
		ibIn = 0;
		while (isIn < sIn.size()) {
			num = Math.min(bIn.get(ibIn).getQuantity(), sIn.get(isIn).getQuantity());

			if (bIn.get(ibIn).getQuantity() > num) {
				result.add(bIn.get(ibIn).split(num));
			} else {
				result.add(bIn.get(ibIn));
				bids.remove(bIn.get(ibIn));
				ibIn++;
			}

			if (sIn.get(isIn).getQuantity() > num) {
				result.add(sIn.get(isIn).split(num));
			} else {
				result.add(sIn.get(isIn));
				asks.remove(sIn.get(isIn));
				isIn++;
			}
		}

		sIn.clear();
		bIn.clear();
		matchingVolume = 0;

		return result;

	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "\n" + Utils.indent(P_THETA + ":" + theta);
		s += "\n" + Utils.indent(P_ORDERLY + ":" + orderly);
		return s;
	}

}