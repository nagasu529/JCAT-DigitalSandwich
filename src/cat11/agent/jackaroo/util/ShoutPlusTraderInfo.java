/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo.util;

import edu.cuny.cat.core.Shout;

/**
 * 
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class ShoutPlusTraderInfo implements Comparable<ShoutPlusTraderInfo> {
	protected Shout shout;

	protected TraderMarketStatistic traderInfo;

	public ShoutPlusTraderInfo(final Shout shout,
			final TraderMarketStatistic traderInfo) {
		this.shout = shout;
		this.traderInfo = traderInfo;
	}

	public Shout getShout() {
		return shout;
	}

	public TraderMarketStatistic getTraderInfo() {
		return traderInfo;
	}

	/**
	 * 
	 */
	@Override
	public int compareTo(final ShoutPlusTraderInfo other) {
		double myPrice;
		if ((traderInfo != null)
				&& (traderInfo.getBestSpecialistTransactionMean() > 0.0D)) {
			if (shout.isAsk()) {
				myPrice = Math.max(traderInfo.getBestSpecialistTransactionMean(),
						shout.getPrice());
			} else {
				myPrice = Math.min(traderInfo.getBestSpecialistTransactionMean(),
						shout.getPrice());
			}
		} else {
			myPrice = shout.getPrice();
		}

		final TraderMarketStatistic otherTraderInfo = other.getTraderInfo();
		final Shout otherShout = other.getShout();
		double otherPrice;
		if ((otherTraderInfo != null)
				&& (otherTraderInfo.getBestSpecialistTransactionMean() > 0.0D)) {
			if (otherShout.isAsk()) {
				otherPrice = Math.max(
						otherTraderInfo.getBestSpecialistTransactionMean(),
						otherShout.getPrice());
			} else {
				otherPrice = Math.min(
						otherTraderInfo.getBestSpecialistTransactionMean(),
						otherShout.getPrice());
			}
		} else {
			otherPrice = otherShout.getPrice();
		}

		if (myPrice > otherPrice) {
			return 1;
		}
		if (myPrice < otherPrice) {
			return -1;
		}
		return 0;
	}
}