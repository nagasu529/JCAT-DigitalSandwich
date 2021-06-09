/*
 * AstonCat-Plus from CAT'10
 */

package cat10.agent.astoncat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cuny.ai.learning.AveragingLearner;
import edu.cuny.ai.learning.ReverseDiscountSlidingLearner;
import edu.cuny.ai.learning.SlidingWindowLearner;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.FeesAnnouncedEvent;
import edu.cuny.cat.event.RegisteredTradersAnnouncedEvent;
import edu.cuny.cat.market.charging.ChargingPolicy;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;

/**
 * 
 * @author AstonCat Team
 * @version $Revision: 1.88 $
 * 
 */

public class AstonCatChargingPolicy extends ChargingPolicy {
	static Logger logger = Logger
			.getLogger(AstonCatChargingPolicy.class);

	static final double DISCOUNT_RATE = 0.8D;

	static final double L3_DOWNTREND_RATIO = 0.97D;

	static final double L3_UPTREND_RATIO = 1.06D;

	static final double L2_DOWNTREND_RATIO = 0.92D;

	static final double L2_UPTREND_RATIO = 1.16D;

	static final double L1_RATIOS[] = { 0.85D, 1.0D, 1.1D, 1.2D, 1.3D, 1.4D,
			1.5D, 1.6D, 1.7D, 1.8D, 1.9D, 2.0D, 2.1D, 2.2D, 2.3D, 2.40D };

	static final double MINI_STEP = 0.00025D;

	static final double SMALL_STEP = 0.0005D;

	static final double LARGE_STEP = 0.00075D;

	static final int AVERAGE_LONG = 100;

	static final int AVERAGE_SHORT = 15;

	private int numOfTradersInMarket;

	private int numOfTradersGlobally;

	protected ReverseDiscountSlidingLearner rdsLearner;

	protected AveragingLearner avgLearner;

	protected SlidingWindowLearner swLearner;

	private int currentday;

	private final Map<String, double[]> dailyFees;

	protected double initialFees[];

	public AstonCatChargingPolicy() {
		initialFees = new double[fees.length];
		dailyFees = Collections.synchronizedMap(new HashMap<String, double[]>());

		rdsLearner = new ReverseDiscountSlidingLearner(AVERAGE_SHORT, DISCOUNT_RATE);
		rdsLearner.initialize();

		avgLearner = new AveragingLearner();
		avgLearner.initialize();

		swLearner = new SlidingWindowLearner(AVERAGE_SHORT);
		swLearner.initialize();
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);

		final Parameter defBase = new Parameter(ChargingPolicy.P_DEF_BASE);

		for (int i = 0; i < fees.length; i++) {
			initialFees[i] = parameters.getDoubleWithDefault(
					base.push(ChargingPolicy.P_FEES[i]),
					defBase.push(ChargingPolicy.P_FEES[i]), initialFees[i]);
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		init1();
	}

	@Override
	public void reset() {
		super.reset();

		rdsLearner.reset();

		init1();
	}

	private void init1() {
		for (int i = 0; i < fees.length; i++) {
			fees[i] = initialFees[i];
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		if ((event instanceof FeesAnnouncedEvent)) {
			dailyFees.put(((FeesAnnouncedEvent) event).getSpecialist().getId(),
					((FeesAnnouncedEvent) event).getSpecialist().getFees());
		} else if ((event instanceof RegisteredTradersAnnouncedEvent)) {
			updateTraderRegistration((RegisteredTradersAnnouncedEvent) event);
		} else if ((event instanceof DayClosedEvent)) {
			updateFeesAfterDay();
		} else if ((event instanceof DayOpeningEvent)) {
			currentday = event.getDay();
			numOfTradersGlobally = 0;
			numOfTradersInMarket = 0;
			dailyFees.clear();
		}
	}

	protected void updateFeesL1And2(double marketTrend, double movingTrend) {
		if (marketTrend > L2_UPTREND_RATIO) {
			if (movingTrend < L3_DOWNTREND_RATIO) {
				fees[4] += MINI_STEP;
			} else {
				fees[4] += SMALL_STEP;

			}
		} else if (marketTrend < L2_DOWNTREND_RATIO) {
			fees[4] -= LARGE_STEP;
		} else {
			if (movingTrend > L3_UPTREND_RATIO) {
				fees[4] += MINI_STEP;
			}
			if (movingTrend < L3_DOWNTREND_RATIO) {
				fees[4] -= MINI_STEP;
			}
		}
	}

	protected void updateFeesAfterDay() {
		final double avgShort = swLearner.act();
		final double avgLife = avgLearner.act();
		final double weightedAvgShort = rdsLearner.act();
		final double target = minimumTraderToAchieve();

		final double targetcompletion = avgShort / target;
		final double marketTrend = avgShort / avgLife;
		final double movingTrend = weightedAvgShort / avgShort;

		int l1Index = 0;
		if (targetcompletion <= L1_RATIOS[l1Index++]) {
			for (int i = 0; i < fees.length; i++) {
				fees[i] = 0.0D;
			}
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			if (marketTrend > L2_UPTREND_RATIO) {
				return;
			} else if (marketTrend < L2_DOWNTREND_RATIO) {
				fees[4] -= LARGE_STEP;
			} else {
				if (movingTrend > L3_UPTREND_RATIO) {
					fees[4] -= MINI_STEP;
				}
				if (movingTrend < L3_DOWNTREND_RATIO) {
					fees[4] -= SMALL_STEP;
				}

			}

			fees[4] = Math.min(fees[4], 0.02D);
			fees[4] = Math.max(fees[4], 0.0D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.025D);
			fees[4] = Math.max(fees[4], 0.01D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.03D);
			fees[4] = Math.max(fees[4], 0.015D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.035D);
			fees[4] = Math.max(fees[4], 0.02D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.04D);
			fees[4] = Math.max(fees[4], 0.025D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.045D);
			fees[4] = Math.max(fees[4], 0.03D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.046D);
			fees[4] = Math.max(fees[4], 0.035D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.047D);
			fees[4] = Math.max(fees[4], 0.035D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.048D);
			fees[4] = Math.max(fees[4], 0.035D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.05D);
			fees[4] = Math.max(fees[4], 0.04D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.0525D);
			fees[4] = Math.max(fees[4], 0.04D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.055D);
			fees[4] = Math.max(fees[4], 0.04D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.06D);
			fees[4] = Math.max(fees[4], 0.045D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.065D);
			fees[4] = Math.max(fees[4], 0.045D);
		} else if (targetcompletion < L1_RATIOS[l1Index++]) {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.07);
			fees[4] = Math.max(fees[4], 0.05D);
		} else {
			updateFeesL1And2(marketTrend, movingTrend);

			fees[4] = Math.min(fees[4], 0.075D);
			fees[4] = Math.max(fees[4], 0.055D);
		}

		if (currentday < 50) {
			fees[4] = 0.04D;
		}

		if (currentday > 100) {
			fees[4] = Math.max(fees[4], 0.01D);
		}

		if (currentday > 200) {
			fees[4] = Math.max(fees[4], 0.02D);
		}

		if (currentday > 300) {
			fees[4] = Math.max(fees[4], 0.03D);
		}

		if (currentday > 400) {
			fees[4] = Math.max(fees[4], 0.04D);
		}

		boolean allFree = true;
		int chargecount = 0;
		final Iterator<double[]> itor = dailyFees.values().iterator();
		while (itor.hasNext()) {
			final double[] fees = itor.next();
			for (double fee : fees) {
				if (fee > 0.0D) {
					allFree = false;
					chargecount++;
				}
			}
		}

		// modified the original statement as it does not make sense
		if ((allFree) || (!allFree) && (chargecount <= 2)) {
			fees[4] = 1.E-005D;
		}
	}

	protected void updateTraderRegistration(
			final RegisteredTradersAnnouncedEvent event) {
		final int numberOfTraders = event.getNumOfTraders();

		numOfTradersGlobally += numberOfTraders;

		if (event.getSpecialist().getId()
				.equalsIgnoreCase(getAuctioneer().getName())) {
			numOfTradersInMarket = numberOfTraders;
			rdsLearner.train(numOfTradersInMarket);
			avgLearner.train(numOfTradersInMarket);
		}

	}

	protected double minimumTraderToAchieve() {
		return numOfTradersGlobally
				/ auctioneer.getRegistry().getNumOfActiveSpecialists();
	}
}
