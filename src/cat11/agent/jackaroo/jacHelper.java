/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import cat11.agent.jackaroo.util.MarketInfo;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpenedEvent;
import edu.cuny.cat.event.IdAssignedEvent;
import edu.cuny.cat.event.ShoutPlacedEvent;
import edu.cuny.cat.event.ShoutReceivedEvent;
import edu.cuny.cat.event.TransactionExecutedEvent;
import edu.cuny.cat.market.Auctioneer;
import edu.cuny.cat.market.Helper;
import edu.cuny.obj.Resetable;
import edu.cuny.struct.FixedLengthQueue;

/**
 * TODO:
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jacHelper extends Helper {

	static Logger logger = Logger.getLogger(jacHelper.class);

	protected Map<String, Shout> shouts = new HashMap<String, Shout>();

	protected static final int marketInfoWindow = 20;

	protected final static int startUseTraderInfo = 15;

	protected int currentDay;

	protected MarketInfo marketInfo;

	protected int receivedAsk = 0;

	protected int receivedBid = 0;

	protected int acceptedAsk = 0;

	protected int acceptedBid = 0;

	protected int transactedAsk = 0;

	protected int transactedBid = 0;

	protected double meanPrice;

	protected double lastDaysTransactionRate = 1.0D;

	FixedLengthQueue transactionRates = new FixedLengthQueue(8);

	public jacHelper() {
		init0();
	}

	private void init0() {
		lastDaysTransactionRate = 1.0D;
	}

	@Override
	public void reset() {
		super.reset();

		if ((marketInfo instanceof Resetable)) {
			marketInfo.reset();
		}

		transactionRates.reset();

		init0();
	}

	/**
	 * 
	 * @return a price range that is used to relax price restriction in shout
	 *         accepting. The range is 20 for the initial days and scales down by
	 *         the average transaction success rate after the period. The range is
	 *         guarantteed to be at least 10.
	 */
	public double getRange() {
		double range;
		if (marketInfo.getTotalDaysRecorded() <= 250) {
			range = 20.0D;
		} else {
			range = 20.0D;
		}

		if (marketInfo.getTotalDaysRecorded() > jacHelper.marketInfoWindow) {
			range *= transactionRates.getMean();
		}

		if (range < 10.0D) {
			range = 10.0D;
		}

		return range;
	}

	/**
	 * 
	 * @return an estimated equilibrium price that is a combination of average
	 *         price of placed shouts, average price of transacted shouts, and
	 *         average transaction prices.
	 */
	public double getMeanPrice() {
		return meanPrice;
	}

	public int getMarketInfoWindow() {
		return jacHelper.marketInfoWindow;
	}

	public MarketInfo getMarketInfo() {
		return marketInfo;
	}

	public int getStartUseTraderInfo() {
		return jacHelper.startUseTraderInfo;
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if (event instanceof IdAssignedEvent) {
			marketInfo = new MarketInfo(((IdAssignedEvent) event).getId(),
					jacHelper.marketInfoWindow);
		} else if ((event instanceof DayOpenedEvent)) {
			processDayOpened((DayOpenedEvent) event);
		} else if ((event instanceof DayClosedEvent)) {
			processDayClosed((DayClosedEvent) event);
		} else if (event instanceof ShoutReceivedEvent) {
			processShoutReceived((ShoutReceivedEvent) event);
		} else if (event instanceof ShoutPlacedEvent) {
			processShoutPlaced((ShoutPlacedEvent) event);
		} else if (event instanceof TransactionExecutedEvent) {
			processTransactionExecuted((TransactionExecutedEvent) event);
		}

		if (marketInfo != null) {
			marketInfo.eventOccurred(event);
		}

	}

	private void processTransactionExecuted(final TransactionExecutedEvent event) {
		transactedAsk += 1;
		transactedBid += 1;
	}

	protected void processDayOpened(final DayOpenedEvent event) {
		currentDay = event.getDay();
		receivedAsk = 0;
		receivedBid = 0;
		acceptedAsk = 0;
		acceptedBid = 0;
		transactedAsk = 0;
		transactedBid = 0;
	}

	protected void processDayClosed(final DayClosedEvent event) {
		meanPrice = ((((marketInfo.getMeanAcceptedShoutPrice() * 0.5D) + (marketInfo
				.getMeanTransShoutPrice() * 0.5D)) * 0.5D) + (marketInfo
				.getMeanTransPrice() * 0.5D));
		try {
			// double acceptRate = (this.acceptedAsk + this.acceptedBid)
			// / (this.receivedAsk + this.receivedBid);

			final double transacRate = (acceptedAsk + acceptedBid) == 0 ? 0.0D
					: ((double) (transactedAsk + transactedBid))
							/ (acceptedAsk + acceptedBid);

			lastDaysTransactionRate = transacRate;
			transactionRates.newData(transacRate);

			// System.out.println("\n++++Closing Day: " + currentDay + "....\n"
			// + " RA+RB: " + receivedAsk + "+" + receivedBid + " = "
			// + (receivedAsk + receivedBid));
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	protected void processShoutReceived(final ShoutReceivedEvent event) {
		final Shout shout = event.getShout();
		final Shout oldShout = shouts.get(shout.getId());

		if (shout.isAsk()) {
			if ((oldShout != null) && (oldShout.getPrice() <= shout.getPrice())) {
				return;
			}
		} else if ((oldShout != null) && (oldShout.getPrice() >= shout.getPrice())) {
			return;
		}

		if (oldShout == null) {
			if (shout.isAsk()) {
				receivedAsk += 1;
			} else if (shout.isBid()) {
				receivedBid += 1;
			} else {
				jacHelper.logger.fatal("something goes wrong in newShout()!!!");
			}
		}
	}

	protected void processShoutPlaced(final ShoutPlacedEvent event) {
		final Shout shout = event.getShout();

		final Shout oldShout = shouts.get(shout.getId());

		if (oldShout == null) {
			if (shout.isAsk()) {
				acceptedAsk += 1;
			} else {
				acceptedBid += 1;
			}
		}

		shouts.put(shout.getId(), shout);
	}

	public static jacHelper getHelper(final Auctioneer auctioneer) {
		jacHelper helper = auctioneer.getHelper(jacHelper.class);
		if (helper == null) {
			helper = new jacHelper();
			helper.initialize();
			auctioneer.setHelper(jacHelper.class, helper);
		}

		return helper;
	}

	@Override
	public String toString() {
		final String s = super.toString();
		return s;
	}
}
