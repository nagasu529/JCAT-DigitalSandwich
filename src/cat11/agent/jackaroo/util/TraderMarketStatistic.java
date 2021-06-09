/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo.util;

import java.util.Iterator;
import java.util.Map;

/**
 * 
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class TraderMarketStatistic {
	protected String traderId;

	protected String myMarketId;

	protected int numberofSpecialists;

	protected int totalNumberofSpecialists;

	protected int historyWindowSize;

	protected int totalDays;

	protected int currentDay;

	protected double marketSelectionStdDev;

	protected int daysInMyMarket;

	protected int theDayBeforeLastDayInMyMarket;

	protected int lastDayInMyMarket;

	protected int numberOfItemsTradedInLastDayInMyMarket;

	protected String bestSpecialist;

	protected int daysInBestSpecialist;

	protected int lastDayInBestSpecialist;

	protected double bestSpecialistLastShoutPrice;

	protected double bestSpecialistShoutMean;

	protected double bestSpecialistShoutStdDev;

	protected double bestSpecialistLastTransactionPrice;

	protected double bestSpecialistTransactionMean;

	protected double bestSpecialistTransactionStdDev;

	protected int bestSpecialistNumberOfItemsTradedInLastDayIn;

	protected int bestSpecialistMaxNumberOfItemsTradedInOneDay;

	protected double avgShoutMean;

	protected Map<String, TraderMarketData> marketMap;

	public TraderMarketStatistic(final String traderId,
			final Map<String, TraderMarketData> data, final int cDay,
			final int noOfMarkets, final String myMarketId) {
		this.traderId = traderId;
		this.myMarketId = myMarketId;
		marketMap = data;
		currentDay = cDay;
		totalNumberofSpecialists = noOfMarkets;

		initialize();
	}

	protected void initialize() {
		numberofSpecialists = -1;

		totalDays = -1;

		historyWindowSize = -1;

		marketSelectionStdDev = -1.0D;

		daysInMyMarket = 0;

		lastDayInMyMarket = -1;

		theDayBeforeLastDayInMyMarket = -1;

		numberOfItemsTradedInLastDayInMyMarket = -1;

		bestSpecialist = "NaN";

		daysInBestSpecialist = -1;

		lastDayInBestSpecialist = -1;

		bestSpecialistLastShoutPrice = -1.0D;

		bestSpecialistShoutMean = -1.0D;

		bestSpecialistShoutStdDev = -1.0D;

		bestSpecialistLastTransactionPrice = -1.0D;

		bestSpecialistTransactionMean = -1.0D;

		bestSpecialistTransactionStdDev = -1.0D;

		bestSpecialistNumberOfItemsTradedInLastDayIn = -1;

		bestSpecialistMaxNumberOfItemsTradedInOneDay = -1;

		avgShoutMean = -1.0D;

		analyze();
	}

	protected void analyze() {
		final Iterator<String> iterator = marketMap.keySet().iterator();

		double totalSq = 0.0D;
		int sumDays = 0;
		int numberofMarkets = 0;

		int numberOfShouts = 0;
		double shoutsPriceSum = 0.0D;

		while (iterator.hasNext()) {
			final String specialistId = iterator.next();
			final TraderMarketData marketData = marketMap.get(specialistId);

			marketData.clearOldData(currentDay);

			historyWindowSize = marketData.getWindowSize();

			if (specialistId.equals(myMarketId)) {
				lastDayInMyMarket = marketData.getLastDayIn();
				theDayBeforeLastDayInMyMarket = marketData.getTheDayBeforLastDayIn();
				daysInMyMarket = marketData.getDaysInSize();

				numberOfItemsTradedInLastDayInMyMarket = marketData
						.getNumberOfItemsTradedInLastDayIn();
			}

			final int dayIn = marketData.getDaysInSize();
			if (dayIn > 0) {
				sumDays += dayIn;
				totalSq += dayIn * dayIn;
				numberofMarkets++;

				if (bestSpecialist.equals("NaN")) {
					bestSpecialist = specialistId;

					daysInBestSpecialist = dayIn;

					lastDayInBestSpecialist = marketData.getLastDayIn();

					bestSpecialistLastShoutPrice = marketData.getLastShoutPrice();

					bestSpecialistShoutMean = marketData.getShoutPriceMean();

					bestSpecialistShoutStdDev = marketData.getShoutPriceStdDev();

					bestSpecialistLastTransactionPrice = marketData
							.getLastTransactionPrice();

					bestSpecialistTransactionMean = marketData.getTransactionPriceMean();

					bestSpecialistTransactionStdDev = marketData
							.getTransactionPriceStdDev();

					bestSpecialistNumberOfItemsTradedInLastDayIn = marketData
							.getNumberOfItemsTradedInLastDayIn();

					bestSpecialistMaxNumberOfItemsTradedInOneDay = marketData
							.getMaxNumberOfItemsTradedInOneDay();
				} else if (dayIn > daysInBestSpecialist) {
					bestSpecialist = specialistId;

					daysInBestSpecialist = dayIn;

					lastDayInBestSpecialist = marketData.getLastDayIn();

					bestSpecialistLastShoutPrice = marketData.getLastShoutPrice();

					bestSpecialistShoutMean = marketData.getShoutPriceMean();

					bestSpecialistShoutStdDev = marketData.getShoutPriceStdDev();

					bestSpecialistLastTransactionPrice = marketData
							.getLastTransactionPrice();

					bestSpecialistTransactionMean = marketData.getTransactionPriceMean();

					bestSpecialistTransactionStdDev = marketData
							.getTransactionPriceStdDev();

					bestSpecialistNumberOfItemsTradedInLastDayIn = marketData
							.getNumberOfItemsTradedInLastDayIn();

					bestSpecialistMaxNumberOfItemsTradedInOneDay = marketData
							.getMaxNumberOfItemsTradedInOneDay();
				}

				numberOfShouts += marketData.getNumberOfShouts();
				shoutsPriceSum += marketData.getAllShoutsSum();
			}

		}

		if (numberOfShouts > 0) {
			avgShoutMean = (shoutsPriceSum / numberOfShouts);
		}

		numberofSpecialists = numberofMarkets;
		totalDays = sumDays;
		if (numberofSpecialists == 0) {
			marketSelectionStdDev = 0.0D;
		} else {
			marketSelectionStdDev = Math
					.sqrt((totalSq / totalNumberofSpecialists)
							- ((((double) totalDays) / totalNumberofSpecialists) * (((double) totalDays) / totalNumberofSpecialists)));
		}
	}

	public int getNumberofSpecialists() {
		if (numberofSpecialists == -1) {
			analyze();
		}

		return numberofSpecialists;
	}

	public int getTotalNumberofSpecialists() {
		if (totalNumberofSpecialists == -1) {
			analyze();
		}

		return totalNumberofSpecialists;
	}

	public int getTotalDays() {
		if (totalDays == -1) {
			analyze();
		}

		return totalDays;
	}

	public int getHistoryWindowSize() {
		if (historyWindowSize == -1) {
			analyze();
		}

		return historyWindowSize;
	}

	public double getMarketSelectionStdDev() {
		if (marketSelectionStdDev == -1.0D) {
			analyze();
		}

		return marketSelectionStdDev;
	}

	public int getTheDayBeforeLastDayInMyMarket() {
		return theDayBeforeLastDayInMyMarket;
	}

	public int getLastDayInMyMarket() {
		return lastDayInMyMarket;
	}

	public int getDaysInMyMarket() {
		return daysInMyMarket;
	}

	public int getNumberOfItemsTradedInLastDayInMyMarket() {
		return numberOfItemsTradedInLastDayInMyMarket;
	}

	public String getBestSpecialist() {
		if (bestSpecialist.equals("NaN")) {
			analyze();
		}

		return bestSpecialist;
	}

	public int getDaysInBestSpecialist() {
		if (daysInBestSpecialist == -1) {
			analyze();
		}

		return daysInBestSpecialist;
	}

	public double getBestSpecialistLastShoutPrice() {
		if (bestSpecialistLastShoutPrice == -1.0D) {
			analyze();
		}

		return bestSpecialistLastShoutPrice;
	}

	public double getBestSpecialistShoutMean() {
		if (bestSpecialistShoutMean == -1.0D) {
			analyze();
		}

		return bestSpecialistShoutMean;
	}

	public double getBestSpecialistShoutStdDev() {
		if (bestSpecialistShoutStdDev == -1.0D) {
			analyze();
		}

		return bestSpecialistShoutStdDev;
	}

	public double getBestSpecialistLastTransactionPrice() {
		return bestSpecialistLastTransactionPrice;
	}

	public double getBestSpecialistTransactionMean() {
		if (bestSpecialistTransactionMean == -1.0D) {
			analyze();
		}

		return bestSpecialistTransactionMean;
	}

	public double getBestSpecialistTransactionStdDev() {
		if (bestSpecialistTransactionStdDev == -1.0D) {
			analyze();
		}

		return bestSpecialistTransactionStdDev;
	}

	public double getShoutsMean() {
		return avgShoutMean;
	}

	public String getTrader() {
		if (traderId == null) {
			analyze();
		}

		return traderId;
	}

	public int getBestSpecialistNumberOfItemsTradedInLastDayIn() {
		return bestSpecialistNumberOfItemsTradedInLastDayIn;
	}

	public int getBestSpecialistMaxNumberOfItemsTradedInOneDay() {
		return bestSpecialistMaxNumberOfItemsTradedInOneDay;
	}

	public Map<String, TraderMarketData> getMarketMap() {
		if (marketMap == null) {
			analyze();
		}

		return marketMap;
	}

	@Override
	public String toString() {
		final String out = "\n********Information in TraderMarketStatistic*********\ntraderId:"
				+ traderId
				+ "\nnumberofSpecialists:"
				+ numberofSpecialists
				+ "\ntotalNumberofSpecialists:"
				+ totalNumberofSpecialists
				+ "\ntotalDays:"
				+ totalDays
				+ "\ncurrentDay:"
				+ currentDay
				+ "\nhistoryWindowSize:"
				+ historyWindowSize
				+ "\nmarketSelectionStdDev:"
				+ marketSelectionStdDev
				+ "\ndaysInMyMarket:"
				+ daysInMyMarket
				+ "\ntheDayBeforeLastDayInMyMarket:"
				+ theDayBeforeLastDayInMyMarket
				+ "\nlastDayInMyMarket:"
				+ lastDayInMyMarket
				+ "\nnumberOfItemsTradedInLastDayInMyMarket:"
				+ numberOfItemsTradedInLastDayInMyMarket
				+ "\nbestSpecialist:"
				+ bestSpecialist
				+ "\ndaysInBestSpecialist:"
				+ daysInBestSpecialist
				+ "\nbestSpecialistLastShoutPrice:"
				+ bestSpecialistLastShoutPrice
				+ "\nbestSpecialistShoutMean:"
				+ bestSpecialistShoutMean
				+ "\nbestSpecialistShoutStdDev:"
				+ bestSpecialistShoutStdDev
				+ "\nbestSpecialistLastTransactionPrice:"
				+ bestSpecialistLastTransactionPrice
				+ "\nbestSpecialistTransactionMean:"
				+ bestSpecialistTransactionMean
				+ "\nbestSpecialistTransactionStdDev:"
				+ bestSpecialistTransactionStdDev
				+ "\nbestSpecialistNumberOfItemsTradedInLastDayIn:"
				+ bestSpecialistNumberOfItemsTradedInLastDayIn
				+ "\nbestSpecialistMaxNumberOfItemsTradedInOneDay:"
				+ bestSpecialistMaxNumberOfItemsTradedInOneDay
				+ "\n**************end**************\n";

		return out;
	}
}