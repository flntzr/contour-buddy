package application;

import java.util.ArrayList;
import java.util.List;

public class PolygonUtil {

	public static int[] c0;
	public static int[] c1;

	public static int[][] getDrawableContoursFromZero(int[][] possibleSegments, int[] contourLengths) {
		int[][] result = new int[possibleSegments.length][];
		for (int i = 0; i < possibleSegments.length; i++) {
			List<Integer> paths = new ArrayList<>();
			paths.add(0);
			int next = possibleSegments[i][0] % contourLengths[i];
			paths.add(next % contourLengths[i]);
			while (next < possibleSegments[i].length) {
				next = possibleSegments[i][next];
				paths.add(next);
			}
			// correct the last entry to close with point 0
			paths.set(paths.size() - 1, 0);
			result[i] = paths.stream().mapToInt(k -> k).toArray();
		}
		return result;
	}

	public static int[][] straightPathsToPossibleSegments(int[][] straightPaths, int[] contourLengths) {
		int lower = 1;
		int[][] possibleSegments = new int[straightPaths.length][];
		for (int i = 0; i < straightPaths.length; i++) {
			possibleSegments[i] = new int[straightPaths[i].length];
			for (int j = 0; j < straightPaths[i].length; j++) {
				if((straightPaths[i][j] - 1) - j <= contourLengths[i] - 3) {
					possibleSegments[i][(j + 1) % straightPaths[i].length] = straightPaths[i][j] - 1;
				}
				else {
					while((straightPaths[i][j] - lower) - j > contourLengths[i] - 3) {
						lower++;
					}
					possibleSegments[i][(j) % straightPaths[i].length] = straightPaths[i][j] - lower;
					lower = 1;
				}
			}
		}
		return possibleSegments;
	}

	public static int[][] getStraightPaths(List<Contour> contours, int width, int height) {
		int[][] result = new int[contours.size()][];
		for (int i = 0; i < contours.size(); i++) {
			result[i] = getStraightPaths(contours.get(i).path, width, height);
		}
		return result;
	}

	private static int[] getStraightPaths(int[] path, int width, int height) {
		int[] result = new int[path.length];
		for (int i = 0; i < path.length; i++) {
			// i is the starting point of the straight path
			int[][] directions = new int[4][2];
			c0 = new int[] { 0, 0 };
			c1 = new int[] { 0, 0 };
			int endIndex = 0;
			int iX = path[i] % width;
			int iY = path[i] / width;
			for (int k = i + 1; k < i + path.length; k++) {
				// k marks the (current) end point of the path
				if (countDirections(directions) > 3) {
					break;
				}
				int boundedK = k % path.length;	
				int kX = path[boundedK] % width;
				int kY = path[boundedK] / width;
				int[] v = { kX - iX, kY - iY };
				if (!respectsConstrains(v)) {
					break;
				}
				updateConstraints(v);
				endIndex = k;
			}
			result[i] = endIndex;
		}
		return result;
	}

	private static boolean respectsConstrains(int[] v) {
		return Util.crossProduct(c0, v) >= 0 && Util.crossProduct(c1, v) <= 0;
	}

	private static void updateConstraints(int[] v) {
		if (Math.abs(v[0]) <= 1 && Math.abs(v[1]) <= 1) {
			return;
		}
		updateConstraint0(v);
		updateConstraint1(v);
	}

	private static void updateConstraint0(int[] v) {
		int dX = 0;
		int dY = 0;
		if (v[1] >= 0 && (v[1] > 0 || v[0] < 0)) {
			dX = v[0] + 1;
		} else {
			dX = v[0] - 1;
		}

		if (v[0] <= 0 && (v[0] < 0 || v[1] < 0)) {
			dY = v[1] + 1;
		} else {
			dY = v[1] - 1;
		}

		int[] d = new int[] { dX, dY };
		if (Util.crossProduct(c0, d) >= 0) {
			c0 = d;
		}
	}

	private static void updateConstraint1(int[] v) {
		int dX = 0;
		int dY = 0;
		if (v[1] <= 0 && (v[1] < 0 || v[0] < 0)) {
			dX = v[0] + 1;
		} else {
			dX = v[0] - 1;
		}

		if (v[0] >= 0 && (v[0] > 0 || v[1] < 0)) {
			dY = v[1] + 1;
		} else {
			dY = v[1] - 1;
		}

		int[] d = new int[] { dX, dY };
		if (Util.crossProduct(c1, d) <= 0) {
			c1 = d;
		}
	}

	private static int countDirections(int[][] directions) {
		for (int i = 0; i < directions.length; i++) {
			if (directions[i][0] == 0 && directions[i][1] == 0) {
				return i;
			}
		}
		return 4;
	}
}
