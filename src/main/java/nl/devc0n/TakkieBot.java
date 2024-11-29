package nl.devc0n;

import static nl.devc0n.constants.Action.UP;

public class TakkieBot {


    public static void main(String[] args) throws Exception {
        BrowserManager browserManager = new BrowserManager();
        browserManager.startBrowser();

        GameHandler gameHandler = new GameHandler(browserManager.getDriver());
        gameHandler.setupInitialGameState();
        GameStateReader gameScoreReader = new GameStateReader(browserManager);


        playGame(gameHandler, gameScoreReader);
    }

    public static void playGame(GameHandler gameHandler, GameStateReader gameStateReader) {
        var isGameOver = false;
        var startTime = System.currentTimeMillis();
        var endTime = System.currentTimeMillis();
        var duration = endTime - startTime;

        while (true) {
            if (isGameOver) {
                endTime = System.currentTimeMillis();
                duration = endTime - startTime;
                System.out.println("Duration: " + duration);
                startTime = System.currentTimeMillis();
                gameHandler.performAction("RESTART");
            }

            gameHandler.performAction(UP.toString());
            isGameOver = gameStateReader.isGameOver();
        }
    }
}
