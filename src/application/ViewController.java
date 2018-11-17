package application;

import java.io.File;
import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ViewController {
	@FXML
	private Slider zoomSlider;

	@FXML
	ImageView imageView;

	@FXML
	Canvas overlayCanvas;

	@FXML
	Button filePicker;

	@FXML
	CheckBox drawGrid;

	@FXML
	CheckBox drawContour;

	private String imagePath = "sample.png";
	private Image image;
	private int imgWidth = 0;
	private int imgHeight = 0;
	private double zoom;
	private boolean showGrid = false;
	private boolean showContour = false;

	@FXML
	public void initialize() {
		this.zoom = 1;
		this.openImage(this.imagePath);
		this.zoomSlider.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				zoom = newValue.doubleValue();
				onZoomChange();
			}
		});
		this.drawGrid.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				showGrid = newValue;
				drawOverlay(showGrid, showContour);
			}
		});
		this.drawContour.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				showContour = newValue;
				drawOverlay(showGrid, showContour);
			}
		});
		this.filePicker.setOnMouseClicked(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				openFilePicker();
			}
		});
	}

	private void onZoomChange() {
		double zoomedWidth = Math.ceil(this.zoom * this.imgWidth);
		double zoomedHeight = Math.ceil(this.zoom * this.imgHeight);
		this.imageView.setFitWidth(zoomedWidth);
		this.imageView.setFitHeight(zoomedHeight);
		Image img = new Image(new File(this.imagePath).toURI().toString(), zoomedWidth, zoomedHeight, true, false);
		this.renderImage(img);
	}

	private void openFilePicker() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		Stage stage = (Stage) this.imageView.getScene().getWindow();
		File file = fileChooser.showOpenDialog(stage);
		if (file != null) {
			try {
				this.openImage(file.getCanonicalPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.zoom = 1;
		this.onZoomChange();
		this.zoomSlider.setValue(1.0);
	}

	private void renderImage(Image img) {
		this.imageView.setImage(img);
		this.drawOverlay(this.showGrid, this.showContour);
	}

	private void drawOverlay(boolean showGrid, boolean showContour) {
		double zoomedWidth = Math.ceil(zoom * this.imgWidth);
		double zoomedHeight = Math.ceil(zoom * this.imgHeight);
		this.overlayCanvas.setWidth(zoomedWidth);
		this.overlayCanvas.setHeight(zoomedHeight);
		GraphicsContext gc = this.overlayCanvas.getGraphicsContext2D();
		gc.clearRect(0, 0, zoomedWidth, zoomedHeight);
		if (showGrid) {
			this.drawGrid(gc, zoomedWidth, zoomedHeight);
		}
	}
	
	private void drawGrid(GraphicsContext gc, double zoomedWidth, double zoomedHeight) {
		gc.setStroke(Color.RED);
		gc.setLineWidth(1);
		double gridPixelDistance = this.zoom > 10 ? 1 : 16;
		double gridSpacing = this.zoom * gridPixelDistance;
		for (double y = 0; y <= zoomedHeight; y += gridSpacing) {
			gc.strokeLine(0, y, zoomedWidth, y);
		}
		for (double x = 0; x <= zoomedWidth; x += gridSpacing) {
			gc.strokeLine(x, 0, x, zoomedHeight);
		}
	}

	private void openImage(String path) {
		this.imagePath = path;
		this.image = new Image(new File(this.imagePath).toURI().toString());
		this.imgWidth = (int) this.image.getWidth();
		this.imgHeight = (int) this.image.getHeight();
		this.renderImage(this.image);
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
