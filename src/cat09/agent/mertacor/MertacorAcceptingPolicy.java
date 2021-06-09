/*
 * Mertacor in CAT'09.
 */

package cat09.agent.mertacor;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.IllegalShoutException;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayClosedEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.RegistrationEvent;
import edu.cuny.cat.market.accepting.SelfBeatingAcceptingPolicy;
import edu.cuny.cat.market.core.TraderInfo;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;
import edu.cuny.math.dist.CumulativeDistribution;
import edu.cuny.util.Utils;

/**
 * accepts shouts as self-beating policy does in the beginning certain number of
 * days, and switches to equilibrium-beating policy afterwards for fresh shouts.
 * 
 * @author Mertacor Team
 * @version $Revision: 1.88 $
 * 
 */

public class MertacorAcceptingPolicy extends SelfBeatingAcceptingPolicy {

	static Logger logger = Logger.getLogger(MertacorAcceptingPolicy.class);

	public static final String P_DEF_BASE = "mertacor_accepting";

	public static final String P_THRESHOLD = "threshold";

	public static final String P_MARGIN = "margin";

	public static final String P_EQ_DAYS = "eqdays";

	public static final double DEF_THRESHOLD = 0.8;

	protected static final double DEF_MARGIN = 0.2;

	protected static final int DEF_EQ_DAYS = 180;

	protected double margin;

	protected int eqDays;

	protected double threshold;

	/**
	 * tells whether a certain percentage of traders are traced; not used in
	 * decision making
	 */
	protected boolean tradersExploring;

	/**
	 * whether the local registry has the global equilibrium price available
	 */
	protected boolean equilibriumMode;

	protected CumulativeDistribution shoutStats;

	protected double minAcceptedPrice;

	protected double maxAcceptedPrice;

	protected static IllegalShoutException bidException = null;

	protected static IllegalShoutException askException = null;

	public MertacorAcceptingPolicy() {
		shoutStats = new CumulativeDistribution();
		init0();
	}

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);
		final Parameter defBase = new Parameter(MertacorAcceptingPolicy.P_DEF_BASE);
		margin = parameters.getDoubleWithDefault(
				base.push(MertacorAcceptingPolicy.P_MARGIN),
				defBase.push(MertacorAcceptingPolicy.P_MARGIN),
				MertacorAcceptingPolicy.DEF_MARGIN);
		eqDays = parameters.getIntWithDefault(
				base.push(MertacorAcceptingPolicy.P_EQ_DAYS),
				defBase.push(MertacorAcceptingPolicy.P_EQ_DAYS),
				MertacorAcceptingPolicy.DEF_EQ_DAYS);
		threshold = parameters.getDoubleWithDefault(
				base.push(MertacorAcceptingPolicy.P_THRESHOLD),
				defBase.push(MertacorAcceptingPolicy.P_THRESHOLD),
				MertacorAcceptingPolicy.DEF_THRESHOLD);
	}

	private void init0() {
		equilibriumMode = false;
		tradersExploring = true;
	}

	@Override
	public void reset() {
		super.reset();
		shoutStats.reset();
		init0();
	}

	@Override
	public void check(final Shout oldShout, final Shout newShout)
			throws IllegalShoutException {
		if (equilibriumMode && (oldShout == null)) {
			if (newShout.isBid()) {
				if (newShout.getPrice() < minAcceptedPrice) {
					throw new IllegalShoutException(
							"unable to beat the minimum bid price allowed");
				}
			} else {
				if (newShout.getPrice() > maxAcceptedPrice) {
					throw new IllegalShoutException(
							"unable to beat the maximum ask price allowed");
				}
			}
		} else {
			super.check(oldShout, newShout);
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);
		if ((event instanceof DayOpeningEvent) && (event.getDay() >= eqDays)) {
			shoutStats.reset();
			if (!Double.isNaN(auctioneer.getRegistry().getGlCEMidPrice())) {
				equilibriumMode = true;
				try {
					minAcceptedPrice = (1.0D - margin)
							* auctioneer.getRegistry().getGlCEMidPrice();
					maxAcceptedPrice = (1.0D + margin)
							* auctioneer.getRegistry().getGlCEMidPrice();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		} else if ((event instanceof RegistrationEvent)
				&& (event.getDay() >= eqDays)) {
			final String traderId = ((RegistrationEvent) event).getTraderId();
			final TraderInfo traderInfo = auctioneer.getRegistry().getTraderInfo(
					traderId);
			if ((traderInfo != null) && traderInfo.isTraced()) {
				shoutStats.newData(traderInfo.getLastShoutPrice());
				margin = 1.25D * shoutStats.getStdDev();
				minAcceptedPrice = (1.0D - margin)
						* auctioneer.getRegistry().getGlCEMidPrice();
				maxAcceptedPrice = (1.0D + margin)
						* auctioneer.getRegistry().getGlCEMidPrice();
			}
		} else if (event instanceof DayClosedEvent) {
			if (tradersExploring) {
				if ((double) auctioneer.getRegistry().getNumOfTracedTraders()
						/ auctioneer.getRegistry().getTraders().size() >= threshold) {
					tradersExploring = false;
				}
			}
		}
	}

	@Override
	public String toString() {
		String s = super.toString();

		s += "\n"
				+ Utils.indent(MertacorAcceptingPolicy.P_MARGIN + ":" + margin + " "
						+ MertacorAcceptingPolicy.P_EQ_DAYS + ":" + eqDays + " "
						+ MertacorAcceptingPolicy.P_THRESHOLD + ":" + threshold);

		return s;
	}
}
