/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo.util;

import java.util.HashMap;

/**
 * Not used !
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class Data {
	public static HashMap<Integer, Double> expectingMatchingBids;

	public static HashMap<Integer, Double> expectingMatchingAsks;

	public static HashMap<Integer, Integer> numberMatchingBids;

	public static HashMap<Integer, Integer> numberMatchingAsks;

	public static int dayNumber = 0;

	public static final double STEP_FOR_CHANGE = 0.02D;

	public static final double DECREASING_INIT_EFFECT = 0.75D;

	public static final double INITIAL_PROB = 1.0D;

	public static final int DETAILS_FACTOR = 1;

	public static final int MIN_PRICE = 50;

	public static final int MAX_PRICE = 150;

	public static final int LONG_TERM_DAYS = 15;

	public static final int SHORT_TERM_DAYS = 5;

	public static final double MAX_INFLUENCE_PRICE = 0.66D;
}