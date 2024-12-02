package nl.devc0n;

import org.deeplearning4j.nn.conf.CNN2DFormat;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TakkieBot {
    private static final String MODEL_PATH = "src/main/resources/model.zip";
    private static final Random random = new Random();
    private static MultiLayerNetwork model;
    private WebDriver driver;

    public static void main(String[] args) throws Exception {
        BrowserManager browserManager = new BrowserManager();
        browserManager.startBrowser();


        List<TrainingData> trainingDataList = new ArrayList<>();

        if (Files.exists(Paths.get(MODEL_PATH))) {
            model = ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH, true);
        } else {
            MultiLayerConfiguration config = new NeuralNetConfiguration.Builder().seed(123).updater(new Adam(0.00025)) // Optimizer
                    .list().layer(0, new ConvolutionLayer.Builder(8, 8) // Convolutional layer for feature extraction
                            .stride(4, 4).nIn(3) // RGB input
                            .nOut(32).activation(Activation.RELU).dataFormat(CNN2DFormat.NHWC) // Set data format to NHWC (channels last)
                            .build()).layer(1, new ConvolutionLayer.Builder(4, 4).stride(2, 2).nIn(32) // Set nIn to match nOut of the previous layer
                            .nOut(64).activation(Activation.RELU).dataFormat(CNN2DFormat.NHWC) // Set data format to NHWC (channels last)
                            .build()).layer(2, new DenseLayer.Builder().nIn(64 * 9 * 9) // Set nIn to match the flattened output of the previous layer
                            .nOut(512).activation(Activation.RELU).build()).layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MSE) // Output layer for Q-values
                            .nIn(512) // Set nIn to match nOut of the previous layer
                            .nOut(5) // Number of possible actions
                            .activation(Activation.IDENTITY).build()).setInputType(InputType.convolutional(84, 84, 3, CNN2DFormat.NHWC)) // Set input type with NHWC format
                    .build();

            model = new MultiLayerNetwork(config);
            model.init();
        }


        GameHandler gameHandler = new GameHandler(browserManager.getDriver());
        gameHandler.setupInitialGameState();
        var programOption = "train";
        switch (programOption) {
            case "gather":
                gatherTrainingData(browserManager);
                break;
            case "train":
                RLTrainer trainer = new RLTrainer();
                trainer.trainOnData();
                ModelSerializer.writeModel(model, MODEL_PATH, true);
                break;
            case "play":
                playGame(gameHandler, browserManager, trainingDataList);
                break;
        }
        printAsciiArt();
    }

    public static void playGame(GameHandler gameHandler, BrowserManager browserManager, List<TrainingData> trainingDataList) throws IOException, InterruptedException {
        var driver = browserManager.getDriver();
        var isGameOver = false;

        BufferedImage template = ImageIO.read(new File("src/main/resources/masks/mask.png"));
        GameOverDetector gameOverDetector = new GameOverDetector(template);
        gameOverDetector.detectGameOverWithTemplate(template);

        var game = driver.findElement(By.id("game"));
        var rect = game.getRect();

        var isPlaying = true;
        LinkedList<TrainingData> currentGameData = new LinkedList<>();
        while (isPlaying) {
            var startTime = System.currentTimeMillis();
            var screenshot = ScreenshotUtil.takeScreenshot(rect);
            var state = ScreenshotUtil.preprocessScreenshot(screenshot);

            // Get action from DQN model
            int action = predictAction(state);

            if (isGameOver) {
                ScreenshotUtil.saveScreenshot(screenshot, "src/main/resources/screenshots/" + startTime + "- action -" + action + ".jpeg");
                ModelSerializer.writeModel(model, MODEL_PATH, true);
                gameHandler.performAction(99);
                isGameOver = false;
                continue;
            }

            var reward = gameHandler.performAction(action);

            // Capture the next state (screenshot)
            var nextScreenshot = ScreenshotUtil.takeScreenshot(rect);
            INDArray nextState = ScreenshotUtil.preprocessScreenshot(nextScreenshot);

            // Store the training data
            currentGameData.add(new TrainingData(state, action, reward, nextState));
            // Ensure only the last 120 actions are stored
            if (currentGameData.size() > 60) {
                currentGameData.poll(); // Remove the oldest entry
            }

            // Check if the game is over and punish the last 5 actions if it is
            isGameOver = gameOverDetector.detectGameOverWithTemplate(screenshot);
            if (isGameOver) {
                // Punish by reducing the reward
                for (int gameDataIndex = Math.max(0, currentGameData.size() - 12); gameDataIndex < currentGameData.size(); gameDataIndex++) {
                    TrainingData step = currentGameData.get(gameDataIndex);
                    step.setReward(step.getReward() - 1); // Punish by reducing the reward
                    train(step.getState(), step.getAction(), step.getReward(), nextState);
                }

                // Add the punished actions to the total training data array (you can store it in a global list)
                trainingDataList.addAll(currentGameData);
                saveTrainingData(trainingDataList, "src/main/resources/trainingData.dat");
            }
        }

        printAsciiArt();
    }

    public static int predictAction(INDArray state) {

        if (random.nextInt(100) < 5) {
            // Return a random number between 0 and 4
            int randomNumber = random.nextInt(5);
            return randomNumber;
        }

        // Predict Q-values for all actions
        INDArray qValues = model.output(state);
        int bestAction = qValues.argMax(1).getInt(0);  // Get the action with the highest Q-value
        return bestAction;
    }

    public static void train(INDArray state, int action, double reward, INDArray nextState) {
        // Parameters
        double gamma = 0.99;  // Discount factor
        double alpha = 0.00025;  // Learning rate

        // Get current Q-values for the current state
        INDArray qValues = model.output(state); // Shape: [batch, numActions]

        // Get the Q-value for the taken action (use action index to pick the Q-value)
        double currentQValue = qValues.getDouble(0, action);

        // Get next Q-values for the next state
        INDArray nextQValues = model.output(nextState);

        // Get the maximum Q-value for the next state (this will be used in the Bellman equation)
        double maxNextQValue = nextQValues.maxNumber().doubleValue();

        // Calculate the target Q-value using the Q-learning update rule
        double targetQValue = reward + gamma * maxNextQValue;

        // Calculate the error (TD error) between the current Q-value and the target Q-value
        double error = targetQValue - currentQValue;

        // Apply the error to the Q-value for the action taken
        qValues.putScalar(0, action, currentQValue + alpha * error);

        // Backpropagate and update the model's weights
        model.fit(state, qValues);  // Train the model with the updated Q-values
    }

    public static List<TrainingData> loadTrainingData(String filePath) throws IOException, ClassNotFoundException {
        try (FileInputStream fileIn = new FileInputStream(filePath); ObjectInputStream in = new ObjectInputStream(fileIn)) {
            return (List<TrainingData>) in.readObject();
        }
    }

    public static void saveTrainingData(List<TrainingData> trainingDataList, String filePath) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(filePath); ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(trainingDataList);
        }
    }

    public static void printAsciiArt() {
        String asciiArt = """
                  TTTTT  AAAAA  K   K  K   K  III  EEEEE      B   B  OOOOO  TTTTT
                    T    A   A  K  K   K K   I   E          B   B  O   O    T  
                    T    AAAAA  KKK    KK    I   EEEE       BBBBB  O   O    T  
                    T    A   A  K  K   K K   I   E          B   B  O   O    T  
                    T    A   A  K   K  K   K  III  EEEEE     B   B  OOOOO    T  
                """;
        System.out.println(asciiArt);
    }

    public static void gatherTrainingData(BrowserManager browserManager) throws IOException {
        var driver = browserManager.getDriver();
        var game = driver.findElement(By.id("game"));
        var rect = game.getRect();
        KeyboardUtil keyboardUtil = new KeyboardUtil();

        while (true) {
            String key = keyboardUtil.listen();
            saveAndSortScreenshot(key, rect);
            System.out.println(key);
        }

    }

    public static void saveAndSortScreenshot(String key, Rectangle rect) throws IOException {
        var newFrame = ScreenshotUtil.takeScreenshot(rect);

        String folderName = "src/main/resources/screenshots/" + key;
        File folder = new File(folderName);
        if (!folder.exists()) folder.mkdirs();
        String fileName = folderName + "/frame_" + System.currentTimeMillis() + ".png";

        ScreenshotUtil.saveScreenshot(newFrame, fileName);
    }


}
