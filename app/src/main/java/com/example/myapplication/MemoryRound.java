package com.example.myapplication;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MemoryRound {

    public enum FlipResult {
        IGNORED,
        FIRST_CARD,
        MATCH,
        MISMATCH
    }

    private static final String[] SYMBOLS = {
            "🍎", "🚀", "⭐", "🎈",
            "🐱", "🌻", "🐟", "🎲"
    };

    private final int[] cards;
    private final boolean[] matched;

    private int firstIndex = -1;
    private int secondIndex = -1;

    private int attempts;
    private int matchedPairs;

    public MemoryRound(int pairCount) {
        if (pairCount < 2 || pairCount > SYMBOLS.length) {
            throw new IllegalArgumentException(
                    "Choose between 2 and 8 pairs."
            );
        }

        List<Integer> deck = new ArrayList<>();

        for (int symbol = 0; symbol < pairCount; symbol++) {
            deck.add(symbol);
            deck.add(symbol);
        }

        Collections.shuffle(deck);

        cards = new int[deck.size()];
        matched = new boolean[deck.size()];

        for (int i = 0; i < deck.size(); i++) {
            cards[i] = deck.get(i);
        }
    }

    private MemoryRound(int[] cards, boolean[] matched) {
        this.cards = cards.clone();
        this.matched = matched.clone();
    }

    public FlipResult flip(int index) {
        checkIndex(index);

        if (isComplete()
                || hasPendingMismatch()
                || matched[index]
                || index == firstIndex) {
            return FlipResult.IGNORED;
        }

        if (firstIndex == -1) {
            firstIndex = index;
            return FlipResult.FIRST_CARD;
        }

        secondIndex = index;
        attempts++;

        if (cards[firstIndex] == cards[secondIndex]) {
            matched[firstIndex] = true;
            matched[secondIndex] = true;

            matchedPairs++;

            firstIndex = -1;
            secondIndex = -1;

            return FlipResult.MATCH;
        }

        // Keep both cards visible until the screen hides them.
        return FlipResult.MISMATCH;
    }

    public void hideMismatch() {
        if (hasPendingMismatch()) {
            firstIndex = -1;
            secondIndex = -1;
        }
    }

    public boolean hasPendingMismatch() {
        return firstIndex >= 0 && secondIndex >= 0;
    }

    public boolean isFaceUp(int index) {
        checkIndex(index);

        return matched[index]
                || index == firstIndex
                || index == secondIndex;
    }

    public boolean isMatched(int index) {
        checkIndex(index);
        return matched[index];
    }

    public String getSymbol(int index) {
        checkIndex(index);
        return SYMBOLS[cards[index]];
    }

    public int getCardCount() {
        return cards.length;
    }

    public int getPairCount() {
        return cards.length / 2;
    }

    public int getMatchedPairs() {
        return matchedPairs;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isComplete() {
        return matchedPairs == getPairCount();
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= cards.length) {
            throw new IllegalArgumentException(
                    "Invalid card position: " + index
            );
        }
    }
    public boolean revealMatchingPairFrom(
            MemoryRound source,
            int sourceIndex
    ) {
        source.checkIndex(sourceIndex);

        if (!source.matched[sourceIndex]) {
            return false;
        }

        int symbol = source.cards[sourceIndex];
        boolean changed = false;

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == symbol && !matched[i]) {
                matched[i] = true;
                changed = true;
            }
        }

        if (changed) {
            matchedPairs++;

            // Clear an unfinished selection if the revealed pair
            // includes one of this player's selected cards.
            if ((firstIndex >= 0 && matched[firstIndex])
                    || (secondIndex >= 0 && matched[secondIndex])) {
                firstIndex = -1;
                secondIndex = -1;
            }
        }

        return changed;
    }
    public Bundle saveState() {
        Bundle state = new Bundle();

        state.putIntArray("cards", cards.clone());
        state.putBooleanArray("matched", matched.clone());
        state.putInt("firstIndex", firstIndex);
        state.putInt("secondIndex", secondIndex);
        state.putInt("attempts", attempts);
        state.putInt("matchedPairs", matchedPairs);

        return state;
    }

    public static MemoryRound restoreState(Bundle state) {
        if (state == null) {
            throw new IllegalArgumentException(
                    "Memory round state is missing."
            );
        }

        int[] cards = state.getIntArray("cards");
        boolean[] matched = state.getBooleanArray("matched");

        if (cards == null || matched == null
                || cards.length != matched.length) {
            throw new IllegalArgumentException(
                    "Memory round state is incomplete."
            );
        }

        MemoryRound round = new MemoryRound(cards, matched);

        round.firstIndex = state.getInt("firstIndex", -1);
        round.secondIndex = state.getInt("secondIndex", -1);
        round.attempts = state.getInt("attempts", 0);
        round.matchedPairs = state.getInt("matchedPairs", 0);

        return round;
    }
}