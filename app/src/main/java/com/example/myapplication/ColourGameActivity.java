package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class ColourGameActivity extends AppCompatActivity {

    public static final String EXTRA_SURVIVAL = "IS_SURVIVAL";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final View[] panels = new View[2];
    private final TextView[] scoreViews = new TextView[2];
    private final TextView[] wordViews = new TextView[2];
    private final TextView[] feedbackViews = new TextView[2];
    private final Button[][] answerButtons = new Button[2][4];

    private final int[] scores = new int[2];
    private final boolean[] answered = new boolean[2];
    private final String[] feedback = new String[2];

    private TextView timerView;
    private Button pauseButton;
    private AlertDialog activeDialog;

    private ColourQuestion question;
    private String roundId;

    private boolean survival;
    private boolean running;
    private boolean paused;
    private boolean gameOver;
    private boolean roundLocked;

    private long timeLeft;
    private long deadline;
    private long nextQuestionAt;
    private long questionDelay;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running || paused || gameOver) {
                return;
            }

            captureTime();

            if (timeLeft <= 0) {
                finishGame();
                return;
            }

            if (roundLocked && questionDelay <= 0) {
                generateQuestion();
                render();
            } else {
                renderTimer();
            }

            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colour_game);

        survival = getIntent().getBooleanExtra(EXTRA_SURVIVAL, true);

        timerView = findViewById(R.id.tvColourTimer);
        pauseButton = findViewById(R.id.btnColourPause);

        panels[0] = findViewById(R.id.layoutColourPlayerOne);
        panels[1] = findViewById(R.id.layoutColourPlayerTwo);

        panels[0].setRotation(180f);

        int[] buttonIds = {
                R.id.btnColourRed,
                R.id.btnColourBlue,
                R.id.btnColourGreen,
                R.id.btnColourPurple
        };

        ColourQuestion.Ink[] choices = ColourQuestion.Ink.values();

        for (int i = 0; i < 2; i++) {
            final int player = i;

            TextView title = panels[i].findViewById(
                    R.id.tvColourPlayerTitle
            );

            title.setText(
                    survival ? "COLOUR CLASH • SURVIVAL"
                            : "PLAYER " + (i + 1)
            );

            scoreViews[i] = panels[i].findViewById(
                    R.id.tvColourPlayerScore
            );

            wordViews[i] = panels[i].findViewById(
                    R.id.tvColourWord
            );

            feedbackViews[i] = panels[i].findViewById(
                    R.id.tvColourFeedback
            );

            for (int j = 0; j < buttonIds.length; j++) {
                final ColourQuestion.Ink choice = choices[j];

                answerButtons[i][j] = panels[i].findViewById(
                        buttonIds[j]
                );

                answerButtons[i][j].setOnClickListener(
                        v -> answer(player, choice)
                );
            }
        }

        if (savedInstanceState == null) {
            resetGame();
        } else {
            restoreGame(savedInstanceState);
        }

        pauseButton.setOnClickListener(v -> pauseGame());
        render();
    }

    private void resetGame() {
        stopClock();

        roundId = UUID.randomUUID().toString();
        timeLeft = survival ? 30000L : 60000L;

        scores[0] = 0;
        scores[1] = 0;

        paused = false;
        gameOver = false;

        generateQuestion();
    }

    private void generateQuestion() {
        question = ColourQuestion.generate(question);

        roundLocked = false;
        questionDelay = 0;

        for (int i = 0; i < 2; i++) {
            answered[i] = false;
            feedback[i] = "Choose the ink colour, not the word!";
        }
    }

    private void answer(int player, ColourQuestion.Ink choice) {
        if (!running || paused || gameOver || roundLocked
                || answered[player] || (survival && player == 0)) {
            return;
        }

        captureTime();

        if (timeLeft <= 0) {
            finishGame();
            return;
        }

        boolean correct = question.isCorrect(choice);
        answered[player] = true;

        if (correct) {
            scores[player] += 10;

            if (survival) {
                timeLeft += 2000;
                feedback[player] = "Correct! +10 points and +2s.";
            } else {
                feedback[player] = "Correct! +10 points.";
                feedback[1 - player] =
                        "Player " + (player + 1) + " scored!";
            }

            scheduleNextQuestion();

        } else {
            timeLeft = Math.max(0, timeLeft - 3000);

            if (survival) {
                feedback[player] = "Wrong! Ink was "
                        + question.ink.label + ". -3s.";

                scheduleNextQuestion();

            } else {
                feedback[player] = "Wrong! Shared timer -3s.";

                if (answered[0] && answered[1]) {
                    feedback[0] = "Both missed. Ink was "
                            + question.ink.label + ".";
                    feedback[1] = feedback[0];

                    scheduleNextQuestion();
                } else {
                    feedback[1 - player] =
                            "Your opponent missed. Your chance!";
                }
            }
        }

        deadline = SystemClock.elapsedRealtime() + timeLeft;

        if (timeLeft <= 0) {
            finishGame();
        } else {
            render();
        }
    }

    private void scheduleNextQuestion() {
        roundLocked = true;
        questionDelay = 700L;
        nextQuestionAt = SystemClock.elapsedRealtime() + questionDelay;
    }

    private void render() {
        for (int i = 0; i < 2; i++) {
            if (survival && i == 0) {
                panels[i].setVisibility(View.GONE);
            } else {
                panels[i].setVisibility(
                        paused ? View.INVISIBLE : View.VISIBLE
                );
            }

            scoreViews[i].setText("Score: " + scores[i]);

            wordViews[i].setText(question.word.label);
            wordViews[i].setTextColor(question.ink.color);

            // Include the ink colour for screen-reader users.
            wordViews[i].setContentDescription(
                    "Word " + question.word.label
                            + ", ink colour " + question.ink.label
            );

            feedbackViews[i].setText(feedback[i]);

            boolean enabled = !paused && !gameOver
                    && !roundLocked && !answered[i]
                    && (!survival || i == 1);

            for (Button button : answerButtons[i]) {
                button.setEnabled(enabled);
                button.setAlpha(enabled ? 1f : 0.5f);
            }
        }

        pauseButton.setEnabled(!gameOver);
        renderTimer();
    }

    private void renderTimer() {
        timerView.setText(
                "TIME: " + ((timeLeft + 999) / 1000) + "s"
        );
    }

    private void startClock() {
        if (running || paused || gameOver) {
            return;
        }

        if (timeLeft <= 0) {
            finishGame();
            return;
        }

        long now = SystemClock.elapsedRealtime();

        deadline = now + timeLeft;

        if (roundLocked) {
            nextQuestionAt = now + questionDelay;
        }

        running = true;
        handler.post(ticker);
    }

    private void captureTime() {
        if (!running) {
            return;
        }

        long now = SystemClock.elapsedRealtime();

        timeLeft = Math.max(0, deadline - now);

        if (roundLocked) {
            questionDelay = Math.max(0, nextQuestionAt - now);
        }
    }

    private void stopClock() {
        captureTime();
        running = false;
        handler.removeCallbacks(ticker);
    }

    private void pauseGame() {
        if (gameOver) {
            return;
        }

        stopClock();

        if (timeLeft <= 0) {
            finishGame();
            return;
        }

        paused = true;
        render();
        showPauseDialog();
    }

    private void showPauseDialog() {
        closeDialog();

        String instructions = survival
                ? "Choose the ink colour."
                + "\n\nCorrect: +10 points and +2 seconds."
                + "\nWrong: -3 seconds."
                : "First correct answer earns 10 points."
                + "\n\nAfter a wrong answer, the opponent can try."
                + "\nEach mistake removes 3 shared seconds.";

        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Colour Clash paused")
                .setMessage(instructions)
                .setCancelable(false)
                .setPositiveButton("Resume", (dialog, which) -> {
                    activeDialog = null;
                    paused = false;
                    render();
                    startClock();
                })
                .setNeutralButton("Restart", (dialog, which) -> {
                    activeDialog = null;
                    restartGame();
                })
                .setNegativeButton(
                        "Exit",
                        (dialog, which) -> finish()
                )
                .create();

        activeDialog.show();
    }

    private void finishGame() {
        if (gameOver) {
            return;
        }

        stopClock();

        timeLeft = 0;
        gameOver = true;
        paused = false;

        feedback[0] = "Time is up!";
        feedback[1] = "Time is up!";

        GameDatabaseHelper.getInstance(getApplicationContext())
                .saveResult(
                        roundId,
                        "COLOUR",
                        survival,
                        scores[0],
                        scores[1],
                        System.currentTimeMillis(),
                        success -> {
                            if (!success) {
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Could not save this score.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );

        render();
        showResultDialog();
    }

    private void showResultDialog() {
        closeDialog();

        String title;
        String message;

        if (survival) {
            title = "Survival finished!";
            message = "Score: " + scores[1]
                    + "\nCorrect answers: " + scores[1] / 10;
        } else {
            if (scores[0] > scores[1]) {
                title = "Player 1 wins!";
            } else if (scores[1] > scores[0]) {
                title = "Player 2 wins!";
            } else {
                title = "Match drawn!";
            }

            message = "Player 1: " + scores[0] + " points"
                    + "\nPlayer 2: " + scores[1] + " points";
        }

        activeDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Play again", (dialog, which) -> {
                    activeDialog = null;
                    restartGame();
                })
                .setNegativeButton(
                        "Back to setup",
                        (dialog, which) -> finish()
                )
                .create();

        activeDialog.show();
    }

    private void restartGame() {
        closeDialog();
        resetGame();
        render();
        startClock();
    }

    private void closeDialog() {
        if (activeDialog != null) {
            activeDialog.dismiss();
            activeDialog = null;
        }
    }

    private void restoreGame(Bundle state) {
        roundId = state.getString("roundId");
        timeLeft = state.getLong("timeLeft");
        gameOver = state.getBoolean("gameOver");
        roundLocked = state.getBoolean("roundLocked");
        questionDelay = state.getLong("questionDelay");

        question = new ColourQuestion(
                ColourQuestion.Ink.valueOf(state.getString("word")),
                ColourQuestion.Ink.valueOf(state.getString("ink"))
        );

        for (int i = 0; i < 2; i++) {
            scores[i] = state.getInt("score" + i);
            answered[i] = state.getBoolean("answered" + i);
            feedback[i] = state.getString(
                    "feedback" + i,
                    "Choose the ink colour!"
            );
        }

        paused = !gameOver;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gameOver) {
            showResultDialog();
        } else if (paused) {
            render();
            showPauseDialog();
        } else {
            startClock();
        }
    }

    @Override
    protected void onPause() {
        stopClock();

        if (!gameOver) {
            paused = true;
            render();
        }

        closeDialog();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (gameOver) {
            finish();
        } else {
            pauseGame();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        captureTime();

        outState.putString("roundId", roundId);
        outState.putLong("timeLeft", timeLeft);
        outState.putBoolean("gameOver", gameOver);
        outState.putBoolean("roundLocked", roundLocked);
        outState.putLong("questionDelay", questionDelay);
        outState.putString("word", question.word.name());
        outState.putString("ink", question.ink.name());

        for (int i = 0; i < 2; i++) {
            outState.putInt("score" + i, scores[i]);
            outState.putBoolean("answered" + i, answered[i]);
            outState.putString("feedback" + i, feedback[i]);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        closeDialog();
        super.onDestroy();
    }
}