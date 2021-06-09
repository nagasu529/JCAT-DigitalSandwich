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

package edu.cuny.cat.market.accepting;

import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.IllegalShoutException;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.ShoutPlacedEvent;
import edu.cuny.cat.event.TransactionExecutedEvent;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.math.dist.CumulativeDistribution;
import edu.cuny.util.Utils;

/**
 * This accepting policy compares a shout with the average price of the existing
 * shouts on the same side and allows the shout to place only if it is no less
 * competitive than the average. It also uses a parameter, ratio, to loose or
 * tighten the restriction by adding the product of ratio and the variance to
 * the average. When ratio is positive, it looses the restriction, while when it
 * is negative, it tightens the restriction.
 * 
 * This policy should be distinguished from
 * {@link SlidingMatchedAverageBeatingAcceptingPolicy}.
 * {@link SlidingMatchedAverageBeatingAcceptingPolicy} averages prices of matched
 * shouts, while this policy averages prices of standing shouts.
 * 
 * 
 * <p>
 * <b>Parameters</b>
 * </p>
 * <table>
 * <tr>
 * <td valign=top><i>base</i><tt>.ratio</tt><br>
 * <font size=-1>double (0 by default)</font></td>
 * <td valign=top>(a factor to adjust the restriction threshold by how many
 * variances)</td>
 * </tr>
 * 
 * </table>
 * 
 * @author Jinzhong Niu
 * @version $Revision: 1.5 $
 */

public class StandingAverageBeatingAcceptingPolicy extends
		OnlyNewShoutDecidingAcceptingPolicy {

	static Logger logger = Logger
			.getLogger(StandingAverageBeatingAcceptingPolicy.class);

	public final static String P_RATIO = "ratio";

	protected ShoutAverageTracker askAverageTracker;

	protected ShoutAverageTracker bidAverageTracker;

	protected double ratio = 0.0;

	public StandingAverageBeatingAcceptingPolicy() {
		askAverageTracker = new ShoutAverageTracker(true);
		bidAverageTracker = new ShoutAverageTracker(false);
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);

		ratio = parameters.getDoubleWithDefault(base.push(P_RATIO), null, ratio);
	}

	@Override
	public void reset() {
		askAverageTracker.reset();
		bidAverageTracker.reset();
	}

	/**
	 * 
	 * @return ratio
	 */
	public double getRatio() {
		return ratio;
	}

	/**
	 * 
	 * @param ratio
	 */
	public void setRatio(double ratio) {
		this.ratio = ratio;
	}

	/**
	 * 
	 * @param isAsk
	 * @return the shout average tracker requested
	 */
	public ShoutAverageTracker getShoutAverage(boolean isAsk) {
		if (isAsk) {
			return askAverageTracker;
		} else {
			return bidAverageTracker;
		}
	}

	/**
	 * accepts all shouts that are no less competitive than the average of
	 * standing shouts from the same side, or {@link IllegalShoutException} is
	 * thrown otherwise.
	 * 
	 * @see edu.cuny.cat.market.accepting.OnlyNewShoutDecidingAcceptingPolicy#check(edu.cuny.cat.core.Shout)
	 */
	@Override
	public void check(final Shout shout) throws IllegalShoutException {
		ShoutAverageTracker avg = getShoutAverage(shout.isAsk());
		double value = avg.getValue();

		if (shout.isAsk()) {
			if (!Double.isNaN(value) && shout.getPrice() > value) {
				throw new IllegalShoutException("too high ask price !");
			}
		} else {
			if (!Double.isNaN(value) && shout.getPrice() < value) {
				throw new IllegalShoutException("too low bid price !");
			}
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if (event instanceof ShoutPlacedEvent) {
			getShoutAverage(((ShoutPlacedEvent) event).getShout().isAsk())
					.setUpdated(false);
		} else if (event instanceof TransactionExecutedEvent) {
			askAverageTracker.setUpdated(false);
			bidAverageTracker.setUpdated(false);
		}
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "\n" + Utils.indent(P_RATIO + ":" + ratio);
		return s;
	}

	/**
	 * tracks the average price of shouts on one side of the market. It updates in
	 * a lazy way by using a flag.
	 */
	protected class ShoutAverageTracker {

		protected CumulativeDistribution dist;

		protected boolean updated;

		protected boolean isAsk;

		protected double value;

		public ShoutAverageTracker(boolean isAsk) {
			this.isAsk = isAsk;
		}

		public void reset() {
			updated = false;
		}

		public boolean isUpdated() {
			return updated;
		}

		public void setUpdated(boolean updated) {
			this.updated = updated;
		}

		public double getValue() {
			if (!updated) {
				update();
				updated = true;
			}

			return value;
		}

		protected void update() {
			Iterator<Shout> itor = null;
			if (isAsk) {
				itor = auctioneer.askIterator();
			} else {
				itor = auctioneer.bidIterator();
			}

			if (dist == null) {
				dist = new CumulativeDistribution();
			} else {
				dist.reset();
			}

			Shout shout = null;
			while (itor.hasNext()) {
				shout = itor.next();
				dist.newData(shout.getPrice(), shout.getQuantity());
			}

			value = Double.NaN;
			if (dist.getN() > 0) {
				value = dist.getMean();

				// relax or tighten up a bit
				if (isAsk) {
					value += ratio * dist.getVariance();
				} else {
					value -= ratio * dist.getVariance();
				}
			}
		}
	}
}
