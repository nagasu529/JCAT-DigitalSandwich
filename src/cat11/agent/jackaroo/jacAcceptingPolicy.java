/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo;

import edu.cuny.cat.core.IllegalShoutException;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayOpenedEvent;
import edu.cuny.cat.event.RoundOpenedEvent;
import edu.cuny.cat.market.accepting.NotAnImprovementOverQuoteException;
import edu.cuny.cat.market.accepting.SelfBeatingAcceptingPolicy;
import edu.cuny.cat.market.accepting.ShoutAcceptingPolicy;
import edu.cuny.cat.market.matching.ShoutEngine;

/**
 * An accepting policy that always first utilizes
 * {@link edu.cuny.cat.market.accepting.SelfBeatingAcceptingPolicy}. It imposes
 * additional restrictions after a few of initial days (4). The restriction is
 * basically similar to
 * {@link edu.cuny.cat.market.accepting.EquilibriumBeatingAcceptingPolicy} using
 * an estimated equilibrium price and a dynamic price range. The restriction is
 * also strengthened based on the number of standing shouts on the side of
 * shouts.
 * 
 * The estimated equilibrium price is a combination of the average price of
 * placed shouts, the average price of matched shouts, and the average
 * transaction price. The price range is 20 for the initial days of the game and
 * scales down by the average transaction success rate afterwards, but is at
 * least 10.
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jacAcceptingPolicy extends ShoutAcceptingPolicy {

	protected jacHelper helper;

	protected static final String DISCLAIMER = "This exception was generated in a lazy manner for performance reasons.  Beware misleading stacktraces.";

	protected static NotAnImprovementOverQuoteException askException = null;

	protected static NotAnImprovementOverQuoteException bidException = null;

	protected int currentDate = 0;

	protected int currentRound = 0;

	protected double priceQuote;

	// dynamics range to relax shout thresholds
	protected double range;

	protected double bidAcceptingPrice;

	protected double askAcceptingPrice;

	protected double dayBeginRange = 3.0D;

	// fixed slack rate for initial days
	protected double gameInitialRange = 20.0D;

	protected int daysRecorded;

	protected int allAcceptingDays = 4;

	protected SelfBeatingAcceptingPolicy selfBeating;

	@Override
	public void initialize() {
		super.initialize();

		helper = jacHelper.getHelper(auctioneer);
		selfBeating = new SelfBeatingAcceptingPolicy();
	}

	@Override
	public void reset() {
		super.reset();

		selfBeating.reset();
	}

	@Override
	public void check(final Shout oldShout, final Shout newShout)
			throws IllegalShoutException {
		selfBeating.check(oldShout, newShout);
		check(newShout);
	}

	public void check(final Shout shout) throws IllegalShoutException {
		if (daysRecorded >= allAcceptingDays) {
			if (shout.isBid()) {
				// TODO:
				// this.bidAcceptingPrice = (this.priceQuote - this.range +
				// this.auctioneer
				// .getShoutEngine().getbOutSize());
				bidAcceptingPrice = ((priceQuote - range) + auctioneer.getShoutEngine()
						.getNumOfUnmatchedBids());
				if (shout.getPrice() <= bidAcceptingPrice) {
					if (jacAcceptingPolicy.bidException == null) {
						jacAcceptingPolicy.bidException = new NotAnImprovementOverQuoteException(
								"This exception was generated in a lazy manner for performance reasons.  Beware misleading stacktraces.");
					}
					throw jacAcceptingPolicy.bidException;
				}

			} else {
				// TODO:
				// this.askAcceptingPrice = (this.priceQuote + this.range -
				// this.auctioneer
				// .getShoutEngine().getsOutSize());
				askAcceptingPrice = ((priceQuote + range) - auctioneer.getShoutEngine()
						.getNumOfUnmatchedAsks());
				if (shout.getPrice() >= askAcceptingPrice) {
					if (jacAcceptingPolicy.askException == null) {
						jacAcceptingPolicy.askException = new NotAnImprovementOverQuoteException(
								"This exception was generated in a lazy manner for performance reasons.  Beware misleading stacktraces.");
					}
					throw jacAcceptingPolicy.askException;
				}
			}
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		if ((event instanceof DayOpenedEvent)) {
			final DayOpenedEvent e = (DayOpenedEvent) event;
			currentDate = e.getDay();

			priceQuote = helper.getMeanPrice();
			range = helper.getRange();

			daysRecorded = helper.getMarketInfo().getTotalDaysRecorded();
		} else if ((event instanceof RoundOpenedEvent)) {
			// NOTE: the assignment of bidAcceptingPrice and askAcceptingPrice is not
			// used at all!

			currentRound = event.getRound();

			final ShoutEngine se = auctioneer.getShoutEngine();

			if (helper.getStartUseTraderInfo() > daysRecorded) {
				bidAcceptingPrice = (priceQuote - gameInitialRange);
				askAcceptingPrice = (priceQuote + gameInitialRange);
			} else {
				if (currentRound < 10) {
					bidAcceptingPrice = (priceQuote - range);

					// TODO:
					// } else if ((se.getbInSize() == 0) &&
					// (se.getbOutSize() > 0)) {

				} else if ((se.getNumOfMatchedBids() == 0)
						&& (se.getNumOfUnmatchedBids() > 0)) {
					bidAcceptingPrice = se.getHighestUnmatchedBid().getPrice();
				}

				if (currentRound < 10) {
					askAcceptingPrice = (priceQuote + range);
					// TODO:
					// } else if ((se.getsInSize() == 0) &&
					// (se.getsOutSize() > 0)) {
				} else if ((se.getNumOfMatchedAsks() == 0)
						&& (se.getNumOfUnmatchedAsks() > 0)) {
					askAcceptingPrice = se.getLowestUnmatchedAsk().getPrice();
				}
			}
		}
	}
}