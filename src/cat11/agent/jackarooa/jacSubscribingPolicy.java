package cat11.agent.jackarooa;

import edu.cuny.cat.market.subscribing.SubscribingPolicy;

public class jacSubscribingPolicy extends SubscribingPolicy {
	@Override
	protected String[] getSubscribees() {
		final String[] sIds = auctioneer
				.getRegistry()
				.getSpecialistIds()
				.toArray(new String[auctioneer.getRegistry().getSpecialistIds().size()]);

		return sIds;
	}
}