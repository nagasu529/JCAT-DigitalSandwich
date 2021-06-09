/*
 * AstonCat-Plus from CAT'10
 */

package cat10.agent.astoncat;

import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.cuny.cat.MarketRegistry;
import edu.cuny.cat.core.Shout;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.GameStartingEvent;
import edu.cuny.cat.event.ShoutPlacedEvent;
import edu.cuny.cat.market.clearing.MarketClearingCondition;
import edu.cuny.cat.market.matching.ShoutEngine;

/**
 * 
 * @author AstonCat Team
 * @version $Revision: 1.88 $
 * 
 */

public class AstonCatClearingCondition extends MarketClearingCondition {
	static Logger logger = Logger.getLogger(AstonCatClearingCondition.class);

	protected ShoutEngine shoutEngine;

	protected MarketRegistry registry;

	protected EquilibriumEstimator equEstimator;
	
	protected PrivateValueRangeEstimator pvrEstimator;

	@Override
	public void initialize() {
		equEstimator = EquilibriumEstimator.getHelper(getAuctioneer());
		pvrEstimator = PrivateValueRangeEstimator.getHelper(getAuctioneer());
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if (event instanceof ShoutPlacedEvent) {
			if (event.getRound() > 2) {
				// standard cda clearing
				triggerClearing();
			} else {
				boolean deviatedMatching = false;
				
				double equPrice = equEstimator.getEquilibriumPrice();
				Shout shout = null;
				double surplus = 0;
				int quantity = 0;
				Iterator<Shout> itor = shoutEngine.matchedBidIterator();
				while (itor.hasNext()) {
					shout = itor.next();
					if (!Double.isNaN(equPrice) && shout.getPrice() < equPrice) {
						deviatedMatching = true;
					}

					surplus += shout.getPrice() * shout.getQuantity();
					quantity += shout.getQuantity();
				}

				itor = shoutEngine.matchedAskIterator();
				while (itor.hasNext()) {
					shout = itor.next();
					if (!Double.isNaN(equPrice) && shout.getPrice() > equPrice) {
						deviatedMatching = true;
					}
					
					surplus -= shout.getPrice() * shout.getQuantity();
				}
				
				double surplusPerTransaction = surplus / quantity;
				if (surplusPerTransaction < 0) {
					logger.error("Profit per unit of goods in transaction should be NOT be negative !");
				}
				
				double thetaS = pvrEstimator.getPrivateValueSpread() * 0.02;
				double thetaL = pvrEstimator.getPrivateValueSpread() * 0.16;
				
				if (deviatedMatching && surplusPerTransaction > thetaS) {
					triggerClearing();
				} else if (!deviatedMatching && surplusPerTransaction > thetaL) {
					triggerClearing();
				} else if (quantity > (registry
						.getNumOfActiveTraders() * 0.1) / registry
						.getNumOfActiveSpecialists()) {
					triggerClearing();					
				}
			}
		} else if (event instanceof GameStartingEvent) {
			shoutEngine = getAuctioneer().getShoutEngine();
			registry = getAuctioneer().getRegistry();
		}
	}

}
