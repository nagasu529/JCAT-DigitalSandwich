/*
 * AstonCat-Plus from CAT'10
 */

package cat10.agent.astoncat;

import org.apache.log4j.Logger;

import edu.cuny.ai.learning.ReverseDiscountSlidingLearner;
import edu.cuny.ai.learning.SlidingWindowLearner;
import edu.cuny.cat.core.Transaction;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.TransactionExecutedEvent;
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

public class EquilibriumEstimator extends Helper {

	static Logger logger = Logger.getLogger(EquilibriumEstimator.class);

	protected ReverseDiscountSlidingLearner stLearner;

	protected SlidingWindowLearner ltLearner;

	protected double w = 0.3D;

	protected double maxTransAsk;

	protected double minTransBid;

	private double midTATB;

	protected double equPrice = Double.NaN;

	public EquilibriumEstimator() {
		stLearner = new ReverseDiscountSlidingLearner(5, 0.9);
		ltLearner = new SlidingWindowLearner(20);
	}

	@Override
	public void initialize() {
		super.initialize();

		init1();

		stLearner.initialize();
		ltLearner.initialize();
	}

	private void init1() {
		equPrice = Double.NaN;
	}

	@Override
	public void reset() {
		super.reset();

		init1();

		if (stLearner instanceof Resetable) {
			stLearner.reset();
		}

		if (ltLearner instanceof Resetable) {
			ltLearner.reset();
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if ((event instanceof TransactionExecutedEvent)) {
			final Transaction transaction = ((TransactionExecutedEvent) event)
					.getTransaction();
			if (transaction.getAsk().getPrice() > maxTransAsk) {
				maxTransAsk = transaction.getAsk().getPrice();
			}

			if (transaction.getBid().getPrice() < minTransBid) {
				minTransBid = transaction.getBid().getPrice();
			}
			midTATB = ((maxTransAsk + minTransBid) / 2.0D);

			stLearner.train(transaction.getPrice());

			if ((stLearner.goodEnough()) && (ltLearner.goodEnough())) {
				equPrice = stLearner.act() * w + ltLearner.act() * (1.0D - w);
			} else if (stLearner.goodEnough()) {
				equPrice = stLearner.act();
			} else {
				equPrice = Double.NaN;
			}

		} else if ((event instanceof DayClosedEvent)) {
			ltLearner.train(midTATB);
			if ((stLearner.goodEnough()) && (ltLearner.goodEnough())) {
				equPrice = stLearner.act() * w + ltLearner.act() * (1.0D - w);
			}
		} else if ((event instanceof DayOpeningEvent)) {
			minTransBid = Double.POSITIVE_INFINITY;
			maxTransAsk = Double.NEGATIVE_INFINITY;
		}
	}

	public double getEquilibriumPrice() {
		return equPrice;
	}

	public double getHighestDailyMatchedAskPrice() {
		return maxTransAsk;
	}

	public double getLowestDailyMatchedBidPrice() {
		return minTransBid;
	}

	public static EquilibriumEstimator getHelper(Auctioneer auctioneer) {
		EquilibriumEstimator equEstimator = auctioneer
				.getHelper(EquilibriumEstimator.class);
		if (equEstimator == null) {
			equEstimator = new EquilibriumEstimator();
			equEstimator.initialize();
			auctioneer.setHelper(EquilibriumEstimator.class, equEstimator);
		}

		return equEstimator;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += Utils.indent(stLearner.toString());
		s += Utils.indent(ltLearner.toString());
		return s;
	}
}
