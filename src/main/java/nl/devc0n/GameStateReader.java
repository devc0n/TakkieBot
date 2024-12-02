package nl.devc0n;

import nl.devc0n.constants.State;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class GameStateReader {
    private final BrowserManager browserManager;

    public GameStateReader(BrowserManager browserManager) {
        this.browserManager = browserManager;
    }

    public boolean isGameOver(BufferedImage image) throws IOException {
        String ocrResult = OCRUtil.extractTextFromImage(image);
        if (ocrResult != null) {
            return ocrResult.toLowerCase().contains("goed gedaan");
        }
        return false;
    }
}

