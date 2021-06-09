/*
 * AstonCat-Plus from CAT'10
 */

package cat10.agent.astoncat;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.IllegalShoutException;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.RoundOpenedEvent;
import edu.cuny.cat.event.TransactionExecutedEvent;
import edu.cuny.cat.market.accepting.QuoteBeatingAcceptingPolicy;

/**
 * 
 * @author AstonCat Team
 * @version $Revision: 1.88 $
 * 
 */

public class AstonCatAcceptingPolicy extends QuoteBeatingAcceptingPolicy {

	static Logger logger = Logger.getLogger(AstonCatAcceptingPolicy.class);

	public static final String P_DEF_BASE = "astoncat_accepting";

	protected static IllegalShoutException bidException = null;

	protected static IllegalShoutException askException = null;

	protected final static double BidRatioUpperBound = 0.95;

	protected final static double AskRatioLowerBound = 1.05;

	protected final static int BETA = 3;

	protected EquilibriumEstimator equEstimator;

	protected int currentday;

	private double askratio;

	private double bidratio;

	protected double update_step;

	protected double shiftingRatio;

	protected double shiftedEquilibrium;

	protected double shiftedEquilibriumWeight;

	protected double expectedHighestAsk;

	protected double expectedLowestBid;

	public AstonCatAcceptingPolicy() {
		init0();
	}

	@Override
	public void initialize() {
		equEstimator = EquilibriumEstimator.getHelper(auctioneer);
	}

	private void init0() {
		expectedHighestAsk = Double.MAX_VALUE;
		expectedLowestBid = 0.0D;
	}

	@Override
	public void reset() {
		super.reset();
		init0();
	}

	protected void initRatiosDaily() {
		double iniRatioStandard = 0.0d;

		if (currentday < 100) {
			iniRatioStandard = 0.35D;
			update_step = 0.006D;
		} else if (currentday < 320) {
			iniRatioStandard = 0.3D;
			update_step = 0.004D;
		} else if (currentday >= 320) {
			iniRatioStandard = 0.2D;
			update_step = 0.001D;
		}

		final int numOfBuyers = getAuctioneer().getRegistry()
				.getNumOfBuyersInMarketDaily();
		final int numOfSellers = getAuctioneer().getRegistry()
				.getNumOfSellersInMarketDaily();
		final int numOfTraders = numOfSellers + numOfBuyers;

		final double buyerCoef = 2 * numOfBuyers * BETA
				/ (numOfTraders + 2 * numOfBuyers * (BETA - 1));
		final double sellerCoef = 2 * numOfSellers * BETA
				/ (numOfTraders + 2 * numOfSellers * (BETA - 1));

		bidratio = 1.0D - iniRatioStandard / buyerCoef;
		askratio = 1.0D + iniRatioStandard / sellerCoef;
	}

	protected void updateRatiosAfterTransaction() {
		askratio -= update_step;
		bidratio += update_step;
		if (askratio < AskRatioLowerBound) {
			askratio = AskRatioLowerBound;
		}
		if (bidratio > BidRatioUpperBound) {
			bidratio = BidRatioUpperBound;
		}
	}

	@Override
	public void check(final Shout shout) throws IllegalShoutException {
		double equPrice = equEstimator.getEquilibriumPrice();
		if (!Double.isNaN(equPrice)) {
			expectedHighestAsk = equPrice * askratio;
			expectedLowestBid = equPrice * bidratio;
		}

		if (shout.isBid()) {
			if ((shout.getPrice() < expectedLowestBid)) {
				bidNotAnImprovementException();
			}
		} else if ((shout.getPrice() > expectedHighestAsk)) {
			askNotAnImprovementException();
		}
	}

	protected void bidNotAnImprovementException() throws IllegalShoutException {
		if (AstonCatAcceptingPolicy.bidException == null) {
			AstonCatAcceptingPolicy.bidException = new IllegalShoutException(
					"Bid cannot beat the estimated equilibrium!");
		}
		throw AstonCatAcceptingPolicy.bidException;
	}

	protected void askNotAnImprovementException() throws IllegalShoutException {
		if (AstonCatAcceptingPolicy.askException == null) {
			AstonCatAcceptingPolicy.askException = new IllegalShoutException(
					"Ask cannot beat the estimated equilibrium!");
		}
		throw AstonCatAcceptingPolicy.askException;
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if ((event instanceof TransactionExecutedEvent)) {
			updateRatiosAfterTransaction();
		} else if ((event instanceof RoundOpenedEvent)) {
			if (event.getRound() == 0) {
				// do this here so that all traders have registered.
				initRatiosDaily();
			}
		} else if ((event instanceof DayOpeningEvent)) {
			currentday = event.getDay();
		}
	}
}
