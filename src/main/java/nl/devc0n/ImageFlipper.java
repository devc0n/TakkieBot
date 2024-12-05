package nl.devc0n;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

import static nl.devc0n.RLTrainer.printProgressBar;

public class ImageFlipper {

    public static void enhanceDataSet() {
        String screenshotsPath = "src/main/resources/screenshots";
        // Load all subfolders (actions)
        File screenshotsFolder = new File(screenshotsPath);
        File[] actionFolders = screenshotsFolder.listFiles(File::isDirectory);

        if (actionFolders == null) {
            System.err.println("No action folders found!");
            return;
        }

        for (File actionFolder : actionFolders) {
            System.out.println("\n Processing " + actionFolder.getAbsolutePath() + "\n");

            String key = switch (actionFolder.getName()) {
                case "up" -> "up-flipped";
                case "left" -> "left-flipped";
                case "right" -> "right-flipped";
                case "down" -> "down-flipped";
                case "noop" -> "noop-flipped";
                default -> "unknown";
            };
            String folderName = "src/main/resources/screenshots/" + key;
            File folder = new File(folderName);
            if (!folder.exists()) folder.mkdirs();


            File[] images = actionFolder.listFiles((dir, name) -> name.startsWith("frame_") && name.endsWith(".png"));

            if (images == null || images.length == 0) {
                System.out.println("No images in folder: " + actionFolder.getName());
                continue;
            }

            // Process each image
            for (int i = 0; i < images.length; i++) {
                printProgressBar(i, images.length);
                File currentImageFile = images[i];

                try {
                    BufferedImage currentImage = ImageIO.read(currentImageFile);
                    BufferedImage flippedImage = flipImage(currentImage);
                    String fileName = folderName + "/" + currentImageFile.getName();
                    ScreenshotUtil.saveScreenshot(flippedImage, fileName);
                } catch (Exception e) {
                    System.err.println("Failed to read image: " + currentImageFile.getName());
                }
            }
        }


    }

    public static BufferedImage flipImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage flippedImage = new BufferedImage(width, height, image.getType());
        Graphics2D g2d = flippedImage.createGraphics();

        AffineTransform transform = new AffineTransform();
        transform.scale(-1, 1);
        transform.translate(-width, 0);

        g2d.drawImage(image, transform, null);
        g2d.dispose();
        return flippedImage;
    }

}
