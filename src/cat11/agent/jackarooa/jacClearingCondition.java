package cat11.agent.jackarooa;

import java.util.HashSet;

import cern.jet.random.Uniform;
import cern.jet.random.engine.RandomEngine;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.RoundClosingEvent;
import edu.cuny.cat.event.ShoutPlacedEvent;
import edu.cuny.cat.market.clearing.MarketClearingCondition;
import edu.cuny.prng.GlobalPRNG;
import edu.cuny.util.Galaxy;

public class jacClearingCondition extends MarketClearingCondition {

	protected jHelper helper;

	Uniform uniformDistribution;

	private final double threshold = 0.0D;

	private final HashSet<Integer> set3;

	private final HashSet<Integer> set4;

	private final HashSet<Integer> set5;

	public jacClearingCondition() {
		final RandomEngine prng = Galaxy.getInstance()
				.getTyped("cat", GlobalPRNG.class).getEngine();

		uniformDistribution = new Uniform(0.0D, 1.0D, prng);

		set3 = new HashSet<Integer>();
		set3.add(Integer.valueOf(1));
		set3.add(Integer.valueOf(3));
		set3.add(Integer.valueOf(5));
		set3.add(Integer.valueOf(7));
		set3.add(Integer.valueOf(8));
		set3.add(Integer.valueOf(9));

		set4 = new HashSet<Integer>();
		set4.add(Integer.valueOf(1));
		set4.add(Integer.valueOf(3));
		set4.add(Integer.valueOf(5));
		set4.add(Integer.valueOf(6));
		set4.add(Integer.valueOf(7));

		set4.add(Integer.valueOf(9));

		set5 = new HashSet<Integer>();
		set5.add(Integer.valueOf(1));
		set5.add(Integer.valueOf(2));
		set5.add(Integer.valueOf(3));
		set5.add(Integer.valueOf(4));
		set5.add(Integer.valueOf(6));
		set5.add(Integer.valueOf(7));
		set5.add(Integer.valueOf(9));
	}

	@Override
	public void initialize() {
		super.initialize();

		helper = jHelper.getHelper(auctioneer);
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
					&& (shout.getSpecialist().getId().equals(auctioneer.getName()))) {
				if (d < threshold) {
					triggerClearing();
				}
			}
		}
	}

	private boolean timeToClear(final int round) {
		final int maxNumberOfItems = helper.getMarketInfo().getMaxItemsTraderHas();

		if (helper.getMarketInfo().getTotalDaysRecorded() < 1) {
			return set4.contains(Integer.valueOf(round));
		}
		if (maxNumberOfItems <= 3) {
			return set3.contains(Integer.valueOf(round));
		}
		if (maxNumberOfItems == 4) {
			return set4.contains(Integer.valueOf(round));
		}
		if (maxNumberOfItems == 5) {
			return set5.contains(Integer.valueOf(round));
		}

		return true;
	}
}