package nl.devc0n;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
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

    private WebDriver driver;

    public void startBrowser() {
        driver = new ChromeDriver();
        driver.manage().window().setSize(new Dimension(375, 700));
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

    public void takeScreenshot() {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String screenshotPath = properties.getProperty("screenshot.path");
        try {
            Files.copy(screenshot.toPath(), Paths.get(screenshotPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        preprocessImage();
    }

    public static void preprocessImage(){
        String screenshotPath = properties.getProperty("screenshot.path");
        Mat image = opencv_imgcodecs.imread(screenshotPath);

        // convert to grayscale
        Mat grayImage = new Mat();
        opencv_imgproc.cvtColor(image,grayImage,opencv_imgproc.COLOR_BGR2GRAY);

        // Apply thresholding
        Mat binaryImage = new Mat();
        opencv_imgproc.threshold(grayImage, binaryImage, 0, 255, opencv_imgproc.THRESH_BINARY | opencv_imgproc.THRESH_OTSU);

        // Remove noise with morphological transformations
        Mat denoisedImage = new Mat();
        Mat kernel = opencv_imgproc.getStructuringElement(opencv_imgproc.MORPH_RECT, new Size(1, 1));
        opencv_imgproc.morphologyEx(binaryImage, denoisedImage, opencv_imgproc.MORPH_CLOSE, kernel);

        // Save the processed image
        String processedPath = properties.getProperty("processed.path");
        opencv_imgcodecs.imwrite(processedPath, denoisedImage);
    }




}