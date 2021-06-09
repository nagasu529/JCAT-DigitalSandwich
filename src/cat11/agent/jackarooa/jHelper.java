package cat11.agent.jackarooa;

import cat11.agent.jackarooa.util.MarketInfo;
import edu.cuny.cat.event.AuctionEvent;
import edu.cuny.cat.event.IdAssignedEvent;
import edu.cuny.cat.market.Auctioneer;
import edu.cuny.cat.market.Helper;
import edu.cuny.obj.Resetable;

public class jHelper extends Helper {

	protected int marketInfoWindow = 20;

	protected double meanPrice;

	public int startUseTraderInfo = 15;

	protected MarketInfo marketInfo;

	@Override
	public void reset() {
		super.reset();

		if ((marketInfo instanceof Resetable)) {
			marketInfo.reset();
		}

	}

	public int getMarketInfoWindow() {
		return marketInfoWindow;
	}

	public double getMeanPrice() {
		return meanPrice;
	}

	public void setMeanPrice(final double meanPrice) {
		this.meanPrice = meanPrice;
	}

	public int getStartUseTraderInfo() {
		return startUseTraderInfo;
	}

	public MarketInfo getMarketInfo() {
		return marketInfo;
	}

	public void setName(final String name) {
		if (marketInfo == null) {
			marketInfo = new MarketInfo(name, marketInfoWindow);
		}
	}

	@Override
	public void eventOccurred(final AuctionEvent event) {

		if (event instanceof IdAssignedEvent) {
			setName(((IdAssignedEvent) event).getId());
		}

		if (marketInfo != null) {
			marketInfo.eventOccurred(event);
		}

	}

	public static jHelper getHelper(final Auctioneer auctioneer) {
		jHelper helper = auctioneer.getHelper(jHelper.class);
		if (helper == null) {
			helper = new jHelper();
			helper.initialize();
			auctioneer.setHelper(jHelper.class, helper);
		}

		return helper;
	}

	@Override
	public String toString() {
		final String s = super.toString();
		return s;
	}

}
