/*
 * Mertacor in CAT'10.
 */

package cat10.agent.mertacor;

import org.apache.log4j.Logger;

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.market.charging.ChargingPolicy;
import edu.cuny.cat.stat.Score;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.util.Utils;

/**
 * Same as {@link cat09.agent.mertacor.MertacorChargingPolicy} in Mertacor from
 * CAT'09, except for values of certain constant variables.
 * 
 * @author Mertacor Team
 * @version $Revision: 1.88 $
 * 
 */

public class MertacorChargingPolicy extends ChargingPolicy {

	static Logger logger = Logger.getLogger(MertacorChargingPolicy.class);

	public static final String P_DEF_BASE = "mertacor_charging";

	public static final String P_ZERO_DAYS = "zerodays";

	public static final int DEFAULT_ZERO_DAYS = 120;

	public static final String P_FIRST_INCREASE = "first";

	public static final int DEF_FIRST_INCREASE = 250;

	public static final String P_DAY_STEP = "daystep";

	public static final int DEF_DAY_STEP = 50;

	public static final String P_STEP = "step";

	public static final double DEF_STEP = 0.005;

	protected int zeroDays;

	protected int firstIncreaseDay;

	protected int dayStep;

	protected int secondCheckDay;

	protected int firstCheckDay;

	protected double feeStep;

	protected double steadyScores[];

	protected double transScores[];

	protected double steadyMeanScore;

	protected double transMeanScore;

	protected int memoryCounter;

	protected double nonZeroProfitFee;

	public MertacorChargingPolicy() {
		init0();
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);

		final Parameter defBase = new Parameter(MertacorChargingPolicy.P_DEF_BASE);
		for (int i = 0; i < fees.length; i++) {
			fees[i] = parameters.getDoubleWithDefault(
					base.push(ChargingPolicy.P_FEES[i]),
					defBase.push(ChargingPolicy.P_FEES[i]), fees[i]);
		}

		zeroDays = parameters.getIntWithDefault(
				base.push(MertacorChargingPolicy.P_ZERO_DAYS),
				defBase.push(MertacorChargingPolicy.P_ZERO_DAYS),
				MertacorChargingPolicy.DEFAULT_ZERO_DAYS);
		firstIncreaseDay = parameters.getIntWithDefault(
				base.push(MertacorChargingPolicy.P_FIRST_INCREASE),
				defBase.push(MertacorChargingPolicy.P_FIRST_INCREASE),
				MertacorChargingPolicy.DEF_FIRST_INCREASE);
		dayStep = parameters.getIntWithDefault(
				base.push(MertacorChargingPolicy.P_DAY_STEP),
				defBase.push(MertacorChargingPolicy.P_DAY_STEP),
				MertacorChargingPolicy.DEF_DAY_STEP);
		feeStep = parameters.getDoubleWithDefault(
				base.push(MertacorChargingPolicy.P_STEP),
				defBase.push(MertacorChargingPolicy.P_STEP),
				MertacorChargingPolicy.DEF_STEP);
	}

	@Override
	public void initialize() {
		super.initialize();

		firstCheckDay = firstIncreaseDay - dayStep;
		secondCheckDay = firstIncreaseDay + dayStep;
		nonZeroProfitFee = fees[ChargingPolicy.PROFIT_INDEX];
		fees[ChargingPolicy.PROFIT_INDEX] = 0.0D;

		steadyScores = new double[dayStep];
		transScores = new double[dayStep];
	}

	private void init0() {
		memoryCounter = 0;
		steadyMeanScore = 0.0D;
		transMeanScore = 0.0D;
	}

	@Override
	public void reset() {
		super.reset();
		init0();
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		if (event instanceof DayClosedEvent) {
			fees[ChargingPolicy.REGISTRATION_INDEX] = 0.0;
			fees[ChargingPolicy.INFORMATION_INDEX] = 0.0;
			fees[ChargingPolicy.SHOUT_INDEX] = 0.0;
			fees[ChargingPolicy.TRANSACTION_INDEX] = 0.0;

			final int dayCounter = event.getDay();
			final Score dailyScore = auctioneer.getRegistry().getMyInfo()
					.getDailyScore();
			if (dayCounter >= secondCheckDay) {
				if (dayCounter == secondCheckDay) {
					transScores[memoryCounter] = dailyScore.total;
					transMeanScore = 0.0D;
					for (int i = 0; i < dayStep; i++) {
						transMeanScore += transScores[i];
					}

					transMeanScore /= dayStep;
					memoryCounter = 0;
					if (transMeanScore >= steadyMeanScore) {
						fees[ChargingPolicy.PROFIT_INDEX] += feeStep;
					}
				} else {
					if (memoryCounter < dayStep) {
						transScores[memoryCounter++] = dailyScore.total;
					}
					if (dayCounter == secondCheckDay + dayStep) {
						transMeanScore = 0.0D;
						for (int i = 0; i < dayStep; i++) {
							transMeanScore += transScores[i];
						}

						transMeanScore /= dayStep;
						memoryCounter = 0;
						if (transMeanScore >= steadyMeanScore) {
							fees[ChargingPolicy.PROFIT_INDEX] += feeStep;
						}
					}
				}
			} else if (dayCounter >= firstIncreaseDay) {
				if (dayCounter == firstIncreaseDay) {
					steadyScores[memoryCounter] = dailyScore.total;
					steadyMeanScore = 0.0D;
					for (int i = 0; i < dayStep; i++) {
						steadyMeanScore += steadyScores[i];
					}

					steadyMeanScore /= dayStep;
					memoryCounter = 0;
					fees[ChargingPolicy.PROFIT_INDEX] += feeStep;
				} else {
					transScores[memoryCounter++] = dailyScore.total;
				}
			} else if (dayCounter > firstCheckDay) {
				if (dayCounter == firstCheckDay + 1) {
					memoryCounter = 0;
				}
				steadyScores[memoryCounter++] = dailyScore.total;
			} else if (dayCounter >= zeroDays) {
				fees[ChargingPolicy.PROFIT_INDEX] = nonZeroProfitFee;
			} else {
				fees[ChargingPolicy.PROFIT_INDEX] = 0.0D;
			}
		}
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "\n"
				+ Utils.indent(MertacorChargingPolicy.P_ZERO_DAYS + ":" + zeroDays
						+ " " + MertacorChargingPolicy.P_FIRST_INCREASE + ":"
						+ firstIncreaseDay + " " + MertacorChargingPolicy.P_DAY_STEP + ":"
						+ dayStep + " " + MertacorChargingPolicy.P_STEP + ":" + feeStep);

		return s;
	}
}
