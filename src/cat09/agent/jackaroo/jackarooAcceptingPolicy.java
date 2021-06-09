/* 
 * jackaroo in CAT'09
 */

package cat09.agent.jackaroo;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.IllegalShoutException;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.RoundClosingEvent;
import edu.cuny.cat.event.TransactionExecutedEvent;
import edu.cuny.cat.market.DuplicateShoutException;
import edu.cuny.cat.market.accepting.SelfBeatingAcceptingPolicy;
import edu.cuny.cat.market.matching.FourHeapShoutEngine;
import edu.cuny.cat.market.quoting.SingleSidedQuotingPolicy;
import edu.cuny.math.MathUtil;

/**
 * TODO:
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jackarooAcceptingPolicy extends SelfBeatingAcceptingPolicy {

	static Logger logger = Logger.getLogger(jackarooAcceptingPolicy.class);

	protected static IllegalShoutException askException = new IllegalShoutException();

	protected static IllegalShoutException bidException = new IllegalShoutException();

	protected int day = -1;

	protected int round = -1;

	protected int adAsk;

	protected int adBid;

	protected int receivedAsk;

	protected int receivedBid;

	protected int placedAsk;

	protected int placedBid;

	protected int transactedAsk;

	protected int transactedBid;

	protected double scheduleRatio;

	protected final Forecasting shoutRecord;

	protected final MyFourHeapShoutEngine simuSE;

	protected final int historyLength;

	protected final Shout shoutHistory[];

	protected SingleSidedQuotingPolicy singleSidedQuoting;

	public jackarooAcceptingPolicy() {
		historyLength = 500;
		shoutHistory = new Shout[historyLength];
		shoutRecord = new Forecasting(200, 2, 0.5D, true);

		singleSidedQuoting = new SingleSidedQuotingPolicy();
		simuSE = new MyFourHeapShoutEngine();
	}

	@Override
	public void reset() {
		super.reset();

		singleSidedQuoting.reset();
		shoutRecord.reset();
		simuSE.reset();
		for (int i = 0; i < shoutHistory.length; i++) {
			shoutHistory[i] = null;
		}
	}

	@Override
	public void check(final Shout oldShout, final Shout newShout)
			throws IllegalShoutException {

		if (oldShout != null) {
			// do AS
			super.check(oldShout, newShout);
		} else {
			if (newShout.isAsk()) {
				receivedAsk++;
			} else {
				receivedBid++;
			}

			try {
				simuSE.newShout(newShout);
				if (shoutHistory[historyLength - 1] != null) {
					// TODO: do not know why deleteShout() rather than removeShout()
					simuSE.deleteShout(shoutHistory[historyLength - 1]);
				}

				// TODO: shoutHistory should be defined based on Forecasting
				for (int i = historyLength - 1; i > 0; i--) {
					shoutHistory[i] = shoutHistory[i - 1];
				}

				shoutHistory[0] = newShout;

				// TODO:
				if (getNumOfShoutsInSimuSE() > historyLength) {
					jackarooAcceptingPolicy.logger.fatal("simuSE in "
							+ getClass().getSimpleName() + " is not cleared periodically !");
				}
			} catch (final DuplicateShoutException e) {
				jackarooAcceptingPolicy.logger.debug(e);
			}

			check(newShout);

			if (newShout.isAsk()) {
				placedAsk++;
			} else {
				placedBid++;
			}

			scheduleRatio = (5.0E-006D + placedAsk)
					/ (1.0E-005D + placedBid + placedAsk);
		}

		if (newShout.isAsk()) {
			shoutRecord.addItem(0, newShout.getPrice());
		} else {
			shoutRecord.addItem(1, newShout.getPrice());
		}
	}

	public void check(final Shout shout) throws IllegalShoutException {

		// okay as long as beating single-sided quote
		if (shout.isAsk() && (shout.getPrice() <= getMatchableAskPrice())) {
			return;
		} else if (shout.isBid() && (shout.getPrice() >= getMatchableBidPrice())) {
			return;
		}

		if (day < 3) {
			// use AQ exactly
			if (shout.isBid()) {
				final double quote = auctioneer.bidQuote();
				if (shout.getPrice() < quote) {
					throw jackarooAcceptingPolicy.bidException;
				}
			} else {
				final double quote = auctioneer.askQuote();
				if (shout.getPrice() > quote) {
					throw jackarooAcceptingPolicy.askException;
				}
			}
		} else {
			double priceQuote = getMediumPrice();

			final double range = getRange();
			if (shout.isBid()) {
				if ((scheduleRatio > 0.5D) && (round > 3)) {
					// bids are scarcer and later in the day, lower threshold further
					priceQuote *= 1.0D + ((0.5D - scheduleRatio) * scheduleRatio) / range;
					adBid++;
				}

				if (shout.getPrice() < priceQuote - range) {
					throw jackarooAcceptingPolicy.bidException;
				}
			} else {
				if ((scheduleRatio < 0.5D) && (round > 3)) {
					// asks are scarcer and later in the day, higher threshold further
					priceQuote *= 1.0D
							+ ((0.5D - scheduleRatio) * (1.0D - scheduleRatio)) / range;
					adAsk++;
				}

				if (shout.getPrice() > priceQuote + range) {
					throw jackarooAcceptingPolicy.askException;
				}
			}
		}
	}

	public double getMatchableAskPrice() {
		return singleSidedQuoting.bidQuote(auctioneer.getShoutEngine());
	}

	public double getMatchableBidPrice() {
		return singleSidedQuoting.askQuote(auctioneer.getShoutEngine());
	}

	/**
	 * calculates a price that is similar to single-sided market quotes, and uses
	 * the boundary prices on the matching side if single-sided quotes are
	 * undefined.
	 * 
	 * @return a price that serves as the estimated equilibrium price.
	 */
	public double getMediumPrice() {
		final double a = simuSE.getLowestUnmatchedAsk() != null ? simuSE
				.getLowestUnmatchedAsk().getPrice()
				: (simuSE.getHighestMatchedAsk() != null ? simuSE
						.getHighestMatchedAsk().getPrice() : Double.POSITIVE_INFINITY);
		final double b = simuSE.getHighestUnmatchedBid() != null ? simuSE
				.getHighestUnmatchedBid().getPrice()
				: (simuSE.getLowestMatchedBid() != null ? simuSE.getLowestMatchedBid()
						.getPrice() : Double.NEGATIVE_INFINITY);
		return (a + b) / 2D;
	}

	/**
	 * 
	 * @return a scaling value to relax the quote-beating threshold
	 */
	public double getRange() {
		final double range = getMediumPrice() / 40D;
		return range < 1.0D ? 1.0D : range;
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		if (event instanceof DayOpeningEvent) {
			processDayOpening((DayOpeningEvent) event);
		} else if (event instanceof RoundClosingEvent) {
			processRoundClosing((RoundClosingEvent) event);
		} else if (event instanceof TransactionExecutedEvent) {
			processTransactionExecuted((TransactionExecutedEvent) event);
		} else if (event instanceof DayClosedEvent) {
			processDayClosed((DayClosedEvent) event);
		}
	}

	protected void processDayOpening(final DayOpeningEvent event) {
		day = event.getDay();

		receivedAsk = 0;
		receivedBid = 0;
		placedAsk = 0;
		placedBid = 0;
		transactedAsk = 0;
		transactedBid = 0;

		// changed from 1.0 to 0.5, which is more reasonable
		scheduleRatio = 0.5D;

		adAsk = 0;
		adBid = 0;
	}

	protected void processRoundClosing(final RoundClosingEvent event) {
		round = event.getRound();
	}

	protected void processTransactionExecuted(final TransactionExecutedEvent event) {
		transactedAsk++;
		transactedBid++;
	}

	private int getNumOfShoutsInSimuSE() {
		return simuSE.getsOutSize() + simuSE.getsInSize() + simuSE.getbOutSize()
				+ simuSE.getbInSize();
	}

	protected void processDayClosed(final DayClosedEvent event) {

		final double acceptRate = (double) (placedAsk + placedBid)
				/ (double) (receivedAsk + receivedBid);
		final double transacRate = (double) (transactedAsk + transactedBid)
				/ (double) (placedAsk + placedBid);

		jackarooAcceptingPolicy.logger.debug("Accept rate: R|A/B: "
				+ MathUtil.round(acceptRate, 2) + " RA/RB " + receivedAsk + "/"
				+ receivedBid + " = " + (receivedAsk + receivedBid) + " Ba:"
				+ MathUtil.round(scheduleRatio, 3));
		jackarooAcceptingPolicy.logger.debug("Transa rate: R|A/B: "
				+ MathUtil.round(transacRate, 2) + " PA/PB " + placedAsk + "/"
				+ placedBid + " = " + (placedAsk + placedBid) + " TA/TB "
				+ transactedAsk + "/" + transactedBid);
		jackarooAcceptingPolicy.logger.debug(" Ask shout mean: "
				+ MathUtil.round(shoutRecord.nonZeroMean(0), 0)
				+ " Bid shout mean: "
				+ MathUtil.round(shoutRecord.nonZeroMean(1), 0)
				+ " = "
				+ MathUtil.round(shoutRecord.nonZeroMean(0) * 0.5D
						+ shoutRecord.nonZeroMean(1) * 0.5D, 2) + " Range "
				+ MathUtil.round(getRange(), 1));
		jackarooAcceptingPolicy.logger.debug(" Simu Ask: "
				+ (simuSE.getLowestUnmatchedAsk() != null ? simuSE
						.getLowestUnmatchedAsk().getPrice() : Double.NaN)
				+ " Simu Bid: "
				+ (simuSE.getHighestUnmatchedBid() != null ? simuSE
						.getHighestUnmatchedBid().getPrice() : Double.NaN) + " = "
				+ MathUtil.round(getMediumPrice(), 2) + " Size "
				+ (getNumOfShoutsInSimuSE()));

		jackarooAcceptingPolicy.logger.debug("Current Day:" + day + "  A/B: "
				+ adAsk + "/" + adBid);
		jackarooAcceptingPolicy.logger.debug("");
	}

	class MyFourHeapShoutEngine extends FourHeapShoutEngine {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void deleteShout(final Shout shout) {
			if (shout.isAsk()) {
				if (!sIn.remove(shout)) {
					sOut.remove(shout);
				}
			} else if (!bIn.remove(shout)) {
				bOut.remove(shout);
			}
		}

		public int getsOutSize() {
			return sOut.size();
		}

		public int getsInSize() {
			return sIn.size();
		}

		public int getbOutSize() {
			return bOut.size();
		}

		public int getbInSize() {
			return bIn.size();
		}
	}
}
