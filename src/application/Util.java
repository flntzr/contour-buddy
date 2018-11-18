package application;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Util {
	public enum Angle {
		DEG0, DEG90, DEG180, DEG270
	}

	public static final int[] COLOR_PALETTE = { 0xffe6194b, 0xff3cb44b, 0xffffe119, 0xff4363d8, 0xfff58231, 0xff911eb4,
			0xff46f0f0, 0xfff032e6, 0xffbcf60c, 0xfffabebe, 0xff008080, 0xffe6beff, 0xff9a6324, 0xfffffac8, 0xff800000,
			0xffaaffc3, 0xff808000, 0xffffd8b1, 0xff000075, 0xff808080, 0xff000000 };

	public static int toArgb(int r, int g, int b) {
		return 0xff000000 | r << 16 | g << 8 | b;
	}

	public static int getGrayValue(int argb) {
		return argb & 0xFF;
	}

	public static int getCenter(int[] weights, boolean isLeftSide) {
		int sum = 0;
		for (int pixel : weights) {
			sum += pixel;
		}
		int half = sum / 2;
		int counter = 0;
		for (int i = 0; i < weights.length; i++) {
			counter += weights[i];
			if (counter > half) {
				return i;
			}
		}
		return isLeftSide ? weights.length - 1 : 0;
	}

	public static int[] getNeighbors(final float radius, final RasterImage image, final int posX, final int posY) {
		List<Integer> neighbors = new ArrayList<>();
		for (int y = Math.round(posY - radius); y <= posY + radius; y++) {
			for (int x = Math.round(posX - radius); x <= posX + radius; x++) {
				double distance = Math.sqrt(Math.pow(y - posY, 2) + Math.pow(x - posX, 2));
				if (distance > radius) {
					continue;
				}
				neighbors.add(Util.getValue(image, x, y));
			}
		}
		return neighbors.stream().mapToInt(i -> i).toArray();
	}

	public static boolean isWithinBoundaries(final RasterImage image, final int x, final int y) {
		return !(x < 0 || x >= image.width || y < 0 || y >= image.height);
	}

	public static int getValue(final RasterImage image, final int x, final int y) {
		if (!Util.isWithinBoundaries(image, x, y)) {
			return 0xffffffff;
		}
		return image.argb[y * image.width + x];
	}

	public static int invertBinary(final int pixel) {
		return pixel == 0xffffffff ? 0xff000000 : 0xffffffff;
	}

	/**
	 * Clockwise rotation of a 2*2 matrix.
	 * 
	 * @param matrix
	 * @param angle
	 * @return
	 */
	public static int[] rotate2by2matrix(int[] matrix, Angle angle) {
		switch (angle) {
		case DEG90:
			return new int[] { matrix[1], matrix[3], matrix[0], matrix[2] };
		case DEG180:
			return new int[] { matrix[3], matrix[2], matrix[1], matrix[0] };
		case DEG270:
			return new int[] { matrix[2], matrix[0], matrix[3], matrix[1] };
		default:
			return matrix;
		}
	}

	public static int[] toOneZeroImage(final RasterImage image) {
		int[] result = new int[image.height * image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				int pos = y * image.width + x;
				result[pos] = Util.getGrayValue(image.argb[pos]) == 255 ? 0 : 1;
			}
		}
		return result;
	}

	public static int toIndex(final RasterImage image, final int x, final int y) {
		return y * image.width + x;
	}

	public static int[] numberArrayToColors(final RasterImage image) {
		int[] result = new int[image.height * image.width];
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				int pos = y * image.width + x;
				if (image.argb[pos] > 0) {
					result[pos] = Util.COLOR_PALETTE[image.argb[pos] % Util.COLOR_PALETTE.length];
				} else {
					result[pos] = 0xffffffff;
				}
			}
		}
		return result;
	}

	public static int[] get4PreviousNeighbors(RasterImage image, int x, int y) {
		int[] neighbors = { getValue(image, x - 1, y - 1), getValue(image, x, y - 1), getValue(image, x + 1, y - 1),
				getValue(image, x - 1, y) };
		return neighbors;
	}

	public static Set<Integer> getSubSet(Set<Set<Integer>> sets, Integer value) {
		for (Set<Integer> s : sets) {
			if (s.contains(value)) {
				return s;
			}
		}
		throw new RuntimeException();
	}

	public static Integer getLowestValue(Set<Integer> set) {
		int min = Integer.MAX_VALUE;
		for (Integer i : set) {
			if (i < min) {
				min = i;
			}
		}
		return min;
	}
}
