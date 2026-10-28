package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MathRushActivity extends AppCompatActivity {

    public static final String EXTRA_SURVIVAL = "IS_SURVIVAL";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean survival;
    private boolean paused = false;
    private boolean running = false;
    private boolean gameOver = false;
    private boolean roundLocked = false;
    private boolean p1Answered = false;
    private boolean p2Answered = false;

    private int p1Score = 0;
    private int p2Score = 0;

    private String roundId;

    private long remaining;
    private long deadline;
    private long roundDelay = 0;
    private long nextRoundAt;

    private String questionText;
    private int[] answers;
    private int correctIndex;
    private String feedback = "Tap the correct answer";

    private TextView tvP1Score, tvP2Score;
    private TextView tvP1Question, tvP2Question;
    private TextView tvP1Feedback, tvP2Feedback;
    private TextView tvTimer;
    private Button btnPause;

    private Button[] p1Buttons;
    private Button[] p2Buttons;

    private AlertDialog activeDialog;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running || paused || gameOver) return;

            captureTime();

            if (remaining <= 0) {
                finishGame();
                return;
            }

            if (roundLocked && roundDelay <= 0) {
                generateQuestion();
                renderRound();
            }

            renderTimer();
            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_rush);

        survival = getIntent().getBooleanExtra(EXTRA_SURVIVAL, true);

        tvP1Score = findViewById(R.id.tvMathP1Score);
        tvP2Score = findViewById(R.id.tvMathP2Score);
        tvP1Question = findViewById(R.id.tvMathP1Question);
        tvP2Question = findViewById(R.id.tvMathP2Question);
        tvP1Feedback = findViewById(R.id.tvMathP1Feedback);
        tvP2Feedback = findViewById(R.id.tvMathP2Feedback);
        tvTimer = findViewById(R.id.tvMathTimer);
        btnPause = findViewById(R.id.btnMathPause);

        p1Buttons = new Button[]{
                findViewById(R.id.btnMathP1A),
                findViewById(R.id.btnMathP1B),
                findViewById(R.id.btnMathP1C),
                findViewById(R.id.btnMathP1D)
        };

        p2Buttons = new Button[]{
                findViewById(R.id.btnMathP2A),
                findViewById(R.id.btnMathP2B),
                findViewById(R.id.btnMathP2C),
                findViewById(R.id.btnMathP2D)
        };

        findViewById(R.id.layoutMathP1).setVisibility(
                survival ? View.GONE : View.VISIBLE);

        TextView tvMode = findViewById(R.id.tvMathMode);
        tvMode.setText(survival
                ? "MATH RUSH • SURVIVAL"
                : "MATH RUSH • DUEL");

        for (int i = 0; i < 4; i++) {
            final int option = i;

            p1Buttons[i].setOnClickListener(
                    v -> answerQuestion(1, option));

            p2Buttons[i].setOnClickListener(
                    v -> answerQuestion(2, option));
        }

        btnPause.setOnClickListener(v -> pauseGame());

        if (savedInstanceState == null) {
            resetRound();
        } else {
            restoreGame(savedInstanceState);
        }

        renderRound();
    }

    private void resetRound() {
        handler.removeCallbacks(ticker);

        running = false;
        paused = false;
        gameOver = false;
        p1Score = 0;
        p2Score = 0;

        roundId = java.util.UUID.randomUUID().toString();

        // Survival starts with 30 seconds; Duel with 60 seconds.
        remaining = survival ? 30000 : 60000;

        generateQuestion();
    }

    private void generateQuestion() {
        int difficultyScore = survival
                ? p2Score : Math.max(p1Score, p2Score);

        MathQuestion question = MathQuestion.generate(difficultyScore);

        questionText = question.text;
        answers = question.options;
        correctIndex = question.correctIndex;

        roundLocked = false;
        roundDelay = 0;
        p1Answered = false;
        p2Answered = false;
        feedback = "Tap the correct answer";
    }

    private void renderRound() {
        tvP1Score.setText("PLAYER 1 • SCORE: " + p1Score);

        tvP2Score.setText(survival
                ? "SCORE: " + p2Score
                : "PLAYER 2 • SCORE: " + p2Score);

        tvP1Question.setText(questionText);
        tvP2Question.setText(questionText);

        tvP1Feedback.setText(feedback);
        tvP2Feedback.setText(feedback);

        boolean available = !paused && !gameOver && !roundLocked;

        for (int i = 0; i < 4; i++) {
            String label = String.valueOf(answers[i]);

            p1Buttons[i].setText(label);
            p2Buttons[i].setText(label);

            p1Buttons[i].setEnabled(
                    available && !survival && !p1Answered);

            p2Buttons[i].setEnabled(
                    available && !p2Answered);
        }

        btnPause.setEnabled(!gameOver);
        renderTimer();
    }

    private void renderTimer() {
        if (gameOver) {
            tvTimer.setText("TIME UP!");
        } else {
            long seconds = (Math.max(0, remaining) + 999) / 1000;
            tvTimer.setText("TIME: " + seconds + "s");
        }
    }

    private void startClock() {
        if (running || paused || gameOver) return;

        if (remaining <= 0) {
            finishGame();
            return;
        }

        long now = SystemClock.elapsedRealtime();

        deadline = now + remaining;

        if (roundLocked) {
            nextRoundAt = now + roundDelay;
        }

        running = true;
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    private void captureTime() {
        if (!running) return;

        long now = SystemClock.elapsedRealtime();

        remaining = Math.max(0, deadline - now);

        if (roundLocked) {
            roundDelay = Math.max(0, nextRoundAt - now);
        }
    }

    private void stopClock() {
        captureTime();
        running = false;
        handler.removeCallbacks(ticker);
    }

    private void answerQuestion(int player, int option) {
        if (!running || paused || gameOver || roundLocked) return;
        if (player == 1 && (survival || p1Answered)) return;
        if (player == 2 && p2Answered) return;

        captureTime();

        if (remaining <= 0) {
            finishGame();
            return;
        }

        boolean correct = option == correctIndex;

        if (survival) {
            if (correct) {
                p2Score += 10;
                remaining += 5000;
                feedback = "Correct! +10 points • +5 seconds";
            } else {
                remaining = Math.max(0, remaining - 5000);
                feedback = "Wrong! Answer: " + answers[correctIndex]
                        + " • −5 seconds";
            }

            scheduleNextQuestion();

        } else {
            if (player == 1) {
                p1Answered = true;
            } else {
                p2Answered = true;
            }

            if (correct) {
                if (player == 1) {
                    p1Score += 10;
                } else {
                    p2Score += 10;
                }

                feedback = "Player " + player + " scores +10!";
                scheduleNextQuestion();

            } else {
                remaining = Math.max(0, remaining - 5000);

                feedback = "Player " + player
                        + " missed! Opponent can answer. −5s";

                if (p1Answered && p2Answered) {
                    feedback = "Both missed! Answer: "
                            + answers[correctIndex];

                    scheduleNextQuestion();
                }
            }
        }

        deadline = SystemClock.elapsedRealtime() + remaining;

        if (remaining <= 0) {
            finishGame();
        } else {
            renderRound();
        }
    }

    private void scheduleNextQuestion() {
        roundLocked = true;
        roundDelay = 700;
        nextRoundAt = SystemClock.elapsedRealtime() + roundDelay;
    }

    private void pauseGame() {
        if (gameOver || paused) return;

        stopClock();

        if (remaining <= 0) {
            finishGame();
            return;
        }

        paused = true;
        renderRound();
        showPauseDialog();
    }

    private void showPauseDialog() {
        if (isFinishing()) return;
        if (activeDialog != null && activeDialog.isShowing()) return;

        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Game paused")
                .setMessage("Take a break. Your time and scores are saved "
                        + "for this round.")
                .setCancelable(false)
                .setPositiveButton("Resume", (dialog, which) -> {
                    activeDialog = null;
                    paused = false;
                    renderRound();
                    startClock();
                })
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setNeutralButton("Restart", (dialog, which) -> restartGame())
                .show();
    }

    private void finishGame() {
        if (gameOver) return;

        stopClock();
        remaining = 0;
        gameOver = true;
        feedback = "Round finished";

        GameDatabaseHelper.getInstance(getApplicationContext())
                .saveResult(
                        roundId,
                        "MATH",
                        survival,
                        p1Score,
                        p2Score,
                        System.currentTimeMillis(),
                        success -> {
                            if (!success) {
                                android.widget.Toast.makeText(
                                        getApplicationContext(),
                                        "Could not save this score.",
                                        android.widget.Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );

        renderRound();
        showResultDialog();
    }

    private void showResultDialog() {
        if (isFinishing()) return;
        if (activeDialog != null && activeDialog.isShowing()) return;

        String title;
        String message;

        if (survival) {
            title = "Survival complete!";
            message = "Your score: " + p2Score
                    + "\nCorrect answers: " + (p2Score / 10);
        } else {
            if (p1Score == p2Score) {
                title = "It's a draw!";
            } else {
                title = p1Score > p2Score
                        ? "Player 1 wins!" : "Player 2 wins!";
            }

            message = "Player 1: " + p1Score
                    + "\nPlayer 2: " + p2Score;
        }

        activeDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Play again",
                        (dialog, which) -> restartGame())
                .setNegativeButton("Back to setup",
                        (dialog, which) -> finish())
                .show();
    }

    private void restartGame() {
        if (activeDialog != null) {
            activeDialog.dismiss();
            activeDialog = null;
        }

        resetRound();
        renderRound();
        startClock();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gameOver) {
            showResultDialog();
        } else if (paused) {
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
        }

        if (activeDialog != null) {
            activeDialog.dismiss();
            activeDialog = null;
        }

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

        outState.putInt("p1Score", p1Score);
        outState.putInt("p2Score", p2Score);
        outState.putLong("remaining", remaining);
        outState.putLong("roundDelay", roundDelay);

        outState.putBoolean("gameOver", gameOver);
        outState.putBoolean("roundLocked", roundLocked);
        outState.putBoolean("p1Answered", p1Answered);
        outState.putBoolean("p2Answered", p2Answered);

        outState.putString("question", questionText);
        outState.putIntArray("answers", answers);
        outState.putInt("correctIndex", correctIndex);
        outState.putString("feedback", feedback);
        outState.putString("roundId", roundId);

        super.onSaveInstanceState(outState);
    }

    private void restoreGame(Bundle state) {
        roundId = state.getString("roundId");

        if (roundId == null) {
            roundId = java.util.UUID.randomUUID().toString();
        }
        p1Score = state.getInt("p1Score");
        p2Score = state.getInt("p2Score");
        remaining = state.getLong("remaining");
        roundDelay = state.getLong("roundDelay");

        gameOver = state.getBoolean("gameOver");
        roundLocked = state.getBoolean("roundLocked");
        p1Answered = state.getBoolean("p1Answered");
        p2Answered = state.getBoolean("p2Answered");

        questionText = state.getString("question");
        answers = state.getIntArray("answers");
        correctIndex = state.getInt("correctIndex");
        feedback = state.getString("feedback");

        // Resume only when the player is ready after recreation.
        paused = !gameOver;
        running = false;
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);

        if (activeDialog != null) {
            activeDialog.dismiss();
            activeDialog = null;
        }

        super.onDestroy();
    }
}