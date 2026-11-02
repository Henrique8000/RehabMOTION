package br.mackenzie.game;

public class PhaseResult {

    public final Phase phase;
    public final int score;
    public final int coins;
    public final int edgeCoins;
    public final int hits;
    public final float amplitudeFraction;
    public final float amplitudeDegrees;

    public PhaseResult(Phase phase, int score, int coins, int edgeCoins, int hits,
                       float amplitudeFraction, float amplitudeDegrees) {
        this.phase = phase;
        this.score = score;
        this.coins = coins;
        this.edgeCoins = edgeCoins;
        this.hits = hits;
        this.amplitudeFraction = amplitudeFraction;
        this.amplitudeDegrees = amplitudeDegrees;
    }
}
