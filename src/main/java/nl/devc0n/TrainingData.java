package nl.devc0n;

import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.Serializable;

public class TrainingData implements Serializable {
    private static final long serialVersionUID = 1L;

    private INDArray state;
    private int action;
    private double reward;
    private INDArray nextState;

    public TrainingData(INDArray state, int action, double reward, INDArray nextState) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
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

    public INDArray getNextState() {
        return nextState;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }
}