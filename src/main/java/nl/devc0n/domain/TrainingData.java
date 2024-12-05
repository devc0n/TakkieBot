package nl.devc0n.domain;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.Serializable;

public class TrainingData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final INDArray state;
    private final int action;
    private double reward;

    public TrainingData(INDArray state, int action, double reward) {
        this.state = state;
        this.action = action;
        this.reward = reward;
    }

    public INDArray getState() {
        return state;
    }

    public int getAction() {
        return action;
    }

    public double getReward() {
        return reward;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }
}