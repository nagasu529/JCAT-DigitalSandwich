/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo;

import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

import cat11.agent.jackaroo.util.Forecasting;
import edu.cuny.cat.core.Specialist;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.FeesAnnouncedEvent;
import edu.cuny.cat.event.ProfitAnnouncedEvent;
import edu.cuny.cat.event.RegisteredTradersAnnouncedEvent;
import edu.cuny.cat.event.TransactionExecutedEvent;
import edu.cuny.cat.market.charging.AdaptiveChargingPolicy;

/**
 * TODO:
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jacChargingPolicy extends AdaptiveChargingPolicy {

	protected static Logger logger = Logger.getLogger(jacChargingPolicy.class);

	protected jacHelper helper;

	protected int currentDate = 0;

	protected int numberOfTradersRegisteredToday = 0;

	protected int totalNumberOfTraders = 0;

	protected int numOfSpecialists = 0;

	protected double avgMarketShare = 1.0D;

	protected int startChargingDay = 100;

	protected int bigChargingDay = 450;

	protected int totalNumberOfActiveSpecialistsInToday = 0;

	protected boolean noOneChargeProfit = true;

	protected double highestProfitChargeNow = 0.0D;

	// protected double[] minFees;
	//
	// protected double[] maxFees;

	protected int numOfChargingMarkets = 0;

	protected double currentCharging = 0.0D;

	protected int chargingLevel = 0;

	protected double minTransactionProfit = 9999.0D;

	protected String traderLeader;

	protected String profitLeader;

	protected String marketLeader;

	protected double marketLeaderFee = 0.0D;

	protected Forecasting marketShareRate;

	protected Hashtable<String, double[]> feeTable = new Hashtable<String, double[]>();

	protected Hashtable<String, Forecasting> traderTrack = new Hashtable<String, Forecasting>();

	protected Hashtable<String, Forecasting> profitTrack = new Hashtable<String, Forecasting>();

	protected Hashtable<String, Double> accumulatedMarketShare = new Hashtable<String, Double>();

	protected Hashtable<String, Double> accumulatedProfitShare = new Hashtable<String, Double>();

	protected Hashtable<String, Double> profitScores = new Hashtable<String, Double>();

	protected Hashtable<String, Double> marketShare = new Hashtable<String, Double>();

	// protected Forecasting transactionCharging = new Forecasting(6, 1, 0.5D,
	// true);

	// protected DataRecord data;

	public jacChargingPolicy() {
		// minFees = new double[5];
		// maxFees = new double[5];
		// data = new DataRecord("Charging.txt");

		// for (int i = 0; i < 6; i++) {
		// transactionCharging.addItem(0, 0.0D);
		// }

		init0();

	}

	@Override
	public void initialize() {
		super.initialize();

		helper = jacHelper.getHelper(auctioneer);
	}

	@Override
	public void reset() {
		super.reset();

		traderTrack.clear();
		profitTrack.clear();
		accumulatedMarketShare.clear();
		accumulatedProfitShare.clear();
		profitScores.clear();
		marketShare.clear();

		init0();
	}

	private void init0() {
		marketShareRate = new Forecasting(10, 1, 0.5D, true);
	}

	/**
	 * 
	 * @return the profit charge to use based on past market shares in the sliding
	 *         window and the average market share among markets.
	 */
	protected double getChargingLevel() {
		final double[] leveledCharges = new double[7];

		leveledCharges[0] = 0.0D;
		leveledCharges[1] = 0.03D;
		leveledCharges[2] = 0.04D;
		leveledCharges[3] = 0.04D;
		leveledCharges[4] = 0.04D;
		leveledCharges[5] = 0.04D;
		leveledCharges[6] = 0.05D;

		calculateScores();
		double curMarketShare = 0.0D;

		// logger.info("total traders: " + totalNumberOfTraders);
		// logger.info("registered traders: " + numberOfTradersRegisteredToday);

		if (totalNumberOfTraders != 0) {
			curMarketShare = ((double) numberOfTradersRegisteredToday)
					/ totalNumberOfTraders;
		}

		marketShareRate.addItem(0, curMarketShare);

		if (currentDate < 50) {
			chargingLevel = 0;
		} else if (currentDate < 130) {
			if (marketShareRate.nonZeroMean(0) < avgMarketShare) {
				chargingLevel = 0;
			} else {
				chargingLevel = 1;
			}
		} else if (currentDate < 200) {
			if (marketShareRate.nonZeroMean(0) < avgMarketShare) {
				chargingLevel = 1;
			} else {
				chargingLevel = 2;
			}
		} else if (currentDate < 300) {
			if (marketShareRate.nonZeroMean(0) < avgMarketShare) {
				chargingLevel = 2;
			} else {
				chargingLevel = 3;
			}
		} else if (currentDate < 400) {
			if (marketShareRate.nonZeroMean(0) < avgMarketShare) {
				chargingLevel = 3;
			} else {
				chargingLevel = 4;
			}
		} else if (currentDate < 450) {
			if (marketShareRate.nonZeroMean(0) < avgMarketShare) {
				chargingLevel = 4;
			} else {
				chargingLevel = 5;
			}
		} else if (marketShareRate.nonZeroMean(0) < avgMarketShare) {
			chargingLevel = 5;
		} else {
			chargingLevel = 6;
		}

		if (marketShareRate.nonZeroMean(0) < (0.5D * avgMarketShare)) {
			chargingLevel = 0;
		}

		if ((helper.getMarketInfo().getTotalDaysRecorded() < 15)
				&& (currentDate >= 30)) {
			chargingLevel = 1;
		}

		// logger.info("market share no zero mean: " +
		// marketShareRate.nonZeroMean(0));
		// logger.info("charging level: " + chargingLevel);
		// logger.info("");

		return leveledCharges[chargingLevel];
	}

	protected void updateFees() {
		final double profitFee = getChargingLevel();

		// double[][] result = calculateAverageFees();

		// if (minTransactionProfit < 9999.0D) {
		// transactionCharging.addItem(0, minTransactionProfit);
		// }
		// double transactionFee = 0.5D * Math
		// .min(this.transactionCharging.lowest(0, 3),
		// this.transactionCharging.mean(0));

		fees[0] = 0.0D;
		fees[1] = 0.0D;
		fees[2] = 0.0D;
		fees[3] = 0.0D;
		fees[4] = profitFee;

		if (fees[4] < 0.0D) {
			fees[4] = 0.0D;
		}
		currentCharging = fees[4];

		minTransactionProfit = 9999.0D;

		// final String ss = MathUtil.round(fees[0], 2) + " "
		// + MathUtil.round(fees[1], 2) + " " + MathUtil.round(fees[2], 2) + " "
		// + MathUtil.round(fees[3], 2) + " " + MathUtil.round(fees[4], 2);

		// data.putIn(ss);

		feeTable.clear();
		numberOfTradersRegisteredToday = 0;
		totalNumberOfTraders = 0;

		totalNumberOfActiveSpecialistsInToday = 0;
		noOneChargeProfit = true;
		highestProfitChargeNow = 0.0D;
	}

	protected void updateSpecialistProfit(final Specialist specialist) {
		Forecasting oldProfit = profitTrack.get(specialist.getId());

		if (oldProfit == null) {
			oldProfit = new Forecasting(8, 1, 0.8D, true);
			oldProfit.addItem(0, specialist.getAccount().getBalance());
			profitTrack.put(specialist.getId(), oldProfit);
		} else {
			final double dailyProfit = specialist.getAccount().getBalance()
					- oldProfit.lastValue(0);

			oldProfit.addItem(0, dailyProfit);
		}
	}

	protected void updateRegisteredTraders(
			final RegisteredTradersAnnouncedEvent event) {
		final int numOfTraders = event.getNumOfTraders();
		totalNumberOfTraders += numOfTraders;

		if (event.getSpecialist().getId().equals(getAuctioneer().getName())) {
			numberOfTradersRegisteredToday = numOfTraders;
		}

		Forecasting oldTrader = traderTrack.get(event.getSpecialist().getId());
		if (oldTrader == null) {
			oldTrader = new Forecasting(8, 1, 0.8D, false);
			traderTrack.put(event.getSpecialist().getId(), oldTrader);
		}
		oldTrader.addItem(0, numOfTraders);
	}

	protected void recordPostedFees(final FeesAnnouncedEvent event) {
		final double[] fees = event.getFees();
		feeTable.put(event.getSpecialist().getId(), fees);

		if (fees[4] > 0.0D) {
			noOneChargeProfit = false;
		}

		if (fees[4] > highestProfitChargeNow) {
			highestProfitChargeNow = fees[4];
		}

		totalNumberOfActiveSpecialistsInToday += 1;
	}

	protected void calculateScores() {

		profitScores.clear();
		marketShare.clear();

		double profitTotal = 0.0D;
		double traderTotal = 0.0D;

		for (final Enumeration<String> enume = profitTrack.keys(); enume
				.hasMoreElements();) {
			final String id = enume.nextElement();
			final Forecasting item = profitTrack.get(id);
			profitScores.put(id, Double.valueOf(item.lastValue(0)));
			profitTotal += item.lastValue(0);
		}

		if (profitTotal != 0.0D) {
			for (final Enumeration<String> enume = profitScores.keys(); enume
					.hasMoreElements();) {
				final String id = enume.nextElement();
				Double temp = profitScores.get(id);
				profitScores.put(id, Double.valueOf(temp.doubleValue() / profitTotal));
				temp = profitScores.get(id);

				final Double tt = accumulatedProfitShare.get(id);
				if (tt == null) {
					accumulatedProfitShare.put(id, Double.valueOf(0.0D));
				} else {
					final double value = tt.doubleValue() + temp.doubleValue();
					accumulatedProfitShare.remove(id);
					accumulatedProfitShare.put(id, Double.valueOf(value));
				}
			}
		}

		int specialistCounter = 0;

		for (final Enumeration<String> enume = traderTrack.keys(); enume
				.hasMoreElements();) {
			final String id = enume.nextElement();
			final Forecasting item = traderTrack.get(id);
			marketShare.put(id, Double.valueOf(item.lastValue(0)));
			traderTotal += item.lastValue(0);

			final Double tt = accumulatedMarketShare.get(id);
			if (tt == null) {
				accumulatedMarketShare.put(id, Double.valueOf(0.0D));
			} else {
				final double value = tt.doubleValue() + item.lastValue(0);
				accumulatedMarketShare.remove(id);
				accumulatedMarketShare.put(id, Double.valueOf(value));
			}

			if (item.lastValue(0) > 0.0D) {
				specialistCounter++;
			}
		}
		numOfSpecialists = specialistCounter;

		avgMarketShare = (1.0D / totalNumberOfActiveSpecialistsInToday);

		traderLeader = "";
		double maxMS = 0.0D;

		// logger.info(accumulatedMarketShare);

		for (final Enumeration<String> enume = accumulatedMarketShare.keys(); enume
				.hasMoreElements();) {
			final String id = enume.nextElement();
			final Double temp = accumulatedMarketShare.get(id);
			if (temp.doubleValue() > maxMS) {
				maxMS = temp.doubleValue();
				traderLeader = id;
			}
		}

		profitLeader = "";
		double maxPS = 0.0D;

		for (final Enumeration<String> enume = accumulatedProfitShare.keys(); enume
				.hasMoreElements();) {
			final String id = enume.nextElement();
			final Double temp = accumulatedProfitShare.get(id);
			if (temp.doubleValue() > maxPS) {
				maxPS = temp.doubleValue();
				profitLeader = id;
			}
		}

		// logger.info(accumulatedProfitShare);

		// logger.info("trader leader: " + traderLeader + "; profit leader: "
		// + profitLeader);

		marketLeader = traderLeader;

		if (currentDate > 5) {
			final double[] feeArray = feeTable.get(marketLeader);

			// TODO: feeArray could be NULL !
			marketLeaderFee = Math.min(0.4D, feeArray == null ? 0.0D : feeArray[4]);
		}

		if (traderTotal != 0.0D) {
			for (final Enumeration<String> enume = marketShare.keys(); enume
					.hasMoreElements();) {
				final String id = enume.nextElement();
				Double temp = marketShare.get(id);
				marketShare.put(id, Double.valueOf(temp.doubleValue() / traderTotal));
				temp = marketShare.get(id);
			}
		}
	}

	protected double[][] calculateAverageFees() {
		// number of markets charging fees on shouts, transactions and profit
		final int[] chargingMap = new int[3];
		for (int i = 0; i < 3; i++) {
			chargingMap[i] = 0;
		}

		// avg, max, min, avg of higher fees
		final double[][] averFees = new double[4][5];

		for (int i = 0; i < 5; i++) {
			averFees[0][i] = 0.0D;
			averFees[1][i] = 0.0D;
			averFees[2][i] = 10000.0D;
			for (final Enumeration<String> enume = feeTable.keys(); enume
					.hasMoreElements();) {
				final double[] feeArray = feeTable.get(enume.nextElement());

				averFees[0][i] += feeArray[i];
				if (averFees[1][i] < feeArray[i]) {
					averFees[1][i] = feeArray[i];
				}
				if ((averFees[2][i] > feeArray[i]) && (feeArray[i] > 0.0D)) {
					averFees[2][i] = feeArray[i];
				}
				if ((i >= 2) && (feeArray[i] > 0.0D)) {
					chargingMap[(i - 2)] += 1;
				}

				if (averFees[2][i] > 999.0D) {
					averFees[2][i] = 0.0D;
				}
			}
		}

		for (int i = 0; i < 5; i++) {
			averFees[3][i] = 0.0D;
			for (final Enumeration<String> enume = feeTable.keys(); enume
					.hasMoreElements();) {
				final double[] feeArray = feeTable.get(enume.nextElement());
				if ((feeArray[i] != averFees[1][i]) || (feeArray[i] == averFees[2][i])) {
					averFees[3][i] += feeArray[i];
				}
			}
		}

		for (int i = 0; i < 5; i++) {
			averFees[0][i] /= feeTable.size();
			averFees[3][i] /= feeTable.size();
		}

		int max = -1;
		for (int j = 0; j < chargingMap.length; j++) {
			if (max < chargingMap[j]) {
				max = j;
			}
		}
		numOfChargingMarkets = max;

		if (numOfChargingMarkets == -1) {
			numOfChargingMarkets = 2;
		}
		return averFees;
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if ((event instanceof DayClosedEvent)) {
			currentDate = event.getDay();
			updateFees();
		} else if ((event instanceof FeesAnnouncedEvent)) {
			recordPostedFees((FeesAnnouncedEvent) event);
		} else if ((event instanceof ProfitAnnouncedEvent)) {
			updateSpecialistProfit(((ProfitAnnouncedEvent) event).getSpecialist());
		} else if ((event instanceof RegisteredTradersAnnouncedEvent)) {
			updateRegisteredTraders((RegisteredTradersAnnouncedEvent) event);
		} else if ((event instanceof TransactionExecutedEvent)) {
			final TransactionExecutedEvent e = (TransactionExecutedEvent) event;
			final double diff = e.getTransaction().getBid().getPrice()
					- e.getTransaction().getAsk().getPrice();
			if (diff < minTransactionProfit) {
				minTransactionProfit = diff;
			}
		}
	}
}