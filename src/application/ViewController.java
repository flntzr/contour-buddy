package application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import application.Util.Angle;
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

	private String imagePath = "klein.png";
	private RasterImage image;
	private double zoom;
	private boolean showGrid = false;
	private boolean showContour = false;
	private List<List<Integer>> contours;

	@FXML
	public void initialize() {
		this.zoom = 1;
		this.openImage(this.imagePath);
		this.contours = this.potrace();
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
		double zoomedWidth = Math.ceil(this.zoom * this.image.width);
		double zoomedHeight = Math.ceil(this.zoom * this.image.width);
		this.imageView.setFitWidth(zoomedWidth);
		this.imageView.setFitHeight(zoomedHeight);
		Image image = new Image(new File(this.imagePath).toURI().toString(), zoomedWidth, zoomedHeight, true, false);
		this.renderImage(image);
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
		this.contours = this.potrace();
	}

	private void renderImage(Image img) {
		this.imageView.setImage(img);
		this.drawOverlay(this.showGrid, this.showContour);
	}

	private void drawOverlay(boolean showGrid, boolean showContour) {
		double zoomedWidth = Math.ceil(zoom * this.image.width);
		double zoomedHeight = Math.ceil(zoom * this.image.height);
		this.overlayCanvas.setWidth(zoomedWidth);
		this.overlayCanvas.setHeight(zoomedHeight);
		GraphicsContext gc = this.overlayCanvas.getGraphicsContext2D();
		gc.clearRect(0, 0, zoomedWidth, zoomedHeight);
		if (showGrid) {
			this.drawGrid(gc, zoomedWidth, zoomedHeight);
		}
		if (showContour) {
			this.drawContours(gc, this.zoom);
		}
	}

	private void drawContours(GraphicsContext gc, double zoom) {
		gc.setLineWidth(3);
		gc.setStroke(Color.GREEN);
		for (int i = 0; i < this.contours.size(); i++) {
			List<Integer> contour = this.contours.get(i);
			int contourLength = contour.size();
			int from = contour.get(0);
			int to = contour.get(1);
			int fromX = from % this.image.width;
			int fromY = from / this.image.width;
			int toX = to % this.image.width;
			int toY = to / this.image.width;
			gc.strokeLine(fromX * zoom, fromY * zoom, toX * zoom, toY * zoom);
			for (int j = 2; j <= contourLength; j++) {
				from = to;
				fromX = toX;
				fromY = toY;
				to = contour.get(j % contourLength);
				toX = to % this.image.width;
				toY = to / this.image.width;
				gc.strokeLine(fromX * zoom, fromY * zoom, toX * zoom, toY * zoom);
			}
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
		this.image = new RasterImage(this.imagePath);
		this.renderImage(this.image.image);
	}

	private List<List<Integer>> potrace() {
		List<List<Integer>> contours = new ArrayList<>();
		int[] pixels = Arrays.copyOf(this.image.argb, this.image.argb.length);
		boolean inDoubtGoRight = true;
		for (int y = 0; y < this.image.height; y++) {
			for (int x = 0; x < this.image.width; x++) {
				int pos = y * this.image.width + x;
				if (Util.getGrayValue(pixels[pos]) == 0) {
					List<Integer> contour = new ArrayList<>();
					contour.add(pos);
					int direction = 180;
					int destination = this.takeStep(this.image, x, y, direction);
					contour.add(destination);
					while (true) {
						int oldX = destination % this.image.width;
						int oldY = destination / this.image.width;
						direction = this.pickDirection(this.image, oldX, oldY, direction, inDoubtGoRight);
						destination = this.takeStep(this.image, oldX, oldY, direction);
						if (destination == pos) {
							break;
						}
						contour.add(destination);
					}
					contours.add(contour);
					// TODO: invert rest of line here so we don't draw the same contour over and
					// over!
					// for now just return the first contour
					return contours;
				}
			}
		}
		return contours;
	}

	/**
	 * Returns the target position index after taking the step. Part of the potrace
	 * algorithm.
	 * 
	 * @param image
	 * @param x
	 * @param y
	 * @param direction in degrees, north is 0. Increases clockwise up to 359.
	 * @return
	 */
	private int takeStep(RasterImage image, int x, int y, int direction) {
		if (direction == 0) {
			y--;
		} else if (direction == 90) {
			x++;
		} else if (direction == 180) {
			y++;
		} else if (direction == 270) {
			x--;
		}
		return y * image.width + x;
	}

	private int pickDirection(RasterImage image, int x, int y, int previousDirection, boolean inDoubtGoRight) {
		int[] neighbors = new int[] { Util.getValue(image, x - 1, y - 1), Util.getValue(image, x, y - 1),
				Util.getValue(image, x - 1, y), Util.getValue(image, x, y) };
		Angle rotationAngle = Angle.DEG0;
		if (previousDirection == 90) {
			rotationAngle = Angle.DEG90;
		} else if (previousDirection == 180) {
			rotationAngle = Angle.DEG180;
		} else if (previousDirection == 270) {
			rotationAngle = Angle.DEG270;
		}
		int[] rotatedNeighbors = Util.rotate2by2matrix(neighbors, rotationAngle);
		boolean topLeftNeighborBlack = Util.getGrayValue(rotatedNeighbors[0]) == 0;
		boolean topRightNeighborBlack = Util.getGrayValue(rotatedNeighbors[1]) == 0;
		int turnAngle = 0;
		if (topLeftNeighborBlack && topRightNeighborBlack) {
			turnAngle = 90;
		} else if (topLeftNeighborBlack && !topRightNeighborBlack) {
			return previousDirection;
		} else if (!topLeftNeighborBlack && !topRightNeighborBlack) {
			turnAngle = -90;
		} else if (!topLeftNeighborBlack && topRightNeighborBlack) {
			turnAngle = inDoubtGoRight ? 90 : -90;
		} else {
			throw new RuntimeException("An invalid state has occured during direction picking.");
		}
		return (previousDirection + turnAngle + 360) % 360;
	}

//	private boolean setArgbPixels(int[] pixels) {
//		if (pixels.length != this.image.width * this.image.height) {
//			return false;
//		}
//		WritableImage writeImage = new WritableImage(this.image.width, this.image.height);
//		PixelWriter pw = writeImage.getPixelWriter();
//		pw.setPixels(0, 0, this.image.width, this.image.height, PixelFormat.getIntArgbInstance(), pixels, 0,
//				this.image.width);
//		return true;
//	}
}
