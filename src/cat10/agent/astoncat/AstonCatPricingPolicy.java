/*
 * AstonCat-Plus from CAT'10
 */

package cat10.agent.astoncat;

import java.io.Serializable;

import org.apache.log4j.Logger;

import edu.cuny.cat.core.Shout;
import edu.cuny.cat.market.MarketQuote;
import edu.cuny.cat.market.pricing.KPricingPolicy;

/**
 * 
 * @author AstonCat Team
 * @version $Revision: 1.88 $
 * 
 */

public class AstonCatPricingPolicy extends KPricingPolicy implements
		Serializable {
	private static final long serialVersionUID = 1L;

	static Logger logger = Logger.getLogger(AstonCatPricingPolicy.class);

	protected EquilibriumEstimator equEstimator;

	public AstonCatPricingPolicy() {
	}

	public AstonCatPricingPolicy(double k) {
		super(k);
	}

	@Override
	public void initialize() {
		equEstimator = EquilibriumEstimator.getHelper(getAuctioneer());
	}

	protected double price(double quote, Shout shout) {
		if ((Double.isNaN(quote)) || (Double.isInfinite(quote))) {
			logger.debug("The value of a market quote of "
					+ this.auctioneer.getName()
					+ " do not produce valid transaction prices !");
			logger.debug("The price of " + shout.toString()
					+ " is used instead to calculate the transaction price.");

			return shout.getPrice();
		}
		return quote;
	}

	@Override
	public double determineClearingPrice(Shout bid, Shout ask,
			MarketQuote clearingQuote) {
		double price = equEstimator.getEquilibriumPrice();
		if (Double.isNaN(price)) {
			double askQuote = price(clearingQuote.getAsk(), ask);
			double bidQuote = price(clearingQuote.getBid(), bid);
			price = kInterval(askQuote, bidQuote);
		} else {
			if (price > bid.getPrice())
				price = bid.getPrice();
			else if (price < ask.getPrice()) {
				price = ask.getPrice();
			}
		}

		return price;
	}
}
