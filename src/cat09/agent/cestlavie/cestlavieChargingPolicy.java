/*
 * cestlavie in CAT'09.
 */

package cat09.agent.cestlavie;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Specialist;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.ProfitAnnouncedEvent;
import edu.cuny.cat.event.RegisteredTradersAnnouncedEvent;
import edu.cuny.cat.market.charging.AdaptiveChargingPolicy;
import edu.cuny.cat.market.charging.ChargingPolicy;
import edu.cuny.util.Utils;

/**
 * An adaptive charging policy that tries to balance the market share and the
 * profit share. If the market share is lower than the profit share, it learns
 * from the leading specialist on market share; otherwise it learns from the
 * leading specialist on profit share.
 * 
 * TODO: this can be simplied by using {@link edu.cuny.cat.MarketRegistry}.
 * 
 * @author cestlavie Team
 * @version $Revision: 1.88 $
 */

public class cestlavieChargingPolicy extends AdaptiveChargingPolicy {

	static Logger logger = Logger.getLogger(cestlavieChargingPolicy.class);

	protected final Map<String, Double> cumulativeProfits;

	protected Map<String, Double> dailyProfits;

	protected Specialist dailyProfitLeader;

	protected double maxDailyProfit;

	protected Map<String, Integer> dailyTraderDistributions;

	protected Specialist dailyPopularityLeader;

	public cestlavieChargingPolicy() {
		cumulativeProfits = Collections
				.synchronizedMap(new HashMap<String, Double>());
		dailyProfits = Collections.synchronizedMap(new HashMap<String, Double>());
		dailyTraderDistributions = Collections
				.synchronizedMap(new HashMap<String, Integer>());
	}

	@Override
	public void reset() {
		super.reset();
		cumulativeProfits.clear();
	}

	protected void dayInitialize() {
		maxDailyProfit = Double.NEGATIVE_INFINITY;
		dailyProfitLeader = null;
		dailyProfits.clear();
		dailyPopularityLeader = null;
		dailyTraderDistributions.clear();
	}

	protected void updateSpecialistProfit(final Specialist specialist) {
		double prevCumulativeProfit = 0.0D;
		if (cumulativeProfits.containsKey(specialist.getId())) {
			prevCumulativeProfit = cumulativeProfits.get(specialist.getId())
					.doubleValue();
		}
		final double dailyProfit = specialist.getAccount().getBalance()
				- prevCumulativeProfit;
		dailyProfits.put(specialist.getId(), new Double(dailyProfit));
		cumulativeProfits.put(specialist.getId(), new Double(specialist
				.getAccount().getBalance()));
		if (maxDailyProfit < dailyProfit) {
			dailyProfitLeader = specialist;
			maxDailyProfit = dailyProfit;
		}
	}

	protected void updateSpecialistPopularity(final Specialist specialist,
			final int numOfTraders) {
		dailyTraderDistributions.put(specialist.getId(), new Integer(numOfTraders));
		if ((dailyPopularityLeader == null)
				|| (dailyTraderDistributions.get(dailyPopularityLeader.getId())
						.intValue() < numOfTraders)) {
			dailyPopularityLeader = specialist;
		}
	}

	protected double calculateProfitShare() {
		double num = Double.NaN;
		double total = 0.0D;
		for (final Double double1 : dailyProfits.values()) {
			total += double1.doubleValue();
		}

		if (dailyProfits.containsKey(getAuctioneer().getName())) {
			num = dailyProfits.get(getAuctioneer().getName()).doubleValue();
		}
		return num / total;
	}

	protected double calculateMarketShare() {
		double num = Double.NaN;
		double total = 0;
		for (final Integer traderNum : dailyTraderDistributions.values()) {
			total += traderNum;
		}

		if (dailyTraderDistributions.containsKey(getAuctioneer().getName())) {
			num = dailyTraderDistributions.get(getAuctioneer().getName())
					.doubleValue();
		}
		return num / total;
	}

	protected void learnCharges(final Specialist leader, final boolean lower,
			final double ratio) {
		if (leader != null) {
			for (int i = 0; i < learners.length; i++) {
				if (learners[i] != null) {
					final double fee = leader.getFees()[i] - (lower ? 1.0D : -1D * ratio)
							* perturbations[i].nextDouble();
					learners[i].train(fee);
					cestlavieChargingPolicy.logger.debug("training "
							+ ChargingPolicy.P_FEES[i] + " learner with fee "
							+ Utils.formatter.format(fee));
					fees[i] = learners[i].act();
					if (fees[i] < 0.0D) {
						fees[i] = 0.0D;
					}

					if ((ChargingPolicy.FEE_TYPES[i] == ChargingPolicy.FRACTIONAL)
							&& (fees[i] > 0.99)) {
						cestlavieChargingPolicy.logger
								.debug("adjusted fractional fee to 0.99 from " + fees[i]);
						fees[i] = 0.99;
					} else if ((ChargingPolicy.FEE_TYPES[i] == ChargingPolicy.FLAT)
							&& (fees[i] > ChargingPolicy.MAXES[ChargingPolicy.FLAT])) {
						fees[i] = ChargingPolicy.MAXES[ChargingPolicy.FLAT];
					}
				}
			}

		} else {
			cestlavieChargingPolicy.logger
					.error("daily leader is null ! This may be a bug in jcat.");
		}
	}

	protected void updateFees(final double ratio) {
		final double marketShare = calculateMarketShare();
		final double profitShare = calculateProfitShare();

		if (marketShare <= profitShare) {
			cestlavieChargingPolicy.logger.debug("learning from popularity leader - "
					+ dailyPopularityLeader.getId() + " ...");
			learnCharges(dailyPopularityLeader, true, ratio);
		} else {
			cestlavieChargingPolicy.logger.debug("learning from profiting leader - "
					+ dailyProfitLeader.getId() + " ...");
			learnCharges(dailyProfitLeader, false, ratio);
		}

		cestlavieChargingPolicy.logger.debug("\n");
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if (event instanceof DayOpeningEvent) {
			dayInitialize();
		} else if (event instanceof DayClosedEvent) {
			updateFees(1.0D);
		} else if (event instanceof ProfitAnnouncedEvent) {
			updateSpecialistProfit(((ProfitAnnouncedEvent) event).getSpecialist());
		} else if (event instanceof RegisteredTradersAnnouncedEvent) {
			updateSpecialistPopularity(((RegisteredTradersAnnouncedEvent) event)
					.getSpecialist(), ((RegisteredTradersAnnouncedEvent) event)
					.getNumOfTraders());
		}
	}
}
