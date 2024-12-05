package nl.devc0n;

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.api.BaseTrainingListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

class TrainingListener extends BaseTrainingListener {
    @Override
    public void iterationDone(Model model, int iteration, int epoch) {
        MultiLayerNetwork net = (MultiLayerNetwork) model;

        // Log training loss
        double loss = net.score();
        System.out.println("Epoch: " + epoch + ", Iteration: " + iteration + ", Loss: " + loss);

        // Optionally, log gradients or weights
        INDArray gradients = net.gradient().gradient();
        double maxGradient = gradients.maxNumber().doubleValue();
        System.out.println("Max Gradient: " + maxGradient);
    }
}
