/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo.util;

/**
 * 
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.core.Specialist;
import edu.cuny.cat.core.Trader;
import edu.cuny.cat.core.Transaction;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.AuctionEventListener;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpenedEvent;
import edu.cuny.cat.event.RoundOpenedEvent;
import edu.cuny.cat.event.ShoutPostedEvent;
import edu.cuny.cat.event.TransactionPostedEvent;
import edu.cuny.obj.Resetable;
import edu.cuny.struct.FixedLengthQueue;

public class MarketInfo implements Resetable, Serializable,
		AuctionEventListener {
	private static final long serialVersionUID = 1L;

	protected String myMarketId;

	protected Map<String, Map<String, TraderMarketData>> dataMap;

	protected SortedSet<String> traders;

	protected SortedSet<String> myEachDayTraders;

	protected SortedSet<String> myAllTraders;

	protected int numberOfTradersUntilLastDay;

	protected int numberOfTradersLastDay;

	protected SortedSet<String> specialists;

	protected Map<String, Shout> shouts = new HashMap<String, Shout>();

	protected FixedLengthQueue shoutRecordAsks = new FixedLengthQueue(5000);

	protected FixedLengthQueue shoutRecordBids = new FixedLengthQueue(5000);

	protected FixedLengthQueue lastDayShoutRecordAsks = new FixedLengthQueue(1000);

	protected FixedLengthQueue lastDayShoutRecordBids = new FixedLengthQueue(1000);

	protected double firstDayShoutMean;

	protected int shoutsCounterDaily;

	protected FixedLengthQueue transRecordAsks = new FixedLengthQueue(2000);

	protected FixedLengthQueue transRecordBids = new FixedLengthQueue(2000);

	protected FixedLengthQueue lastDayTransRecordAsks = new FixedLengthQueue(500);

	protected FixedLengthQueue lastDayTransRecordBids = new FixedLengthQueue(500);

	protected int transCounterDaily;

	protected FixedLengthQueue myTransPricesDaily = new FixedLengthQueue(200);

	protected int myAcceptCountDaily;

	protected FixedLengthQueue merTransPricesDaily = new FixedLengthQueue(200);

	protected int merAcceptCountDaily;

	protected FixedLengthQueue transPrices = new FixedLengthQueue(1000);

	protected int currentDay;

	protected int currentRound;

	// days passed in the game
	protected int totalDaysRecorded;

	protected int historyDaySize;

	protected static final int defaultHistoryDaySize = 30;

	protected int maxItemsTraderHas;

	// protected DataRecord dataOut;
	//
	// protected DataRecord myTraderOut;

	public MarketInfo(final String myMarketId) {
		this(myMarketId, MarketInfo.defaultHistoryDaySize);
	}

	public MarketInfo(final String myMarketId, final int daySize) {
		this.myMarketId = myMarketId;

		dataMap = Collections
				.synchronizedMap(new HashMap<String, Map<String, TraderMarketData>>());

		traders = new TreeSet<String>();
		myEachDayTraders = new TreeSet<String>();
		myAllTraders = new TreeSet<String>();
		specialists = new TreeSet<String>();

		historyDaySize = daySize;

		init0();

		// initOutPut();
	}

	private void init0() {
		numberOfTradersLastDay = 0;

		totalDaysRecorded = 0;
		maxItemsTraderHas = -1;
	}

	// private void initOutPut() {
	// dataOut = new DataRecord("marketInfo.txt");
	//
	// myTraderOut = new DataRecord("myTraders.csv");
	//
	// final String title =
	// "currentDay;TodayTraders;AllRecordedTraders;NewTradersRecordedToday;OldTradersLostToday;TodayTraderNames;myAcceptedShouts;myTransactions;myTransMean;myTransPricesDaily;MerAcceptedShouts;MerTransactions;MerTransMean;MerTransPrices";
	//
	// myTraderOut.putIn(title);
	// }

	@Override
	public void reset() {
		dataMap.clear();

		traders.clear();
		myAllTraders.clear();
		specialists.clear();

		shoutRecordAsks.reset();
		shoutRecordBids.reset();

		transRecordAsks.reset();
		transRecordBids.reset();

		transPrices.reset();

		init0();
	}

	public TraderMarketStatistic getTraderMarketStatistic(final String traderId) {
		TraderMarketStatistic result;
		if ((dataMap.containsKey(traderId)) && (currentDay > -1)) {
			final Map<String, TraderMarketData> marketMap = dataMap.get(traderId);
			result = new TraderMarketStatistic(traderId, marketMap, currentDay,
					specialists.size(), myMarketId);
		} else {
			result = null;
		}
		return result;
	}

	public int getTotalDaysRecorded() {
		return totalDaysRecorded;
	}

	public int getMaxItemsTraderHas() {
		return maxItemsTraderHas;
	}

	public double getMeanAcceptedShoutPrice() {
		return (shoutRecordAsks.getMean() + shoutRecordBids.getMean()) / 2.0D;
	}

	public double getLastDayMeanAcceptedShoutPrice() {
		return (lastDayShoutRecordAsks.getMean() + lastDayShoutRecordBids.getMean()) / 2.0D;
	}

	public double getFirstDayMeanAcceptedShoutPrice() {
		return firstDayShoutMean;
	}

	public double getMeanTransShoutPrice() {
		return (transRecordAsks.getMean() + transRecordBids.getMean()) / 2.0D;
	}

	public double getLastDayMeanTransShoutPrice() {
		return (lastDayTransRecordAsks.getMean() + lastDayTransRecordBids.getMean()) / 2.0D;
	}

	public int getTransCounter() {
		return transCounterDaily;
	}

	public int getShoutsCounter() {
		return shoutsCounterDaily;
	}

	public double getMeanTransPrice() {
		return transPrices.getMean();
	}

	public void printMerTransInfo() {
		System.out.println("Mer accepted shouts:" + merAcceptCountDaily);
		System.out.println("Mer's tans No|mean:" + merTransPricesDaily.getN() + "|"
				+ merTransPricesDaily.getMean());
		System.out.println(merTransPricesDaily.toString());
	}

	protected void addShoutPrice(final String traderId,
			final String specialistId, final double price, final int day,
			final int daySize) {
		Map<String, TraderMarketData> traderMarketDataMap;
		if (dataMap.containsKey(traderId)) {
			traderMarketDataMap = dataMap.get(traderId);
		} else {
			traderMarketDataMap = Collections
					.synchronizedMap(new HashMap<String, TraderMarketData>());
		}
		TraderMarketData traderMarketData;
		if (traderMarketDataMap.containsKey(specialistId)) {
			traderMarketData = traderMarketDataMap.get(specialistId);
		} else {
			traderMarketData = new TraderMarketData(traderId, specialistId, daySize);
		}

		traderMarketData.addDay(day);
		traderMarketData.addShoutPrice(price);

		traderMarketDataMap.put(specialistId, traderMarketData);
		dataMap.put(traderId, traderMarketDataMap);

		traders.add(traderId);
		specialists.add(specialistId);
	}

	protected void addTransactionPrice(final String traderId,
			final String specialistId, final double price, final int day,
			final int daySize) {
		Map<String, TraderMarketData> traderMarketDataMap;
		if (dataMap.containsKey(traderId)) {
			traderMarketDataMap = dataMap.get(traderId);
		} else {
			traderMarketDataMap = Collections
					.synchronizedMap(new HashMap<String, TraderMarketData>());
		}
		TraderMarketData traderMarketData;
		if (traderMarketDataMap.containsKey(specialistId)) {
			traderMarketData = traderMarketDataMap.get(specialistId);
		} else {
			traderMarketData = new TraderMarketData(traderId, specialistId, daySize);
		}

		traderMarketData.addDay(day);
		traderMarketData.addTransactionPrice(price);

		final int maxItemsTradedForThisTrader = traderMarketData
				.getMaxNumberOfItemsTradedInOneDay();

		if (maxItemsTradedForThisTrader > maxItemsTraderHas) {
			maxItemsTraderHas = maxItemsTradedForThisTrader;
		}

		traderMarketDataMap.put(specialistId, traderMarketData);
		dataMap.put(traderId, traderMarketDataMap);

		traders.add(traderId);
		specialists.add(specialistId);
	}

	protected void processShoutPosted(final ShoutPostedEvent event) {
		final Shout shout = event.getShout();
		final Trader trader = shout.getTrader();
		final Specialist specialist = shout.getSpecialist();

		final int day = event.getDay();

		if ((trader == null) || (specialist == null) || (day != currentDay)) {
			System.out
					.println("A shout POST without trader or specialist id or day time does not match, can't add to MarketInfo");
		} else {
			final String traderId = trader.getId();
			final String specialistId = specialist.getId();
			final double price = shout.getPrice();

			if (shouts.containsKey(shout.getId())) {
				final Shout oldShout = shouts.get(shout.getId());

				if (oldShout.getPrice() != price) {
					oldShout.setPrice(price);
					addShoutPrice(traderId, specialistId, price, day, historyDaySize);
				}
			} else {
				shouts.put(shout.getId(), shout);
				addShoutPrice(traderId, specialistId, price, day, historyDaySize);

				if (currentRound <= 9) {
					if (shout.isAsk()) {
						shoutRecordAsks.newData(shout.getPrice());
						lastDayShoutRecordAsks.newData(shout.getPrice());
					} else {
						shoutRecordBids.newData(shout.getPrice());
						lastDayShoutRecordBids.newData(shout.getPrice());
					}
					shoutsCounterDaily += 1;
				}
			}

			if (specialist.getId().equals(myMarketId)) {
				myEachDayTraders.add(trader.getId());
				myAllTraders.add(trader.getId());
				myAcceptCountDaily += 1;
			}

			if (specialist.getId().equals("Mertacor")) {
				merAcceptCountDaily += 1;
			}
		}
	}

	protected void processTransactionPosted(final TransactionPostedEvent event) {
		final Transaction transaction = event.getTransaction();
		final Specialist specialist = transaction.getSpecialist();

		final Trader askTrader = transaction.getAsk().getTrader();
		final Trader bidTrader = transaction.getBid().getTrader();

		final int day = event.getDay();

		if ((askTrader == null) || (bidTrader == null) || (specialist == null)
				|| (day != currentDay)) {
			System.out
					.println("A Transaction POST without trader or specialist id or day time does not match, can't add to MarketInfo");
		} else {
			final String askTraderId = askTrader.getId();
			final String bidTraderId = bidTrader.getId();
			final String specialistId = specialist.getId();
			final double price = transaction.getPrice();

			addTransactionPrice(askTraderId, specialistId, price, day, historyDaySize);
			addTransactionPrice(bidTraderId, specialistId, price, day, historyDaySize);

			if (specialist.getId().equals(myMarketId)) {
				myEachDayTraders.add(askTrader.getId());
				myEachDayTraders.add(bidTrader.getId());
				myAllTraders.add(askTrader.getId());
				myAllTraders.add(bidTrader.getId());

				myTransPricesDaily.newData(price);
			}

			if (specialist.getId().equals("Mertacor")) {
				merTransPricesDaily.newData(price);
			}

		}

		transRecordAsks.newData(transaction.getAsk().getPrice());
		transRecordBids.newData(transaction.getBid().getPrice());
		lastDayTransRecordAsks.newData(transaction.getAsk().getPrice());
		lastDayTransRecordBids.newData(transaction.getBid().getPrice());
		transCounterDaily += 1;
		transPrices.newData(transaction.getPrice());
	}

	protected void processDayOpened(final DayOpenedEvent event) {
		currentDay = event.getDay();
		totalDaysRecorded += 1;
		shoutsCounterDaily = 0;
		transCounterDaily = 0;
		lastDayShoutRecordAsks.reset();
		lastDayShoutRecordBids.reset();
		lastDayTransRecordAsks.reset();
		lastDayTransRecordBids.reset();

		myTransPricesDaily.reset();
		myAcceptCountDaily = 0;
		merTransPricesDaily.reset();
		merAcceptCountDaily = 0;

		numberOfTradersLastDay = myEachDayTraders.size();
		myEachDayTraders.clear();

		numberOfTradersUntilLastDay = myAllTraders.size();
	}

	protected void processDayClosed(final DayClosedEvent event) {
		if (currentDay == 0) {
			firstDayShoutMean = getMeanAcceptedShoutPrice();
		}

		// if ((totalDaysRecorded % historyDaySize) == (historyDaySize - 1)) {
		// ;
		// }
		shouts.clear();

		// String traderR = this.currentDay
		// + ";"
		// + this.myEachDayTraders.size()
		// + ";"
		// + this.myAllTraders.size()
		// + ";"
		// + (this.myAllTraders.size() - this.numberOfTradersUntilLastDay)
		// + ";"
		// + (this.numberOfTradersLastDay
		// + (this.myAllTraders.size() - this.numberOfTradersUntilLastDay) -
		// this.myEachDayTraders
		// .size()) + ";" + this.myEachDayTraders.toString() + ";"
		// + this.myAcceptCount + ";" + this.myTransPrices.getN() + ";"
		// + this.myTransPrices.getMean() + ";" + this.myTransPrices.toString()
		// + ";" + this.merAcceptCount + ";" + this.merTransPrices.getN() + ";"
		// + this.merTransPrices.getMean() + ";" + this.merTransPrices.toString();
	}

	public void getMyTodaysTradersInfo() {
		final Iterator<String> i = myEachDayTraders.iterator();
		System.out
				.println("Traders in My Market Today:"
						+ myEachDayTraders.size()
						+ "\nNumber of new traders we get in last day:"
						+ (myAllTraders.size() - numberOfTradersUntilLastDay)
						+ "\nNumber of traders lost in today:"
						+ ((numberOfTradersLastDay + (myAllTraders.size() - numberOfTradersUntilLastDay)) - myEachDayTraders
								.size()));

		while (i.hasNext()) {
			System.out.println(i.next());
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		if ((event instanceof ShoutPostedEvent)) {
			processShoutPosted((ShoutPostedEvent) event);
		} else if ((event instanceof TransactionPostedEvent)) {
			processTransactionPosted((TransactionPostedEvent) event);
		} else if ((event instanceof DayOpenedEvent)) {
			processDayOpened((DayOpenedEvent) event);
		} else if ((event instanceof DayClosedEvent)) {
			processDayClosed((DayClosedEvent) event);
		} else if ((event instanceof RoundOpenedEvent)) {
			currentRound = event.getRound();
		}
	}

	// protected void printMarketInfo() {
	// dataOut
	// .putIn("***********************\n  start print market info \n***********************");
	// final Iterator<String> iterator = dataMap.keySet().iterator();
	//
	// while (iterator.hasNext()) {
	// final String traderId = iterator.next();
	// final Map<String, TraderMarketData> marketMap = dataMap.get(traderId);
	//
	// dataOut.writeIn(">>>>>>>>>>>" + traderId + "<<<<<<<<<<<<");
	//
	// final Iterator<String> innerIterator = marketMap.keySet().iterator();
	// while (innerIterator.hasNext()) {
	// final String specialistId = innerIterator.next();
	//
	// dataOut.writeIn("++++++++++" + specialistId + "++++++++++++");
	//
	// final TraderMarketData marketData = marketMap.get(specialistId);
	//
	// dataOut.writeIn(marketData.toString());
	// }
	// }
	//
	// dataOut
	// .putIn("***********************\n  end print market info \n***********************");
	// }
}