package nl.devc0n;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;


public class GameOverDetector {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private Mat template; // Store the template as an OpenCV Mat

    // Constructor to initialize the template
    public GameOverDetector(BufferedImage templateImage) {
        // Convert BufferedImage template to OpenCV Mat
        this.template = bufferedImageToMat(templateImage);
    }

    public boolean detectGameOverWithTemplate(BufferedImage screenShot) throws IOException {





        // Convert the screenshot to OpenCV Mat
        Mat screenshotMat = bufferedImageToMat(screenShot);

        if (screenshotMat.empty() || template.empty()) {
            System.err.println("Failed to load images.");
            return false;
        }

        // Create a result matrix to hold matching results
        int resultCols = screenshotMat.cols() - template.cols() + 1;
        int resultRows = screenshotMat.rows() - template.rows() + 1;
        Mat result = new Mat(resultRows, resultCols, CvType.CV_32FC1);

        // Perform template matching
        Imgproc.matchTemplate(screenshotMat, template, result, Imgproc.TM_CCOEFF_NORMED);

        // Find the best match location
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        double threshold = 0.15; // Set a threshold for match quality (35% match)

        // Check if the match is strong enough
        var isGameOver = mmr.maxVal >= threshold;

        return isGameOver;
    }

    public Mat bufferedImageToMat(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            // Convert the image to TYPE_3BYTE_BGR
            BufferedImage convertedImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            convertedImg.getGraphics().drawImage(image, 0, 0, null);
            image = convertedImg;
        }
        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

}
