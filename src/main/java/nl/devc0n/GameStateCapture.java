package nl.devc0n;

import java.awt.*;
        import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GameStateCapture {

    /**
     * setup the game area and capture the game state
     *  half of the screen width and height is 375 and 667
     *  use iphone SE preset in the browser dev tools
     *
     * @param args
     * @throws Exception
     */

    public static void main(String[] args) throws Exception {
        // Define the game area (Adjust coordinates and size)
        Rectangle gameArea = new Rectangle(167, 187, 375, 667); // x, y, width, height
        Robot robot = new Robot();

        // Capture a screenshot
        BufferedImage screenshot = robot.createScreenCapture(gameArea);

        // Save the screenshot (for testing purposes)
        ImageIO.write(screenshot, "png", new File("game_state.png"));

    }
}
