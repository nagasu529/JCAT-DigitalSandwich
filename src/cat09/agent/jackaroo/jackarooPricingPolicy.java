/* 
 * jackaroo in CAT'09
 */

package cat09.agent.jackaroo;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.market.MarketQuote;
import edu.cuny.cat.market.pricing.DiscriminatoryPricingPolicy;
import edu.cuny.cat.market.pricing.KPricingPolicy;
import edu.cuny.cat.market.pricing.UniformPricingPolicy;

/**
 * A pricing policy that combines {@link DiscriminatoryPricingPolicy} and
 * {@link edu.cuny.cat.market.pricing.UniformPricingPolicy}, and uses the former in the
 * first 10 days.
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jackarooPricingPolicy extends KPricingPolicy {

	public final static int START_UNIFORM_DAY = 10;

	protected UniformPricingPolicy uniformPolicy;

	protected DiscriminatoryPricingPolicy discriminatoryPolicy;

	protected int day;

	public jackarooPricingPolicy() {
		k = KPricingPolicy.DEFAULT_K;
		uniformPolicy = new UniformPricingPolicy(k);
		discriminatoryPolicy = new DiscriminatoryPricingPolicy(k);
		init0();
	}

	private void init0() {
		day = -1;
	}

	@Override
	public void reset() {
		super.reset();
		init0();
		uniformPolicy.reset();
		discriminatoryPolicy.reset();
	}

	@Override
	public double determineClearingPrice(final Shout bid, final Shout ask,
			final MarketQuote clearingQuote) {
		if (day >= jackarooPricingPolicy.START_UNIFORM_DAY) {
			// TODO:
			// old code:
			// if (ask.getPrice() > ((MyAuctioneer) auctioneer).getMediumPrice()) {
			// return ask.getPrice();
			// }
			// if (bid.getPrice() < ((MyAuctioneer) auctioneer).getMediumPrice()) {
			// return bid.getPrice();
			// } else {
			// return ((MyAuctioneer) auctioneer).getMediumPrice();
			// }

			return uniformPolicy.determineClearingPrice(bid, ask, clearingQuote);
		} else {
			return discriminatoryPolicy.determineClearingPrice(bid, ask,
					clearingQuote);
		}
	}

	public void errorOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if (event instanceof DayOpeningEvent) {
			day = event.getDay();
		}
	}
}
