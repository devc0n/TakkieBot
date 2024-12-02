package nl.devc0n;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class BrowserManager {

    private static final Properties properties = new Properties();

    static {
        try (FileInputStream input = new FileInputStream("src/main/resources/config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Integer> screenshotTimes = new ArrayList<Integer>();
    private WebDriver driver;

    public void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--mute-audio"); // Mute audio
        driver = new ChromeDriver(options);
        driver.manage().window().setSize(new Dimension(400, 900));
        driver.get("https://sinterklaasspel.hema.nl/launch");
    }

    public WebDriver getDriver() {
        return driver;
    }

    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }


//    public BufferedImage takeScreenshot() throws IOException {
//        var screenshotStartTimer = System.currentTimeMillis();
//
//        // Take a screenshot of the entire page
//        byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
//        BufferedImage fullImage = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
//
//        // Get the dimensions of the viewport element
//        var viewport = driver.findElement(By.className("view-frame"));
//        var rect = viewport.getRect();
//
//        // Crop the screenshot to the dimensions of the viewport
//        BufferedImage croppedImage = fullImage.getSubimage(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
//
//        var screenshotAfterTimer = System.currentTimeMillis();
//        screenshotTimes.add((int) (screenshotAfterTimer - screenshotStartTimer));
//
//        return croppedImage;
//    }
//
//
//    public void saveScreenshot() throws IOException {
//        // Take a screenshot and save it to a file
//        var viewport = driver.findElement(By.className("view-frame"));
//        File screenshotFile = viewport.getScreenshotAs(OutputType.FILE);
//
//        // Convert the screenshot file into a BufferedImage
//        BufferedImage bufferedImage = ImageIO.read(screenshotFile);
//
////        // Optionally manipulate the BufferedImage (e.g., crop or resize)
////        System.out.println("Image Width: " + bufferedImage.getWidth());
////        System.out.println("Image Height: " + bufferedImage.getHeight());
//
//        File outputDir = new File("src/main/resources/screenshots");
//        if (!outputDir.exists()) {
//            outputDir.mkdirs();
//        }
//
//        var id = System.currentTimeMillis();
//
//        // Save the BufferedImage back to disk (if needed)
//        File outputImageFile = new File("src/main/resources/screenshots/screenshot-" + id + ".jpeg");
//        ImageIO.write(bufferedImage, "jpeg", outputImageFile);
//    }




}