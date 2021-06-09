/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo;

import org.apache.log4j.Logger;

import cat11.agent.jackaroo.util.MarketInfo;
import cat11.agent.jackaroo.util.TraderMarketStatistic;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.core.Trader;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.ShoutPlacedEvent;
import edu.cuny.cat.market.MarketQuote;
import edu.cuny.cat.market.pricing.KPricingPolicy;
import edu.cuny.math.MathUtil;

/**
 * This policy sets the transaction in the mid-point of the bid-ask spread for
 * the initial days in the game, and
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jacPricingPolicy extends KPricingPolicy {

	protected int numOfAsksDaily;

	protected int numOfBidsDaily;

	protected int currentDay = 0;

	// protected double minDayDiffR = 0.75D;
	//
	// protected double minDaysInBestR = 0.4D;
	//
	// protected double minMarketSelectDev = 3.0D;
	//
	// protected int minJumpDate = 3;
	//
	// protected int longTimeNoCome = 6;

	protected static final double minK = 0.15D;

	protected static final double maxK = 0.85D;

	// protected DataRecord dataOut;

	protected static final boolean useMeanPrice = true;

	protected double meanPrice;

	protected int startUseTraderInfo;

	protected int daysRecorded = 0;

	protected jacHelper helper;

	static Logger logger = Logger.getLogger(jacPricingPolicy.class);

	public jacPricingPolicy() {
	}

	public jacPricingPolicy(final double k) {
		super(k);
	}

	@Override
	public void initialize() {
		super.initialize();

		helper = jacHelper.getHelper(auctioneer);
	}

	// @SuppressWarnings("unused")
	// private void initOutPut() {
	// dataOut = new DataRecord("pricing.csv");
	//
	// final String title =
	// "currentDay,Trader,Buyer/Seller,TotalRecordDays,HistoryWindowSize,NumberofSelectedSpecialists,NumberofTotalSpecialists,MarketSelectionStdDev,TheDayBeforeLastDayInMyMarket,LastDayInMyMarket,DaysInMyMarket,NumberOfItemsTradedInLastDayInMyMarket,BestSpecialist,DaysInBestSpecialist,BestSpecialistLastShoutPrice,BestSpecialistShoutMean,BestSpecialistShoutStdDev,BestSpecialistLastTransactionPrice,BestSpecialistTransactionMean,BestSpecialistTransactionStdDev,BestSpecialistNumberOfItemsTradedInLastDayIn,BestSpecialistMaxNumberOfItemsTradedInOneDay,DayDiffR(TotalRecordDays/HistoryWindowSize),DaysInBestR(DaysInBestSpecialist/HistoryWindowSize),CurrentShoutPrice,CurrentTransactionPrice";
	//
	// dataOut.putIn(title);
	// }

	@Override
	public double determineClearingPrice(final Shout bid, final Shout ask,
			final MarketQuote clearingQuote) {
		// String bidOut = "";
		// String askOut = "";
		double priceEndPoint;
		double priceStartPoint;
		if (daysRecorded < helper.getStartUseTraderInfo()) {
			k = 0.5D;
			priceStartPoint = ask.getPrice();
			priceEndPoint = bid.getPrice();
		} else {
			final MarketInfo marketInfo = helper.getMarketInfo();
			final Trader bidTrader = auctioneer.getShout(bid.getId()).getTrader();
			final Trader askTrader = auctioneer.getShout(ask.getId()).getTrader();
			double bidK;
			if (bidTrader != null) {
				final String bidTraderId = bidTrader.getId();

				final TraderMarketStatistic bidStat = marketInfo
						.getTraderMarketStatistic(bidTraderId);

				if (bidStat != null) {
					final int bidHistoryWindowSize = bidStat.getHistoryWindowSize();

					final double bidDayDiffR = ((double) bidStat.getTotalDays())
							/ Math.min(bidHistoryWindowSize, daysRecorded);

					final double bidDaysInBestR = ((double) bidStat
							.getDaysInBestSpecialist())
							/ Math.min(bidHistoryWindowSize, daysRecorded);

					// double bidMarketSelectDev = bidStat.getMarketSelectionStdDev();

					final String bidBestMarket = bidStat.getBestSpecialist();

					final int bidLastDayInMyMarket = bidStat.getLastDayInMyMarket();

					final int bidTheDayBeforeLastDayInMyMarket = bidStat
							.getTheDayBeforeLastDayInMyMarket();

					// int bidDaysInMyMarket = bidStat.getDaysInMyMarket();

					final double bidTM = bidStat.getBestSpecialistTransactionMean();
					if ((bidDaysInBestR >= 0.33D) && (bidDayDiffR >= 0.9D)) {
						priceEndPoint = MathUtil.min(bid.getPrice(), bidTM);
						if (bidBestMarket.equals(auctioneer.getName())) {
							bidK = jacPricingPolicy.maxK;
						} else {
							if ((bidTheDayBeforeLastDayInMyMarket == -1)
									|| ((bidLastDayInMyMarket - bidTheDayBeforeLastDayInMyMarket) > 10)) {
								bidK = jacPricingPolicy.minK;
							} else {
								bidK = 0.35D;
							}
						}
					} else {
						priceEndPoint = bid.getPrice();
						bidK = -1.0D;
					}

					// bidOut = bidOut + currentDay + ", " + bidStat.getTrader() + ", "
					// + "B, " + bidStat.getTotalDays() + ", "
					// + bidStat.getHistoryWindowSize() + ", "
					// + bidStat.getNumberofSpecialists() + ", "
					// + bidStat.getTotalNumberofSpecialists() + ", "
					// + bidStat.getMarketSelectionStdDev() + ", "
					// + bidStat.getTheDayBeforeLastDayInMyMarket() + ", "
					// + bidStat.getLastDayInMyMarket() + ", "
					// + bidStat.getDaysInMyMarket() + ", "
					// + bidStat.getNumberOfItemsTradedInLastDayInMyMarket() + ", "
					// + bidStat.getBestSpecialist() + ", "
					// + bidStat.getDaysInBestSpecialist() + ", "
					// + bidStat.getBestSpecialistLastShoutPrice() + ", "
					// + bidStat.getBestSpecialistShoutMean() + ", "
					// + bidStat.getBestSpecialistShoutStdDev() + ", "
					// + bidStat.getBestSpecialistLastTransactionPrice() + ", "
					// + bidStat.getBestSpecialistTransactionMean() + ", "
					// + bidStat.getBestSpecialistTransactionStdDev() + ", "
					// + bidStat.getBestSpecialistNumberOfItemsTradedInLastDayIn()
					// + ", "
					// + bidStat.getBestSpecialistMaxNumberOfItemsTradedInOneDay()
					// + ", " + bidDayDiffR + ", " + bidDaysInBestR + ","
					// + bid.getPrice();
				} else {
					priceEndPoint = bid.getPrice();
					bidK = -2.0D;
				}

			} else {
				priceEndPoint = bid.getPrice();
				bidK = 0.5D;
			}

			double askK;
			if (askTrader != null) {
				final String askTraderId = askTrader.getId();

				final TraderMarketStatistic askStat = marketInfo
						.getTraderMarketStatistic(askTraderId);

				if (askStat != null) {
					final int askHistoryWindowSize = askStat.getHistoryWindowSize();

					final double askDayDiffR = ((double) askStat.getTotalDays())
							/ Math.min(askHistoryWindowSize, daysRecorded);

					final double askDaysInBestR = ((double) askStat
							.getDaysInBestSpecialist())
							/ Math.min(askHistoryWindowSize, daysRecorded);

					// double askMarketSelectDev = askStat.getMarketSelectionStdDev();

					final String askBestMarket = askStat.getBestSpecialist();

					final int askLastDayInMyMarket = askStat.getLastDayInMyMarket();

					final int askTheDayBeforeLastDayInMyMarket = askStat
							.getTheDayBeforeLastDayInMyMarket();

					// int askDaysInMyMarket = askStat.getDaysInMyMarket();

					final double askTM = askStat.getBestSpecialistTransactionMean();
					if ((askDaysInBestR >= 0.33D) && (askDayDiffR >= 0.9D)) {
						priceStartPoint = MathUtil.max(askTM, ask.getPrice());
						if (askBestMarket.equals(auctioneer.getName())) {
							askK = jacPricingPolicy.minK;
						} else {
							if ((askTheDayBeforeLastDayInMyMarket == -1)
									|| ((askLastDayInMyMarket - askTheDayBeforeLastDayInMyMarket) > 10)) {
								askK = jacPricingPolicy.maxK;
							} else {
								askK = 0.65D;
							}
						}
					} else {
						priceStartPoint = ask.getPrice();
						askK = -1.0D;
					}

					// askOut = askOut + currentDay + ", " + askStat.getTrader() + ", "
					// + "S, " + askStat.getTotalDays() + ", "
					// + askStat.getHistoryWindowSize() + ", "
					// + askStat.getNumberofSpecialists() + ", "
					// + askStat.getTotalNumberofSpecialists() + ", "
					// + askStat.getMarketSelectionStdDev() + ", "
					// + askStat.getTheDayBeforeLastDayInMyMarket() + ", "
					// + askStat.getLastDayInMyMarket() + ", "
					// + askStat.getDaysInMyMarket() + ", "
					// + askStat.getNumberOfItemsTradedInLastDayInMyMarket() + ", "
					// + askStat.getBestSpecialist() + ", "
					// + askStat.getDaysInBestSpecialist() + ", "
					// + askStat.getBestSpecialistLastShoutPrice() + ", "
					// + askStat.getBestSpecialistShoutMean() + ", "
					// + askStat.getBestSpecialistShoutStdDev() + ", "
					// + askStat.getBestSpecialistLastTransactionPrice() + ", "
					// + askStat.getBestSpecialistTransactionMean() + ", "
					// + askStat.getBestSpecialistTransactionStdDev() + ", "
					// + askStat.getBestSpecialistNumberOfItemsTradedInLastDayIn()
					// + ", "
					// + askStat.getBestSpecialistMaxNumberOfItemsTradedInOneDay()
					// + ", " + askDayDiffR + ", " + askDaysInBestR + ","
					// + ask.getPrice();
				} else {
					priceStartPoint = ask.getPrice();
					askK = -2.0D;
				}

			} else {
				priceStartPoint = ask.getPrice();
				askK = 0.5D;
			}

			if ((bidK >= askK) && (askK >= 0.0D)) {
				k = ((askK + bidK) / 2.0D);
			} else if ((askK > bidK) && (bidK >= 0.0D)) {
				k = ((askK + bidK) / 2.0D);
			} else if ((askK >= 0.0D) && (bidK == -1.0D)) {
				k = askK;
			} else if ((askK >= 0.0D) && (bidK == -2.0D)) {
				k = jacPricingPolicy.maxK;
			} else if ((bidK >= 0.0D) && (askK == -1.0D)) {
				k = bidK;
			} else if ((bidK >= 0.0D) && (askK == -2.0D)) {
				k = jacPricingPolicy.minK;
			} else if ((bidK < askK) && (askK < 0.0D)) {
				k = jacPricingPolicy.maxK;
			} else if ((bidK > askK) && (bidK < 0.0D)) {
				k = jacPricingPolicy.minK;
			} else {
				k = 0.5D;
			}

		}

		double finalPrice = kInterval(priceStartPoint, priceEndPoint);

		if (Double.isNaN(finalPrice)) {
			jacPricingPolicy.logger.info("start price: " + priceStartPoint
					+ " end price: " + priceEndPoint + " final price: " + finalPrice);
		}

		double pBal;
		if (daysRecorded < helper.getStartUseTraderInfo()) {
			pBal = 0.0D;
		} else {
			pBal = 0.25D;
		}

		if ((jacPricingPolicy.useMeanPrice) && (meanPrice > 0.0D)
				&& (daysRecorded > 0)) {
			finalPrice = (finalPrice * pBal) + (meanPrice * (1.0D - pBal));
		}

		if (finalPrice < ask.getPrice()) {
			finalPrice = ask.getPrice();
		} else if (finalPrice > bid.getPrice()) {
			finalPrice = bid.getPrice();
		}

		// if (!askOut.equals("")) {
		// askOut = askOut + ", " + finalPrice;
		// }
		//
		// if (!bidOut.equals("")) {
		// bidOut = bidOut + ", " + finalPrice;
		// }

		if (Double.isNaN(finalPrice)) {
			jacPricingPolicy.logger.info("ask price: " + ask.getPrice()
					+ " bid price: " + bid.getPrice() + " final price: " + finalPrice);
		}

		return finalPrice;
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if ((event instanceof DayOpeningEvent)) {
			numOfAsksDaily = 1;
			numOfBidsDaily = 1;
			final DayOpeningEvent e = (DayOpeningEvent) event;
			currentDay = e.getDay();
			meanPrice = helper.getMeanPrice();

		} else if ((event instanceof ShoutPlacedEvent)) {
			final Shout shout = ((ShoutPlacedEvent) event).getShout();
			if (shout.isAsk()) {
				numOfAsksDaily += 1;
			} else {
				numOfBidsDaily += 1;
			}
		} else if ((event instanceof DayClosedEvent)) {
			daysRecorded = helper.getMarketInfo().getTotalDaysRecorded();
		}
	}
}