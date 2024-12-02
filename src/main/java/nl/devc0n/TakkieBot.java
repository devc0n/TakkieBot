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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class TakkieBot {
    private static final String MODEL_PATH = "src/main/resources/model.zip";
    private static final Random random = new Random();
    private static MultiLayerNetwork model;

//    static {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    }

    public static void main(String[] args) throws Exception {
        BufferedImage template = ImageIO.read(new File("src/main/resources/masks/mask.png"));
        GameOverDetector gameOverDetector = new GameOverDetector(template);

        BrowserManager browserManager = new BrowserManager();
        browserManager.startBrowser();


        List<TrainingData> trainingDataList = new ArrayList<>();

        if (Files.exists(Paths.get(MODEL_PATH))) {
            model = ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH, true);
        } else {
            MultiLayerConfiguration config = new NeuralNetConfiguration.Builder().seed(123)
                    .updater(new Adam(0.00025)) // Optimizer
                    .list().layer(0, new ConvolutionLayer.Builder(8,
                            8) // Convolutional layer for feature extraction
                            .stride(4, 4).nIn(3) // RGB input
                            .nOut(32).activation(Activation.RELU)
                            .dataFormat(CNN2DFormat.NHWC) // Set data format to NHWC (channels last)
                            .build()).layer(1, new ConvolutionLayer.Builder(4, 4).stride(2, 2)
                            .nIn(32) // Set nIn to match nOut of the previous layer
                            .nOut(64).activation(Activation.RELU)
                            .dataFormat(CNN2DFormat.NHWC) // Set data format to NHWC (channels last)
                            .build()).layer(2, new DenseLayer.Builder().nIn(64 * 9 *
                                    9) // Set nIn to match the flattened output of the previous layer
                            .nOut(512).activation(Activation.RELU).build()).layer(3,
                            new OutputLayer.Builder(
                                    LossFunctions.LossFunction.MSE) // Output layer for Q-values
                                    .nIn(512) // Set nIn to match nOut of the previous layer
                                    .nOut(5) // Number of possible actions
                                    .activation(Activation.IDENTITY).build()).setInputType(
                            InputType.convolutional(84, 84, 3,
                                    CNN2DFormat.NHWC)) // Set input type with NHWC format
                    .build();

            model = new MultiLayerNetwork(config);
            model.init();
        }

        try {
            trainingDataList = loadTrainingData("src/main/resources/trainingData.dat");
            for (TrainingData data : trainingDataList) {
                train(data.getState(), data.getAction(), data.getReward(), data.getNextState());
            }
        } catch (Exception e) {
            System.out.println("No training data found. Starting from scratch.");
        }


        GameHandler gameHandler = new GameHandler(browserManager.getDriver());
        gameHandler.setupInitialGameState();
        GameStateReader gameScoreReader = new GameStateReader(browserManager);

        // Load training data and train the model


        playGame(gameHandler, gameScoreReader, browserManager, gameOverDetector, trainingDataList);
    }

    public static void playGame(GameHandler gameHandler, GameStateReader gameStateReader,
                                BrowserManager browserManager, GameOverDetector gameOverDetector,
                                List<TrainingData> trainingDataList)
            throws IOException, InterruptedException {
        var driver = browserManager.getDriver();
        var isGameOver = false;

        Queue<GameStep> recentSteps = new LinkedList<>();



        var game = driver.findElement(By.id("game"));
        var rect = game.getRect();

        var width = rect.getWidth() * 0.66;
        var heigth = rect.getHeight() * 0.66;
        var diffWidth = (rect.getWidth() - width) / 2;
        int x = (int) ((rect.getX() + 40) + diffWidth);
        var diffHeigth = (rect.getHeight() - heigth) / 2;
        int y = (int) ((rect.getY() + 180) + diffHeigth);
        int gameWidth = rect.getWidth() / 2;
        int gameHeight = rect.getHeight() / 2;

        int i = 1;

        while (true) {
            var screenshot = ScreenshotUtil.takeScreenshot(x, y, gameWidth, gameHeight);
            var state = ScreenshotUtil.preprocessScreenshot(screenshot);

//            var time = System.currentTimeMillis();
//            ScreenshotUtil.saveScreenshot(screenshot,
//                    "src/main/resources/screenshots/screenshot-" + time + ".jpeg");

            // Get action from DQN model
            int action = predictAction(state);

            if (isGameOver) {
                ModelSerializer.writeModel(model, MODEL_PATH, true);
                gameHandler.performAction(99);
                isGameOver = false;
                i++;
                continue;
            }

            var reward = gameHandler.performAction(action);

            // Capture the next state (screenshot)
            var nextScreenshot = ScreenshotUtil.takeScreenshot(x, y, gameWidth, gameHeight);
            INDArray nextState = ScreenshotUtil.preprocessScreenshot(nextScreenshot);
            train(state, action, reward, nextState);

            // Store the training data
            trainingDataList.add(new TrainingData(state, action, reward, nextState));


            recentSteps.add(new GameStep(state, action, reward));
            if (recentSteps.size() > 5) {
                recentSteps.poll();
            }

            // Check if the game is over and punish the last 5 actions if it is
            isGameOver = gameOverDetector.detectGameOverWithTemplate(screenshot);
            if (isGameOver) {
                for (GameStep step : recentSteps) {
                    step.setReward(step.getReward() - 1); // Punish by reducing the reward
                    train(step.getState(), step.getAction(), step.getReward(), nextState);
                }
            }

            if (i % 25 == 0) {
                // Save the training data to a file every 25 games.
                saveTrainingData(trainingDataList, "src/main/resources/trainingData.dat");
            }
        }
    }

    public static int predictAction(INDArray state) {

        if (random.nextInt(100) < 1) {
            // Return a random number between 0 and 4
            int randomNumber = random.nextInt(5);
            return randomNumber;
        }

        // Predict Q-values for all actions
        INDArray qValues = model.output(state);
        System.out.println(qValues);
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

    public static List<TrainingData> loadTrainingData(String filePath)
            throws IOException, ClassNotFoundException {
        try (FileInputStream fileIn = new FileInputStream(filePath);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            return (List<TrainingData>) in.readObject();
        }
    }

    public static void saveTrainingData(List<TrainingData> trainingDataList, String filePath) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(filePath);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(trainingDataList);
        }
    }
}
