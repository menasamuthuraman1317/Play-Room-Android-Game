package com.example.myapplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class MathQuestion {

    private static final Random RANDOM = new Random();

    public final String text;
    public final int[] options;
    public final int correctIndex;

    private MathQuestion(
            String text,
            int[] options,
            int correctIndex
    ) {
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public static MathQuestion generate(int score) {
        int level = Math.min(3, Math.max(0, score / 50));
        int maximum = 10 + level * 10;

        // All four operations are available at every score.
        int operation = RANDOM.nextInt(4);

        int first;
        int second;
        int answer;
        String symbol;

        switch (operation) {
            case 0:
                first = 1 + RANDOM.nextInt(maximum);
                second = 1 + RANDOM.nextInt(maximum);
                answer = first + second;
                symbol = "+";
                break;

            case 1:
                first = 1 + RANDOM.nextInt(maximum);
                second = 1 + RANDOM.nextInt(maximum);

                // Keep subtraction answers non-negative.
                if (second > first) {
                    int temporary = first;
                    first = second;
                    second = temporary;
                }

                answer = first - second;
                symbol = "−";
                break;

            case 2:
                int factorLimit = 6 + level * 2;

                first = 2 + RANDOM.nextInt(factorLimit - 1);
                second = 2 + RANDOM.nextInt(factorLimit - 1);

                answer = first * second;
                symbol = "×";
                break;

            default:
                // Build a division question with an exact whole answer.
                second = 2 + RANDOM.nextInt(11);
                answer = 1 + RANDOM.nextInt(maximum);
                first = second * answer;
                symbol = "÷";
                break;
        }

        Set<Integer> uniqueOptions = new LinkedHashSet<>();
        uniqueOptions.add(answer);

        while (uniqueOptions.size() < 4) {
            int candidate = answer + RANDOM.nextInt(21) - 10;

            if (candidate >= 0) {
                uniqueOptions.add(candidate);
            }
        }

        List<Integer> shuffledOptions =
                new ArrayList<>(uniqueOptions);

        Collections.shuffle(shuffledOptions, RANDOM);

        int[] options = new int[4];

        for (int i = 0; i < options.length; i++) {
            options[i] = shuffledOptions.get(i);
        }

        return new MathQuestion(
                first + " " + symbol + " " + second + " = ?",
                options,
                shuffledOptions.indexOf(answer)
        );
    }
}