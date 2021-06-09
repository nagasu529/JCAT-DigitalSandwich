package cat11.agent.jackarooa;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.IllegalShoutException;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayOpenedEvent;
import edu.cuny.cat.event.RoundOpenedEvent;
import edu.cuny.cat.market.accepting.NotAnImprovementOverQuoteException;
import edu.cuny.cat.market.accepting.OnlyNewShoutDecidingAcceptingPolicy;
import edu.cuny.cat.market.matching.ShoutEngine;

public class jacAcceptingPolicy extends OnlyNewShoutDecidingAcceptingPolicy {
	protected static final String DISCLAIMER = "This exception was generated in a lazy manner for performance reasons.  Beware misleading stacktraces.";

	protected static NotAnImprovementOverQuoteException askException = null;

	protected static NotAnImprovementOverQuoteException bidException = null;

	protected jHelper helper;

	@SuppressWarnings("unused")
	private int currentDate = 0;

	private int currentRound = 0;

	private double priceQuote;

	private double range;

	private double bidAcceptingPrice;

	private double askAcceptingPrice;

	private final double gameInitialRange = 20.0D;

	int startUseTraderInfo;

	int daysRecorded;

	int allAcceptingDays = 4;

	static Logger logger = Logger.getLogger(jacAcceptingPolicy.class);

	@Override
	public void initialize() {
		super.initialize();

		helper = jHelper.getHelper(auctioneer);
	}

	@Override
	public void check(final Shout shout) throws IllegalShoutException {
		if (daysRecorded >= allAcceptingDays) {
			if (shout.isBid()) {
				// TODO:
				// this.bidAcceptingPrice = (this.priceQuote - this.range +
				// ((jacDoubleAuctioneer) this.auctioneer)
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
				// ((jacDoubleAuctioneer) this.auctioneer)
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
			range = ((jacDoubleAuctioneer) auctioneer).getRange();

			startUseTraderInfo = helper.getStartUseTraderInfo();
			daysRecorded = helper.getMarketInfo().getTotalDaysRecorded();
		} else if ((event instanceof RoundOpenedEvent)) {
			currentRound = event.getRound();

			// TODO:
			// ExposedFourHeapShoutEngine se = ((jacDoubleAuctioneer) this.auctioneer)
			// .getShoutEngine();
			final ShoutEngine se = auctioneer.getShoutEngine();

			if (startUseTraderInfo > daysRecorded) {
				bidAcceptingPrice = (priceQuote - gameInitialRange);
				askAcceptingPrice = (priceQuote + gameInitialRange);
			} else {
				if (currentRound < 10) {
					bidAcceptingPrice = (priceQuote - range);
					// TODO:
					// } else if ((se.getbInSize() == 0) && (se.getbOutSize() > 0)) {
				} else if ((se.getNumOfMatchedBids() == 0)
						&& (se.getNumOfUnmatchedBids() > 0)) {
					bidAcceptingPrice = se.getHighestUnmatchedBid().getPrice();
				}

				if (currentRound < 10) {
					askAcceptingPrice = (priceQuote + range);
					// TODO:
					// } else if ((se.getsInSize() == 0) && (se.getsOutSize() > 0))
				} else if ((se.getNumOfMatchedAsks() == 0)
						&& (se.getNumOfUnmatchedAsks() > 0)) {
					askAcceptingPrice = se.getLowestUnmatchedAsk().getPrice();
				}
			}
		}
	}
}