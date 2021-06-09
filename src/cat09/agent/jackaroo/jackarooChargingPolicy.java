/* 
 * jackaroo in CAT'09
 */

package cat09.agent.jackaroo;

import org.apache.log4j.Logger;

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.RegisteredTradersAnnouncedEvent;
import edu.cuny.cat.market.charging.ChargingPolicy;

/**
 * TODO:
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jackarooChargingPolicy extends ChargingPolicy {

	static Logger logger = Logger.getLogger(jackarooChargingPolicy.class);

	protected static int START_CHARGING_DAY = 15;

	protected static int HEAVY_CHARGING_DAY = 430;

	protected int day;

	protected int numberOfTradersRegisteredToday;

	protected int totalNumberOfTraders;

	protected int numOfSpecialists;

	protected int chargingLevel;

	protected Forecasting marketShareRate;

	public jackarooChargingPolicy() {
		marketShareRate = new Forecasting(10, 1, 0.5D, true);
		init0();
	}

	private void init0() {
		day = -1;
		chargingLevel = 0;
	}

	@Override
	public void reset() {
		super.reset();
		init0();
		marketShareRate.reset();
	}

	protected double calculateProfitFee() {
		// 5 different levels of profit fee
		final double feeChoices[] = new double[5];
		feeChoices[0] = 0.0D;
		feeChoices[1] = 0.06;
		feeChoices[2] = 0.14;
		feeChoices[3] = 0.21;
		feeChoices[4] = 0.35;

		double curMarketShare = 0.0D;
		if (totalNumberOfTraders != 0) {
			curMarketShare = (double) numberOfTradersRegisteredToday
					/ totalNumberOfTraders;
		}

		// this, compared to 1, indicates the capability of this specialist
		// attracting traders
		final double abstractms = curMarketShare * numOfSpecialists;
		marketShareRate.addItem(0, abstractms);

		if (day < jackarooChargingPolicy.START_CHARGING_DAY) {
			chargingLevel = 0;
		} else if (day == jackarooChargingPolicy.START_CHARGING_DAY) {
			chargingLevel = 1;
		} else if ((day > jackarooChargingPolicy.START_CHARGING_DAY)
				&& (day <= jackarooChargingPolicy.HEAVY_CHARGING_DAY)) {
			if ((chargingLevel == 3) && (marketShareRate.nonZeroMean(0) < 1.6)) {
				chargingLevel = 2;
			}
			if ((chargingLevel == 2) && (marketShareRate.nonZeroMean(0) > 1.7)) {
				chargingLevel = 3;
			}
			if ((chargingLevel == 2) && (marketShareRate.nonZeroMean(0) < 1.1)) {
				chargingLevel = 1;
			}
			if ((chargingLevel == 1) && (marketShareRate.nonZeroMean(0) > 1.4)) {
				chargingLevel = 2;
			}
		} else if (day > jackarooChargingPolicy.HEAVY_CHARGING_DAY) {
			if (marketShareRate.nonZeroMean(0) >= 1.25D) {
				chargingLevel = 4;
			} else {
				chargingLevel = 3;
			}
		}
		return feeChoices[chargingLevel];
	}

	protected void updateFees() {
		fees[0] = 0.0D;
		fees[1] = 0.0D;
		fees[2] = 0.0D;
		fees[3] = 0.0D;
		fees[4] = calculateProfitFee();
		if (fees[4] < 0.0D) {
			fees[4] = 0.0D;
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if (event instanceof DayOpeningEvent) {
			numOfSpecialists = 0;
			numberOfTradersRegisteredToday = 0;
			totalNumberOfTraders = 0;
		} else if (event instanceof DayClosedEvent) {
			day = event.getDay();
			updateFees();
		} else if (event instanceof RegisteredTradersAnnouncedEvent) {
			final RegisteredTradersAnnouncedEvent rtaEvent = (RegisteredTradersAnnouncedEvent) event;

			final int numOfTraders = rtaEvent.getNumOfTraders();
			totalNumberOfTraders += numOfTraders;
			if (rtaEvent.getSpecialist().getId().equals(getAuctioneer().getName())) {
				numberOfTradersRegisteredToday = numOfTraders;
			}

			if (numOfTraders > 0) {
				numOfSpecialists++;
			}
		}
	}
}
