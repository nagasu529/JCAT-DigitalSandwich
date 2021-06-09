/*
 * Mertacor in CAT'09.
 */

package cat09.agent.mertacor;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.market.MarketQuote;
import edu.cuny.cat.market.core.SpecialistInfo;
import edu.cuny.cat.market.pricing.KPricingPolicy;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.util.Utils;

/**
 * uses an estimated global equilibrium price to clear the market if the
 * estimate is available, or otherwise adopts a dynamic discriminatory pricing
 * policy to balance demand and supply.
 * 
 * @author Mertacor Team
 * @version $Revision: 1.88 $
 * 
 */

public class MertacorPricingPolicy extends KPricingPolicy {

	static Logger logger = Logger.getLogger(MertacorPricingPolicy.class);

	public static final String P_DEF_BASE = "mertacor_pricing";

	public static final String P_DEFAULT_K = "defaultk";

	public static final String P_MEMORY_SIZE = "memorysize";

	public static final String P_THRESHOLD = "threshold";

	public static final String P_LOWER_BOUND = "lowerbound";

	public static final String P_UPPER_BOUND = "upperbound";

	public static final int DEF_MEMORY_SIZE = 5;

	public static final double DEF_THRESHOLD = 0.1;

	public static final double DEF_LOWER_BOUND = 0.3;

	public static final double DEF_UPPER_BOUND = 0.7;

	protected double defaultK;

	protected int memorySize;

	protected double supply[];

	protected double demand[];

	protected int counter;

	protected double threshold;

	protected double lowerBound;

	protected double upperBound;

	public MertacorPricingPolicy() {
		this(KPricingPolicy.DEFAULT_K, KPricingPolicy.DEFAULT_K,
				MertacorPricingPolicy.DEF_MEMORY_SIZE,
				MertacorPricingPolicy.DEF_THRESHOLD,
				MertacorPricingPolicy.DEF_LOWER_BOUND,
				MertacorPricingPolicy.DEF_UPPER_BOUND);
	}

	public MertacorPricingPolicy(final double k, final double defaultK,
			final int memorySize, final double threshold, final double lowerBound,
			final double upperBound) {
		super(k);
		this.defaultK = defaultK;
		this.memorySize = memorySize;
		this.threshold = threshold;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);

		defaultK = parameters.getDoubleWithDefault(base
				.push(MertacorPricingPolicy.P_DEFAULT_K), (new Parameter(
				MertacorPricingPolicy.P_DEF_BASE))
				.push(MertacorPricingPolicy.P_DEFAULT_K), defaultK);
		memorySize = parameters.getIntWithDefault(base
				.push(MertacorPricingPolicy.P_MEMORY_SIZE), (new Parameter(
				MertacorPricingPolicy.P_DEF_BASE))
				.push(MertacorPricingPolicy.P_MEMORY_SIZE), memorySize);
		threshold = parameters.getDoubleWithDefault(base
				.push(MertacorPricingPolicy.P_THRESHOLD), (new Parameter(
				MertacorPricingPolicy.P_DEF_BASE))
				.push(MertacorPricingPolicy.P_THRESHOLD), threshold);
		lowerBound = parameters.getDoubleWithDefault(base
				.push(MertacorPricingPolicy.P_LOWER_BOUND), (new Parameter(
				MertacorPricingPolicy.P_DEF_BASE))
				.push(MertacorPricingPolicy.P_LOWER_BOUND), lowerBound);
		upperBound = parameters.getDoubleWithDefault(base
				.push(MertacorPricingPolicy.P_UPPER_BOUND), (new Parameter(
				MertacorPricingPolicy.P_DEF_BASE))
				.push(MertacorPricingPolicy.P_UPPER_BOUND), upperBound);
	}

	@Override
	public void initialize() {
		super.initialize();

		supply = new double[memorySize];
		demand = new double[memorySize];
	}

	@Override
	public void reset() {
		counter = 0;
	}

	@Override
	public double determineClearingPrice(final Shout bid, final Shout ask,
			final MarketQuote clearingQuote) {
		if (!Double.isNaN(auctioneer.getRegistry().getGlCEMidPrice())) {
			double price = auctioneer.getRegistry().getGlCEMidPrice();
			if (price > Math.max(ask.getPrice(), bid.getPrice())) {
				price = Math.max(ask.getPrice(), bid.getPrice());
			} else if (price < Math.min(ask.getPrice(), bid.getPrice())) {
				price = Math.min(ask.getPrice(), bid.getPrice());
			}
			return price;
		} else {
			return kInterval(ask.getPrice(), bid.getPrice());
		}
	}

	/**
	 * adjusts k based on the supply and demand in the market over the last
	 * several days so as to balance supply and demand.
	 */
	protected void updateK() {
		double ourSupply = 0.0D;
		double ourDemand = 0.0D;
		double total = 0.0D;
		if (counter < memorySize) {
			k = defaultK;
			return;
		}

		for (int i = 0; i < memorySize; i++) {
			ourSupply += supply[i];
			ourDemand += demand[i];
		}

		ourSupply /= memorySize;
		ourDemand /= memorySize;
		total = ourSupply + ourDemand;
		if (Math.abs(ourSupply / total - ourDemand / total) > threshold) {
			k = ourDemand / total;
			if (k < lowerBound) {
				k = lowerBound;
			} else if (k > upperBound) {
				k = upperBound;
			}
		} else {
			k = defaultK;
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);
		if (event instanceof DayClosedEvent) {
			final SpecialistInfo specialistInfo = auctioneer.getRegistry()
					.getMyInfo();
			supply[counter % memorySize] = specialistInfo.getSupply();
			demand[counter++ % memorySize] = specialistInfo.getDemand();
			updateK();
		}
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "\n"
				+ Utils.indent(MertacorPricingPolicy.P_DEFAULT_K + ":" + defaultK + " "
						+ MertacorPricingPolicy.P_LOWER_BOUND + ":" + lowerBound + " "
						+ MertacorPricingPolicy.P_UPPER_BOUND + ":" + upperBound);
		s += "\n"
				+ Utils.indent(MertacorPricingPolicy.P_MEMORY_SIZE + ":" + memorySize
						+ " " + MertacorPricingPolicy.P_THRESHOLD + ":" + threshold);
		return s;
	}
}
