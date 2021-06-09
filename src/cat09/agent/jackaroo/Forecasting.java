/* 
 * jackaroo in CAT'09
 */

package cat09.agent.jackaroo;

import java.util.Arrays;

import edu.cuny.obj.Resetable;

/**
 * TODO:
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class Forecasting implements Resetable {

	public Forecasting(final int length, final int nums, final double alpha,
			final boolean isDouble) {
		this.length = length;
		this.alpha = alpha;
		this.isDouble = isDouble;
		if (isDouble) {
			itemsd = new double[nums][length];
		} else {
			itemsi = new int[nums][length];
		}
	}

	public void initialize() {
		final int nums = isDouble ? itemsd.length : itemsi.length;
		for (int i = 0; i < nums; i++) {
			for (int j = 0; j < length; j++) {
				if (isDouble) {
					itemsd[i][j] = 0.0;
				} else {
					itemsi[i][j] = 0;
				}
			}
		}
	}

	@Override
	public void reset() {
		initialize();
	}

	public void addItem(final int index, final int value) {
		for (int i = 1; i < length; i++) {
			itemsi[index][i - 1] = itemsi[index][i];
		}

		itemsi[index][length - 1] = value;
	}

	public void addItem(final int index, final double value) {
		for (int i = 1; i < length; i++) {
			itemsd[index][i - 1] = itemsd[index][i];
		}

		itemsd[index][length - 1] = value;
	}

	public void display(final int index) {
		if (isDouble) {
			for (int i = 0; i < length; i++) {
				System.out.println(" " + itemsd[index][i]);
			}

		} else {
			for (int i = 0; i < length; i++) {
				System.out.println(" " + itemsi[index][i]);
			}

		}
	}

	public double mean(final int index) {
		double total = 0.0D;
		for (int i = 0; i < length; i++) {
			if (isDouble) {
				total += itemsd[index][i];
			} else {
				total += itemsi[index][i];
			}
		}

		return total / length;
	}

	public double nonZeroMean(final int index) {
		double total = 0.0D;
		double nonZeroCnt = 0.0D;
		for (int i = 0; i < length; i++) {
			if (isDouble) {
				if (itemsd[index][i] > 0.0D) {
					total += itemsd[index][i];
					nonZeroCnt++;
				}
				continue;
			}
			if (itemsi[index][i] > 0) {
				total += itemsi[index][i];
				nonZeroCnt++;
			}
		}

		if (nonZeroCnt > 0.0D) {
			return total / nonZeroCnt;
		} else {
			return 0.0D;
		}
	}

	public double ngmax(final int index) {
		double limit = 0.0D;
		for (int i = 0; i < length; i++) {
			if (isDouble) {
				if (itemsd[index][i] > limit) {
					limit = itemsd[index][i];
				}
				continue;
			}
			if (itemsi[index][i] > limit) {
				limit = itemsi[index][i];
			}
		}

		return limit;
	}

	public double lowestExceptZero(final int index) {
		double min = 0.0D;
		for (int i = 0; i < length; i++) {
			if (isDouble) {
				if ((min == 0.0D) && (itemsd[index][i] != 0.0D)) {
					min = itemsd[index][i];
				}
				if ((itemsd[index][i] != 0.0D) && (itemsd[index][i] < min)) {
					min = itemsd[index][i];
				}
				continue;
			}
			if ((min == 0.0D) && (itemsi[index][i] != 0)) {
				min = itemsi[index][i];
			}
			if ((itemsi[index][i] != 0) && (itemsi[index][i] < min)) {
				min = itemsi[index][i];
			}
		}

		return min;
	}

	public double lowest(final int index, final int segment) {
		double mind = 0.0D;
		double mini = 0.0D;
		final int l = Math.min(length, segment);
		if (isDouble) {
			mind = itemsd[index][0];
		} else {
			mini = itemsi[index][0];
		}
		for (int i = 0; i < l; i++) {
			if (isDouble) {
				if (itemsd[index][i] < mind) {
					mind = itemsd[index][i];
				}
				continue;
			}
			if (itemsi[index][i] < mini) {
				mini = itemsi[index][i];
			}
		}

		if (isDouble) {
			return mind;
		} else {
			return mini;
		}
	}

	public double sum(final int index) {
		double total = 0.0D;
		for (int i = 0; i < length; i++) {
			if (isDouble) {
				total += itemsd[index][i];
			} else {
				total += itemsi[index][i];
			}
		}

		return total;
	}

	public double lastValue(final int index) {
		if (isDouble) {
			return itemsd[index][length - 1];
		} else {
			return itemsi[index][length - 1];
		}
	}

	public double firstValue(final int index) {
		if (isDouble) {
			return itemsd[index][0];
		} else {
			return itemsi[index][0];
		}
	}

	public double lastValue(final int index, final int t) {
		int p = length + t;
		if (p < 0) {
			p = 0;
		}
		if (p > length - 1) {
			p = length - 1;
		}
		if (isDouble) {
			return itemsd[index][p];
		} else {
			return itemsi[index][p];
		}
	}

	public double predictWithPLS(final int index, final int t) {
		LSEline(index);
		return b0 + b1 * ((length + t) - 1);
	}

	public double decline(final int index) {
		LSEline(index);
		return b1;
	}

	private void LSEline(final int index) {
		final double mx = length / 2D;
		final double my = mean(index);
		double ssx = 0.0D;
		for (int i = 0; i < length; i++) {
			ssx += (i - mx) * (i - mx);
		}

		double ssxy = 0.0D;
		for (int i = 0; i < length; i++) {
			if (isDouble) {
				ssxy += (i - mx) * (itemsd[index][i] - my);
			} else {
				ssxy += (i - mx) * (itemsi[index][i] - my);
			}
		}

		b1 = ssxy / ssx;
		b0 = my - b1 * mx;
	}

	public double derivation(final int index) {
		final double mx = mean(index);
		double ssx = 0.0D;
		for (int i = 0; i < length; i++) {
			ssx += (i - mx) * (i - mx);
		}

		return Math.sqrt(ssx) / length;
	}

	public int secondOrderESF(final int index, final int t) {
		final double s[][] = new double[3][length];
		if (isDouble) {
			s[0][0] = itemsd[index][0];
		} else {
			s[0][0] = itemsi[index][0];
		}
		for (int i = 1; i < length; i++) {
			if (isDouble) {
				s[0][i] = alpha * itemsd[index][i] + (1.0D - alpha) * s[0][i - 1];
			} else {
				s[0][i] = alpha * itemsi[index][i] + (1.0D - alpha) * s[0][i - 1];
			}
		}

		for (int i = 1; i < length; i++) {
			s[1][i] = alpha * s[0][i] + (1.0D - alpha) * s[1][i - 1];
		}

		final int l = length - 1;
		final double predict = (2D + (alpha * t) / (1.0D - alpha)) * s[0][l]
				- (1.0D + (alpha * t) / (1.0D - alpha)) * s[1][l];
		return (int) predict;
	}

	public double medium(final int index) {
		Arrays.sort(itemsd);
		return itemsd[index][length / 2];
	}

	public int thirdOrderESF(final int index, final int t) {
		final double s[][] = new double[3][length];
		if (isDouble) {
			s[0][0] = itemsd[index][0];
		} else {
			s[0][0] = itemsd[index][0];
		}
		for (int i = 1; i < length; i++) {
			if (isDouble) {
				s[0][i] = alpha * itemsd[index][i] + (1.0D - alpha) * s[0][i - 1];
			} else {
				s[0][i] = alpha * itemsi[index][i] + (1.0D - alpha) * s[0][i - 1];
			}
		}

		for (int i = 1; i < length; i++) {
			s[1][i] = alpha * s[0][i] + (1.0D - alpha) * s[1][i - 1];
		}

		for (int i = 1; i < length; i++) {
			s[2][i] = alpha * s[1][i] + (1.0D - alpha) * s[2][i - 1];
		}

		final int l = length - 1;
		final double predict = (((6D * (1.0D - alpha) * (1.0D - alpha)
				+ (6D - 5D * alpha) * alpha * t + alpha * alpha * t * t) * s[0][l])
				/ (2D * (1.0D - alpha) * (1.0D - alpha)) - ((6D * (1.0D - alpha)
				* (1.0D - alpha) + 2D * (5D - 4D * alpha) * alpha * t + 2D * alpha
				* alpha * t * t) * s[1][l])
				/ (2D * (1.0D - alpha) * (1.0D - alpha)))
				+ ((2D * (1.0D - alpha) * (1.0D - alpha) + (4D - 3D * alpha) * alpha
						* t + alpha * alpha * t * t) * s[2][l])
				/ (2D * (1.0D - alpha) * (1.0D - alpha));
		return (int) predict;
	}

	private final int length;

	private int itemsi[][];

	private double itemsd[][];

	private final double alpha;

	private double b0;

	private double b1;

	private final boolean isDouble;
}
