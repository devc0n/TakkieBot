package nl.devc0n;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;

public class GameHandler {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public GameHandler(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public boolean performAction(String action) {
        switch (action) {
            case "FLY":
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_UP);
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_DOWN);
                driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
                return false;
            case "UP":
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_UP);
                return false;
            case "LEFT":
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_LEFT);
                return false;
            case "RIGHT":
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_RIGHT);
                return false;
            case "DOWN":
                driver.findElement(By.tagName("body")).sendKeys(Keys.ARROW_DOWN);
                return false;
            case "RESTART":
                WebElement startGameButton = wait.until(
                        ExpectedConditions.presenceOfElementLocated(By.className("icon-only")));
                startGameButton.click();
                return true;
            default:
                System.out.println("Unknown action: " + action);
                return false;
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
