/*
 * Mertacor in CAT'09.
 */

package cat09.agent.mertacor;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.DayOpeningEvent;
import edu.cuny.cat.event.RoundClosingEvent;
import edu.cuny.cat.event.ShoutPlacedEvent;
import edu.cuny.cat.market.clearing.MarketClearingCondition;

/**
 * periodically clears for the first several rounds, the number of which equals
 * the number of entitlements a trader has, and then continuously clears
 * afterwards.
 * 
 * @author Mertacor Team
 * @version $Revision: 1.88 $
 * 
 */

public class MertacorClearingCondition extends MarketClearingCondition {

	static Logger logger = Logger.getLogger(MertacorClearingCondition.class);

	protected int roundClearings;

	public MertacorClearingCondition() {
		init0();
	}

	protected void init0() {
		roundClearings = 1;
	}

	@Override
	public void reset() {
		super.reset();
		init0();
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);
		if (event instanceof DayOpeningEvent) {
			roundClearings = auctioneer.getRegistry().getTraderEntitlement();
		} else if ((event instanceof RoundClosingEvent)
				&& (event.getRound() < roundClearings)) {
			triggerClearing();
		} else if ((event instanceof ShoutPlacedEvent)
				&& (event.getRound() >= roundClearings)) {
			final Shout shout = ((ShoutPlacedEvent) event).getShout();
			if (shout.getTrader() != null) {
				return;
			}
			triggerClearing();
		}
	}
}
