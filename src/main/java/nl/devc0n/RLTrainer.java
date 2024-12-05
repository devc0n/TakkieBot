package nl.devc0n;

import org.jetbrains.annotations.NotNull;
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
        String timestampPart = filename.split("_")[1];
        return Long.parseLong(timestampPart);
    }

    private static @NotNull String extractAction(String filename) {

        return filename.split("_")[3].split("\\.")[0];
    }

    private static int convertActionToIndex(String action) {
        return switch (action) {
            case "up" -> 1;
            case "up-flipped" -> 1;
            case "left" -> 2;
            case "left-flipped" -> 3;
            case "right" -> 3;
            case "right-flipped" -> 2;
            case "down" -> 4;
            case "down-flipped" -> 4;
            default -> 0;
        };
    }

    public static void printProgressBar(int completed, int total) {
        int barLength = 50; // Length of the progress bar
        int progress = (int) ((double) completed / total * barLength);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < progress) {
                bar.append("=");
            } else {
                bar.append(" ");
            }
        }
        bar.append("] ").append(completed * 100 / total).append("%");

        // Print the progress bar
        System.out.print("\r" + bar); // Overwrite the line
    }

    public void trainOnData() {
        String screenshotsPath = "src/main/resources/screenshots";
        // Load all subfolders (playthroughs)
        File screenshotsFolder = new File(screenshotsPath);
        File[] playthroughFolders = screenshotsFolder.listFiles(File::isDirectory);

        if (playthroughFolders == null) {
            System.err.println("No action folders found!");
            return;
        }

        for (File playthroughFolder : playthroughFolders) {
            System.out.println("\n Processing " + playthroughFolder.getAbsolutePath() + "\n");


            File[] images = playthroughFolder.listFiles((dir, name) -> name.startsWith("frame_") && name.endsWith(".png"));

            if (images == null || images.length == 0) {
                System.out.println("No images in folder: " + playthroughFolder.getName());
                continue;
            }

            // Sort images by timestamp (assumes filenames are in format frame_<timestamp>.png)
            Arrays.sort(images, Comparator.comparingLong(file -> extractTimestamp(file.getName())));

            // Process each image
            for (int i = 0; i < images.length; i++) {

                printProgressBar(i, images.length);
                File currentImageFile = images[i];

                try {
                    // Load current and next images as BufferedImage
                    BufferedImage currentImage = ImageIO.read(currentImageFile);
                    var currentImageFlipped = ImageFlipper.flipImage(currentImage);

                    // Preprocess images to INDArray
                    INDArray currentState = ScreenshotUtil.preprocessScreenshot(currentImage);
                    INDArray currentStateFlipped = ScreenshotUtil.preprocessScreenshot(currentImageFlipped);

                    var action = convertActionToIndex(extractAction(currentImageFile.getName()));
                    var flippedAction = action;
                    if (action == 2) {
                        flippedAction = 3;
                    }
                    if (action == 3) {
                        flippedAction = 2;
                    }
//                    double reward = action == 0 ? 0.1 : 1;
                    double reward = 1;

                    // if last 24 images just punish -1;
                    if (images.length - 24 < i) {
                        reward = 0;
                    }

                    // Call the train method
                    train(currentState, action, reward);
                    train(currentStateFlipped, flippedAction, reward);

                } catch (Exception e) {
                    System.err.println("Failed to read image: " + currentImageFile.getName());
                }
            }
        }
    }


}
