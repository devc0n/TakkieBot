package nl.devc0n;

import org.nd4j.linalg.api.ndarray.INDArray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import static nl.devc0n.TakkieBot.train;

public class RLTrainer {

    private static long extractTimestamp(String filename) {
        // Extract timestamp from filename (e.g., frame_<timestamp>.png)
        String timestampPart = filename.replace("frame_", "").replace(".png", "");
        return Long.parseLong(timestampPart);
    }


    public void trainOnData() {
        String screenshotsPath = "src/main/resources/screenshots";
        // Load all subfolders (actions)
        File screenshotsFolder = new File(screenshotsPath);
        File[] actionFolders = screenshotsFolder.listFiles(File::isDirectory);

        if (actionFolders == null) {
            System.err.println("No action folders found!");
            return;
        }

        for (File actionFolder : actionFolders) {

            int action = switch (actionFolder.getName()) {
                case "up" -> 1;
                case "left" -> 2;
                case "right" -> 3;
                case "down" -> 4;
                default -> 0;
            };

            File[] images = actionFolder.listFiles((dir, name) -> name.startsWith("frame_") && name.endsWith(".png"));

            if (images == null || images.length == 0) {
                System.out.println("No images in folder: " + actionFolder.getName());
                continue;
            }

            // Sort images by timestamp (assumes filenames are in format frame_<timestamp>.png)
            Arrays.sort(images, Comparator.comparingLong(file -> extractTimestamp(file.getName())));


            // Process each image
            for (int i = 0; i < images.length; i++) {
                File currentImageFile = images[i];
                File nextImageFile = (i + 1 < images.length) ? images[i + 1] : images[i];

                try {
                    // Load current and next images as BufferedImage
                    BufferedImage currentImage = ImageIO.read(currentImageFile);
                    BufferedImage nextImage = (nextImageFile != null) ? ImageIO.read(nextImageFile) : null;

                    // Preprocess images to INDArray
                    INDArray currentState = ScreenshotUtil.preprocessScreenshot(currentImage);
                    INDArray nextState = (nextImage != null) ? ScreenshotUtil.preprocessScreenshot(nextImage) : null;

                    // Define a reward for this transition (adjust this logic as needed)
                    double reward = 1;

                    // Call the train method
                    train(currentState, action, reward, nextState);
                } catch (Exception e) {
                    System.err.println("Failed to read image: " + currentImageFile.getName());
                }
            }
        }
    }


}
