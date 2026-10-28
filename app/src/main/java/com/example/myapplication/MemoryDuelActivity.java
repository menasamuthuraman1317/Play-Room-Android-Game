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

public class MemoryDuelActivity extends AppCompatActivity {

    private static final long START_TIME = 60000L;
    private static final long MISMATCH_DELAY = 900L;

    private final Handler handler = new Handler(Looper.getMainLooper());


    private final MemoryRound[] rounds = new MemoryRound[2];
    private final MemoryBoard[] boards = new MemoryBoard[2];

    private final int[] playerScores = new int[2];

    private final View[] panels = new View[2];
    private final TextView[] scores = new TextView[2];
    private final TextView[] feedbackViews = new TextView[2];

    private final String[] feedback = new String[2];
    private final long[] hideDelay = new long[2];
    private final long[] hideAt = new long[2];

    private TextView timerView;
    private Button pauseButton;
    private AlertDialog activeDialog;

    private String roundId;
    private String resultMessage = "";

    private long timeLeft = START_TIME;
    private long deadline;

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
                finishMatch("The shared timer expired.");
                return;
            }

            boolean changed = false;

            for (int i = 0; i < 2; i++) {
                if (rounds[i].hasPendingMismatch()
                        && hideDelay[i] <= 0) {

                    rounds[i].hideMismatch();
                    feedback[i] = "Try again. Remember those positions!";
                    changed = true;
                }
            }

            if (changed) {
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
        setContentView(R.layout.activity_memory_duel);

        timerView = findViewById(R.id.tvMemoryDuelTimer);
        pauseButton = findViewById(R.id.btnMemoryDuelPause);

        panels[0] = findViewById(R.id.layoutMemoryPlayerOne);
        panels[1] = findViewById(R.id.layoutMemoryPlayerTwo);

        panels[0].setRotation(180f);

        for (int i = 0; i < 2; i++) {
            final int player = i;

            TextView title = panels[i].findViewById(
                    R.id.tvMemoryPlayerTitle
            );

            title.setText("PLAYER " + (i + 1));

            scores[i] = panels[i].findViewById(
                    R.id.tvMemoryPlayerScore
            );

            feedbackViews[i] = panels[i].findViewById(
                    R.id.tvMemoryPlayerFeedback
            );

            GridLayout grid = panels[i].findViewById(
                    R.id.gridMemoryPlayerCards
            );

            boards[i] = new MemoryBoard(
                    grid,
                    index -> flipCard(player, index),
                    true
            );
        }

        if (savedInstanceState == null) {
            resetMatch();
        } else {
            roundId = savedInstanceState.getString("roundId");
            timeLeft = savedInstanceState.getLong("timeLeft", START_TIME);
            gameOver = savedInstanceState.getBoolean("gameOver");

            resultMessage = savedInstanceState.getString(
                    "resultMessage", ""
            );

            paused = !gameOver;

            for (int i = 0; i < 2; i++) {
                rounds[i] = MemoryRound.restoreState(
                        savedInstanceState.getBundle("round" + i)
                );

                hideDelay[i] = savedInstanceState.getLong(
                        "hideDelay" + i
                );

                feedback[i] = savedInstanceState.getString(
                        "feedback" + i,
                        "Find the matching pairs!"
                );
                playerScores[i] = savedInstanceState.getInt(
                        "playerScore" + i, 0
                );

                boards[i].setRound(rounds[i]);
            }
        }

        pauseButton.setOnClickListener(v -> pauseGame());
        render();
    }

    private void resetMatch() {
        stopClock();

        // Same starting arrangement, independent progress.
        rounds[0] = new MemoryRound(6);
        rounds[1] = MemoryRound.restoreState(rounds[0].saveState());

        roundId = UUID.randomUUID().toString();
        timeLeft = START_TIME;
        resultMessage = "";

        paused = false;
        gameOver = false;

        for (int i = 0; i < 2; i++) {
            hideDelay[i] = 0;
            playerScores[i] = 0;
            hideAt[i] = 0;
            feedback[i] = "Find pairs to score. Found pairs open on both boards!";
            boards[i].setRound(rounds[i]);
        }
    }

    private void flipCard(int player, int index) {
        if (!running || paused || gameOver) {
            return;
        }

        captureTime();

        if (timeLeft <= 0) {
            finishMatch("The shared timer expired.");
            return;
        }

        MemoryRound.FlipResult result = rounds[player].flip(index);

        switch (result) {
            case IGNORED:
                return;

            case FIRST_CARD:
                feedback[player] = "Find the matching symbol.";
                break;

            case MATCH:
                playerScores[player] += 10;

                int opponent = 1 - player;

                rounds[opponent].revealMatchingPairFrom(
                        rounds[player],
                        index
                );

                if (!rounds[opponent].hasPendingMismatch()) {
                    hideDelay[opponent] = 0;
                    hideAt[opponent] = 0;
                }

                feedback[player] = "Pair found! +10 points.";

                feedback[opponent] = "Player " + (player + 1)
                        + " found a pair. Revealed on both boards.";
                break;

            case MISMATCH:
                timeLeft = Math.max(0, timeLeft - 3000);
                hideDelay[player] = MISMATCH_DELAY;

                hideAt[player] = SystemClock.elapsedRealtime()
                        + MISMATCH_DELAY;

                feedback[player] = "Wrong pair! Shared timer -3s.";
                break;
        }

        deadline = SystemClock.elapsedRealtime() + timeLeft;

        if (timeLeft <= 0) {
            finishMatch("The shared timer expired.");
        } else if (rounds[player].isComplete()) {
            finishMatch("All six pairs have been found!");
        } else {
            render();
        }
    }

    private int getScore(int player) {
        return playerScores[player];
    }

    private void render() {
        for (int i = 0; i < 2; i++) {
            panels[i].setVisibility(
                    paused ? View.INVISIBLE : View.VISIBLE
            );

            boards[i].setInteractionEnabled(!paused && !gameOver);

            scores[i].setText(
                    "Score: " + getScore(i)
                            + " • Pairs: " + rounds[i].getMatchedPairs()
                            + " / " + rounds[i].getPairCount()
            );

            feedbackViews[i].setText(feedback[i]);
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
            finishMatch("The shared timer expired.");
            return;
        }

        long now = SystemClock.elapsedRealtime();

        deadline = now + timeLeft;

        for (int i = 0; i < 2; i++) {
            if (rounds[i].hasPendingMismatch()) {
                hideAt[i] = now + hideDelay[i];
            }
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

        for (int i = 0; i < 2; i++) {
            if (rounds[i] != null && rounds[i].hasPendingMismatch()) {
                hideDelay[i] = Math.max(0, hideAt[i] - now);
            }
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
            finishMatch("The shared timer expired.");
            return;
        }

        paused = true;
        render();
        showPauseDialog();
    }

    private void showPauseDialog() {
        closeDialog();

        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Memory Duel paused")
                .setMessage(
                        "Race to claim matching pairs."
                                + "\n\nFound pairs open on both boards."
                                + "\nOnly the finder earns 10 points."
                                + "\nWrong pairs remove 3 shared seconds."
                                + "\nThe higher score wins."
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
                    restartMatch();
                })
                .setNegativeButton(
                        "Exit",
                        (dialog, which) -> finish()
                )
                .create();

        activeDialog.show();
    }

    private void finishMatch(String reason) {
        if (gameOver) {
            return;
        }

        stopClock();

        gameOver = true;
        paused = false;
        resultMessage = reason;

        for (int i = 0; i < 2; i++) {
            feedback[i] = "Match finished!";
        }

        GameDatabaseHelper.getInstance(getApplicationContext())
                .saveResult(
                        roundId,
                        "MEMORY",
                        false,
                        getScore(0),
                        getScore(1),
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

        int firstScore = getScore(0);
        int secondScore = getScore(1);

        String title;

        if (firstScore > secondScore) {
            title = "Player 1 wins!";
        } else if (secondScore > firstScore) {
            title = "Player 2 wins!";
        } else {
            title = "Match drawn!";
        }

        activeDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(
                        resultMessage
                                + "\n\nPlayer 1: " + firstScore + " points"
                                + "\nPlayer 2: " + secondScore + " points"
                )
                .setCancelable(false)
                .setPositiveButton("Play again", (dialog, which) -> {
                    activeDialog = null;
                    restartMatch();
                })
                .setNegativeButton(
                        "Back to setup",
                        (dialog, which) -> finish()
                )
                .create();

        activeDialog.show();
    }

    private void restartMatch() {
        closeDialog();
        resetMatch();
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

        outState.putString("roundId", roundId);
        outState.putLong("timeLeft", timeLeft);
        outState.putBoolean("gameOver", gameOver);
        outState.putString("resultMessage", resultMessage);

        for (int i = 0; i < 2; i++) {
            outState.putBundle("round" + i, rounds[i].saveState());
            outState.putLong("hideDelay" + i, hideDelay[i]);
            outState.putString("feedback" + i, feedback[i]);
            outState.putInt("playerScore" + i, playerScores[i]);
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