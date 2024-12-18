package nl.devc0n;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class ScreenshotUtil {

    private static final Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage takeScreenshot(org.openqa.selenium.Rectangle rect) {
        var width = 250;
        var height = 300;

        var x = rect.getX() + 18 + 125;
        var y = rect.getY() + 150 + 230;

        Rectangle screenRect = new Rectangle(x, y, width, height);
        return robot.createScreenCapture(screenRect);
    }

    public static void saveScreenshot(BufferedImage image, String filePath) throws IOException {
        File outputFile = new File(filePath);
        ImageIO.write(image, "jpeg", outputFile);
    }

    public static INDArray preprocessScreenshot(BufferedImage screenshot) {
// Resize the image to 84x84
        BufferedImage resizedImage = new BufferedImage(84, 84, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(screenshot, 0, 0, 84, 84, null);
        g2d.dispose();

// Normalize the image to [0, 1]
        int width = resizedImage.getWidth();
        int height = resizedImage.getHeight();
        float[] pixels = new float[width * height * 3];  // RGB channels
        int i = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = resizedImage.getRGB(x, y);
                pixels[i++] = ((pixel >> 16) & 0xFF) / 255.0f;  // Red
                pixels[i++] = ((pixel >> 8) & 0xFF) / 255.0f;   // Green
                pixels[i++] = (pixel & 0xFF) / 255.0f;           // Blue
            }
        }

// Reshape to NHWC format (batch size, height, width, channels)
        return Nd4j.create(pixels).reshape(1, 84, 84, 3);  // Shape: [batch, height, width, channels]

    }

    public static void saveINDArrayAsImage(INDArray array, String filepath) throws IOException {
        int width = (int) array.shape()[1];
        int height = (int) array.shape()[2];
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = (int) (array.getFloat(0, y, x, 0) * 255);
                int g = (int) (array.getFloat(0, y, x, 1) * 255);
                int b = (int) (array.getFloat(0, y, x, 2) * 255);
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }

        saveScreenshot(image, filepath);
    }


}
