/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo;

import java.util.Collection;

import org.apache.log4j.Logger;

import edu.cuny.cat.market.subscribing.SubscribingPolicy;

/**
 * Subscribes for information from all specialists.
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class jacSubscribingPolicy extends SubscribingPolicy {

	protected static Logger logger = Logger.getLogger(jacSubscribingPolicy.class);

	@Override
	protected String[] getSubscribees() {
		final Collection<String> idColl = auctioneer.getRegistry()
				.getSpecialistIds();
		final String[] sIds = idColl.toArray(new String[idColl.size()]);

		return sIds;
	}
}