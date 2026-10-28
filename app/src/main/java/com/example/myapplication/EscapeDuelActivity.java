package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.UUID;

public class EscapeDuelActivity extends AppCompatActivity {

    private static final long START_TIME = 45000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private EscapeDuelMatch match;
    private EscapeBoard board;

    private TextView timerView;
    private TextView movesView;
    private TextView feedbackView;

    private AlertDialog activeDialog;

    private String roundId;
    private String feedback;

    private long timeLeft = START_TIME;
    private long deadline;

    private boolean running;
    private boolean playing;
    private boolean resultSaved;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running || !playing || match.isFinished()) {
                return;
            }

            captureTime();

            if (timeLeft <= 0) {
                finishAttempt();
                return;
            }

            renderTimer();
            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            resetMatch();
        } else {
            match = EscapeDuelMatch.restoreState(
                    savedInstanceState.getBundle("match")
            );

            roundId = savedInstanceState.getString("roundId");
            timeLeft = savedInstanceState.getLong("timeLeft", START_TIME);
            resultSaved = savedInstanceState.getBoolean("resultSaved");

            feedback = savedInstanceState.getString(
                    "feedback",
                    "Move to EXIT. Blocked moves cost 2 seconds."
            );
        }

        // Restore behind the privacy screen.
        playing = false;
        showHandoff();
    }

    private void resetMatch() {
        stopClock();

        match = new EscapeDuelMatch();
        roundId = UUID.randomUUID().toString();

        timeLeft = START_TIME;
        resultSaved = false;
        playing = false;

        feedback = "Move to EXIT. Blocked moves cost 2 seconds.";
    }

    private void showHandoff() {
        closeDialog();
        board = null;

        setContentView(R.layout.activity_escape_handoff);

        TextView title = findViewById(R.id.tvEscapeHandoffTitle);
        TextView message = findViewById(R.id.tvEscapeHandoffMessage);

        Button ready = findViewById(R.id.btnEscapeHandoffReady);
        Button menu = findViewById(R.id.btnEscapeHandoffMenu);

        menu.setOnClickListener(v -> showMenu());

        if (match.isFinished()) {
            title.setText("MATCH FINISHED");
            message.setText("Both players have completed their attempts.");
            ready.setText("VIEW RESULT");
            ready.setOnClickListener(v -> showResult());
            return;
        }

        int player = match.getCurrentPlayer() + 1;

        title.setText("PASS TO PLAYER " + player);

        message.setText(
                "Only Player " + player + " should view the maze."
                        + "\n\nThe other player should look away."
                        + "\n\nTime available: "
                        + ((timeLeft + 999) / 1000) + " seconds."
                        + "\nYour timer starts when you tap READY."
        );

        ready.setText("PLAYER " + player + " IS READY");

        ready.setOnClickListener(v -> {
            if (playing || match.isFinished()) {
                return;
            }

            playing = true;
            showGame();
            startClock();
        });
    }

    private void showGame() {
        setContentView(R.layout.activity_escape_game);

        TextView title = findViewById(R.id.tvEscapeTitle);
        TextView turn = findViewById(R.id.tvEscapeTurn);

        title.setText("ESCAPE THE GRID • DUEL");

        turn.setText(
                "Player " + (match.getCurrentPlayer() + 1)
                        + ": reach the exit"
        );

        timerView = findViewById(R.id.tvEscapeTimer);
        movesView = findViewById(R.id.tvEscapeMoves);
        feedbackView = findViewById(R.id.tvEscapeFeedback);

        GridLayout grid = findViewById(R.id.gridEscapeMaze);

        board = new EscapeBoard(grid);
        board.setRound(match.getCurrentRound());

        findViewById(R.id.btnEscapeUp)
                .setOnClickListener(v -> move(-1, 0));

        findViewById(R.id.btnEscapeDown)
                .setOnClickListener(v -> move(1, 0));

        findViewById(R.id.btnEscapeLeft)
                .setOnClickListener(v -> move(0, -1));

        findViewById(R.id.btnEscapeRight)
                .setOnClickListener(v -> move(0, 1));

        findViewById(R.id.btnEscapePause)
                .setOnClickListener(v -> pauseGame());

        renderGame();
    }

    private void move(int rowChange, int columnChange) {
        if (!running || !playing || match.isFinished()) {
            return;
        }

        captureTime();

        if (timeLeft <= 0) {
            finishAttempt();
            return;
        }

        EscapeRound round = match.getCurrentRound();

        EscapeRound.MoveResult result = round.move(
                rowChange, columnChange
        );

        switch (result) {
            case BLOCKED:
                timeLeft = Math.max(0, timeLeft - 2000);
                feedback = "Blocked! -2 seconds.";
                break;

            case MOVED:
                int row = round.getPosition() / EscapeRound.SIZE + 1;
                int column = round.getPosition() % EscapeRound.SIZE + 1;

                feedback = "Position: row " + row
                        + ", column " + column + ".";
                break;

            case ESCAPED:
                finishAttempt();
                return;

            case ALREADY_FINISHED:
                return;
        }

        deadline = SystemClock.elapsedRealtime() + timeLeft;

        if (timeLeft <= 0) {
            finishAttempt();
        } else {
            renderGame();
        }
    }

    private void renderGame() {
        if (!playing || board == null) {
            return;
        }

        board.render();

        movesView.setText(
                "Moves: " + match.getCurrentRound().getMoves()
        );

        feedbackView.setText(feedback);
        renderTimer();
    }

    private void renderTimer() {
        if (playing && timerView != null) {
            timerView.setText(
                    "TIME: " + ((timeLeft + 999) / 1000) + "s"
            );
        }
    }

    private void startClock() {
        if (running || !playing || match.isFinished()) {
            return;
        }

        if (timeLeft <= 0) {
            finishAttempt();
            return;
        }

        deadline = SystemClock.elapsedRealtime() + timeLeft;
        running = true;
        handler.post(ticker);
    }

    private void captureTime() {
        if (running) {
            timeLeft = Math.max(
                    0,
                    deadline - SystemClock.elapsedRealtime()
            );
        }
    }

    private void stopClock() {
        captureTime();
        running = false;
        handler.removeCallbacks(ticker);
    }

    private void finishAttempt() {
        if (match.isFinished()) {
            return;
        }

        stopClock();
        playing = false;

        match.finishCurrentAttempt(timeLeft);

        if (match.isFinished()) {
            finishMatch();
        } else {
            // Player 2 starts with a fresh timer and untouched maze.
            timeLeft = START_TIME;
            feedback = "Move to EXIT. Blocked moves cost 2 seconds.";
            showHandoff();
        }
    }

    private void finishMatch() {
        stopClock();
        playing = false;

        if (!resultSaved) {
            resultSaved = true;

            GameDatabaseHelper.getInstance(getApplicationContext())
                    .saveResult(
                            roundId,
                            "ESCAPE",
                            false,
                            match.getScore(0),
                            match.getScore(1),
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
        }

        showHandoff();
        showResult();
    }

    private String playerSummary(int player) {
        EscapeRound round = match.getRound(player);

        String outcome = round.hasEscaped()
                ? "Escaped in " + round.getMoves() + " moves"
                : "Time expired";

        return "Player " + (player + 1)
                + ": " + match.getScore(player) + " points"
                + "\n" + outcome;
    }

    private void showResult() {
        if (!match.isFinished()) {
            return;
        }

        closeDialog();

        activeDialog = new AlertDialog.Builder(this)
                .setTitle(match.getResultTitle())
                .setMessage(
                        playerSummary(0)
                                + "\n\n" + playerSummary(1)
                                + "\n\nShortest route: "
                                + match.getRound(0).getMinimumMoves()
                                + " moves"
                )
                .setCancelable(false)
                .setPositiveButton(
                        "Play again",
                        (dialog, which) -> restartMatch()
                )
                .setNegativeButton(
                        "Back to setup",
                        (dialog, which) -> finish()
                )
                .create();

        activeDialog.show();
    }

    private void pauseGame() {
        if (match.isFinished()) {
            return;
        }

        stopClock();

        if (timeLeft <= 0) {
            finishAttempt();
            return;
        }

        playing = false;
        showHandoff();
        showMenu();
    }

    private void showMenu() {
        closeDialog();

        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Escape Duel menu")
                .setMessage(
                        "The timer is paused."
                                + "\n\nBoth players face the same maze."
                                + "\nA shorter route and more remaining "
                                + "time earn more points."
                                + "\n\nRestart resets both attempts."
                )
                .setPositiveButton("Continue", (dialog, which) -> {
                    activeDialog = null;

                    if (match.isFinished()) {
                        showResult();
                    }
                })
                .setNeutralButton(
                        "Restart",
                        (dialog, which) -> restartMatch()
                )
                .setNegativeButton(
                        "Exit",
                        (dialog, which) -> finish()
                )
                .create();

        activeDialog.show();
    }

    private void restartMatch() {
        closeDialog();
        resetMatch();
        showHandoff();
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

        if (match.isFinished()) {
            finishMatch();
        } else if (playing) {
            startClock();
        }
    }

    @Override
    protected void onPause() {
        stopClock();
        closeDialog();

        if (playing && !match.isFinished()) {
            playing = false;
            showHandoff();
        }

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (match.isFinished()) {
            finish();
        } else {
            pauseGame();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        captureTime();

        outState.putBundle("match", match.saveState());
        outState.putString("roundId", roundId);
        outState.putLong("timeLeft", timeLeft);
        outState.putBoolean("resultSaved", resultSaved);
        outState.putString("feedback", feedback);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        closeDialog();
        super.onDestroy();
    }
}