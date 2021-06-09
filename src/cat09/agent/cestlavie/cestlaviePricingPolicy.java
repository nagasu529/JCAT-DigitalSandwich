/*
 * cestlavie in CAT'09.
 */

package cat09.agent.cestlavie;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.market.MarketQuote;
import edu.cuny.cat.market.pricing.ScheduleBalancingPricingPolicy;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.struct.FixedLengthQueue;

/**
 * A pricing policy that combines
 * {@link edu.cuny.cat.market.pricing.NPricingPolicy} with <code>n=100</code>
 * and {@link edu.cuny.cat.market.pricing.ScheduleBalancingPricingPolicy}. When a
 * transaction occurs, it first calculates a transaction price in the same way
 * as <code>ScheduleBalancingPricingPolicy</code> does, enqueue this transaction
 * price, and finally use the average of transaction prices in the sliding
 * window as <code>NPricingPolicy</code> does.
 * 
 * @author cestlavie Team
 * @version $Revision: 1.88 $
 */

public class cestlaviePricingPolicy extends ScheduleBalancingPricingPolicy {

	static Logger logger = Logger.getLogger(cestlaviePricingPolicy.class);

	public static final String P_DEF_BASE = "cestlavie_pricing";

	public static final String P_N = "n";

	public static final int DEFAULT_N = 10;

	protected int n;

	protected FixedLengthQueue queue;

	public cestlaviePricingPolicy() {
		this(cestlaviePricingPolicy.DEFAULT_N);
	}

	public cestlaviePricingPolicy(final int n) {
		this.n = n;
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);
		n = parameters
				.getIntWithDefault(base.push(cestlaviePricingPolicy.P_N),
						(new Parameter(cestlaviePricingPolicy.P_DEF_BASE))
								.push(cestlaviePricingPolicy.P_N),
						cestlaviePricingPolicy.DEFAULT_N);
	}

	@Override
	public void initialize() {
		super.initialize();
		queue = new FixedLengthQueue(2 * n);
	}

	@Override
	public void reset() {
		super.reset();
		queue.reset();
	}

	@Override
	public double determineClearingPrice(final Shout bid, final Shout ask,
			final MarketQuote clearingQuote) {
		queue.newData(kInterval(bid.getPrice(), ask.getPrice()));
		final double avg = queue.getMean();
		final double price = avg < bid.getPrice() ? avg > ask.getPrice() ? avg
				: ask.getPrice() : bid.getPrice();
		return price;
	}

	@Override
	public String toString() {
		return super.toString() + " " + cestlaviePricingPolicy.P_N + ":" + n;
	}
}
