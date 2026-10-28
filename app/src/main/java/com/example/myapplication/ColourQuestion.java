package com.example.myapplication;

import android.graphics.Color;

import java.util.Random;

public final class ColourQuestion {

    public enum Ink {
        RED("RED", Color.rgb(198, 40, 40)),
        BLUE("BLUE", Color.rgb(21, 101, 192)),
        GREEN("GREEN", Color.rgb(46, 125, 50)),
        PURPLE("PURPLE", Color.rgb(123, 31, 162));

        public final String label;
        public final int color;

        Ink(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private static final Random RANDOM = new Random();

    public final Ink word;
    public final Ink ink;

    public ColourQuestion(Ink word, Ink ink) {
        if (word == null || ink == null) {
            throw new IllegalArgumentException(
                    "Both the word and ink colour are required."
            );
        }

        this.word = word;
        this.ink = ink;
    }

    public boolean isCorrect(Ink chosen) {
        return chosen == ink;
    }

    public static ColourQuestion generate(
            ColourQuestion previous
    ) {
        Ink[] colors = Ink.values();

        Ink nextWord;
        Ink nextInk;

        do {
            nextInk = colors[RANDOM.nextInt(colors.length)];

            // Choose a different word to create the challenge.
            do {
                nextWord = colors[RANDOM.nextInt(colors.length)];
            } while (nextWord == nextInk);

        } while (previous != null
                && previous.word == nextWord
                && previous.ink == nextInk);

        return new ColourQuestion(nextWord, nextInk);
    }
}