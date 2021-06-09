package cat11.agent.jackarooa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections15.buffer.PriorityBuffer;
import org.apache.commons.collections15.map.HashedMap;
import org.apache.log4j.Logger;

import cat11.agent.jackaroo.jacShoutEngine;
import cat11.agent.jackaroo.util.AscendingShoutPlusTraderInfoComparator;
import cat11.agent.jackaroo.util.DescendingShoutPlusTraderInfoComparator;
import cat11.agent.jackaroo.util.ShoutPlusTraderInfo;
import cat11.agent.jackaroo.util.TraderMarketStatistic;
import edu.cuny.cat.core.IllegalShoutException;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.core.Trader;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpenedEvent;
import edu.cuny.cat.event.RoundOpenedEvent;
import edu.cuny.cat.event.ShoutPostedEvent;
import edu.cuny.cat.event.TransactionExecutedEvent;
import edu.cuny.cat.market.DuplicateShoutException;
import edu.cuny.cat.market.GenericDoubleAuctioneer;
import edu.cuny.cat.market.Helper;
import edu.cuny.cat.market.accepting.NotAnImprovementOverSelfException;
import edu.cuny.struct.FixedLengthQueue;

public class jacDoubleAuctioneer extends GenericDoubleAuctioneer {
	private static final long serialVersionUID = 1L;

	protected jHelper helper;

	protected int receivedAsk = 0;

	protected int receivedBid = 0;

	protected int acceptedAsk = 0;

	protected int acceptedBid = 0;

	protected int transactedAsk = 0;

	protected int transactedBid = 0;

	protected int currentDay;

	protected int currentRound;

	protected boolean reorderMatchedShouts = false;

	protected SortedSet<ShoutPlusTraderInfo> matchedBidsSet;

	protected SortedSet<ShoutPlusTraderInfo> matchedAsksSet;

	protected static AscendingShoutPlusTraderInfoComparator ascendingOrder;

	protected static DescendingShoutPlusTraderInfoComparator descendingOrder;

	protected boolean useMaxVolumeMatching = true;

	protected boolean controlMaxMatch = true;

	protected boolean Matching1;

	protected double gap;

	protected double initialgap;

	protected double transMeanGap;

	protected double matchMeanDistance = 8.0D;

	protected int pureMaximalMatchingUntilDay = 0;

	protected int removeTooBadTransBeforeRound = 0;

	protected int daysAfterStartUseMean = 10;

	protected int maximalMatchStartFromRound = 0;

	protected double lastDaysTransactionRate = 1.0D;

	FixedLengthQueue historyTransactions = new FixedLengthQueue(8);

	static Logger logger = Logger.getLogger(jacDoubleAuctioneer.class);

	public jacDoubleAuctioneer() {
		helpers = new HashedMap<Class<? extends Helper>, Helper>();
		helper = jHelper.getHelper(this);
		init0();
	}

	private void init0() {
		matchedBidsSet = new TreeSet<ShoutPlusTraderInfo>(
				jacDoubleAuctioneer.descendingOrder);
		matchedAsksSet = new TreeSet<ShoutPlusTraderInfo>(
				jacDoubleAuctioneer.ascendingOrder);
		@SuppressWarnings("unused")
		final String title = "currentDate,Round,Trader,Buyer/Seller,TotalRecordDays,HistoryWindowSize,NumberofSelectedSpecialists,NumberofTotalSpecialists,MarketSelectionStdDev,TheDayBeforeLastDayInMyMarket,LastDayInMyMarket,DaysInMyMarket,NumberOfItemsTradedInLastDayInMyMarket,BestSpecialist,DaysInBestSpecialist,BestSpecialistLastShoutPrice,BestSpecialistShoutMean,BestSpecialistShoutStdDev,BestSpecialistLastTransactionPrice,BestSpecialistTransactionMean,BestSpecialistTransactionStdDev,BestSpecialistNumberOfItemsTradedInLastDayIn,BestSpecialistMaxNumberOfItemsTradedInOneDay,DayDiffR(TotalRecordDays/HistoryWindowSize),DaysInBestR(DaysInBestSpecialist/HistoryWindowSize),CurrentShoutPrice";
	}

	@Override
	public void reset() {
		super.reset();

		init0();
	}

	@Override
	public void newShout(final Shout shout) throws IllegalShoutException,
			DuplicateShoutException {
		if (!shout.isValid()) {
			jacDoubleAuctioneer.logger.error("malformed shout: " + shout);
			throw new IllegalShoutException("Malformed shout");
		}
		if (acceptingPolicy == null) {
			jacDoubleAuctioneer.logger.error("No accepting policy set up !");
			throw new IllegalShoutException("Null accepting policy");
		}

		final Shout oldShout = shouts.get(shout.getId());

		if (shout.isAsk()) {
			if ((oldShout != null) && (oldShout.getPrice() <= shout.getPrice())) {
				throw new NotAnImprovementOverSelfException("ask");
			}
		} else if ((oldShout != null) && (oldShout.getPrice() >= shout.getPrice())) {
			throw new NotAnImprovementOverSelfException("bid");
		}

		if (oldShout == null) {
			if (shout.isAsk()) {
				receivedAsk += 1;
			} else if (shout.isBid()) {
				receivedBid += 1;
			} else {
				jacDoubleAuctioneer.logger
						.fatal("something goes wrong in newShout()!!!");
			}
		}

		acceptingPolicy.check(getShout(shout.getId()), shout);
		shoutEngine.newShout(shout);

		if (oldShout == null) {
			if (shout.isAsk()) {
				acceptedAsk += 1;
			} else {
				acceptedBid += 1;
			}
		}
	}

	protected void clear1() {
		final List<Shout> shouts = shoutEngine.matchShouts();

		matchedAsksSet.clear();
		matchedBidsSet.clear();

		final Iterator<Shout> i = shouts.iterator();
		while (i.hasNext()) {
			final Shout bid = i.next();
			final Shout ask = i.next();

			final Trader bidTrader = getShout(bid.getId()).getTrader();
			final Trader askTrader = getShout(ask.getId()).getTrader();

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

			final double price = determineClearingPrice(bid, ask, currentQuote);
			clear(ask, bid, price);

			matchedBidsSet.remove(bidT);
			matchedAsksSet.remove(askT);
		}
	}

	protected void clear2(final int maxMatch) {
		// TODO:
		// int moreMatch = maxMatch - this.shoutEngine.getbInSize();
		// int nochangeMatches = this.shoutEngine.getbInSize() - moreMatch;
		// PriorityBuffer bOut = this.shoutEngine.getbOut();
		// PriorityBuffer sOut = this.shoutEngine.getsOut();
		final int moreMatch = maxMatch - shoutEngine.getNumOfMatchedBids();
		int nochangeMatches = shoutEngine.getNumOfMatchedBids() - moreMatch;
		final PriorityBuffer<Shout> bOut = shoutEngine.getUnmatchedBids();
		final PriorityBuffer<Shout> sOut = shoutEngine.getUnmatchedAsks();

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
					.getStartUseTraderInfo()) && (controlMaxMatch)) {
				final Trader bidTrader = getShout(bOutTop.getId()).getTrader();
				final Trader askTrader = getShout(sOutTop.getId()).getTrader();

				if (bidTrader != null) {
					final String obidTraderId = bidTrader.getId();

					final TraderMarketStatistic obidStat = helper.getMarketInfo()
							.getTraderMarketStatistic(obidTraderId);

					if (obidStat != null) {
						@SuppressWarnings("unused")
						final String bidOut = currentDay
								+ ", "
								+ currentRound
								+ ", "
								+ obidStat.getTrader()
								+ ", "
								+ "B, "
								+ obidStat.getTotalDays()
								+ ", "
								+ obidStat.getHistoryWindowSize()
								+ ", "
								+ obidStat.getNumberofSpecialists()
								+ ", "
								+ obidStat.getTotalNumberofSpecialists()
								+ ", "
								+ obidStat.getMarketSelectionStdDev()
								+ ", "
								+ obidStat.getTheDayBeforeLastDayInMyMarket()
								+ ", "
								+ obidStat.getLastDayInMyMarket()
								+ ", "
								+ obidStat.getDaysInMyMarket()
								+ ", "
								+ obidStat.getNumberOfItemsTradedInLastDayInMyMarket()
								+ ", "
								+ obidStat.getBestSpecialist()
								+ ", "
								+ obidStat.getDaysInBestSpecialist()
								+ ", "
								+ obidStat.getBestSpecialistLastShoutPrice()
								+ ", "
								+ obidStat.getBestSpecialistShoutMean()
								+ ", "
								+ obidStat.getBestSpecialistShoutStdDev()
								+ ", "
								+ obidStat.getBestSpecialistLastTransactionPrice()
								+ ", "
								+ obidStat.getBestSpecialistTransactionMean()
								+ ", "
								+ obidStat.getBestSpecialistTransactionStdDev()
								+ ", "
								+ obidStat.getBestSpecialistNumberOfItemsTradedInLastDayIn()
								+ ", "
								+ obidStat.getBestSpecialistMaxNumberOfItemsTradedInOneDay()
								+ ", "
								+ (((double) obidStat.getTotalDays()) / Math.min(helper
										.getMarketInfoWindow(), helper.getMarketInfo()
										.getTotalDaysRecorded()))
								+ ", "
								+ (((double) obidStat.getDaysInBestSpecialist()) / Math.min(
										helper.getMarketInfoWindow(), helper.getMarketInfo()
												.getTotalDaysRecorded())) + "," + bOutTop.getPrice();

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
								|| (bOutTop.getPrice() > (shoutEngine.getHighestMatchedAsk()
										.getPrice() - (gap / 2.0D)))) {
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
						|| (bOutTop.getPrice() > (shoutEngine.getHighestMatchedAsk()
								.getPrice() - (gap / 2.0D)))) {
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
						@SuppressWarnings("unused")
						final String askOut = currentDay
								+ ", "
								+ currentRound
								+ ", "
								+ oaskStat.getTrader()
								+ ", "
								+ "S, "
								+ oaskStat.getTotalDays()
								+ ", "
								+ oaskStat.getHistoryWindowSize()
								+ ", "
								+ oaskStat.getNumberofSpecialists()
								+ ", "
								+ oaskStat.getTotalNumberofSpecialists()
								+ ", "
								+ oaskStat.getMarketSelectionStdDev()
								+ ", "
								+ oaskStat.getTheDayBeforeLastDayInMyMarket()
								+ ", "
								+ oaskStat.getLastDayInMyMarket()
								+ ", "
								+ oaskStat.getDaysInMyMarket()
								+ ", "
								+ oaskStat.getNumberOfItemsTradedInLastDayInMyMarket()
								+ ", "
								+ oaskStat.getBestSpecialist()
								+ ", "
								+ oaskStat.getDaysInBestSpecialist()
								+ ", "
								+ oaskStat.getBestSpecialistLastShoutPrice()
								+ ", "
								+ oaskStat.getBestSpecialistShoutMean()
								+ ", "
								+ oaskStat.getBestSpecialistShoutStdDev()
								+ ", "
								+ oaskStat.getBestSpecialistLastTransactionPrice()
								+ ", "
								+ oaskStat.getBestSpecialistTransactionMean()
								+ ", "
								+ oaskStat.getBestSpecialistTransactionStdDev()
								+ ", "
								+ oaskStat.getBestSpecialistNumberOfItemsTradedInLastDayIn()
								+ ", "
								+ oaskStat.getBestSpecialistMaxNumberOfItemsTradedInOneDay()
								+ ", "
								+ (((double) oaskStat.getTotalDays()) / Math.min(helper
										.getMarketInfoWindow(), helper.getMarketInfo()
										.getTotalDaysRecorded()))
								+ ", "
								+ (((double) oaskStat.getDaysInBestSpecialist()) / Math.min(
										helper.getMarketInfoWindow(), helper.getMarketInfo()
												.getTotalDaysRecorded())) + "," + sOutTop.getPrice();

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
								|| (sOutTop.getPrice() < (shoutEngine.getLowestMatchedBid()
										.getPrice() + (gap / 2.0D)))) {
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
						|| (sOutTop.getPrice() < (shoutEngine.getLowestMatchedBid()
								.getPrice() + (gap / 2.0D)))) {
					goodExtraAsks.add(sOutTop);
					numberOfExtraGoodAsks++;
					goodExtraAsk = true;
				} else {
					noGoodMatches.add(sOutTop);
				}

			} else if ((helper.getMarketInfo().getTotalDaysRecorded() > daysAfterStartUseMean)
					&& (currentDay >= pureMaximalMatchingUntilDay)) {
				if ((sOutTop.getPrice() <= (helper.getMeanPrice() + initialgap))
						|| (sOutTop.getPrice() <= (shoutEngine.getLowestMatchedBid()
								.getPrice() + initialgap))) {
					goodExtraAsks.add(sOutTop);
					numberOfExtraGoodAsks++;
					goodExtraAsk = true;
				} else {
					noGoodMatches.add(sOutTop);
				}
				if ((bOutTop.getPrice() >= (shoutEngine.getHighestMatchedAsk()
						.getPrice() - initialgap))
						|| (bOutTop.getPrice() >= (helper.getMeanPrice() - initialgap))) {
					goodExtraBids.add(bOutTop);
					numberOfExtraGoodBids++;
					goodExtraBid = true;
				} else {
					noGoodMatches.add(bOutTop);
				}

			} else if (currentDay >= pureMaximalMatchingUntilDay) {
				if (sOutTop.getPrice() <= (shoutEngine.getLowestMatchedBid().getPrice() + initialgap)) {
					goodExtraAsks.add(sOutTop);
					numberOfExtraGoodAsks++;
					goodExtraAsk = true;
				} else {
					noGoodMatches.add(sOutTop);
				}
				if (bOutTop.getPrice() >= (shoutEngine.getHighestMatchedAsk()
						.getPrice() - initialgap)) {
					goodExtraBids.add(bOutTop);
					numberOfExtraGoodBids++;
					goodExtraBid = true;
				} else {
					noGoodMatches.add(bOutTop);
				}

			} else if (currentDay < pureMaximalMatchingUntilDay) {
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

		final List<Shout> shouts = shoutEngine.matchShouts();
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

				final double price1 = determineClearingPrice(matchedBid, extraAsk,
						currentQuote);
				clear(extraAsk, matchedBid, price1);

				final Shout extraBid = goodExtraBids.remove(index);
				int iA = 0;
				while (matchedAsks.get(iA).getPrice() > extraBid.getPrice()) {
					iA++;
				}
				final Shout matchedAsk = matchedAsks.remove(iA);

				final double price2 = determineClearingPrice(extraBid, matchedAsk,
						currentQuote);
				clear(matchedAsk, extraBid, price2);

				index--;
			}

			if (matchedBids.size() != matchedAsks.size()) {
				System.out.println("?????Mathing 1 has problem!!!!!!!!!!!");
			} else {
				int iM = matchedBids.size() - 1;
				while (iM >= 0) {
					final Shout ask = matchedAsks.remove(iM);
					final Shout bid = matchedBids.remove(iM);
					final double price = determineClearingPrice(bid, ask, currentQuote);
					clear(ask, bid, price);
					iM--;
				}

			}

			if (goodExtraAsks.size() > 0) {
				int iA = goodExtraAsks.size() - 1;
				while (iA >= 0) {
					final Shout returnAsk = goodExtraAsks.get(iA);
					try {
						shoutEngine.newShout(returnAsk);
					} catch (final DuplicateShoutException e) {
					}

					iA--;
				}

			}

			if (goodExtraBids.size() > 0) {
				int iB = goodExtraBids.size() - 1;
				while (iB >= 0) {
					final Shout returnBid = goodExtraBids.get(iB);
					try {
						shoutEngine.newShout(returnBid);
					} catch (final DuplicateShoutException e) {
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

					final double price1 = determineClearingPrice(obid, ask, currentQuote);
					clear(ask, obid, price1);

					final double price2 = determineClearingPrice(bid, oask, currentQuote);
					clear(oask, bid, price2);
				} else {
					if ((currentRound < removeTooBadTransBeforeRound)
							&& (helper.getMarketInfo().getTotalDaysRecorded() > daysAfterStartUseMean)
							&& ((ask.getPrice() > (helper.getMeanPrice() + matchMeanDistance)) || (bid
									.getPrice() < (helper.getMeanPrice() - matchMeanDistance)))) {
						try {
							shoutEngine.newShout(ask);
							shoutEngine.newShout(bid);
						} catch (final DuplicateShoutException e) {
						}

					} else {
						final double price = determineClearingPrice(bid, ask, currentQuote);
						clear(ask, bid, price);
					}

					nochangeMatches--;
				}
			}

			while (extraAskI.hasNext()) {
				final Shout returnAsk = extraAskI.next();
				try {
					shoutEngine.newShout(returnAsk);
				} catch (final DuplicateShoutException e) {
					System.out.println("DuplicateShoutException in clear");
					System.out.println("can't return good ask:" + returnAsk.getPrice());
				}

			}

			while (extraBidI.hasNext()) {
				final Shout returnBid = extraBidI.next();
				try {
					shoutEngine.newShout(returnBid);
				} catch (final DuplicateShoutException e) {
					System.out.println("DuplicateShoutException in clear");
					System.out.println("can't return good bid:" + returnBid.getPrice());
				}

			}

			Matching1 = true;
		}

		if (extraGoodMatches < moreMatch) {
			final Iterator<Shout> returnI = noGoodMatches.iterator();
			while (returnI.hasNext()) {
				final Shout badShout = returnI.next();
				try {
					shoutEngine.newShout(badShout);
				} catch (final DuplicateShoutException e) {
					System.out.println("DuplicateShoutException in clear");
					System.out.println("can't return nogood ask/bid:"
							+ badShout.getPrice());
				}
			}
		}
	}

	protected void clear3() {
		final List<Shout> shouts = shoutEngine.matchShouts();
		final Iterator<Shout> i = shouts.iterator();
		while (i.hasNext()) {
			final Shout bid = i.next();
			final Shout ask = i.next();

			if ((currentRound < removeTooBadTransBeforeRound)
					&& (helper.getMarketInfo().getTotalDaysRecorded() > daysAfterStartUseMean)
					&& ((ask.getPrice() > (helper.getMeanPrice() + matchMeanDistance)) || (bid
							.getPrice() < (helper.getMeanPrice() - matchMeanDistance)))) {
				try {
					shoutEngine.newShout(ask);
					shoutEngine.newShout(bid);
					System.out.println("+++Return a bad transaction ask|bid:"
							+ ask.getPrice() + "|" + bid.getPrice());
				} catch (final DuplicateShoutException e) {
					System.out.println("DuplicateShoutException in clear");
					System.out.println("can't return bad transactions ask|bid:"
							+ ask.getPrice() + "|" + bid.getPrice());
				}
			} else {
				final double price = determineClearingPrice(bid, ask, currentQuote);
				clear(ask, bid, price);
			}
		}
	}

	@Override
	public void clear() {
		updateQuote();

		int maxMatch = 0;
		try {
			// TODO:
			// maxMatch = getMaxNumberOfMatch(this.shoutEngine.getbIn(),
			// this.shoutEngine.getbOut(), this.shoutEngine.getsIn(),
			// this.shoutEngine.getsOut());
			maxMatch = jacShoutEngine.getMaxNumberOfMatch(
					shoutEngine.getMatchedBids(), shoutEngine.getUnmatchedBids(),
					shoutEngine.getMatchedAsks(), shoutEngine.getUnmatchedAsks());
		} catch (final DuplicateShoutException e) {
			jacDoubleAuctioneer.logger.debug(e);
		}

		if ((reorderMatchedShouts)
				&& (helper.getMarketInfo().getTotalDaysRecorded() >= helper
						.getStartUseTraderInfo())) {
			clear1();
		} else if ((useMaxVolumeMatching)
				// TODO:
				// && (maxMatch > this.shoutEngine.getbInSize())
				&& (maxMatch > shoutEngine.getNumOfMatchedAsks())
				&& (currentRound >= maximalMatchStartFromRound)) {
			clear2(maxMatch);
		} else {
			clear3();
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if ((event instanceof ShoutPostedEvent)) {
			processShoutPosted((ShoutPostedEvent) event);
		} else if ((event instanceof DayOpenedEvent)) {
			processDayOpened((DayOpenedEvent) event);
		} else if ((event instanceof RoundOpenedEvent)) {
			processRoundOpened((RoundOpenedEvent) event);
		}
	}

	protected void processDayOpened(final DayOpenedEvent event) {
		currentDay = event.getDay();
		receivedAsk = 0;
		receivedBid = 0;
		acceptedAsk = 0;
		acceptedBid = 0;
		transactedAsk = 0;
		transactedBid = 0;

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

	@Override
	protected void processDayClosed(final DayClosedEvent event) {
		helper
				.setMeanPrice(((((helper.getMarketInfo().getMeanAcceptedShoutPrice() * 0.5D) + (helper
						.getMarketInfo().getMeanTransShoutPrice() * 0.5D)) * 0.5D) + (helper
						.getMarketInfo().getMeanTransPrice() * 0.5D)));
		try {
			final int receivedShout = (receivedAsk + receivedBid);
			final int acceptedShout = (acceptedAsk + acceptedBid);
			final int transactedShout = (transactedAsk + transactedBid);
			@SuppressWarnings("unused")
			final double acceptRate = (receivedShout == 0) ? 0
					: ((double) acceptedShout) / receivedShout;
			final double transacRate = (acceptedShout == 0) ? 0
					: ((double) transactedShout) / acceptedShout;

			lastDaysTransactionRate = transacRate;
			historyTransactions.newData(transacRate);

			// System.out.println("\n++++Closing Day: " + currentDay + "....\n"
			// + " RA+RB: " + receivedAsk + "+" + receivedBid + " = "
			// + (receivedAsk + receivedBid));
		} catch (final Exception e) {
			System.out.println(e.getMessage());
		}

		super.processDayClosed(event);
	}

	// leave there temporarily, seems affect performance
	protected void processShoutPosted(final ShoutPostedEvent event) {
		final Shout shout = event.getShout();

		if ((shout.getSpecialist() == null)
				|| (!name.equals(shout.getSpecialist().getId()))) {
			// logger.info("shout posted in other markets");
			return;
		}

		if (shout.getState() != 1) {
			return;
		}

		if (shouts.containsKey(shout.getId())) {
			final Shout oldShout = shouts.get(shout.getId());
			if (oldShout.getTrader() == null) {
				oldShout.setTrader(shout.getTrader());
				// logger.info("update trader");
			}

			if (oldShout.getSpecialist() == null) {
				oldShout.setSpecialist(shout.getSpecialist());
				// logger.info("update specialist");
			}

			if (oldShout.getPrice() != shout.getPrice()) {
				if (oldShout.getState() == 1) {
					oldShout.setPrice(shout.getPrice());
					// logger.info("update price");
				} else {
					jacDoubleAuctioneer.logger
							.error("Only prices of placed shouts can be modified !");
				}
			}
		} else {
			jacDoubleAuctioneer.logger
					.error("shout posted was not found in placed shout !");
			jacDoubleAuctioneer.logger.error(shout.toString());
			shouts.put(shout.getId(), shout);
		}
	}

	@Override
	protected void processTransactionExecuted(final TransactionExecutedEvent event) {
		super.processTransactionExecuted(event);

		transactedAsk += 1;
		transactedBid += 1;
	}

	public double getRange() {
		double range;
		if (helper.getMarketInfo().getTotalDaysRecorded() <= 250) {
			range = 20.0D;
		} else {
			range = 20.0D;
		}

		if (helper.getMarketInfo().getTotalDaysRecorded() > 20) {
			range *= historyTransactions.getMean();
		}

		if (range < 10.0D) {
			range = 10.0D;
		}

		return range;
	}
}