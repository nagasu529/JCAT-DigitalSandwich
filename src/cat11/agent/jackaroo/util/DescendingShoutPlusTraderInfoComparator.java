/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo.util;

import java.io.Serializable;
import java.util.Comparator;

public class DescendingShoutPlusTraderInfoComparator implements
		Comparator<ShoutPlusTraderInfo>, Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public int compare(final ShoutPlusTraderInfo shoutTraderInfo1,
			final ShoutPlusTraderInfo shoutTraderInfo2) {
		return shoutTraderInfo2.compareTo(shoutTraderInfo1);
	}
}