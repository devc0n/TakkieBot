package nl.devc0n;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class OCRUtil {

    private static final Properties properties = new Properties();

    static {
        try (FileInputStream input = new FileInputStream("src/main/resources/config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String extractTextFromImage(BufferedImage image) {

        String tessdataPath = properties.getProperty("tessdata.path");
        String tessdataLanguage = properties.getProperty("tessdata.language");

        ITesseract instance = new Tesseract();
        instance.setDatapath(tessdataPath);
        instance.setLanguage(tessdataLanguage);

        try {
            // Perform OCR on the BufferedImage
            String result = instance.doOCR(image);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
