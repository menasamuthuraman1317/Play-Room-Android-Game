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

public class EscapeGameActivity extends AppCompatActivity {

    private static final long START_TIME = 45000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private EscapeRound round;
    private EscapeBoard board;

    private View content;
    private TextView timerView;
    private TextView movesView;
    private TextView feedbackView;

    private Button pauseButton;
    private final Button[] moveButtons = new Button[4];

    private AlertDialog activeDialog;

    private String roundId;
    private String feedback;
    private String resultMessage = "";

    private long timeLeft = START_TIME;
    private long deadline;

    private int score;

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

            renderTimer();
            handler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escape_game);

        content = findViewById(R.id.layoutEscapeContent);
        timerView = findViewById(R.id.tvEscapeTimer);
        movesView = findViewById(R.id.tvEscapeMoves);
        feedbackView = findViewById(R.id.tvEscapeFeedback);
        pauseButton = findViewById(R.id.btnEscapePause);

        TextView title = findViewById(R.id.tvEscapeTitle);
        title.setText("ESCAPE THE GRID • SURVIVAL");

        GridLayout grid = findViewById(R.id.gridEscapeMaze);
        board = new EscapeBoard(grid);

        moveButtons[0] = findViewById(R.id.btnEscapeUp);
        moveButtons[1] = findViewById(R.id.btnEscapeDown);
        moveButtons[2] = findViewById(R.id.btnEscapeLeft);
        moveButtons[3] = findViewById(R.id.btnEscapeRight);

        moveButtons[0].setOnClickListener(v -> move(-1, 0));
        moveButtons[1].setOnClickListener(v -> move(1, 0));
        moveButtons[2].setOnClickListener(v -> move(0, -1));
        moveButtons[3].setOnClickListener(v -> move(0, 1));

        if (savedInstanceState == null) {
            resetRound();
        } else {
            round = EscapeRound.restoreState(
                    savedInstanceState.getBundle("round")
            );

            roundId = savedInstanceState.getString("roundId");
            timeLeft = savedInstanceState.getLong("timeLeft", START_TIME);
            score = savedInstanceState.getInt("score");
            gameOver = savedInstanceState.getBoolean("gameOver");

            feedback = savedInstanceState.getString(
                    "feedback", "Find your way to EXIT."
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

        round = new EscapeRound();
        roundId = UUID.randomUUID().toString();

        timeLeft = START_TIME;
        score = 0;
        paused = false;
        gameOver = false;
        resultMessage = "";

        feedback = "Reach EXIT. Dark blue cells are walls. "
                + "Blocked moves cost 2 seconds.";

        board.setRound(round);
    }

    private void move(int rowChange, int columnChange) {
        if (!running || paused || gameOver) {
            return;
        }

        captureTime();

        if (timeLeft <= 0) {
            finishRound(false);
            return;
        }

        EscapeRound.MoveResult result = round.move(
                rowChange, columnChange
        );

        switch (result) {
            case BLOCKED:
                timeLeft = Math.max(0, timeLeft - 2000);
                feedback = "Blocked! -2 seconds. Try another direction.";
                break;

            case MOVED:
                int row = round.getPosition() / EscapeRound.SIZE + 1;
                int column = round.getPosition() % EscapeRound.SIZE + 1;

                feedback = "Position: row " + row
                        + ", column " + column + ".";
                break;

            case ESCAPED:
                finishRound(true);
                return;

            case ALREADY_FINISHED:
                return;
        }

        deadline = SystemClock.elapsedRealtime() + timeLeft;

        if (timeLeft <= 0) {
            finishRound(false);
        } else {
            render();
        }
    }

    private void render() {
        content.setVisibility(paused ? View.INVISIBLE : View.VISIBLE);

        board.render();
        movesView.setText("Moves: " + round.getMoves());
        feedbackView.setText(feedback);

        for (Button button : moveButtons) {
            button.setEnabled(!paused && !gameOver);
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
            finishRound(false);
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
                .setTitle("Escape the Grid paused")
                .setMessage(
                        "Move the orange marker to EXIT."
                                + "\n\nWalls and boundaries cost 2 seconds."
                                + "\nTake a short route to earn more points."
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

    private void finishRound(boolean escaped) {
        if (gameOver) {
            return;
        }

        stopClock();

        gameOver = true;
        paused = false;

        if (escaped) {
            int extraMoves = Math.max(
                    0,
                    round.getMoves() - round.getMinimumMoves()
            );

            int routeScore = Math.max(10, 100 - extraMoves * 5);
            int timeBonus = (int) (timeLeft / 1000);

            score = routeScore + timeBonus;

            resultMessage = "You reached the exit!"
                    + "\nYour moves: " + round.getMoves()
                    + "\nShortest route: " + round.getMinimumMoves()
                    + "\nRoute points: " + routeScore
                    + "\nTime bonus: " + timeBonus;
        } else {
            score = 0;
            resultMessage = "Time is up!"
                    + "\nTry a different route next time.";
        }

        feedback = escaped ? "Escaped!" : "Time is up!";

        GameDatabaseHelper.getInstance(getApplicationContext())
                .saveResult(
                        roundId,
                        "ESCAPE",
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
                        round.hasEscaped() ? "Victory!" : "Round finished"
                )
                .setMessage(
                        resultMessage + "\n\nFinal score: " + score
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
        outState.putLong("timeLeft", timeLeft);
        outState.putInt("score", score);
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