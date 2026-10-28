package com.example.myapplication;

import android.os.Bundle;

public final class EscapeDuelMatch {

    private final EscapeRound[] rounds = new EscapeRound[2];
    private final int[] scores = new int[2];
    private final boolean[] completed = new boolean[2];
    private final long[] remainingTime = new long[2];

    private int currentPlayer = 0;

    public EscapeDuelMatch() {
        rounds[0] = new EscapeRound();

        // Copy the initial maze before either player moves.
        rounds[1] = EscapeRound.restoreState(
                rounds[0].saveState()
        );
    }

    private EscapeDuelMatch(
            EscapeRound playerOneRound,
            EscapeRound playerTwoRound
    ) {
        rounds[0] = playerOneRound;
        rounds[1] = playerTwoRound;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public EscapeRound getCurrentRound() {
        return rounds[currentPlayer];
    }

    public EscapeRound getRound(int player) {
        checkPlayer(player);
        return rounds[player];
    }

    public int getScore(int player) {
        checkPlayer(player);
        return scores[player];
    }

    public long getRemainingTime(int player) {
        checkPlayer(player);
        return remainingTime[player];
    }

    public boolean hasCompleted(int player) {
        checkPlayer(player);
        return completed[player];
    }

    public boolean isFinished() {
        return completed[0] && completed[1];
    }

    public void finishCurrentAttempt(long millisecondsLeft) {
        if (isFinished() || completed[currentPlayer]) {
            return;
        }

        EscapeRound round = getCurrentRound();

        // An unfinished maze only ends when its timer expires.
        if (!round.hasEscaped() && millisecondsLeft > 0) {
            throw new IllegalStateException(
                    "This player still has time to finish."
            );
        }

        long safeTime = Math.max(0, millisecondsLeft);
        remainingTime[currentPlayer] = safeTime;

        if (round.hasEscaped()) {
            int extraMoves = Math.max(
                    0,
                    round.getMoves() - round.getMinimumMoves()
            );

            int routeScore = Math.max(10, 100 - extraMoves * 5);
            int timeBonus = (int) (safeTime / 1000);

            scores[currentPlayer] = routeScore + timeBonus;
        } else {
            scores[currentPlayer] = 0;
        }

        completed[currentPlayer] = true;

        if (currentPlayer == 0) {
            currentPlayer = 1;
        }
    }

    public String getResultTitle() {
        if (!isFinished()) {
            return "Match in progress";
        }

        if (scores[0] > scores[1]) {
            return "Player 1 wins!";
        }

        if (scores[1] > scores[0]) {
            return "Player 2 wins!";
        }

        return "Match drawn!";
    }

    private static void checkPlayer(int player) {
        if (player < 0 || player > 1) {
            throw new IllegalArgumentException(
                    "Player index must be 0 or 1."
            );
        }
    }

    public Bundle saveState() {
        Bundle state = new Bundle();

        state.putInt("currentPlayer", currentPlayer);

        for (int i = 0; i < 2; i++) {
            state.putBundle("round" + i, rounds[i].saveState());
            state.putInt("score" + i, scores[i]);
            state.putBoolean("completed" + i, completed[i]);
            state.putLong("remainingTime" + i, remainingTime[i]);
        }

        return state;
    }

    public static EscapeDuelMatch restoreState(Bundle state) {
        if (state == null) {
            throw new IllegalArgumentException(
                    "Escape Duel state is missing."
            );
        }

        EscapeDuelMatch match = new EscapeDuelMatch(
                EscapeRound.restoreState(state.getBundle("round0")),
                EscapeRound.restoreState(state.getBundle("round1"))
        );

        match.currentPlayer = state.getInt("currentPlayer", 0);
        checkPlayer(match.currentPlayer);

        for (int i = 0; i < 2; i++) {
            match.scores[i] = state.getInt("score" + i);
            match.completed[i] = state.getBoolean("completed" + i);
            match.remainingTime[i] = state.getLong("remainingTime" + i);
        }

        return match;
    }
}