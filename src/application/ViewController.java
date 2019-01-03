package application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	@FXML
	CheckBox drawPolygon;

	private String imagePath = "klein.png";
	private RasterImage image;
	private double zoom;
	private boolean showGrid = false;
	private boolean showContour = false;
	private boolean showPolygon = false;
	private List<Contour> contours;
	private int[][] straightPaths;
	private int[][] possibleSegments;
	private int[][] polygons;

	@FXML
	public void initialize() {
		this.zoom = 1;
		this.openImage(this.imagePath);
		this.contours = this.potrace();
		this.preparePolygons();
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
				drawOverlay(showGrid, showContour, showPolygon);
			}
		});
		this.drawContour.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				showContour = newValue;
				drawOverlay(showGrid, showContour, showPolygon);
			}
		});
		this.drawPolygon.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				showPolygon = newValue;
				drawOverlay(showGrid, showContour, showPolygon);
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
		this.preparePolygons();
		this.renderImage(this.image.image);
	}

	private void renderImage(Image img) {
		this.imageView.setImage(img);
		this.drawOverlay(this.showGrid, this.showContour, this.showPolygon);
	}

	private void drawOverlay(boolean showGrid, boolean showContour, boolean showPolygon) {
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
		if (showPolygon) {
			this.drawPolygons(gc, this.zoom);
		}
	}

	private void preparePolygons() {
		this.straightPaths = PolygonUtil.getStraightPaths(this.contours, this.image.width, this.image.height);
		int[] countourLengths = new int[this.contours.size()];
		for (int i = 0; i < contours.size(); i++) {
			countourLengths[i] = contours.get(i).path.length;
		}

		this.possibleSegments = PolygonUtil.straightPathsToPossibleSegments(this.straightPaths, countourLengths);
		List<int[]> polys = new ArrayList<>();
		for (int i = 0; i < this.possibleSegments.length; i++) {
			polys.add(PolygonUtil.getDrawablePolygons(this.possibleSegments[i], countourLengths[i], 0));
		}
		this.polygons = new int[polys.size()][];
		for (int i = 0; i < polys.size(); i++) {
			this.polygons[i] = polys.get(i);
		}
	}

	private void drawPolygons(GraphicsContext gc, double zoom) {
		int lineWidth = (int) Math.max(1, zoom / 2);
		int circleSize = lineWidth;
		gc.setLineWidth(lineWidth);
		gc.setStroke(Color.PURPLE);
		for (int i = 0; i < this.polygons.length; i++) {
			int[] contour = this.contours.get(i).path;
			int[] polygon = this.polygons[i];
			int from = contour[polygon[0]];
			int to = contour[polygon[1]];
			int fromX = from % this.image.width;
			int fromY = from / this.image.width;
			int toX = to % this.image.width;
			int toY = to / this.image.width;
			gc.strokeLine(fromX * zoom, fromY * zoom, toX * zoom, toY * zoom);
			gc.strokeOval(fromX * zoom - circleSize / 2, fromY * zoom - circleSize / 2, circleSize, circleSize);
			for (int j = 2; j < polygon.length; j++) {
				from = to;
				to = contour[polygon[j]];
				fromX = from % this.image.width;
				fromY = from / this.image.width;
				toX = to % this.image.width;
				toY = to / this.image.width;
				gc.strokeLine(fromX * zoom, fromY * zoom, toX * zoom, toY * zoom);
				gc.strokeOval(fromX * zoom - circleSize / 2, fromY * zoom - circleSize / 2, circleSize, circleSize);
			}
		}
	}

	private void drawContours(GraphicsContext gc, double zoom) {
		int lineWidth = (int) Math.max(1, zoom / 2);
		gc.setLineWidth(lineWidth);
		for (int i = 0; i < this.contours.size(); i++) {
			Contour contour = this.contours.get(i);
			gc.setStroke(contour.isOuter ? Color.RED : Color.ORANGE);
			int from = contour.path[0];
			int to = contour.path[1];
			int fromX = from % this.image.width;
			int fromY = from / this.image.width;
			int toX = to % this.image.width;
			int toY = to / this.image.width;
			gc.strokeLine(fromX * zoom, fromY * zoom, toX * zoom, toY * zoom);
			for (int j = 2; j <= contour.path.length; j++) {
				from = to;
				fromX = toX;
				fromY = toY;
				to = contour.path[j % contour.path.length];
				toX = to % this.image.width;
				toY = to / this.image.width;
				gc.strokeLine(fromX * zoom, fromY * zoom, toX * zoom, toY * zoom);
			}
		}
	}

	private int[] invertContouredArea(int[] pixels, int width, int height, List<Integer> contour) {
		int contourLength = contour.size();
		int from = contour.get(0);
		int to = contour.get(1);
		int fromX = from % width;
		int fromY = from / width;
		int toX = to % width;
		int toY = to / width;
		pixels = Util.invertRemainingLine(pixels, width, height, fromX, fromY);
		for (int j = 2; j <= contourLength; j++) {
			from = to;
			fromX = toX;
			fromY = toY;
			to = contour.get(j % contourLength);
			toX = to % width;
			toY = to / width;
			if (toY == fromY + 1) {
				// going down
				pixels = Util.invertRemainingLine(pixels, width, height, fromX, fromY);
			} else if (toY == fromY - 1) {
				// going up
				pixels = Util.invertRemainingLine(pixels, width, height, toX, toY);
			}
		}
		return pixels;
	}

	private void drawGrid(GraphicsContext gc, double zoomedWidth, double zoomedHeight) {
		gc.setStroke(Color.BLACK);
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

	private List<Contour> potrace() {
		List<Contour> contours = new ArrayList<>();
		int[] pixels = Arrays.copyOf(this.image.argb, this.image.argb.length);
		boolean inDoubtGoRight = true;
		for (int y = 0; y < this.image.height; y++) {
			for (int x = 0; x < this.image.width; x++) {
				int pos = y * this.image.width + x;
				if (Util.getGrayValue(pixels[pos]) == 0) {
					List<Integer> contourPath = new ArrayList<>();
					contourPath.add(pos);
					int direction = 180;
					int destination = this.takeStep(this.image, x, y, direction);
					contourPath.add(destination);
					while (true) {
						int oldX = destination % this.image.width;
						int oldY = destination / this.image.width;
						direction = this.pickDirection(pixels, this.image.width, this.image.height, oldX, oldY,
								direction, inDoubtGoRight);
						destination = this.takeStep(this.image, oldX, oldY, direction);
						if (destination == pos) {
							break;
						}
						contourPath.add(destination);
					}
					boolean isOuter = Util.getGrayValue(this.image.argb[pos]) == 0;
					Contour contour = new Contour(contourPath.stream().mapToInt(i -> i).toArray(), isOuter);
					contours.add(contour);
					pixels = this.invertContouredArea(pixels, this.image.width, this.image.height, contourPath);
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

	private int pickDirection(int[] pixels, int width, int height, int x, int y, int previousDirection,
			boolean inDoubtGoRight) {
		int[] neighbors = new int[] { Util.getValue(pixels, width, height, x - 1, y - 1),
				Util.getValue(pixels, width, height, x, y - 1), Util.getValue(pixels, width, height, x - 1, y),
				Util.getValue(pixels, width, height, x, y) };
		int[] rotatedNeighbors = Util.rotate2by2matrix(neighbors, previousDirection);
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
}
