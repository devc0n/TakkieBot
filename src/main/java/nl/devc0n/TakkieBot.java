package nl.devc0n;

import nl.devc0n.domain.TrainingData;
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
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static nl.devc0n.RLTrainer.printProgressBar;
import static nl.devc0n.ScreenshotUtil.preprocessScreenshot;
import static org.nd4j.linalg.ops.transforms.Transforms.softmax;

public class TakkieBot {
    private static final String MODEL_PATH = "src/main/resources/fresh.zip";
    private static MultiLayerNetwork model;

    private static int minSteps = 0;
    private static int maxSteps = 0;

    public static void main(String[] args) throws Exception {
        if (Files.exists(Paths.get(MODEL_PATH))) {
            model = ModelSerializer.restoreMultiLayerNetwork(MODEL_PATH, true);
            System.out.println("Loaded model");
        } else {

//            MultiLayerConfiguration config = new NeuralNetConfiguration.Builder().seed(123) // Random seed for reproducibility
//                    .updater(new Adam(0.00025)) // Optimizer
//                    .list().layer(0, new ConvolutionLayer.Builder(8, 8) // First convolutional layer
//                            .stride(4, 4).nIn(3) // RGB input channels
//                            .nOut(32) // Number of filters
//                            .activation(Activation.RELU) // Activation function
//                            .dataFormat(CNN2DFormat.NHWC) // Set data format to NHWC (channels last)
//                            .build()).layer(1, new ConvolutionLayer.Builder(4, 4) // Second convolutional layer
//                            .stride(2, 2).nIn(32) // Input depth matches output depth of the previous layer
//                            .nOut(64) // Number of filters
//                            .activation(Activation.RELU).dataFormat(CNN2DFormat.NHWC) // Set data format to NHWC
//                            .build()).layer(2, new DenseLayer.Builder() // Fully connected layer
//                            .nIn(64 * 9 * 9) // Flattened input size (matches output size of previous layer)
//                            .nOut(512) // Number of neurons
//                            .activation(Activation.RELU) // Activation function
//                            .build()).layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT) // Output layer for classification
//                            .nIn(512) // Input matches output of previous dense layer
//                            .nOut(5) // 5 possible actions (no-op, up, down, left, right)
//                            .activation(Activation.SOFTMAX) // Softmax for probabilities
//                            .build()).setInputType(InputType.convolutional(84, 84, 3, CNN2DFormat.NHWC)) // Input type: 84x84 RGB images
//                    .build();

            var imgHeight = 84;
            var imgWidth = 84;

            MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                    .seed(123)
                    .updater(new Adam(0.00025))
                    .list()
                    .layer(0, new ConvolutionLayer.Builder(8, 8)
                            .stride(4, 4)
                            .nIn(3)  // Number of input channels
                            .nOut(32)
                            .activation(Activation.RELU)
                            .dataFormat(CNN2DFormat.NHWC)  // Set data format to NHWC (channels last)
                            .build())
                    .layer(1, new ConvolutionLayer.Builder(4, 4)
                            .stride(2, 2)
                            .nIn(32)
                            .nOut(64)
                            .activation(Activation.RELU)
                            .dataFormat(CNN2DFormat.NHWC)  // Set data format to NHWC
                            .build())
                    .layer(2, new DenseLayer.Builder()
                            .nIn(64 * 9 * 9)
                            .nOut(512)
                            .activation(Activation.RELU)
                            .build())
                    .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                            .nIn(512)
                            .nOut(5)
                            .activation(Activation.SOFTMAX)
                            .build())
                    .setInputType(InputType.convolutional(84, 84, 3, CNN2DFormat.NHWC))  // Set input type to NHWC
                    .build();

            model = new MultiLayerNetwork(config);
            model.init();
            System.out.println("Created new model");
        }

//        model.setListeners(new TrainingListener());


        var programOption = "play";
        switch (programOption) {
            case "gather":
                gatherTrainingData();
                break;
            case "manual_training":
                RLTrainer trainer = new RLTrainer();
                trainer.trainOnData();
                ModelSerializer.writeModel(model, MODEL_PATH, true);
                break;
            case "automatic_training":
                var data = loadAllFromFolder("src/main/resources/training/");
                data.forEach(game -> {
                    int amountOfSteps = game.size();
                    game.forEach(trainingData -> {
                        var reward = -1.0;
                        if (trainingData.getReward() > 0) {
                            reward = calculateReward(amountOfSteps, minSteps, maxSteps, true);
                        }
                        train(trainingData.getState(), trainingData.getAction(), reward);
                    });
                });
                ModelSerializer.writeModel(model, MODEL_PATH, true);
                break;
            case "enhance":
                ImageFlipper.enhanceDataSet();
                break;
            case "play":
                int numberOfInstances = 1; // Number of parallel game instances
                ExecutorService executor = Executors.newFixedThreadPool(numberOfInstances);

                for (int count = 1; count <= numberOfInstances; count++) {
                    int gameId = count; // Capture the game ID for the thread
                    executor.submit(() -> {
                        try {
                            playGame(gameId);
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
                break;
        }
    }

    public static void playGame(int instanceNumber) throws IOException, InterruptedException {
        BrowserManager browserManager = new BrowserManager();
        browserManager.startBrowser(instanceNumber);
        GameHandler gameHandler = new GameHandler(browserManager.getDriver());
        gameHandler.setupInitialGameState();

        var driver = browserManager.getDriver();
        var isGameOver = false;


        GameOverDetector gameOverDetector = new GameOverDetector();


        var game = driver.findElement(By.id("game"));
        var rect = game.getRect();

        if (instanceNumber > 1) {
            var xPosition = rect.x + (516 * (instanceNumber - 1));
            rect = new Rectangle(xPosition, rect.y, rect.height, rect.width);
        }

        LinkedList<TrainingData> currentGameData = new LinkedList<>();
        var startTime = System.currentTimeMillis();

//        double epsilon = 0.229; // Start with full exploration
        double epsilon = 0.1; // Start with full exploration
        double minEpsilon = 0.01; // Minimum eploration rate
        double decayRate = 0.995; // How quickly epsilon decreases
        var gameCount = 1;
        var playing = true;
        while (playing) {
            var screenshot = ScreenshotUtil.takeScreenshot(rect);
            // ScreenshotUtil.saveScreenshot(screenshot, "src/main/resources/screenshots/instance-" + instanceCount + ".png");
            var state = preprocessScreenshot(screenshot);

            // Predict action probabilities for the current state
            var startProb = System.currentTimeMillis();
            INDArray probabilities = model.output(state);

            // Choose the action with the highest probability
//            int action = Nd4j.argMax(probabilities).getInt(0);


//            int action = epsilonGreedyActionSelection(probabilities, epsilon);
            int action = Nd4j.argMax(probabilities).getInt(0);


//            int action = sampleActionFromProbabilities(probabilities);


            if (isGameOver) {
                gameHandler.performAction(99);
                isGameOver = false;
                startTime = System.currentTimeMillis();
                gameCount++;
                if (gameCount % 10 == 0) {
                    epsilon = Math.max(minEpsilon, epsilon * decayRate);
                }
//                if (epsilon == minEpsilon) {
//                    playing = false;
//                }
                continue;
            }

            var reward = gameHandler.performAction(action);

            // Store the training data
            currentGameData.add(new TrainingData(state, action, reward));

            // Check if the game is over and punish the last 5 actions if it is
            isGameOver = gameOverDetector.detectGameOverWithTemplate(screenshot);
            if (isGameOver) {
                Map<String, Integer> actionMap = new HashMap<>();
                var endTime = System.currentTimeMillis();
                var totalDuration = endTime - startTime;

                long minutes = totalDuration / 60000; // Calculate the number of minutes
                long seconds = (totalDuration % 60000) / 1000; // Calculate the remaining seconds
                long totalSeconds = totalDuration / 1000; // Calculate the remaining seconds

                System.out.printf(
                        "Instance #%d | Games Played: %d | Current Epsilon: %.4f | Played for: %02d:%02d%n",
                        instanceNumber, gameCount, epsilon, minutes, seconds
                );

//                System.out.printf("%02d:%02d%n", minutes, seconds); // Print in MM:SS format
//                System.out.println("Current epsilon: " + epsilon);
                var size = currentGameData.size();
                var startToPunishIndex = Math.max(size - 12, 0);
                for (int i = 0; i < size; i++) {
                    var calculatedReward = 0.0;
                    if (i >= startToPunishIndex) {
                        calculatedReward = -5;
                    } else {
                        calculatedReward = totalSeconds * 0.01;
                    }
                    // clip reward betweeen -1 and 1 for normalization
                    calculatedReward = Math.max(-1, Math.min(1, calculatedReward));
                    var step = currentGameData.get(i);
                    step.setReward(calculatedReward);



switch (step.getAction()){
    case 0:
        actionMap.put("noop", actionMap.getOrDefault("noop", 0) + 1);
        break;
    case 1:
        actionMap.put("up", actionMap.getOrDefault("up", 0) + 1);
        break;
    case 2:
        actionMap.put("left", actionMap.getOrDefault("left", 0) + 1);
        break;
    case 3:
        actionMap.put("right", actionMap.getOrDefault("right", 0) + 1);
        break;
    case 4:
        actionMap.put("down", actionMap.getOrDefault("down", 0) + 1);
        break;
}

                    train(step.getState(), step.getAction(), step.getReward());
                }
                System.out.println("Actions taken during the game:");
                for (Map.Entry<String, Integer> entry : actionMap.entrySet()) {
                    System.out.printf("Action: %s | Count: %d%n", entry.getKey(), entry.getValue());
                }
                ModelSerializer.writeModel(model, MODEL_PATH, true);
                save(currentGameData, "src/main/resources/training/" + instanceNumber + "_" + System.currentTimeMillis() + ".dat");
                currentGameData.clear();
            }
        }
    }

    private static int epsilonGreedyActionSelection(INDArray probabilities, double epsilon) {
        // Generate a random value between 0 and 1
        Random random = new Random();
        double randValue = random.nextDouble();
        if (randValue < epsilon) {
            // Exploration! choose a random action
            System.out.println("Epsilon greedy");
            return random.nextInt((int) probabilities.length());
        } else {
            return Nd4j.argMax(probabilities).getInt(0);
        }

    }

    public static int sampleActionFromProbabilities(INDArray probabilities) {
        // Generate a random value between 0 and 1
        Random random = new Random();
        double randValue = random.nextDouble();

        // Sample an action based on the probabilities
        double cumulative = 0.0;
        for (int i = 0; i < probabilities.columns(); i++) {
            cumulative += probabilities.getDouble(i);
            if (randValue < cumulative) {
                return i;  // Return the selected action index
            }
        }

        // Fallback in case no action is selected
        return probabilities.columns() - 1;
    }


    public static double[] computeDiscountedRewards(double[] rewards, double gamma) {
        int n = rewards.length;
        double[] discountedRewards = new double[n];
        double cumulativeReward = 0.0;

        // Compute the discounted rewards in reverse order
        for (int t = n - 1; t >= 0; t--) {
            cumulativeReward = rewards[t] + gamma * cumulativeReward;
            discountedRewards[t] = cumulativeReward;
        }

        return discountedRewards;
    }


    public static void train(INDArray state, int action, double reward) {
        // Parameters
        double rewardScaling = 1.0; // Scale rewards for numerical stability

        // Get the predicted probabilities for the current state
        INDArray predictions = model.output(state);

        // Create the target probability distribution
        INDArray targetDistribution = Nd4j.zeros(predictions.shape()); // Same shape as predictions
        targetDistribution.putScalar(0, action, reward * rewardScaling); // Set the probability for the taken action

        // Normalize the target distribution to sum to 1
        targetDistribution = softmax(targetDistribution);

        // Train the model: input state and target distribution
        model.fit(state, targetDistribution);
    }

    public static void gatherTrainingData() throws IOException, InterruptedException {
        BrowserManager browserManager = new BrowserManager();
        browserManager.startBrowser(1);
        GameHandler gameHandler = new GameHandler(browserManager.getDriver());
        gameHandler.setupInitialGameState();
        var gameOverDetector = new GameOverDetector();

        var driver = browserManager.getDriver();
        var game = driver.findElement(By.id("game"));
        var rect = game.getRect();
        KeyboardUtil keyboardUtil = new KeyboardUtil();

        var isGameOver = false;
        var gameId = 7;
        while (true) {
            var screenshot = ScreenshotUtil.takeScreenshot(rect);
            String key = keyboardUtil.listen();
            saveAndSortScreenshot(key, rect, gameId);

//            ScreenshotUtil.saveScreenshot(screenshot, "src/main/resources/screenshots/instance-" + instanceCount + ".png");
            // Check if the game is over and punish the last 5 actions if it is
            isGameOver = gameOverDetector.detectGameOverWithTemplate(screenshot);


            if (isGameOver) {
                gameHandler.performAction(99);
                isGameOver = false;
                gameId++;
                continue;
            }
        }
    }

    public static void saveAndSortScreenshot(String key, Rectangle rect, int gameId) throws IOException {
        var newState = ScreenshotUtil.preprocessScreenshot(ScreenshotUtil.takeScreenshot(rect));

        String folderName = "src/main/resources/screenshots/" + gameId;
        File folder = new File(folderName);
        if (!folder.exists()) folder.mkdirs();
        String fileName = folderName + "/frame_" + System.currentTimeMillis() + "_action_" + key + ".png";

        ScreenshotUtil.saveINDArrayAsImage(newState, fileName);
    }

    public static void save(LinkedList<TrainingData> data, String fileName) {

        String folderName = "src/main/resources/training";
        File folder = new File(folderName);
        if (!folder.exists()) folder.mkdirs();

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double calculateReward(int steps, int minSteps, int maxSteps, boolean useLogistic) {
        double reward;
        if (useLogistic) {
            double k = 0.05; // Steepness of the logistic curve
            int midpoint = (minSteps + maxSteps) / 2;
            reward = (2 / (1 + Math.exp(-k * (steps - midpoint)))) - 1;
        } else {
            // Linear scaling
            reward = 2.0 * (steps - minSteps) / (maxSteps - minSteps) - 1;
        }

        // Clip reward between -1 and 1
        return Math.max(-1, Math.min(1, reward));
    }

    public static List<LinkedList<TrainingData>> loadAllFromFolder(String folderPath) {
        List<LinkedList<TrainingData>> allTrainingData = new ArrayList<>();
        File folder = new File(folderPath);

        // Check if the folder exists and is a directory
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Invalid folder path: " + folderPath);
            return allTrainingData;
        }

        // Get all files in the folder
        File[] files = folder.listFiles();

        if (files != null) {
            var i = 0;
            System.out.println("Loading " + files.length + " training data");
            for (File file : files) {
                printProgressBar(i, files.length);
                i++;
                if (file.isFile()) { // Ensure it's a file, not a subdirectory
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                        @SuppressWarnings("unchecked")
                        LinkedList<TrainingData> data = (LinkedList<TrainingData>) ois.readObject();
                        if (minSteps == 0) {
                            minSteps = data.size();
                            maxSteps = data.size();
                        }
                        minSteps = Math.min(minSteps, data.size());
                        maxSteps = Math.max(maxSteps, data.size());

                        allTrainingData.add(data);
                    } catch (Exception e) {
                        System.err.println("Failed to load file: " + file.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
        return allTrainingData;
    }

}
