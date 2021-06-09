/*
 * Mertacor in CAT'09.
 */

package cat09.agent.mertacor;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.AvailableMarketsAnnouncedEvent;
import edu.cuny.cat.event.AvailableTradersAnnouncedEvent;
import edu.cuny.cat.market.subscribing.SubscribingPolicy;
import edu.cuny.config.param.Parameter;
import edu.cuny.config.param.ParameterDatabase;

/**
 * 
 * 
 * @author Mertacor Team
 * @version $Revision: 1.88 $
 * 
 */

public class MertacorSubscribingPolicy extends SubscribingPolicy {

	static Logger logger = Logger.getLogger(MertacorSubscribingPolicy.class);

	public static final String P_NUM = "num";

	public static final String P_FREEONLY = "freeonly";

	public static final String P_DEF_BASE = "mertacor_subscribing";

	protected final int DEF_NUM_OF_SUBSCRIBEES = 4;

	protected final int DEF_NUM_OF_TRADERS = 400;

	protected final int MAJORITY_NUM_OF_TRADERS = 286;

	/**
	 * the number of traders in the game
	 */
	protected int numOfTraders;

	/**
	 * remember the index of last subscribee in the list of specialists so as to
	 * take turns to subscribe with all specialists over time
	 */
	protected int subscribeeIndex;

	/**
	 * number of specialists to subscribe for information from
	 */
	protected int numOfSubscribees;

	/**
	 * controls whether to subscribe for info only when there is no charges.
	 */
	protected boolean freeOnly;

	@Override
	public void setup(final ParameterDatabase parameters, final Parameter base) {
		super.setup(parameters, base);

		final Parameter defBase = new Parameter(
				MertacorSubscribingPolicy.P_DEF_BASE);

		numOfSubscribees = parameters.getIntWithDefault(
				base.push(MertacorSubscribingPolicy.P_NUM),
				defBase.push(MertacorSubscribingPolicy.P_NUM), DEF_NUM_OF_SUBSCRIBEES);
		if (numOfSubscribees < 0) {
			MertacorSubscribingPolicy.logger
					.fatal("Number of subscribees cannot be negative !");
			numOfSubscribees = 0;
		}

		freeOnly = parameters.getBoolean(
				base.push(MertacorSubscribingPolicy.P_FREEONLY),
				defBase.push(MertacorSubscribingPolicy.P_FREEONLY), false);
	}

	@Override
	public void reset() {
		subscribeeIndex = 0;
	}

	@Override
	public String[] getSubscribees() {
		// all active specialists except myself
		final List<String> activeSpecialistIdList = new ArrayList<String>();
		for (final String activeSpecialistId : auctioneer.getRegistry()
				.getActiveOpponentIds()) {
			if (!auctioneer.getName().equalsIgnoreCase(activeSpecialistId)) {
				if (freeOnly) {
					if (auctioneer.getRegistry().getSpecialistInfo(activeSpecialistId)
							.getInformationFee() == 0.0D) {
						activeSpecialistIdList.add(activeSpecialistId);
					}
				} else {
					activeSpecialistIdList.add(activeSpecialistId);
				}
			}
		}

		int num = activeSpecialistIdList.size();

		if (num > numOfSubscribees) {
			num = numOfSubscribees;
		}

		final String subscribees[] = new String[num + 1];
		for (; num > 0; num--) {
			subscribees[num] = activeSpecialistIdList.get(subscribeeIndex++
					% activeSpecialistIdList.size());
		}
		subscribees[num] = auctioneer.getName();

		return subscribees;
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {
		super.eventOccurred(event);

		if (event instanceof AvailableTradersAnnouncedEvent) {
			numOfTraders = ((AvailableTradersAnnouncedEvent) event).getTraders()
					.size();
		} else if (event instanceof AvailableMarketsAnnouncedEvent) {
			// determines how many specialists to subscribe for info from and
			// whether to do so when they charge a fee
			numOfSubscribees = DEF_NUM_OF_SUBSCRIBEES;

			if (numOfTraders == 0) {
				numOfTraders = DEF_NUM_OF_TRADERS;
			}

			final int numOfSpecialists = ((AvailableMarketsAnnouncedEvent) event)
					.getMarkets().size();
			numOfSubscribees = (int) (Math.floor((double) MAJORITY_NUM_OF_TRADERS
					/ numOfTraders)
					* numOfSpecialists - 1.0);

			if (numOfSubscribees < DEF_NUM_OF_SUBSCRIBEES) {
				numOfSubscribees = DEF_NUM_OF_SUBSCRIBEES;
			}

			// Mertacor has the following in CAT'10, but it doesn't seem to process as
			// the game just started.
			// if (!Double.isNaN(auctioneer.getRegistry().getGlCEMidPrice())) {
			// freeOnly = true;
			// }

		}
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += " " + MertacorSubscribingPolicy.P_NUM + ":" + numOfSubscribees + " "
				+ MertacorSubscribingPolicy.P_FREEONLY + ":" + freeOnly;
		return s;
	}
}
