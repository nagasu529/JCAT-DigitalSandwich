/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections15.buffer.PriorityBuffer;
import org.apache.log4j.Logger;

import cat11.agent.jackaroo.util.AscendingShoutPlusTraderInfoComparator;
import cat11.agent.jackaroo.util.DescendingShoutPlusTraderInfoComparator;
import cat11.agent.jackaroo.util.ShoutPlusTraderInfo;
import cat11.agent.jackaroo.util.TraderMarketStatistic;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.core.Trader;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayOpenedEvent;
import edu.cuny.cat.event.RoundOpenedEvent;
import edu.cuny.cat.market.DuplicateShoutException;
import edu.cuny.cat.market.matching.FourHeapShoutEngine;

/**
 * This shout engine chooses to match the shouts in three different ways
 * depending upon configured parameters and the time elapsed in the game and
 * during a game day:
 * 
 * 1. Do equilibrium matching but reorder matched shouts so that more
 * competitive bids with more competitive asks if the reordering flag is set and
 * the game has passed a certain number of initial days.
 * 
 * @see #matchShouts1(ArrayList)
 * 
 *      2. Do maximal matching otherwise and if the number of rounds elapsed
 *      during a day is beyond a certain value (0 by default).
 * @see #matchShouts2(ArrayList, int)
 * 
 *      3. Do equilibrium matching, otherwise.
 * @see #matchShouts3(ArrayList)
 * 
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jacShoutEngine extends FourHeapShoutEngine {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static Logger logger = Logger.getLogger(jacShoutEngine.class);

	protected jacHelper helper;

	protected int currentDay;

	protected int currentRound;

	protected boolean Matching1;

	protected double gap;

	protected double initialgap;

	protected double matchMeanDistance = 8.0D;

	protected static final boolean reorderMatchedShouts = false;

	protected static final boolean useMaxVolumeMatching = true;

	protected static final boolean controlMaxMatch = true;

	// protected double transMeanGap;

	protected static final int pureMaximalMatchingUntilDay = 0;

	protected static final int removeTooBadTransBeforeRound = 0;

	protected static final int daysAfterStartUseMean = 10;

	protected static final int maximalMatchStartFromRound = 0;

	protected SortedSet<ShoutPlusTraderInfo> matchedBidsSet;

	protected SortedSet<ShoutPlusTraderInfo> matchedAsksSet;

	protected static AscendingShoutPlusTraderInfoComparator ascendingOrder;

	protected static DescendingShoutPlusTraderInfoComparator descendingOrder;

	public jacShoutEngine() {
		matchedBidsSet = new TreeSet<ShoutPlusTraderInfo>(
				jacShoutEngine.descendingOrder);
		matchedAsksSet = new TreeSet<ShoutPlusTraderInfo>(
				jacShoutEngine.ascendingOrder);
	}

	@Override
	public void initialize() {
		super.initialize();

		helper = jacHelper.getHelper(auctioneer);
	}

	@Override
	public void reset() {
		super.reset();

		matchedBidsSet.clear();
		matchedAsksSet.clear();
	}

	/**
	 * 
	 * @param bIn
	 * @param bOut
	 * @param sIn
	 * @param sOut
	 * @return the maximal matching volume.
	 * @throws DuplicateShoutException
	 */
	public static int getMaxNumberOfMatch(final PriorityBuffer<Shout> bIn,
			final PriorityBuffer<Shout> bOut, final PriorityBuffer<Shout> sIn,
			final PriorityBuffer<Shout> sOut) throws DuplicateShoutException {
		if ((bIn.size() == 0) || (sIn.size() == 0)) {
			return 0;
		}
		if ((bOut.size() == 0) || (sOut.size() == 0)) {
			return bIn.size();
		}

		// TODO:
		// final ExposedFourHeapShoutEngine engine1 = new
		// ExposedFourHeapShoutEngine();
		// final ExposedFourHeapShoutEngine engine2 = new
		// ExposedFourHeapShoutEngine();
		final FourHeapShoutEngine engine1 = new FourHeapShoutEngine();
		final FourHeapShoutEngine engine2 = new FourHeapShoutEngine();

		final Iterator<Shout> ibIn = bIn.iterator();
		while (ibIn.hasNext()) {
			engine1.newShout(ibIn.next());
		}

		final Iterator<Shout> ibOut = bOut.iterator();
		while (ibOut.hasNext()) {
			engine2.newShout(ibOut.next());
		}

		final Iterator<Shout> isIn = sIn.iterator();
		while (isIn.hasNext()) {
			engine2.newShout(isIn.next());
		}

		final Iterator<Shout> isOut = sOut.iterator();
		while (isOut.hasNext()) {
			engine1.newShout(isOut.next());
		}

		// TODO:
		// final int m1 = jacShoutEngine.getMaxNumberOfMatch(engine1.getbIn(),
		// engine1.getbOut(), engine1.getsIn(), engine1.getsOut());
		// final int m2 = jacShoutEngine.getMaxNumberOfMatch(engine2.getbIn(),
		// engine2.getbOut(), engine2.getsIn(), engine2.getsOut());
		final int m1 = jacShoutEngine.getMaxNumberOfMatch(engine1.getMatchedBids(),
				engine1.getUnmatchedBids(), engine1.getMatchedAsks(),
				engine1.getUnmatchedAsks());
		final int m2 = jacShoutEngine.getMaxNumberOfMatch(engine2.getMatchedBids(),
				engine2.getUnmatchedBids(), engine2.getMatchedAsks(),
				engine2.getUnmatchedAsks());

		return Math.min(m1, m2) + bIn.size();
	}

	/**
	 * seems to match shouts at the equilibrium point, and match more competitive
	 * bids with more competitive asks.
	 * 
	 * @param result
	 */
	protected void matchShouts1(final ArrayList<Shout> result) {
		final List<Shout> shouts = super.matchShouts();

		matchedAsksSet.clear();
		matchedBidsSet.clear();

		final Iterator<Shout> i = shouts.iterator();
		while (i.hasNext()) {
			final Shout bid = i.next();
			final Shout ask = i.next();

			final Trader bidTrader = auctioneer.getShout(bid.getId()).getTrader();
			final Trader askTrader = auctioneer.getShout(ask.getId()).getTrader();

			if (bidTrader != null) {
				final TraderMarketStatistic bidStat = helper.getMarketInfo()
						.getTraderMarketStatistic(bidTrader.getId());
				final ShoutPlusTraderInfo bidTraderInfo = new ShoutPlusTraderInfo(bid,
						bidStat);
				matchedBidsSet.add(bidTraderInfo);
			} else {
				final ShoutPlusTraderInfo bidTraderInfo = new ShoutPlusTraderInfo(bid,
						null);
				matchedBidsSet.add(bidTraderInfo);
			}

			if (askTrader != null) {
				final TraderMarketStatistic askStat = helper.getMarketInfo()
						.getTraderMarketStatistic(askTrader.getId());
				final ShoutPlusTraderInfo askTraderInfo = new ShoutPlusTraderInfo(ask,
						askStat);
				matchedAsksSet.add(askTraderInfo);
			} else {
				final ShoutPlusTraderInfo askTraderInfo = new ShoutPlusTraderInfo(ask,
						null);
				matchedAsksSet.add(askTraderInfo);
			}
		}

		while ((!matchedAsksSet.isEmpty()) && (!matchedBidsSet.isEmpty())) {
			final ShoutPlusTraderInfo bidT = matchedBidsSet.first();
			final ShoutPlusTraderInfo askT = matchedAsksSet.first();

			final Shout bid = bidT.getShout();
			final Shout ask = askT.getShout();

			result.add(bid);
			result.add(ask);

			matchedBidsSet.remove(bidT);
			matchedAsksSet.remove(askT);
		}

	}

	protected void matchShouts2(final ArrayList<Shout> result, final int maxMatch) {
		final int moreMatch = maxMatch - getNumOfMatchedBids();
		int nochangeMatches = getNumOfMatchedBids() - moreMatch;

		final ArrayList<Shout> goodExtraAsks = new ArrayList<Shout>();
		final ArrayList<Shout> goodExtraBids = new ArrayList<Shout>();

		final ArrayList<Shout> noGoodMatches = new ArrayList<Shout>();
		int readMore = moreMatch;
		int numberOfExtraGoodAsks = 0;
		int numberOfExtraGoodBids = 0;

		while ((readMore > 0) && (!sOut.isEmpty()) && (!bOut.isEmpty())) {
			final Shout sOutTop = sOut.remove();
			final Shout bOutTop = bOut.remove();
			@SuppressWarnings("unused")
			boolean goodExtraAsk = false;
			@SuppressWarnings("unused")
			boolean goodExtraBid = false;

			if ((helper.getMarketInfo().getTotalDaysRecorded() >= helper
					.getStartUseTraderInfo()) && (jacShoutEngine.controlMaxMatch)) {
				final Trader bidTrader = auctioneer.getShout(bOutTop.getId())
						.getTrader();
				final Trader askTrader = auctioneer.getShout(sOutTop.getId())
						.getTrader();

				if (bidTrader != null) {
					final String obidTraderId = bidTrader.getId();

					final TraderMarketStatistic obidStat = helper.getMarketInfo()
							.getTraderMarketStatistic(obidTraderId);

					if (obidStat != null) {
						// String bidOut = this.currentDay
						// + ", "
						// + this.currentRound
						// + ", "
						// + obidStat.getTrader()
						// + ", "
						// + "B, "
						// + obidStat.getTotalDays()
						// + ", "
						// + obidStat.getHistoryWindowSize()
						// + ", "
						// + obidStat.getNumberofSpecialists()
						// + ", "
						// + obidStat.getTotalNumberofSpecialists()
						// + ", "
						// + obidStat.getMarketSelectionStdDev()
						// + ", "
						// + obidStat.getTheDayBeforeLastDayInMyMarket()
						// + ", "
						// + obidStat.getLastDayInMyMarket()
						// + ", "
						// + obidStat.getDaysInMyMarket()
						// + ", "
						// + obidStat.getNumberOfItemsTradedInLastDayInMyMarket()
						// + ", "
						// + obidStat.getBestSpecialist()
						// + ", "
						// + obidStat.getDaysInBestSpecialist()
						// + ", "
						// + obidStat.getBestSpecialistLastShoutPrice()
						// + ", "
						// + obidStat.getBestSpecialistShoutMean()
						// + ", "
						// + obidStat.getBestSpecialistShoutStdDev()
						// + ", "
						// + obidStat.getBestSpecialistLastTransactionPrice()
						// + ", "
						// + obidStat.getBestSpecialistTransactionMean()
						// + ", "
						// + obidStat.getBestSpecialistTransactionStdDev()
						// + ", "
						// + obidStat.getBestSpecialistNumberOfItemsTradedInLastDayIn()
						// + ", "
						// + obidStat.getBestSpecialistMaxNumberOfItemsTradedInOneDay()
						// + ", "
						// + obidStat.getTotalDays()
						// / Math.min(helper.getMarketInfoWindow(), helper
						// .getMarketInfo().getTotalDaysRecorded())
						// + ", "
						// + obidStat.getDaysInBestSpecialist()
						// / Math.min(helper.getMarketInfoWindow(), helper
						// .getMarketInfo().getTotalDaysRecorded()) + ","
						// + bOutTop.getPrice();

						if (((obidStat.getMarketSelectionStdDev() >= 2.3D)
								&& ((((double) obidStat.getDaysInBestSpecialist()) / Math.min(
										helper.getMarketInfoWindow(), helper.getMarketInfo()
												.getTotalDaysRecorded())) >= 0.45D)
								&& ((((double) obidStat.getTotalDays()) / Math.min(helper
										.getMarketInfoWindow(), helper.getMarketInfo()
										.getTotalDaysRecorded())) >= 0.85D)
								&& (bOutTop.getPrice() > (helper.getMeanPrice() - (gap * 2.0D))) && (obidStat
								.getShoutsMean() > (helper.getMeanPrice() - (gap / 2.0D))))
								|| (bOutTop.getPrice() > (helper.getMeanPrice() - (gap / 2.0D)))
								|| (bOutTop.getPrice() > (getHighestMatchedAsk().getPrice() - (gap / 2.0D)))) {
							goodExtraBids.add(bOutTop);
							numberOfExtraGoodBids++;
							goodExtraBid = true;
						} else {
							noGoodMatches.add(bOutTop);
						}

					} else {
						noGoodMatches.add(bOutTop);
					}

				} else if ((bOutTop.getPrice() > (helper.getMeanPrice() - (gap / 2.0D)))
						|| (bOutTop.getPrice() > (getHighestMatchedAsk().getPrice() - (gap / 2.0D)))) {
					goodExtraBids.add(bOutTop);
					numberOfExtraGoodBids++;
					goodExtraBid = true;
				} else {
					noGoodMatches.add(bOutTop);
				}

				if (askTrader != null) {
					final String oaskTraderId = askTrader.getId();

					final TraderMarketStatistic oaskStat = helper.getMarketInfo()
							.getTraderMarketStatistic(oaskTraderId);

					if (oaskStat != null) {
						// String askOut = this.currentDay
						// + ", "
						// + this.currentRound
						// + ", "
						// + oaskStat.getTrader()
						// + ", "
						// + "S, "
						// + oaskStat.getTotalDays()
						// + ", "
						// + oaskStat.getHistoryWindowSize()
						// + ", "
						// + oaskStat.getNumberofSpecialists()
						// + ", "
						// + oaskStat.getTotalNumberofSpecialists()
						// + ", "
						// + oaskStat.getMarketSelectionStdDev()
						// + ", "
						// + oaskStat.getTheDayBeforeLastDayInMyMarket()
						// + ", "
						// + oaskStat.getLastDayInMyMarket()
						// + ", "
						// + oaskStat.getDaysInMyMarket()
						// + ", "
						// + oaskStat.getNumberOfItemsTradedInLastDayInMyMarket()
						// + ", "
						// + oaskStat.getBestSpecialist()
						// + ", "
						// + oaskStat.getDaysInBestSpecialist()
						// + ", "
						// + oaskStat.getBestSpecialistLastShoutPrice()
						// + ", "
						// + oaskStat.getBestSpecialistShoutMean()
						// + ", "
						// + oaskStat.getBestSpecialistShoutStdDev()
						// + ", "
						// + oaskStat.getBestSpecialistLastTransactionPrice()
						// + ", "
						// + oaskStat.getBestSpecialistTransactionMean()
						// + ", "
						// + oaskStat.getBestSpecialistTransactionStdDev()
						// + ", "
						// + oaskStat.getBestSpecialistNumberOfItemsTradedInLastDayIn()
						// + ", "
						// + oaskStat.getBestSpecialistMaxNumberOfItemsTradedInOneDay()
						// + ", "
						// + oaskStat.getTotalDays()
						// / Math.min(helper.getMarketInfoWindow(), helper
						// .getMarketInfo().getTotalDaysRecorded())
						// + ", "
						// + oaskStat.getDaysInBestSpecialist()
						// / Math.min(helper.getMarketInfoWindow(), helper
						// .getMarketInfo().getTotalDaysRecorded()) + ","
						// + sOutTop.getPrice();

						if (((oaskStat.getMarketSelectionStdDev() >= 2.3D)
								&& ((((double) oaskStat.getDaysInBestSpecialist()) / Math.min(
										helper.getMarketInfoWindow(), helper.getMarketInfo()
												.getTotalDaysRecorded())) >= 0.45D)
								&& ((((double) oaskStat.getTotalDays()) / Math.min(helper
										.getMarketInfoWindow(), helper.getMarketInfo()
										.getTotalDaysRecorded())) >= 0.85D)
								&& (sOutTop.getPrice() < (helper.getMeanPrice() + (gap * 2.0D))) && (oaskStat
								.getShoutsMean() < (helper.getMeanPrice() + (gap / 2.0D))))
								|| (sOutTop.getPrice() < (helper.getMeanPrice() + (gap / 2.0D)))
								|| (sOutTop.getPrice() < (getLowestMatchedBid().getPrice() + (gap / 2.0D)))) {
							goodExtraAsks.add(sOutTop);
							numberOfExtraGoodAsks++;
							goodExtraAsk = true;
						} else {
							noGoodMatches.add(sOutTop);
						}

					} else {
						noGoodMatches.add(sOutTop);
					}

				} else if ((sOutTop.getPrice() < (helper.getMeanPrice() + (gap / 2.0D)))
						|| (sOutTop.getPrice() < (getLowestMatchedBid().getPrice() + (gap / 2.0D)))) {
					goodExtraAsks.add(sOutTop);
					numberOfExtraGoodAsks++;
					goodExtraAsk = true;
				} else {
					noGoodMatches.add(sOutTop);
				}

			} else if ((helper.getMarketInfo().getTotalDaysRecorded() > jacShoutEngine.daysAfterStartUseMean)
					&& (currentDay >= jacShoutEngine.pureMaximalMatchingUntilDay)) {
				if ((sOutTop.getPrice() <= (helper.getMeanPrice() + initialgap))
						|| (sOutTop.getPrice() <= (getLowestMatchedBid().getPrice() + initialgap))) {
					goodExtraAsks.add(sOutTop);
					numberOfExtraGoodAsks++;
					goodExtraAsk = true;
				} else {
					noGoodMatches.add(sOutTop);
				}
				if ((bOutTop.getPrice() >= (getHighestMatchedAsk().getPrice() - initialgap))
						|| (bOutTop.getPrice() >= (helper.getMeanPrice() - initialgap))) {
					goodExtraBids.add(bOutTop);
					numberOfExtraGoodBids++;
					goodExtraBid = true;
				} else {
					noGoodMatches.add(bOutTop);
				}

			} else if (currentDay >= jacShoutEngine.pureMaximalMatchingUntilDay) {
				if (sOutTop.getPrice() <= (getLowestMatchedBid().getPrice() + initialgap)) {
					goodExtraAsks.add(sOutTop);
					numberOfExtraGoodAsks++;
					goodExtraAsk = true;
				} else {
					noGoodMatches.add(sOutTop);
				}
				if (bOutTop.getPrice() >= (getHighestMatchedAsk().getPrice() - initialgap)) {
					goodExtraBids.add(bOutTop);
					numberOfExtraGoodBids++;
					goodExtraBid = true;
				} else {
					noGoodMatches.add(bOutTop);
				}

			} else if (currentDay < jacShoutEngine.pureMaximalMatchingUntilDay) {
				goodExtraAsks.add(sOutTop);
				numberOfExtraGoodAsks++;
				goodExtraAsk = true;

				goodExtraBids.add(bOutTop);
				numberOfExtraGoodBids++;
				goodExtraBid = true;
			} else {
				noGoodMatches.add(bOutTop);
				noGoodMatches.add(sOutTop);
			}

			readMore--;
		}

		final Iterator<Shout> extraAskI = goodExtraAsks.iterator();
		final Iterator<Shout> extraBidI = goodExtraBids.iterator();

		final List<Shout> shouts = super.matchShouts();
		final Iterator<Shout> i = shouts.iterator();

		final int extraGoodMatches = Math.min(numberOfExtraGoodAsks,
				numberOfExtraGoodBids);
		nochangeMatches += moreMatch - extraGoodMatches;

		if (Matching1) {
			final ArrayList<Shout> matchedAsks = new ArrayList<Shout>();
			final ArrayList<Shout> matchedBids = new ArrayList<Shout>();

			while (i.hasNext()) {
				matchedBids.add(i.next());
				matchedAsks.add(i.next());
			}

			int index = extraGoodMatches - 1;
			while (index >= 0) {
				final Shout extraAsk = goodExtraAsks.remove(index);
				int iB = 0;
				while (matchedBids.get(iB).getPrice() < extraAsk.getPrice()) {
					iB++;
				}
				final Shout matchedBid = matchedBids.remove(iB);

				result.add(matchedBid);
				result.add(extraAsk);

				final Shout extraBid = goodExtraBids.remove(index);
				int iA = 0;
				while (matchedAsks.get(iA).getPrice() > extraBid.getPrice()) {
					iA++;
				}
				final Shout matchedAsk = matchedAsks.remove(iA);

				result.add(extraBid);
				result.add(matchedAsk);

				index--;
			}

			if (matchedBids.size() != matchedAsks.size()) {
				System.out.println("?????Mathing 1 has problem!!!!!!!!!!!");
			} else {
				int iM = matchedBids.size() - 1;
				while (iM >= 0) {
					final Shout ask = matchedAsks.remove(iM);
					final Shout bid = matchedBids.remove(iM);

					result.add(bid);
					result.add(ask);

					iM--;
				}

			}

			if (goodExtraAsks.size() > 0) {
				int iA = goodExtraAsks.size() - 1;
				while (iA >= 0) {
					final Shout returnAsk = goodExtraAsks.get(iA);
					try {
						newShout(returnAsk);
					} catch (final DuplicateShoutException e) {
						e.printStackTrace();
					}

					iA--;
				}

			}

			if (goodExtraBids.size() > 0) {
				int iB = goodExtraBids.size() - 1;
				while (iB >= 0) {
					final Shout returnBid = goodExtraBids.get(iB);
					try {
						newShout(returnBid);
					} catch (final DuplicateShoutException e) {
						e.printStackTrace();
					}

					iB--;
				}
			}

			Matching1 = false;
		} else {
			while (i.hasNext()) {
				final Shout bid = i.next();
				final Shout ask = i.next();
				if ((extraGoodMatches > 0) && (nochangeMatches <= 0)) {
					final Shout oask = extraAskI.next();
					final Shout obid = extraBidI.next();

					result.add(obid);
					result.add(ask);

					result.add(bid);
					result.add(oask);

				} else {
					if ((currentRound < jacShoutEngine.removeTooBadTransBeforeRound)
							&& (helper.getMarketInfo().getTotalDaysRecorded() > jacShoutEngine.daysAfterStartUseMean)
							&& ((ask.getPrice() > (helper.getMeanPrice() + matchMeanDistance)) || (bid
									.getPrice() < (helper.getMeanPrice() - matchMeanDistance)))) {
						try {
							newShout(ask);
							newShout(bid);
						} catch (final DuplicateShoutException e) {
							e.printStackTrace();
						}

					} else {
						result.add(bid);
						result.add(ask);
					}

					nochangeMatches--;
				}
			}

			while (extraAskI.hasNext()) {
				final Shout returnAsk = extraAskI.next();
				try {
					newShout(returnAsk);
				} catch (final DuplicateShoutException e) {
					System.out.println("DuplicateShoutException in clear");
					System.out.println("can't return good ask:" + returnAsk.getPrice());
					e.printStackTrace();
				}

			}

			while (extraBidI.hasNext()) {
				final Shout returnBid = extraBidI.next();
				try {
					newShout(returnBid);
				} catch (final DuplicateShoutException e) {
					System.out.println("DuplicateShoutException in clear");
					System.out.println("can't return good bid:" + returnBid.getPrice());
					e.printStackTrace();
				}

			}

			Matching1 = true;
		}

		if (extraGoodMatches < moreMatch) {
			final Iterator<Shout> returnI = noGoodMatches.iterator();
			while (returnI.hasNext()) {
				final Shout badShout = returnI.next();
				try {
					newShout(badShout);
				} catch (final DuplicateShoutException e) {
					System.out.println("DuplicateShoutException in clear");
					System.out.println("can't return nogood ask/bid:"
							+ badShout.getPrice());
					e.printStackTrace();
				}

			}

		}
	}

	protected void matchShouts3(final ArrayList<Shout> result) {
		final List<Shout> shouts = super.matchShouts();
		final Iterator<Shout> i = shouts.iterator();
		while (i.hasNext()) {
			final Shout bid = i.next();
			final Shout ask = i.next();

			if ((currentRound < jacShoutEngine.removeTooBadTransBeforeRound)
					&& (helper.getMarketInfo().getTotalDaysRecorded() > jacShoutEngine.daysAfterStartUseMean)
					&& ((ask.getPrice() > (helper.getMeanPrice() + matchMeanDistance)) || (bid
							.getPrice() < (helper.getMeanPrice() - matchMeanDistance)))) {
				try {
					newShout(ask);
					newShout(bid);
					System.out.println("+++Return a bad transaction ask|bid:"
							+ ask.getPrice() + "|" + bid.getPrice());
				} catch (final DuplicateShoutException e) {
					System.out.println("DuplicateShoutException in clear");
					System.out.println("can't return bad transactions ask|bid:"
							+ ask.getPrice() + "|" + bid.getPrice());
					e.printStackTrace();
				}
			} else {
				result.add(bid);
				result.add(ask);
			}
		}
	}

	@Override
	public List<Shout> matchShouts() {
		final ArrayList<Shout> result = new ArrayList<Shout>();

		int maxMatch = 0;

		try {
			maxMatch = jacShoutEngine.getMaxNumberOfMatch(bIn, bOut, sIn, sOut);
		} catch (final DuplicateShoutException e) {
			jacShoutEngine.logger.debug(e);
		}

		if ((jacShoutEngine.reorderMatchedShouts)
				&& (helper.getMarketInfo().getTotalDaysRecorded() >= helper
						.getStartUseTraderInfo())) {
			matchShouts1(result);

			// TODO:
			// } else if ((this.useMaxVolumeMatching) && (maxMatch > getbInSize()) &&
			// (this.currentRound >= this.maximalMatchStartFromRound))
			// {
			// int moreMatch = maxMatch - getbInSize();
			// int nochangeMatches = getbInSize() - moreMatch;

		} else if ((jacShoutEngine.useMaxVolumeMatching)
				&& (maxMatch > getNumOfMatchedBids())
				&& (currentRound >= jacShoutEngine.maximalMatchStartFromRound)) {
			matchShouts2(result, maxMatch);
		} else {
			matchShouts3(result);
		}

		return result;
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if (event instanceof DayOpenedEvent) {
			processDayOpened((DayOpenedEvent) event);
		} else if (event instanceof RoundOpenedEvent) {
			processRoundOpened((RoundOpenedEvent) event);
		}
	}

	protected void processDayOpened(final DayOpenedEvent event) {
		currentDay = event.getDay();

		if (helper.getMarketInfo().getTotalDaysRecorded() < 2) {
			initialgap = 9.0D;
			matchMeanDistance = initialgap;
		} else if (helper.getMarketInfo().getTotalDaysRecorded() < 3) {
			initialgap = 8.0D;
			matchMeanDistance = initialgap;
		} else if (helper.getMarketInfo().getTotalDaysRecorded() < 9) {
			initialgap = 7.0D;
			matchMeanDistance = initialgap;
		} else if (helper.getMarketInfo().getTotalDaysRecorded() < 12) {
			initialgap = 6.0D;
			matchMeanDistance = initialgap;
		} else if (helper.getMarketInfo().getTotalDaysRecorded() < 20) {
			gap = 5.0D;
			matchMeanDistance = gap;
		} else if (helper.getMarketInfo().getTotalDaysRecorded() < 30) {
			gap = 5.0D;
		} else if (helper.getMarketInfo().getTotalDaysRecorded() < 80) {
			gap = 5.0D;
		} else {
			gap = 5.0D;
		}

		if ((currentDay % 2) == 0) {
			Matching1 = false;
		} else {
			Matching1 = true;
		}

	}

	protected void processRoundOpened(final RoundOpenedEvent event) {
		currentRound = event.getRound();

		// System.out.println("RoundOpened: " + currentRound);
	}

}
