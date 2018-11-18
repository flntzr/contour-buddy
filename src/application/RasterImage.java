package application;

import java.io.File;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;

public class RasterImage {
	public int[] argb;
	public int width;
	public int height;
	public Image image;

	public RasterImage(String path) {
		this.image = new Image(new File(path).toURI().toString());
		this.width = (int) this.image.getWidth();
		this.height = (int) this.image.getHeight();
		this.argb = this.getArgbPixels();
	}

	private int[] getArgbPixels() {
		int[] pixels = new int[this.width * this.height];
		this.image.getPixelReader().getPixels(0, 0, this.width, this.height, PixelFormat.getIntArgbInstance(), pixels,
				0, this.width);
		return pixels;
	}
}
