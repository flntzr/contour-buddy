package application;

public class Contour {
	public int[] path;
	public boolean isOuter;

	public Contour(int[] path, boolean outer) {
		this.path = path;
		this.isOuter = outer;
	}
}
