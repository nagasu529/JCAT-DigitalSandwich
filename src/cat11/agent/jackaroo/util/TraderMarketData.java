/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo.util;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cuny.struct.FixedLengthQueue;

/**
 * 
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class TraderMarketData {

	protected String traderId;

	protected String specialistId;

	protected int windowSize;

	protected static final int defaultWindowSize = 30;

	protected SortedSet<Integer> daysIn;

	protected int theDayBeforLastDayIn;

	protected int lastDayIn;

	protected int maxNumberOfItemsTradedInOneDay;

	protected int numberOfItemsTradedInLastDayIn;

	protected FixedLengthQueue acceptedShoutPrices;

	protected double lastShoutPrice;

	protected FixedLengthQueue transactionPrices;

	protected double lastTransactionPrice;

	public TraderMarketData(final String traderId, final String specialistId,
			final int windowSize) {
		this.traderId = traderId;
		this.specialistId = specialistId;
		this.windowSize = windowSize;

		initialize();
	}

	private void initialize() {
		daysIn = new TreeSet<Integer>();
		lastDayIn = -1;
		theDayBeforLastDayIn = -1;

		lastShoutPrice = -1.0D;
		lastTransactionPrice = -1.0D;

		maxNumberOfItemsTradedInOneDay = 0;
		numberOfItemsTradedInLastDayIn = 0;

		acceptedShoutPrices = new FixedLengthQueue((int) (windowSize * 3 * 0.33D));

		transactionPrices = new FixedLengthQueue((int) (windowSize * 1.5D * 0.33D));
	}

	public void reset() {
		daysIn.clear();
		lastDayIn = -1;
		theDayBeforLastDayIn = -1;

		lastShoutPrice = -1.0D;
		lastTransactionPrice = -1.0D;

		maxNumberOfItemsTradedInOneDay = 0;
		numberOfItemsTradedInLastDayIn = 0;

		acceptedShoutPrices.reset();
		transactionPrices.reset();
	}

	public boolean addDay(final int day) {
		if (!daysIn.contains(Integer.valueOf(day))) {
			theDayBeforLastDayIn = lastDayIn;
			lastDayIn = day;

			if (numberOfItemsTradedInLastDayIn > maxNumberOfItemsTradedInOneDay) {
				maxNumberOfItemsTradedInOneDay = numberOfItemsTradedInLastDayIn;
			}

			numberOfItemsTradedInLastDayIn = 0;

			clearOldData(day);

			return daysIn.add(Integer.valueOf(day));
		}
		return true;
	}

	public boolean removeDay(final int day) {
		return daysIn.remove(Integer.valueOf(day));
	}

	public void clearOldData(final int currentDay) {
		if (!daysIn.isEmpty()) {
			int earliestDayIn = daysIn.first().intValue();
			while (((currentDay - earliestDayIn) >= windowSize)
					&& (!daysIn.isEmpty())) {
				removeDay(earliestDayIn);
				if (!daysIn.isEmpty()) {
					earliestDayIn = daysIn.first().intValue();
				}
			}
		}
	}

	public boolean addShoutPrice(final double price) {
		lastShoutPrice = price;
		acceptedShoutPrices.newData(price);
		return true;
	}

	public boolean addTransactionPrice(final double price) {
		lastTransactionPrice = price;
		transactionPrices.newData(price);

		numberOfItemsTradedInLastDayIn += 1;

		return true;
	}

	public int getDaysInSize() {
		return daysIn.size();
	}

	public int getWindowSize() {
		return windowSize;
	}

	public int getLastDayIn() {
		return lastDayIn;
	}

	public int getTheDayBeforLastDayIn() {
		return theDayBeforLastDayIn;
	}

	public int getShoutPriceQueueSize() {
		return acceptedShoutPrices.getN();
	}

	public int getTransactionPriceQueueSize() {
		return transactionPrices.getN();
	}

	public double getShoutPriceMean() {
		return acceptedShoutPrices.getMean();
	}

	public double getTransactionPriceMean() {
		return transactionPrices.getMean();
	}

	public double getShoutPriceStdDev() {
		return acceptedShoutPrices.getStdDev();
	}

	public double getTransactionPriceStdDev() {
		return transactionPrices.getStdDev();
	}

	public double getLastShoutPrice() {
		return lastShoutPrice;
	}

	public double getLastTransactionPrice() {
		return lastTransactionPrice;
	}

	public int getNumberOfShouts() {
		return acceptedShoutPrices.getN();
	}

	public double getAllShoutsSum() {
		return acceptedShoutPrices.getTotal();
	}

	public int getMaxNumberOfItemsTradedInOneDay() {
		return maxNumberOfItemsTradedInOneDay;
	}

	public int getNumberOfItemsTradedInLastDayIn() {
		return numberOfItemsTradedInLastDayIn;
	}

	@Override
	public String toString() {
		String out;
		if ((traderId != null) && (specialistId != null)) {
			out = "For trader:" + traderId + " in market:" + specialistId;
		} else {
			out = "No trader and market ids specified";
		}

		out = out + "\nNo. of days:" + getDaysInSize() + "\nwith day set:"
				+ Arrays.toString(daysIn.toArray(new Integer[getDaysInSize()]))
				+ "\nlast day in:" + lastDayIn + "\nwith windowsize:" + windowSize
				+ "\nShout mean price:" + getShoutPriceMean()
				+ " for total No. of shouts:" + getShoutPriceQueueSize()
				+ "\nwith shout details:\n" + acceptedShoutPrices.toString()
				+ "\nTransaction mean price:" + getTransactionPriceMean()
				+ " for total No. of transactions:" + getTransactionPriceQueueSize()
				+ "\nwith transaction price details:\n" + transactionPrices.toString();

		return out;
	}
}