package com.example.myapplication;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GuessRound {

    public static final int MAX_GUESSES = 3;

    public enum GuessResult {
        CORRECT,
        INCORRECT,
        OUT_OF_GUESSES
    }

    private final GuessCharacter secret;

    private final Set<String> remainingIds = new LinkedHashSet<>();
    private final Set<String> askedQuestionIds = new LinkedHashSet<>();

    private int wrongGuesses = 0;
    private boolean won = false;

    public GuessRound(String secretCharacterId) {
        secret = GuessCharacterBank.findById(secretCharacterId);

        for (GuessCharacter character : GuessCharacterBank.getAll()) {
            remainingIds.add(character.id);
        }
    }

    public boolean ask(GuessQuestion question) {
        if (isFinished()) {
            throw new IllegalStateException("This round has finished.");
        }

        if (question == null) {
            throw new IllegalArgumentException("Choose a question.");
        }

        if (hasAsked(question)) {
            throw new IllegalArgumentException(
                    "This question has already been answered."
            );
        }

        boolean answer = question.matches(secret);

        askedQuestionIds.add(question.id);

        // Use a copy so removal does not interrupt iteration.
        for (String id : new ArrayList<>(remainingIds)) {
            GuessCharacter candidate = GuessCharacterBank.findById(id);

            if (!question.keepsCandidate(candidate, answer)) {
                remainingIds.remove(id);
            }
        }

        return answer;
    }

    public GuessResult guess(String characterId) {
        if (isFinished()) {
            throw new IllegalStateException("This round has finished.");
        }

        if (!remainingIds.contains(characterId)) {
            throw new IllegalArgumentException(
                    "Choose a remaining character."
            );
        }

        if (secret.id.equals(characterId)) {
            won = true;
            return GuessResult.CORRECT;
        }

        wrongGuesses++;
        remainingIds.remove(characterId);

        if (wrongGuesses >= MAX_GUESSES) {
            return GuessResult.OUT_OF_GUESSES;
        }

        return GuessResult.INCORRECT;
    }

    public boolean hasAsked(GuessQuestion question) {
        return askedQuestionIds.contains(question.id);
    }

    public boolean isRemaining(String characterId) {
        return remainingIds.contains(characterId);
    }

    public int getRemainingCount() {
        return remainingIds.size();
    }

    public int getQuestionsAsked() {
        return askedQuestionIds.size();
    }

    public int getGuessesLeft() {
        return Math.max(0, MAX_GUESSES - wrongGuesses);
    }

    public boolean hasWon() {
        return won;
    }

    public boolean isFinished() {
        return won || wrongGuesses >= MAX_GUESSES;
    }

    public String getSecretId() {
        return secret.id;
    }

    public String getSecretName() {
        return secret.name;
    }

    public int getScore() {
        if (!won) {
            return 0;
        }

        // Fewer questions and mistakes earn more points.
        return Math.max(
                10,
                100 - getQuestionsAsked() * 5 - wrongGuesses * 20
        );
    }

    // Preserve progress when Android recreates the screen.
    public Bundle saveState() {
        Bundle state = new Bundle();

        state.putString("secretId", secret.id);
        state.putStringArrayList(
                "remainingIds",
                new ArrayList<>(remainingIds)
        );
        state.putStringArrayList(
                "askedQuestionIds",
                new ArrayList<>(askedQuestionIds)
        );
        state.putInt("wrongGuesses", wrongGuesses);
        state.putBoolean("won", won);

        return state;
    }

    public static GuessRound restoreState(Bundle state) {
        if (state == null) {
            throw new IllegalArgumentException("Round state is missing.");
        }

        String secretId = state.getString("secretId");

        ArrayList<String> remaining =
                state.getStringArrayList("remainingIds");

        ArrayList<String> asked =
                state.getStringArrayList("askedQuestionIds");

        if (secretId == null || remaining == null || asked == null) {
            throw new IllegalArgumentException("Round state is incomplete.");
        }

        GuessRound round = new GuessRound(secretId);

        round.remainingIds.clear();
        round.remainingIds.addAll(remaining);

        round.askedQuestionIds.addAll(asked);
        round.wrongGuesses = state.getInt("wrongGuesses", 0);
        round.won = state.getBoolean("won", false);

        return round;
    }
}