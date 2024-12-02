package nl.devc0n;

import org.nd4j.linalg.api.ndarray.INDArray;

public class GameStep {
    private INDArray state;
    private int action;
    private double reward;

    public GameStep(INDArray state, int action, double reward) {
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
