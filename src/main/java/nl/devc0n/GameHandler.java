package nl.devc0n;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class GameHandler {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Map<Integer, Integer> actionMap = new HashMap<Integer, Integer>();

    public GameHandler(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public double performAction(int action) throws InterruptedException {
        actionMap.put(action, actionMap.getOrDefault(action, 0) + 1);
        System.out.println(actionMap);
        switch (action) {
            case 0:
                return 0.2;
            case 1:
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_UP);
                return 0.2;
            case 2:
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_LEFT);
                return 0.2;
            case 3:
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_RIGHT);
                return 0.2;
            case 4:
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_DOWN);
                return 0.2;
            case 99:
                Thread.sleep(10000);
                WebElement startGameButton = wait.until(
                        ExpectedConditions.presenceOfElementLocated(By.className("icon-only")));
                startGameButton.click();
                Thread.sleep(4000);
                return 0;
            default:
                System.out.println("Unknown action: " + action);
                return 0;
        }
    }

    public void setupInitialGameState() throws InterruptedException {
        // Accept cookies
        WebElement acceptCookiesButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll")));
        acceptCookiesButton.click();

        // Start the game
        WebElement startGameButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.className("launch-button")));
        startGameButton.click();

        // Close instructions
        WebElement closeInstructionsButton =
                wait.until(ExpectedConditions.presenceOfElementLocated(By.className("close")));
        closeInstructionsButton.click();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("game-canvas")));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

}
