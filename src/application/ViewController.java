package application;

import java.io.File;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class ViewController {
	@FXML
	private Slider zoomSlider;

	@FXML
	ImageView imageView;

	private final String imagePath = "sample.png";
	private Image image;
	private int imgWidth = 0;
	private int imgHeight = 0;

	@FXML
	public void initialize() {
		zoomSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				double zoom = newValue.doubleValue();
				onZoomChange(zoom);
			}
		});
		this.openImage(this.imagePath);
	}

	private void onZoomChange(double zoom) {
		double zoomedWidth = Math.ceil(zoom * this.imgWidth);
		double zoomedHeight = Math.ceil(zoom * this.imgHeight);
		this.imageView.setFitWidth(zoomedWidth);
		this.imageView.setFitHeight(zoomedHeight);
		Image img = new Image(new File(this.imagePath).toURI().toString(), zoomedWidth, zoomedHeight, true, false);
		this.imageView.setImage(img);
//		this.drawOverlay();
	}

	private void openImage(String path) {
		this.image = new Image(new File(path).toURI().toString());
		this.imgWidth = (int) this.image.getWidth();
		this.imgHeight = (int) this.image.getHeight();
		this.imageView.setImage(this.image);
	}

	private int[] getArgbPixels() {
		int[] pixels = new int[this.imgWidth * this.imgHeight];
		image.getPixelReader().getPixels(0, 0, this.imgWidth, this.imgHeight, PixelFormat.getIntArgbInstance(), pixels,
				0, this.imgWidth);
		return pixels;
	}

	private boolean setArgbPixels(int[] pixels) {
		if (pixels.length != this.imgWidth * this.imgHeight) {
			return false;
		}
		WritableImage writeImage = new WritableImage(this.imgWidth, this.imgHeight);
		PixelWriter pw = writeImage.getPixelWriter();
		pw.setPixels(0, 0, this.imgWidth, this.imgHeight, PixelFormat.getIntArgbInstance(), pixels, 0, this.imgWidth);
		return true;
	}
}
