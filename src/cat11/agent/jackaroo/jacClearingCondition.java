/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo;

import java.util.HashSet;

import org.apache.log4j.Logger;

import cern.jet.random.Uniform;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.RoundClosingEvent;
import edu.cuny.cat.event.ShoutPlacedEvent;
import edu.cuny.cat.market.clearing.MarketClearingCondition;
import edu.cuny.prng.GlobalPRNG;
import edu.cuny.util.Galaxy;

/**
 * A combination of a less frequent version of
 * {@link edu.cuny.cat.market.clearing.RoundClearingCondition} and
 * {@link edu.cuny.cat.market.clearing.ProbabilisticClearingCondition}.
 * 
 * It clears the market probabilistically after certain specified rounds end
 * based on estimated entitlement of traders and a probabilistic threshold and
 * also clears the market probabilistically when a new shout is placed. The two
 * probabilistic thresholds sum to be 1. By default, the probability of round
 * clearing is 1 and the arrival of shouts does not trigger clearing at all.
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jacClearingCondition extends MarketClearingCondition {

	static Logger logger = Logger.getLogger(jacClearingCondition.class);

	protected jacHelper helper;

	protected Uniform uniformDistribution;

	protected final double threshold = 0.0D;

	protected final HashSet<Integer> clearingRoundsWithEntitlement3;

	protected final HashSet<Integer> clearingRoundsWithEntitlement4;

	protected final HashSet<Integer> clearingRoundsWithEntitlement5;

	public jacClearingCondition() {
		uniformDistribution = new Uniform(0.0D, 1.0D, Galaxy.getInstance()
				.getDefaultTyped(GlobalPRNG.class).getEngine());

		clearingRoundsWithEntitlement3 = new HashSet<Integer>();
		clearingRoundsWithEntitlement3.add(Integer.valueOf(1));
		clearingRoundsWithEntitlement3.add(Integer.valueOf(3));
		clearingRoundsWithEntitlement3.add(Integer.valueOf(5));
		clearingRoundsWithEntitlement3.add(Integer.valueOf(7));
		clearingRoundsWithEntitlement3.add(Integer.valueOf(8));
		clearingRoundsWithEntitlement3.add(Integer.valueOf(9));

		clearingRoundsWithEntitlement4 = new HashSet<Integer>();
		clearingRoundsWithEntitlement4.add(Integer.valueOf(1));
		clearingRoundsWithEntitlement4.add(Integer.valueOf(3));
		clearingRoundsWithEntitlement4.add(Integer.valueOf(5));
		clearingRoundsWithEntitlement4.add(Integer.valueOf(6));
		clearingRoundsWithEntitlement4.add(Integer.valueOf(7));
		clearingRoundsWithEntitlement4.add(Integer.valueOf(9));

		clearingRoundsWithEntitlement5 = new HashSet<Integer>();
		clearingRoundsWithEntitlement5.add(Integer.valueOf(1));
		clearingRoundsWithEntitlement5.add(Integer.valueOf(2));
		clearingRoundsWithEntitlement5.add(Integer.valueOf(3));
		clearingRoundsWithEntitlement5.add(Integer.valueOf(4));
		clearingRoundsWithEntitlement5.add(Integer.valueOf(6));
		clearingRoundsWithEntitlement5.add(Integer.valueOf(7));
		clearingRoundsWithEntitlement5.add(Integer.valueOf(9));
	}

	@Override
	public void initialize() {
		helper = jacHelper.getHelper(auctioneer);
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		final double d = uniformDistribution.nextDouble();
		final int round = event.getRound();

		if (((event instanceof RoundClosingEvent)) && (d >= threshold)
				&& (timeToClear(round))) {
			triggerClearing();
		} else if ((event instanceof ShoutPlacedEvent)) {
			final Shout shout = ((ShoutPlacedEvent) event).getShout();
			if ((shout.getSpecialist() != null)
					&& (!shout.getSpecialist().getId().equals(auctioneer.getName()))) {
				jacClearingCondition.logger
						.error("Unexpected shout placed event regarding a shout placed in other market !");
			}

			if (d < threshold) {
				triggerClearing();
			}
		}
	}

	private boolean timeToClear(final int round) {
		final int maxNumberOfItems = helper.getMarketInfo().getMaxItemsTraderHas();

		if (helper.getMarketInfo().getTotalDaysRecorded() < 1) {
			return clearingRoundsWithEntitlement4.contains(Integer.valueOf(round));
		}
		if (maxNumberOfItems <= 3) {
			return clearingRoundsWithEntitlement3.contains(Integer.valueOf(round));
		}
		if (maxNumberOfItems == 4) {
			return clearingRoundsWithEntitlement4.contains(Integer.valueOf(round));
		}
		if (maxNumberOfItems == 5) {
			return clearingRoundsWithEntitlement5.contains(Integer.valueOf(round));
		}

		return true;
	}
}