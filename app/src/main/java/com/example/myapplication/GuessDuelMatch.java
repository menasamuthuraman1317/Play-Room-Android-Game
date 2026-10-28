package com.example.myapplication;

import android.os.Bundle;

public final class GuessDuelMatch {

    public static final int NO_WINNER = 0;
    public static final int DRAW = -1;

    private final GuessRound playerOneRound;
    private final GuessRound playerTwoRound;

    private int currentPlayer = 1;
    private int winner = NO_WINNER;

    private boolean waitingForNextTurn;
    private String lastMessage = "Ask a question or make a guess.";

    public GuessDuelMatch(
            String playerOneSecret,
            String playerTwoSecret
    ) {
        // Each player investigates the opponent's character.
        playerOneRound = new GuessRound(playerTwoSecret);
        playerTwoRound = new GuessRound(playerOneSecret);
    }

    private GuessDuelMatch(
            GuessRound playerOneRound,
            GuessRound playerTwoRound
    ) {
        this.playerOneRound = playerOneRound;
        this.playerTwoRound = playerTwoRound;
    }

    public GuessRound getCurrentRound() {
        return getRound(currentPlayer);
    }

    public GuessRound getRound(int player) {
        if (player == 1) {
            return playerOneRound;
        }

        if (player == 2) {
            return playerTwoRound;
        }

        throw new IllegalArgumentException("Player must be 1 or 2.");
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int getNextPlayer() {
        return currentPlayer == 1 ? 2 : 1;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean isWaitingForNextTurn() {
        return waitingForNextTurn;
    }

    public boolean isFinished() {
        return winner != NO_WINNER;
    }

    public int getWinner() {
        return winner;
    }

    public boolean ask(GuessQuestion question) {
        requireActiveTurn();

        boolean answer = getCurrentRound().ask(question);

        lastMessage = question.text
                + "\nAnswer: " + (answer ? "YES" : "NO");

        waitingForNextTurn = true;

        return answer;
    }

    public GuessRound.GuessResult guess(String characterId) {
        requireActiveTurn();

        GuessRound.GuessResult result =
                getCurrentRound().guess(characterId);

        String name = GuessCharacterBank
                .findById(characterId).name;

        switch (result) {
            case CORRECT:
                winner = currentPlayer;
                lastMessage = "Player " + currentPlayer
                        + " correctly guessed " + name + "!";
                break;

            case OUT_OF_GUESSES:
                winner = getNextPlayer();
                lastMessage = "Player " + currentPlayer
                        + " has no guesses left. Player "
                        + winner + " wins!";
                break;

            case INCORRECT:
                lastMessage = name
                        + " is not the secret character."
                        + "\nPass the phone for the next turn.";
                waitingForNextTurn = true;
                break;
        }

        return result;
    }

    // Called after the next player confirms they are ready.
    public void beginNextTurn() {
        if (isFinished() || !waitingForNextTurn) {
            return;
        }

        currentPlayer = getNextPlayer();
        waitingForNextTurn = false;

        lastMessage = "Player " + currentPlayer
                + ": ask a question or make a guess.";
    }

    public void finishOnTimeout() {
        if (isFinished()) {
            return;
        }

        winner = DRAW;
        waitingForNextTurn = false;
        lastMessage = "The shared timer expired. Match drawn!";
    }

    public int getScore(int player) {
        GuessRound playerRound = getRound(player);

        if (!isFinished() || winner != player) {
            return 0;
        }

        if (playerRound.hasWon()) {
            return playerRound.getScore();
        }

        // Opponent used all three guesses.
        return 50;
    }

    private void requireActiveTurn() {
        if (isFinished()) {
            throw new IllegalStateException("The match has finished.");
        }

        if (waitingForNextTurn) {
            throw new IllegalStateException(
                    "Pass the phone before the next action."
            );
        }
    }

    public Bundle saveState() {
        Bundle state = new Bundle();

        state.putBundle("playerOneRound", playerOneRound.saveState());
        state.putBundle("playerTwoRound", playerTwoRound.saveState());
        state.putInt("currentPlayer", currentPlayer);
        state.putInt("winner", winner);
        state.putBoolean("waitingForNextTurn", waitingForNextTurn);
        state.putString("lastMessage", lastMessage);

        return state;
    }

    public static GuessDuelMatch restoreState(Bundle state) {
        if (state == null) {
            throw new IllegalArgumentException("Match state is missing.");
        }

        GuessDuelMatch match = new GuessDuelMatch(
                GuessRound.restoreState(
                        state.getBundle("playerOneRound")
                ),
                GuessRound.restoreState(
                        state.getBundle("playerTwoRound")
                )
        );

        match.currentPlayer = state.getInt("currentPlayer", 1);
        match.winner = state.getInt("winner", NO_WINNER);

        match.waitingForNextTurn = state.getBoolean(
                "waitingForNextTurn", false
        );

        match.lastMessage = state.getString(
                "lastMessage",
                "Ask a question or make a guess."
        );

        return match;
    }
}