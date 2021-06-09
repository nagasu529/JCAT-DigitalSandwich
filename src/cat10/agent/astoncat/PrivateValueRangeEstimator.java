/*
 * AstonCat-Plus from CAT'10
 */

package cat10.agent.astoncat;

import org.apache.log4j.Logger;

import edu.cuny.ai.learning.SlidingWindowLearner;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.ShoutReceivedEvent;
import edu.cuny.cat.market.Auctioneer;
import edu.cuny.cat.market.Helper;
import edu.cuny.obj.Resetable;
import edu.cuny.util.Utils;

/**
 * 
 * @author AstonCat Team
 * @version $Revision: 1.88 $
 * 
 */

public class PrivateValueRangeEstimator extends Helper {

	static Logger logger = Logger.getLogger(PrivateValueRangeEstimator.class);

	protected SlidingWindowLearner learner;

	protected double highestReceviedBidPriceDaily;

	protected double lowestReceivedAskPriceDaily;

	public PrivateValueRangeEstimator() {
		learner = new SlidingWindowLearner(100);
	}

	@Override
	public void initialize() {
		super.initialize();

		learner.initialize();
	}

	@Override
	public void reset() {
		super.reset();

		if (learner instanceof Resetable) {
			learner.reset();
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if ((event instanceof DayOpeningEvent)) {
			lowestReceivedAskPriceDaily = Double.POSITIVE_INFINITY;
			highestReceviedBidPriceDaily = Double.NEGATIVE_INFINITY;
		} else if ((event instanceof DayClosedEvent)) {
			learner.train(highestReceviedBidPriceDaily - lowestReceivedAskPriceDaily);
		} else if (event instanceof ShoutReceivedEvent) {
			final Shout shout = ((ShoutReceivedEvent) event).getShout();
			if (shout.isAsk()) {
				if (lowestReceivedAskPriceDaily > shout.getPrice()) {
					lowestReceivedAskPriceDaily = shout.getPrice();
				}
			} else {
				if (highestReceviedBidPriceDaily < shout.getPrice()) {
					highestReceviedBidPriceDaily = shout.getPrice();
				}
			}
		}
	}

	public double getPrivateValueSpread() {
		return learner.act();
	}

	public double getHighestDailyReceivedBidPrice() {
		return highestReceviedBidPriceDaily;
	}

	public double getLowestDailyReceivedAskPrice() {
		return lowestReceivedAskPriceDaily;
	}

	public static PrivateValueRangeEstimator getHelper(Auctioneer auctioneer) {
		PrivateValueRangeEstimator pvEstimator = auctioneer
				.getHelper(PrivateValueRangeEstimator.class);
		if (pvEstimator == null) {
			pvEstimator = new PrivateValueRangeEstimator();
			pvEstimator.initialize();
			auctioneer.setHelper(PrivateValueRangeEstimator.class, pvEstimator);
		}

		return pvEstimator;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += Utils.indent(learner.toString());
		return s;
	}
}
