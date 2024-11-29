package nl.devc0n;

import nl.devc0n.constants.State;

public class GameStateReader {
    private final BrowserManager browserManager;

    public GameStateReader(BrowserManager browserManager) {
        this.browserManager = browserManager;
    }

    public State getGameState(){
        if (isGameOver()) {
            return State.RESTARTING;
        }else {
            return State.PLAYING;
        }
    }

    public boolean isGameOver() {
        browserManager.takeScreenshot();
        String ocrResult = OCRUtil.extractTextFromImage();
        if (ocrResult != null) {
            return ocrResult.toLowerCase().contains("goed gedaan");
        }
        return false;
    }
}

