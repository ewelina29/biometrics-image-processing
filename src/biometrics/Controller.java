package biometrics;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.LookupTable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by boska - 02-17
 */

public class Controller implements Initializable {


    public TextField brightnessLevel;
    private double scale = 1;
    private double imageHeight;
    private double imageWidth;
    private int[] redTable;
    private int[] greenTable;
    private int[] blueTable;
    private int[] rgbTable;

    private int[][] mask;

    @FXML
    private ImageView imageView;
    @FXML
    private WritableImage image;
    @FXML
    private Slider scaleSlider;
    @FXML
    private Label redLabel;
    @FXML
    private Label greenLabel;
    @FXML
    private Label blueLabel;
    @FXML
    private TextField redTextField;
    @FXML
    private TextField greenTextField;
    @FXML
    private TextField blueTextField;
    @FXML
    private TextField aStretch;
    @FXML
    private TextField bStretch;
    @FXML
    private BarChart redHistogram;
    @FXML
    private BarChart greenHistogram;
    @FXML
    private BarChart blueHistogram;
    @FXML
    private BarChart rgbHistogram;
    @FXML
    private TextField binaryTextField;
    @FXML
    private TextField niblackWindowSizeText;
    @FXML
    private TextField niblackKValueText;
    private int maskSize = 3;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        handleScaleSlider();
        imageView.setOnMouseMoved(event -> showRGB(event.getX(), event.getY()));
        imageView.setOnMouseClicked(event -> changeRGB(event.getX(), event.getY()));


    }

    private void changeRGB(double x, double y) {
        if (imageView.getImage() != null) {
            WritableImage image = (WritableImage) imageView.getImage();
            if (isIntRGB(redTextField.getText()) && isIntRGB(greenTextField.getText()) && isIntRGB(blueTextField.getText())) {
                int red = Integer.parseInt(redTextField.getText());
                int green = Integer.parseInt(greenTextField.getText());
                int blue = Integer.parseInt(blueTextField.getText());

                image.getPixelWriter().setColor((int) (x / scale), (int) (y / scale), Color.rgb(red, green, blue));
            }
        }
    }

    private void showRGB(double x, double y) {
        if (imageView.getImage() != null) {
            int argb = imageView.getImage().getPixelReader().getArgb((int) (x / scale), (int) (y / scale));
            int red = (argb >> 16) & 0xFF;
            int green = (argb >> 8) & 0xFF;
            int blue = (argb) & 0xFF;
            redLabel.setText(Integer.toString(red));
            greenLabel.setText(Integer.toString(green));
            blueLabel.setText(Integer.toString(blue));


        }
    }

    public void handleLoadImageButton() {

        setImage(loadImage());
        updateHistograms();
    }

    public void handleSaveImageButton() {
        saveImage(imageView.getImage());
    }

    private void saveImage(Image image) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showSaveDialog(new Stage());
        fileChooser.setTitle("Save image");
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setHeaderText("File can't be saved");
            alert.showAndWait();
        }

    }

    private void handleScaleSlider() {
        scaleSlider.valueChangingProperty().addListener((observable, oldValue, newValue) -> {
            scale = scaleSlider.getValue();
            scaleImage();
        });
    }

    public WritableImage loadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.jpg", "*.png", "*.bmp"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("BMP", "*.bmp")
        );

        File file = fileChooser.showOpenDialog(new Stage());

        try {
            String imagePath = file.getPath();
            image = SwingFXUtils.toFXImage(ImageIO.read(new File(imagePath)), null);

            return image;

        } catch (NullPointerException e) {
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("ERROR");
            alert.setHeaderText("File can't be opened");
            alert.showAndWait();


        }

        return null;


    }

    public void handleBrightenImageButton() {
        changeDarkness(true);
        updateHistograms();
    }

    public void handleDarkenImageButton() {
        changeDarkness(false);
        updateHistograms();
    }

    public void handleStretchHistogramButton() {
        if (isIntRGB(aStretch.getText()) && isIntRGB(bStretch.getText())) {
            int a = Integer.parseInt(aStretch.getText());
            int b = Integer.parseInt(bStretch.getText());
            stretchHistogram(a, b);
            updateHistograms();
        }
    }

    public void handleEqualizeHistogramButton() {
        equalizeHistogram();
        updateHistograms();
    }

    private void equalizeHistogram() {
        double[] RGB_EqualizeTable = createLUTEqualizeHistogram(createDF(rgbTable));


        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();

        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {

                int[] rgbColor = analyzePixel(j, i);
                Color color = Color.grayRgb((int) RGB_EqualizeTable[rgbColor[3]]);
                writer.setColor(j, i, color);
            }
        }

        setImage(wi);


    }

    private double[] createLUTEqualizeHistogram(double[] df) {
        double firstNonZero = 0;
        for (double i : df) {
            if (i != 0) {
                firstNonZero = i;
                break;
            }
        }
        double LUT[] = new double[256];
        double pixel;
        for (int i = 0; i < 256; i++) {
            pixel = (df[i] - firstNonZero) / (1 - firstNonZero) * 255;
            if (pixel > 255)
                pixel = 255;
            else if (pixel < 0)
                pixel = 0;

            LUT[i] = pixel;

        }
        return LUT;


    }

    private double[] createDF(int[] table) {
        double[] dfTable = new double[256];
        double sum = imageHeight * imageWidth;
        double h = 0;
        for (int i = 0; i < 256; i++) {
            h += table[i];
            dfTable[i] = h / sum;
        }

        return dfTable;
    }

    private void stretchHistogram(int a, int b) {

        double[] stretchTable = createLUTStretchHistogram(a, b);

        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();

        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {

                int[] rgbColor = analyzePixel(j, i);
                Color color = Color.grayRgb((int) stretchTable[rgbColor[3]]);

                writer.setColor(j, i, color);
            }
        }

        setImage(wi);

    }

    private double[] createLUTStretchHistogram(int a, int b) {
        double LUT[] = new double[256];
        double pixel;

        for (int i = 0; i < rgbTable.length; i++) {
            if (i < a || i > b)
                rgbTable[i] = 0;
        }

        int min = getMinPixel();
        int max = getMaxPixel();

        for (int i = 0; i < 256; i++) {

            pixel = (255 * (i - min)) / (max - min);
            if (pixel > 255) {
                pixel = 255;
            } else if (pixel < 0) {
                pixel = 0;
            }
            LUT[i] = pixel;

        }
        return LUT;
    }

    private int getMaxPixel() {
        int max = 255;
        for (int i = 255; i > 0; i--) {
            if (rgbTable[i] != 0) {
                max = i;
                break;
            }
        }
        return max;
    }

    private int getMinPixel() {
        int min = 0;
        for (int i = 0; i < 256; i++) {
            if (rgbTable[i] != 0) {
                min = i;
                break;
            }
        }
        return min;
    }

    private void changeDarkness(boolean brighten) {
        double[] R_BrightTable = createLUTDarkenOrBrighten(brighten);
        double[] G_BrightTable = createLUTDarkenOrBrighten(brighten);
        double[] B_BrightTable = createLUTDarkenOrBrighten(brighten);
        double[] brightTable = createLUTDarkenOrBrighten(brighten);


        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();

        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {

                int[] rgbColor = analyzePixel(j, i);
                Color color = Color.grayRgb((int) brightTable[rgbColor[3]]);

                writer.setColor(j, i, color);
            }
        }

        setImage(wi);


    }

    private int[] analyzePixel(int x, int y) {
        int pixel = imageView.getImage().getPixelReader().getArgb(x, y);
        int[] rgbTable = new int[4];
        rgbTable[0] = (pixel >> 16) & 0xFF;
        rgbTable[1] = (pixel >> 8) & 0xFF;
        rgbTable[2] = (pixel) & 0xFF;
        rgbTable[3] = (rgbTable[0] + rgbTable[1] + rgbTable[2]) / 3;

        return rgbTable;
    }

    private double[] createLUTDarkenOrBrighten(boolean brighten) {
        double LUT[] = new double[256];
        double pixel;
        for (int i = 0; i < 256; i++) {
            if (!brighten) {
                if (!this.brightnessLevel.getText().equals(""))
                    pixel = i - Integer.valueOf(this.brightnessLevel.getText());
                else
                    pixel = Math.pow(i, 2) / 255;
            } else {
                if (!this.brightnessLevel.getText().equals(""))
                    pixel = i + Integer.valueOf(this.brightnessLevel.getText());
                else
                    pixel = Math.log(i + 1) * 45;
            }
            if (pixel > 255)
                pixel = 255;
            else if (pixel < 0)
                pixel = 0;
            LUT[i] = pixel;
        }

        return LUT;
    }

    private void updateHistograms() {
        countPixels();
        redHistogram.getData().clear();
        greenHistogram.getData().clear();
        blueHistogram.getData().clear();
        rgbHistogram.getData().clear();

        redHistogram.setTitle("Red histogram");
        greenHistogram.setTitle("Green histogram");
        blueHistogram.setTitle("Blue histogram");
        rgbHistogram.setTitle("RGB histogram");

        XYChart.Series seriesRed = new XYChart.Series();
        XYChart.Series seriesGreen = new XYChart.Series();
        XYChart.Series seriesBlue = new XYChart.Series();
        XYChart.Series seriesRGB = new XYChart.Series();

        double divider = imageHeight * imageWidth;

        for (int i = 0; i < 256; i++) {
            seriesRed.getData().add(new XYChart.Data(String.valueOf(i), redTable[i] / divider));
            seriesGreen.getData().add(new XYChart.Data(String.valueOf(i), greenTable[i] / divider));
            seriesBlue.getData().add(new XYChart.Data(String.valueOf(i), blueTable[i] / divider));
            seriesRGB.getData().add(new XYChart.Data(String.valueOf(i), rgbTable[i] / divider));
        }

        redHistogram.getData().add(seriesRed);
        greenHistogram.getData().add(seriesGreen);
        blueHistogram.getData().add(seriesBlue);
        rgbHistogram.getData().add(seriesRGB);

        for (Node n : redHistogram.lookupAll(".default-color0.chart-bar")) {
            n.setStyle("-fx-bar-fill: red");
        }

        for (Node n : greenHistogram.lookupAll(".default-color0.chart-bar")) {
            n.setStyle("-fx-bar-fill: green");
        }
        for (Node n : blueHistogram.lookupAll(".default-color0.chart-bar")) {
            n.setStyle("-fx-bar-fill: blue");
        }
        for (Node n : rgbHistogram.lookupAll(".default-color0.chart-bar")) {
            n.setStyle("-fx-bar-fill: black");
        }

    }

    private void countPixels() {
        redTable = new int[256];
        greenTable = new int[256];
        blueTable = new int[256];
        rgbTable = new int[256];

        for (int i = 0; i < 256; i++) {
            redTable[i] = greenTable[i] = blueTable[i] = rgbTable[i] = 0;
        }
        for (int x = 0; x < imageView.getImage().getWidth(); x++) {
            for (int y = 0; y < imageView.getImage().getHeight(); y++) {
                int argb = imageView.getImage().getPixelReader().getArgb(x, y);
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = (argb) & 0xFF;

                redTable[red]++;
                greenTable[green]++;
                blueTable[blue]++;
                rgbTable[(red + green + blue) / 3]++;

            }
        }

    }

    private void setImage(Image image) {
        imageView.setImage(image);
        imageView.setFitWidth(image.getWidth());
        imageView.setFitHeight(image.getHeight());

        imageHeight = image.getHeight();
        imageWidth = image.getWidth();

    }

    private void scaleImage() {
        imageView.setFitHeight(imageHeight * scale);
        imageView.setFitWidth(imageWidth * scale);
    }

    private boolean isIntRGB(String data) {
        try {
            if (Integer.parseInt(data) >= 0 && Integer.parseInt(data) <= 255) ;
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void handleBinarizationButton() {
        if (isIntRGB(binaryTextField.getText())) {

            int threshold = (Integer.parseInt(binaryTextField.getText()));
            createBinaryImage(threshold);
        }
        updateHistograms();
    }

    private void createBinaryImage(int threshold) {

        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();
        Color color;

        for (int i = 0; i < imageWidth; i++) {
            for (int j = 0; j < imageHeight; j++) {
                int[] rgbColor = analyzePixel(i, j);
                if (rgbColor[3] <= threshold)
                    color = Color.BLACK;
                else
                    color = Color.WHITE;

                writer.setColor(i, j, color);
            }
        }

        setImage(wi);

    }

    public void handleOtsuButton() {
        otsuMethod();
        updateHistograms();
    }

    private void otsuMethod() {
        int total = (int) imageHeight * (int) imageWidth;
        float sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * rgbTable[i];
        }

        float sumB = 0;
        int wB = 0;
        int wF;

        float max = 0;

        int threshold = 0;
        for (int i = 0; i < 256; i++) {
            wB += rgbTable[i];
            if (wB == 0)
                continue;
            wF = total - wB;
            if (wF == 0)
                break;
            sumB += (float) i * rgbTable[i];
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            float between = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            if (between > max) {
                max = between;
                threshold = i;
            }
        }
        createBinaryImage(threshold);
    }

    public void handleNiblackButton() {
        niblackMethod();
        updateHistograms();
    }

    private void niblackMethod() {
        int[][] niblackThresoldTable = createNiblackTable();

        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();
        Color color;


        for (int i = 0; i < imageWidth; i++) {
            for (int j = 0; j < imageHeight; j++) {
                int[] rgbTable = analyzePixel(i, j);
                if (rgbTable[3] >= niblackThresoldTable[i][j])
                    color = Color.WHITE;
                else
                    color = Color.BLACK;
                writer.setColor(i, j, color);
            }
        }
        setImage(wi);


    }

    private int[][] createNiblackTable() {
        int[][] niblackThresholdTable = new int[(int) imageWidth][(int) imageHeight];
        for (int i = 0; i < imageWidth; i++) {
            for (int j = 0; j < imageHeight; j++) {
                niblackThresholdTable[i][j] = getNiblackThresold(i, j);
            }
        }
        return niblackThresholdTable;

    }

    private int getNiblackThresold(int x, int y) {
        int winSize = Integer.parseInt(niblackWindowSizeText.getText());
        float param = Float.parseFloat(niblackKValueText.getText());
        float sum = 0;
        float sumWar = 0;
        float avg, dev;
        if (winSize % 2 == 0)
            winSize++;
        int winXstart = x - ((winSize - 1) / 2);
        int winYstart = y - ((winSize - 1) / 2);
        for (int i = 0; i < winSize; i++) {
            for (int j = 0; j < winSize; j++) {
                if (winXstart >= 0 && winYstart >= 0 && (winXstart + i + 1) < imageWidth && (winYstart + j + 1) < imageHeight) {
                    int[] rgbTable = analyzePixel(winXstart + i, winYstart + j);
                    sum += rgbTable[3];
                }

            }
        }
        avg = sum / (winSize * winSize);

        for (int i = 0; i < winSize; i++) {
            for (int j = 0; j < winSize; j++) {
                if (winXstart >= 0 && winYstart >= 0 && (winXstart + i + 1) < imageWidth && (winYstart + j + 1) < imageHeight) {
                    int[] rgbTable = analyzePixel(winXstart + i, winYstart + j);

                    sumWar += (rgbTable[3] - avg) * (rgbTable[3] - avg);
                }
            }
        }
        dev = (float) Math.sqrt(sumWar / avg);

        return (int) (avg + (param * dev));
    }

    public void customFilterButtonHandler() throws IOException, InterruptedException {
        FiltersController filtersWindow = new FiltersController();
        filtersWindow.displayTransformationWindow(this, 4);

        int size = filtersWindow.getParameter();
        int[][] mask = new int[size][size];
        FiltersController filtersWindow2 = new FiltersController();
        filtersWindow2.display(this, mask, size);


        this.mask = filtersWindow2.getMask();
        this.maskSize = size;

        applyLinearFilter();
        updateHistograms();

    }

    public void lowPassFilterButtonHandler() {
        mask = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mask[i][j] = 1;
            }

        }
        applyLinearFilter();
        updateHistograms();
    }

    public void prewittFilterButtonHandler() {
        mask = new int[3][3];

        for (int i = 0; i < 3; i++) {
            mask[i][0] = -1;
            mask[i][1] = 0;
            mask[i][2] = 1;
        }

        applyLinearFilter();
        updateHistograms();
    }

    public void highPassFilterButtonHandler() {
        mask = new int[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mask[i][j] = -1;
            }
        }
        mask[1][1] = 9;

        applyLinearFilter();
        updateHistograms();
    }


    public void sobelFilterButtonHandler() {
        mask = new int[3][3];

        mask[0][0] = 1;
        mask[0][1] = 2;
        mask[0][2] = 1;
        mask[1][0] = mask[1][1] = mask[1][2] = 0;
        mask[2][0] = -1;
        mask[2][1] = -2;
        mask[2][2] = -1;

        applyLinearFilter();
        updateHistograms();
    }

    public void laplaceFilterButtonHandler() {
        mask = new int[3][3];

        mask[0][0] = 0;
        mask[0][1] = -1;
        mask[0][2] = 0;
        mask[1][0] = -1;
        mask[1][1] = 4;
        mask[1][2] = -1;
        mask[2][0] = 0;
        mask[2][1] = -1;
        mask[2][2] = 0;

        applyLinearFilter();
        updateHistograms();
    }


    public void gaussFilterButtonHandler() {
        mask = new int[3][3];

        mask[0][0] = 1;
        mask[0][1] = 2;
        mask[0][2] = 1;
        mask[1][0] = 2;
        mask[1][1] = 4;
        mask[1][2] = 2;
        mask[2][0] = 1;
        mask[2][1] = 2;
        mask[2][2] = 1;

        applyLinearFilter();
        updateHistograms();
    }

    public void edgesCornersDetFilterButtonHandler() {
        mask = new int[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == 0 || i == 2)
                    mask[i][j] = -1;
                else
                    mask[i][j] = 2;
            }

        }

        applyLinearFilter();
        updateHistograms();
    }

    private void applyLinearFilter() {

        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();

        int sum = 0;
        int kernelR = 0, kernelG = 0, kernelB = 0;
        for (int i = 0; i < this.maskSize; i++) {
            for (int j = 0; j < this.maskSize; j++) {
                sum += mask[i][j];


            }
        }


        for (int i = 1; i < imageWidth - 1; i++) {
            for (int j = 1; j < imageHeight - 1; j++) {

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        kernelR += (mask[x + 1][y + 1] * (analyzePixel(i + x, j + y))[0]);
                        kernelG += (mask[x + 1][y + 1] * (analyzePixel(i + x, j + y))[1]);
                        kernelB += (mask[x + 1][y + 1] * (analyzePixel(i + x, j + y))[2]);


                    }
                }

                int newPixelR, newPixelG, newPixelB;
                if (sum != 0) {
                    newPixelR = kernelR / sum;
                    newPixelG = kernelG / sum;
                    newPixelB = kernelB / sum;

                } else {
                    newPixelR = kernelR + 128;
                    newPixelG = kernelG + 128;
                    newPixelB = kernelB + 128;

                }
                if (newPixelR > 255)
                    newPixelR = 255;
                else if (newPixelR < 0)
                    newPixelR = 0;

                if (newPixelG > 255)
                    newPixelG = 255;
                else if (newPixelG < 0)
                    newPixelG = 0;

                if (newPixelB > 255)
                    newPixelB = 255;
                else if (newPixelB < 0)
                    newPixelB = 0;


                writer.setColor(i, j, Color.rgb(newPixelR, newPixelG, newPixelB));
                kernelR = kernelG = kernelB = 0;

            }


        }

        setImage(wi);
    }

    public void kuwaharaFilterButtonHandler() {
        applyKuwaharaFilter();
        updateHistograms();
    }

    private void applyKuwaharaFilter() {
        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();
        int[][] area1;
        int[][] area2;
        int[][] area3;
        int[][] area4;


        int[] meansR = new int[4];
        int[] meansG = new int[4];
        int[] meansB = new int[4];

        int[] variancesR = new int[4];
        int[] variancesG = new int[4];
        int[] variancesB = new int[4];

        int tmpVarianceR, tmpVarianceG, tmpVarianceB, minMeanR, minMeanG, minMeanB, tmpIndexR = 0, tmpIndexG = 0, tmpIndexB = 0;


        for (int i = 2; i < imageWidth - 2; i++) {
            for (int j = 2; j < imageHeight - 2; j++) {

                // 1
                area1 = createArea(i - 2, j - 2, "red");
                meansR[0] = countMeanKuwahara(area1);
                variancesR[0] = countVarianceKuwahara(area1, meansR[0]);

                area1 = createArea(i - 2, j - 2, "green");
                meansG[0] = countMeanKuwahara(area1);
                variancesG[0] = countVarianceKuwahara(area1, meansG[0]);

                area1 = createArea(i - 2, j - 2, "blue");
                meansB[0] = countMeanKuwahara(area1);
                variancesB[0] = countVarianceKuwahara(area1, meansB[0]);
                // 2
                area2 = createArea(i - 2, j, "red");
                meansR[1] = countMeanKuwahara(area2);
                variancesR[1] = countVarianceKuwahara(area2, meansR[1]);

                area2 = createArea(i - 2, j, "green");
                meansG[1] = countMeanKuwahara(area2);
                variancesG[1] = countVarianceKuwahara(area2, meansG[1]);

                area2 = createArea(i - 2, j, "blue");
                meansB[1] = countMeanKuwahara(area2);
                variancesB[1] = countVarianceKuwahara(area2, meansB[1]);

                //3
                area3 = createArea(i, j, "red");
                meansR[2] = countMeanKuwahara(area3);
                variancesR[2] = countVarianceKuwahara(area3, meansR[2]);

                area3 = createArea(i, j, "green");
                meansG[2] = countMeanKuwahara(area3);
                variancesG[2] = countVarianceKuwahara(area3, meansG[2]);

                area3 = createArea(i, j, "blue");
                meansB[2] = countMeanKuwahara(area3);
                variancesB[2] = countVarianceKuwahara(area3, meansB[2]);

                // 4
                area4 = createArea(i, j - 2, "red");
                meansR[3] = countMeanKuwahara(area4);
                variancesR[3] = countVarianceKuwahara(area4, meansR[3]);

                area4 = createArea(i, j - 2, "green");
                meansG[3] = countMeanKuwahara(area4);
                variancesG[3] = countVarianceKuwahara(area4, meansG[3]);

                area4 = createArea(i, j - 2, "blue");
                meansB[3] = countMeanKuwahara(area4);
                variancesB[3] = countVarianceKuwahara(area4, meansB[3]);


                tmpVarianceR = variancesR[0];
                tmpVarianceG = variancesG[0];
                tmpVarianceB = variancesB[0];

                for (int k = 1; k < 4; k++) {

                    if (variancesR[k] < tmpVarianceR) {
                        tmpVarianceR = variancesR[k];
                        tmpIndexR = k;
                    }
                    if (variancesG[k] < tmpVarianceG) {
                        tmpVarianceG = variancesG[k];
                        tmpIndexG = k;
                    }
                    if (variancesB[k] < tmpVarianceB) {
                        tmpVarianceB = variancesB[k];
                        tmpIndexB = k;
                    }
                }
                minMeanR = meansR[tmpIndexR];
                minMeanG = meansG[tmpIndexG];
                minMeanB = meansB[tmpIndexB];

                if (minMeanR > 255)
                    minMeanR = 255;

                if (minMeanG > 255)
                    minMeanG = 255;

                if (minMeanB > 255)
                    minMeanB = 255;
                writer.setColor(i, j, Color.rgb(minMeanR, minMeanG, minMeanB));
            }


        }

        setImage(wi);
    }

    private int[][] createArea(int xStart, int yStart, String color) {

        int[][] area = new int[3][3];
        int x = 0, y = 0;
        for (int i = xStart; i < xStart + 3; i++) {
            for (int j = yStart; j < yStart + 3; j++) {
                if (color.equals("red"))
                    area[x][y] = (analyzePixel(i, j))[0];
                else if (color.equals("green"))
                    area[x][y] = (analyzePixel(i, j))[1];
                else if (color.equals("blue"))
                    area[x][y] = (analyzePixel(i, j))[2];
                y++;

            }
            x++;
            y = 0;
        }
        return area;

    }

    private int countMeanKuwahara(int[][] area) {
        int sum = 0;

        int mean;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sum += area[i][j];

            }

        }
        mean = sum / 9;

        return mean;
    }

    private int countVarianceKuwahara(int[][] area, int mean) {
        int variance;
        int sum = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                sum += (area[i][j] - mean) * (area[i][j] - mean);
            }
        }
        variance = sum / 9;
        return variance;

    }

    public void median3x3FilterButtonHandler() {
        applyMedian3x3Filter();
        updateHistograms();
    }

    private void applyMedian3x3Filter() {
        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();

        for (int i = 1; i < imageWidth - 1; i++) {
            for (int j = 1; j < imageHeight - 1; j++) {

                int a = 0;
                int areaR[] = new int[9];
                int areaG[] = new int[9];
                int areaB[] = new int[9];


                for (int x = -1; x < 2; x++) {
                    for (int y = -1; y < 2; y++) {

                        areaR[a] = analyzePixel(i + x, j + y)[0];
                        areaG[a] = analyzePixel(i + x, j + y)[1];
                        areaB[a] = analyzePixel(i + x, j + y)[2];
                        a++;


                    }
                }
                int newPixelR = findMedian(areaR, 9);
                int newPixelG = findMedian(areaG, 9);
                int newPixelB = findMedian(areaB, 9);

                writer.setColor(i, j, Color.rgb(newPixelR, newPixelG, newPixelB));

            }
        }

        setImage(wi);
    }

    public void median5x5FilterButtonHandler() {
        applyMedian5x5Filter();
        updateHistograms();
    }

    private void applyMedian5x5Filter() {
        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();

        for (int i = 2; i < imageWidth - 2; i++) {
            for (int j = 2; j < imageHeight - 2; j++) {

                int a = 0;
                int areaR[] = new int[25];
                int areaG[] = new int[25];
                int areaB[] = new int[25];
                for (int x = -2; x < 3; x++) {
                    for (int y = -2; y < 3; y++) {

                        areaR[a] = analyzePixel(i + x, j + y)[0];
                        areaG[a] = analyzePixel(i + x, j + y)[1];
                        areaB[a] = analyzePixel(i + x, j + y)[2];
                        a++;



                    }
                }
                int newPixelR = findMedian(areaR, 25);
                int newPixelG = findMedian(areaG, 25);
                int newPixelB = findMedian(areaB, 25);

                writer.setColor(i, j, Color.rgb(newPixelR, newPixelG, newPixelB));

            }
        }

        setImage(wi);
    }

    private int findMedian(int[] area, int n) {
        Arrays.sort(area);
        return area[n / 2];
    }

    public void pointTransformation(ActionEvent event) throws IOException {

        MenuItem node = (MenuItem) event.getSource();
        int operation = Integer.valueOf(node.getUserData().toString());

        FiltersController paramWindow = new FiltersController();
        paramWindow.displayTransformationWindow(this, operation);

        int param = paramWindow.getParameter();

        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();
        Color color;

        switch (operation) {
            case 0: //adding

                for (int i = 0; i < imageWidth; i++) {
                    for (int j = 0; j < imageHeight; j++) {
                        int[] rgbColor = analyzePixel(i, j);
                        int newRed = rgbColor[0] + param > 255 ? 255 : rgbColor[0] + param;
                        int newGreen = rgbColor[1] + param > 255 ? 255 : rgbColor[1] + param;
                        int newBlue = rgbColor[2] + param > 255 ? 255 : rgbColor[2] + param;
                        color = Color.rgb(newRed, newGreen, newBlue);
                        writer.setColor(i, j, color);
                    }
                }
                break;
            case 1: //subtracting
                for (int i = 0; i < imageWidth; i++) {
                    for (int j = 0; j < imageHeight; j++) {
                        int[] rgbColor = analyzePixel(i, j);
                        int newRed = rgbColor[0] - param < 0 ? 0 : rgbColor[0] - param;
                        int newGreen = rgbColor[1] - param < 0 ? 0 : rgbColor[1] - param;
                        int newBlue = rgbColor[2] - param < 0 ? 0 : rgbColor[2] - param;
                        color = Color.rgb(newRed, newGreen, newBlue);
                        writer.setColor(i, j, color);
                    }
                }
                break;
            case 2: //multiplying
                for (int i = 0; i < imageWidth; i++) {
                    for (int j = 0; j < imageHeight; j++) {
                        int[] rgbColor = analyzePixel(i, j);
                        int newRed = rgbColor[0] * param > 255 ? 255 : rgbColor[0] * param;
                        int newGreen = rgbColor[1] * param > 255 ? 255 : rgbColor[1] * param;
                        int newBlue = rgbColor[2] * param > 255 ? 255 : rgbColor[2] * param;
                        color = Color.rgb(newRed, newGreen, newBlue);
                        writer.setColor(i, j, color);
                    }
                }
                break;
            case 3:
                for (int i = 0; i < imageWidth; i++) {
                    for (int j = 0; j < imageHeight; j++) {
                        int[] rgbColor = analyzePixel(i, j);
                        int newRed = rgbColor[0] / param;
                        int newGreen = rgbColor[1] / param;
                        int newBlue = rgbColor[2] / param;
                        color = Color.rgb(newRed, newGreen, newBlue);
                        writer.setColor(i, j, color);
                    }
                }
                break;

        }

        setImage(wi);
        this.updateHistograms();


    }

    public void greyScaleTransformation(ActionEvent event) throws IOException {

        MenuItem node = (MenuItem) event.getSource();
        int version = Integer.valueOf(node.getUserData().toString());

        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();
        Color color;

        switch (version) {
            case 1: //normal greyScale

                for (int i = 0; i < imageWidth; i++) {
                    for (int j = 0; j < imageHeight; j++) {
                        int[] rgbColor = analyzePixel(i, j);
                        color = Color.grayRgb(rgbColor[3]);
                        writer.setColor(i, j, color);
                    }
                }
                break;
            case 2: //YUV greyScale
                for (int i = 0; i < imageWidth; i++) {
                    for (int j = 0; j < imageHeight; j++) {
                        int[] rgbColor = analyzePixel(i, j);
                        color = Color.grayRgb((int) (0.299 * rgbColor[0] + 0.587 * rgbColor[1] + 0.115 * rgbColor[2]));
                        writer.setColor(i, j, color);
                    }
                }
                break;

        }

        setImage(wi);
        this.updateHistograms();


    }

    public void binarization(ActionEvent event) throws IOException {

        MenuItem node = (MenuItem) event.getSource();
        int version = Integer.valueOf(node.getUserData().toString());
        int sum, threshold, newThreshold;
        int total = (int) imageHeight * (int) imageWidth;


        switch (version) {
            case 0: //percent black selection
                FiltersController paramWindow = new FiltersController();
                paramWindow.displayBinarizationWindow(this, version);

                double param = (double)paramWindow.getParameter()/100.0;

                sum = 0;
                if (param == 1) {
                    createBinaryImage(255);
                    break;
                }
                for (int i = 0; i < 256; i++) {
                    sum += rgbTable[i];

                    if (sum >= total * (param)) {
                        createBinaryImage(i);
                        break;
                    }
                }
//                createBinaryImage(255);

                break;

            case 1: //mean iterative selection
                sum = 0;
                int leftAvg, rightAvg, leftSum, rightSum, totalLeftSum, totalRightSum;
                for (int i = 0; i < 256; i++) {
                    sum += i * rgbTable[i];
                }
                threshold = sum / total;
                for (int j = 0; j < 256; j++) {
                    leftSum = 0;
                    rightSum = 0;
                    totalLeftSum = 0;
                    totalRightSum = 0;

                    for (int i = 0; i < 256; i++) {
                        if (i < threshold) {
                            leftSum += i * rgbTable[i];
                            totalLeftSum += rgbTable[i];
                        } else {
                            rightSum += i * rgbTable[i];
                            totalRightSum += rgbTable[i];
                        }
                    }
                    leftAvg = leftSum / totalLeftSum;
                    rightAvg = rightSum / totalRightSum;
                    newThreshold = (int) (0.5 * (leftAvg + rightAvg));
                    if (newThreshold == threshold) {
                        createBinaryImage(newThreshold);
                        break;
                    } else {
                        threshold = newThreshold;
                    }
                }
                break;
            case 2:// entropy selection
                double whiteH, blackH, blackSum, whiteSum, blackDivider, whiteDivider, max = Integer.MIN_VALUE;
                newThreshold = 0;
                for (int i = 0; i < 256; i++) {
                    threshold = i;
                    blackSum = whiteSum = blackDivider = whiteDivider = 0;
                    for (int j = 0; j <= threshold; j++) {
                        for (int k = 0; k <= threshold; k++) {
                            blackDivider += k * rgbTable[k];
                        }
                        blackSum += j * rgbTable[j] / blackDivider;


                    }
                    blackH = (-1 * blackSum * Math.log(blackSum));

                    for (int j = threshold + 1; j < 256; j++) {
                        for (int k = threshold + 1; k < 256; k++) {
                            whiteDivider += k * rgbTable[k];
                        }
                        if (whiteDivider != 0) {
                            whiteSum += j * rgbTable[j] / whiteDivider;
                        }

                    }
                    whiteH = (-1 * whiteSum * Math.log(whiteSum));


                    if (whiteH + blackH > max) {
                        max = whiteH + blackH;
                        newThreshold = threshold;
                    }

                }
                createBinaryImage(newThreshold);


        }
        updateHistograms();
    }

    public void morphologyOperation(ActionEvent event) throws IOException {

        MenuItem node = (MenuItem) event.getSource();
        int operation = Integer.valueOf(node.getUserData().toString());

        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();
        boolean change = false;
        otsuMethod();
        switch (operation) {
            case 0: //dilation

                dilation();
                break;
            case 1: //erosion
                erosion();
                break;
            case 2: //opening
                erosion();
                dilation();
                break;
            case 3: //closing
                dilation();
                erosion();
                break;
            case 4:
                thinning();
                break;
            case 5:
                fattening();
                break;
        }
        updateHistograms();


    }

    private void dilation() {
        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();
        boolean change;
        for (int i = 1; i < imageWidth - 1; i++) {
            for (int j = 1; j < imageHeight - 1; j++) {
                change = false;

                for (int x = i - 1; x <= i + 1; x++) {
                    for (int y = j - 1; y <= j + 1; y++) {
                        if (x == i && y == j)
                            continue;
                        if (analyzePixel(x, y)[3] == 0) {
                            change = true;
                        }

                    }
                }
                if (change) {
                    writer.setColor(i, j, Color.BLACK);
                } else {
                    writer.setColor(i, j, Color.grayRgb(analyzePixel(i, j)[3]));
                }
            }
        }
        setImage(wi);
    }

    private void erosion() {
        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();
        boolean change;
        for (int i = 1; i < imageWidth - 1; i++) {
            for (int j = 1; j < imageHeight - 1; j++) {
                change = false;
                for (int x = i - 1; x <= i + 1; x++) {
                    for (int y = j - 1; y <= j + 1; y++) {
                        if (x == i && y == j)
                            continue;
                        if (analyzePixel(x, y)[3] == 255) {
                            change = true;
                        }
                    }
                    if (change)
                        writer.setColor(i, j, Color.WHITE);
                    else
                        writer.setColor(i, j, Color.grayRgb(analyzePixel(i, j)[3]));

                }
            }
        }
        setImage(wi);
    }

    private void thinning() {

        int[][] matrixSE1 = getSE1();
        int[][] matrixSE2 = getSE2();
        for (int i = 0; i < 20; i++) {
            hitOrMiss(matrixSE1, true);
            hitOrMiss(matrixSE2, true);
            rotateMatrixRight(matrixSE1);
            rotateMatrixRight(matrixSE2);
        }


    }

    private void fattening() {
        int[][] matrixSE3 = getSE3();
        int[][] matrixSE4 = getSE4();
        for (int i = 0; i < 20; i++) {
            hitOrMiss(matrixSE3, false);
            hitOrMiss(matrixSE4, false);
            rotateMatrixRight(matrixSE3);
            rotateMatrixRight(matrixSE4);
        }
    }

    private void hitOrMiss(int[][] se, boolean thinning) {
        WritableImage wi = new WritableImage((int) imageWidth, (int) imageHeight);
        PixelWriter writer = wi.getPixelWriter();
        boolean change = true;

        for (int i = 1; i < imageWidth - 1; i++) {
            for (int j = 1; j < imageHeight - 1; j++) {

                change = true;

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {

                        if (se[x + 1][y + 1] == 1) {
                            if (analyzePixel(i + x, j + y)[3] != 0) {
                                change = false;
                            }
                        } else if (se[x + 1][y + 1] == 0) {
                            if (analyzePixel(i + x, j + y)[3] != 255) {
                                change = false;
                            }
                        }

                    }
                }
                if (thinning) {
                    if (!change && analyzePixel(i, j)[3] == 0) {

                        writer.setColor(i, j, Color.BLACK);
                    } else {
                        writer.setColor(i, j, Color.WHITE);
                    }
                } else {
                    if (!change && analyzePixel(i, j)[3] == 255) {

                        writer.setColor(i, j, Color.WHITE);
                    } else {
                        writer.setColor(i, j, Color.BLACK);
                    }
                }

            }
        }
        setImage(wi);
    }

    private void rotateMatrixLeft(int[][] matrix) {
        if (matrix == null)
            return;
        if (matrix.length != matrix[0].length)// invalid input
            return;
        getTranspose(matrix);
        rotateAlongMidRow(matrix);
    }

    private void rotateMatrixRight(int[][] matrix) {
        if (matrix == null)
            return;
        if (matrix.length != matrix[0].length)// invalid input
            return;
        rotateAlongDiagonal(matrix);
        rotateAlongMidRow(matrix);
    }

    private void getTranspose(int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = i + 1; j < matrix.length; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[j][i];
                matrix[j][i] = temp;
            }
        }
    }

    private void rotateAlongMidRow(int[][] matrix) {
        int len = matrix.length;
        for (int i = 0; i < len / 2; i++) {
            for (int j = 0; j < len; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[len - 1 - i][j];
                matrix[len - 1 - i][j] = temp;
            }
        }
    }

    private void rotateAlongDiagonal(int[][] matrix) {
        int len = matrix.length;
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len - 1 - i; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[len - 1 - j][len - 1 - i];
                matrix[len - 1 - j][len - 1 - i] = temp;
            }
        }
    }

    private int[][] getSE1() {
        int[][] matrix = new int[3][3];
        matrix[0][0] = matrix[0][1] = matrix[0][2] = 0;
        matrix[1][0] = matrix[1][2] = -1;
        matrix[1][1] = matrix[2][0] = matrix[2][1] = matrix[2][2] = 1;


        return matrix;
    }


    private int[][] getSE2() {
        int[][] matrix = new int[3][3];
        matrix[0][1] = matrix[0][2] = matrix[1][2] = 0;
        matrix[0][0] = matrix[2][0] = matrix[2][2] = -1;
        matrix[1][0] = matrix[1][1] = matrix[2][1] = 1;


        return matrix;
    }


    private int[][] getSE3() {
        int[][] matrix = new int[3][3];
        matrix[1][1] = matrix[2][2] = 0;
        matrix[0][2] = matrix[1][2] = matrix[2][1] = -1;
        matrix[0][0] = matrix[0][1] = matrix[1][0] = matrix[2][0] = 1;

        return matrix;
    }

    private int[][] getSE4() {
        int[][] matrix = new int[3][3];
        matrix[1][1] = matrix[2][0] = 0;
        matrix[0][0] = matrix[1][0] = matrix[2][1] = -1;
        matrix[0][1] = matrix[0][2] = matrix[1][2] = matrix[2][1] = 1;

        return matrix;
    }
}
































