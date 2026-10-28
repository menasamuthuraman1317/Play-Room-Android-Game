package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class MemoryGameActivity extends AppCompatActivity {

    private static final long START_TIME = 45000L;
    private static final long MISMATCH_DELAY = 900L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private MemoryRound round;
    private MemoryBoard board;
    private GridLayout grid;

    private TextView timerView;
    private TextView scoreView;
    private TextView progressView;
    private TextView feedbackView;
    private Button pauseButton;

    private AlertDialog activeDialog;

    private String roundId;
    private String feedback;
    private String resultMessage = "";

    private int score;

    private long timeLeft = START_TIME;
    private long deadline;
    private long hideDelay;
    private long hideAt;

    private boolean running;
    private boolean paused;
    private boolean gameOver;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running || paused || gameOver) {
                return;
            }

            captureTime();

            if (timeLeft <= 0) {
                finishRound(false);
                return;
            }

            if (round.hasPendingMismatch() && hideDelay <= 0) {
                round.hideMismatch();
                feedback = "Try again. Remember where those symbols were!";
                render();
            }

            renderTimer();
            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_game);

        timerView = findViewById(R.id.tvMemoryTimer);
        scoreView = findViewById(R.id.tvMemoryScore);
        progressView = findViewById(R.id.tvMemoryProgress);
        feedbackView = findViewById(R.id.tvMemoryFeedback);
        pauseButton = findViewById(R.id.btnMemoryPause);
        grid = findViewById(R.id.gridMemoryCards);

        TextView title = findViewById(R.id.tvMemoryTitle);
        title.setText("MEMORY DETECTIVE • SURVIVAL");

        board = new MemoryBoard(grid, this::flipCard);

        if (savedInstanceState == null) {
            resetRound();
        } else {
            round = MemoryRound.restoreState(
                    savedInstanceState.getBundle("round")
            );

            roundId = savedInstanceState.getString("roundId");
            score = savedInstanceState.getInt("score");
            timeLeft = savedInstanceState.getLong("timeLeft", START_TIME);
            hideDelay = savedInstanceState.getLong("hideDelay");
            gameOver = savedInstanceState.getBoolean("gameOver");

            feedback = savedInstanceState.getString(
                    "feedback", "Find the matching pairs."
            );

            resultMessage = savedInstanceState.getString(
                    "resultMessage", ""
            );

            paused = !gameOver;
            board.setRound(round);
        }

        pauseButton.setOnClickListener(v -> pauseGame());
        render();
    }

    private void resetRound() {
        stopClock();

        round = new MemoryRound(6);
        roundId = UUID.randomUUID().toString();

        score = 0;
        timeLeft = START_TIME;
        hideDelay = 0;
        hideAt = 0;

        paused = false;
        gameOver = false;
        resultMessage = "";

        feedback = "Tap two cards. Match: +10 points and +5s. "
                + "Wrong pair: -3s.";

        board.setRound(round);
    }

    private void flipCard(int index) {
        if (!running || paused || gameOver) {
            return;
        }

        captureTime();

        if (timeLeft <= 0) {
            finishRound(false);
            return;
        }

        MemoryRound.FlipResult result = round.flip(index);

        switch (result) {
            case IGNORED:
                return;

            case FIRST_CARD:
                feedback = "Now find the matching symbol.";
                break;

            case MATCH:
                score += 10;
                timeLeft += 5000;
                feedback = "Pair found! +10 points and +5 seconds.";
                break;

            case MISMATCH:
                timeLeft = Math.max(0, timeLeft - 3000);
                hideDelay = MISMATCH_DELAY;
                hideAt = SystemClock.elapsedRealtime() + hideDelay;
                feedback = "Not a match! -3 seconds.";
                break;
        }

        deadline = SystemClock.elapsedRealtime() + timeLeft;

        if (timeLeft <= 0) {
            finishRound(false);
        } else if (round.isComplete()) {
            finishRound(true);
        } else {
            render();
        }
    }

    private void render() {
        // Hide the board while paused so the cards cannot be studied.
        grid.setVisibility(paused ? View.INVISIBLE : View.VISIBLE);

        board.setInteractionEnabled(!paused && !gameOver);

        scoreView.setText("Score: " + score);

        progressView.setText(
                "Pairs: " + round.getMatchedPairs()
                        + " / " + round.getPairCount()
                        + " • Attempts: " + round.getAttempts()
        );

        feedbackView.setText(feedback);
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
            finishRound(false);
            return;
        }

        long now = SystemClock.elapsedRealtime();

        deadline = now + timeLeft;

        if (round.hasPendingMismatch()) {
            hideAt = now + hideDelay;
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

        if (round != null && round.hasPendingMismatch()) {
            hideDelay = Math.max(0, hideAt - now);
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
            finishRound(false);
            return;
        }

        paused = true;
        render();
        showPauseDialog();
    }

    private void showPauseDialog() {
        closeDialog();

        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Memory Detective paused")
                .setMessage(
                        "Find all six pairs before time runs out."
                                + "\n\nMatched pairs add time."
                                + " Wrong pairs subtract time."
                )
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

    private void finishRound(boolean won) {
        if (gameOver) {
            return;
        }

        stopClock();

        gameOver = true;
        paused = false;

        if (won) {
            int bonus = (int) (timeLeft / 1000);
            score += bonus;

            resultMessage = "You found every pair!"
                    + "\nTime bonus: " + bonus + " points";
        } else {
            resultMessage = "Time is up!"
                    + "\nYou found " + round.getMatchedPairs()
                    + " of " + round.getPairCount() + " pairs.";
        }

        feedback = resultMessage;

        GameDatabaseHelper.getInstance(getApplicationContext())
                .saveResult(
                        roundId,
                        "MEMORY",
                        true,
                        0,
                        score,
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

        activeDialog = new AlertDialog.Builder(this)
                .setTitle(
                        round.isComplete() ? "Victory!" : "Round finished"
                )
                .setMessage(
                        resultMessage
                                + "\n\nScore: " + score
                                + "\nAttempts: " + round.getAttempts()
                )
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
        resetRound();
        render();
        startClock();
    }

    private void closeDialog() {
        if (activeDialog != null) {
            activeDialog.dismiss();
            activeDialog = null;
        }
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

        outState.putBundle("round", round.saveState());
        outState.putString("roundId", roundId);
        outState.putInt("score", score);
        outState.putLong("timeLeft", timeLeft);
        outState.putLong("hideDelay", hideDelay);
        outState.putBoolean("gameOver", gameOver);
        outState.putString("feedback", feedback);
        outState.putString("resultMessage", resultMessage);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        closeDialog();
        super.onDestroy();
    }
}