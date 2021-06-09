/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * 
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class AscendingShoutPlusTraderInfoComparator implements
		Comparator<ShoutPlusTraderInfo>, Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public int compare(final ShoutPlusTraderInfo shoutTraderInfo1,
			final ShoutPlusTraderInfo shoutTraderInfo2) {
		return shoutTraderInfo1.compareTo(shoutTraderInfo2);
	}
}