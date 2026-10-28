package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class GuessGameActivity extends AppCompatActivity {

    private static final long START_TIME = 90000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private GuessRound round;
    private GuessBoard board;

    private TextView timerView;
    private TextView scoreView;
    private TextView progressView;
    private TextView answerView;
    private TextView selectedView;

    private Button guessButton;
    private Button pauseButton;
    private LinearLayout categoriesLayout;

    private AlertDialog activeDialog;

    private String roundId;
    private String lastAnswer;
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
            renderTimer();

            if (timeLeft <= 0) {
                finishRound("Time is up!");
            } else {
                handler.postDelayed(this, 100);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guess_game);

        timerView = findViewById(R.id.tvGuessTimer);
        scoreView = findViewById(R.id.tvGuessScore);
        progressView = findViewById(R.id.tvGuessProgress);
        answerView = findViewById(R.id.tvGuessLastAnswer);
        selectedView = findViewById(R.id.tvGuessSelectedCandidate);
        guessButton = findViewById(R.id.btnGuessSubmit);
        pauseButton = findViewById(R.id.btnGuessPause);
        categoriesLayout = findViewById(R.id.layoutGuessCategories);

        TextView title = findViewById(R.id.tvGuessGameTitle);
        TextView turn = findViewById(R.id.tvGuessTurn);

        title.setText("GUESS THE PERSON • SURVIVAL");
        turn.setText("Find the secret character");

        GridLayout grid = findViewById(R.id.gridGuessGame);
        board = new GuessBoard(grid, id -> renderSelection());

        createCategoryButtons();

        if (savedInstanceState == null) {
            resetRound();
        } else {
            round = GuessRound.restoreState(
                    savedInstanceState.getBundle("round")
            );

            roundId = savedInstanceState.getString("roundId");
            timeLeft = savedInstanceState.getLong("timeLeft", START_TIME);
            gameOver = savedInstanceState.getBoolean("gameOver");
            lastAnswer = savedInstanceState.getString("lastAnswer");
            resultMessage = savedInstanceState.getString(
                    "resultMessage", ""
            );

            paused = !gameOver;
            board.setRound(round);
            board.restoreSelection(
                    savedInstanceState.getString("selectedId")
            );
        }

        guessButton.setOnClickListener(v -> confirmGuess());
        pauseButton.setOnClickListener(v -> pauseGame());

        render();
    }

    private void resetRound() {
        stopClock();

        String previousSecret = round == null
                ? null : round.getSecretId();

        List<GuessCharacter> choices = new ArrayList<>(
                GuessCharacterBank.getAll()
        );

        if (previousSecret != null) {
            for (int i = choices.size() - 1; i >= 0; i--) {
                if (choices.get(i).id.equals(previousSecret)) {
                    choices.remove(i);
                }
            }
        }

        GuessCharacter secret = choices.get(
                new Random().nextInt(choices.size())
        );

        round = new GuessRound(secret.id);
        roundId = UUID.randomUUID().toString();

        timeLeft = START_TIME;
        paused = false;
        gameOver = false;
        resultMessage = "";

        lastAnswer = "Choose a category to ask a question. "
                + "Characters that do not match will be eliminated.";

        board.setRound(round);
    }

    private void createCategoryButtons() {
        categoriesLayout.removeAllViews();

        for (GuessQuestion.Category category
                : GuessQuestion.Category.values()) {

            AppCompatButton button = new AppCompatButton(this);
            button.setText(category.label);
            button.setAllCaps(false);
            button.setTextColor(android.graphics.Color.WHITE);
            button.setSupportBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.rgb(18, 86, 160)
                    )
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(dp(4), 0, dp(4), 0);
            button.setMinHeight(dp(48));
            button.setLayoutParams(params);

            button.setOnClickListener(
                    v -> showQuestions(category)
            );

            categoriesLayout.addView(button);
        }
    }

    private void showQuestions(GuessQuestion.Category category) {
        if (!canAct()) {
            return;
        }

        List<GuessQuestion> available = new ArrayList<>();

        for (GuessQuestion question
                : GuessQuestion.getForCategory(category)) {
            if (!round.hasAsked(question)) {
                available.add(question);
            }
        }

        if (available.isEmpty()) {
            Toast.makeText(
                    this,
                    "You have asked every question in this category.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String[] labels = new String[available.size()];

        for (int i = 0; i < available.size(); i++) {
            labels[i] = available.get(i).text;
        }

        closeDialog();

        activeDialog = new AlertDialog.Builder(this)
                .setTitle(category.label + " — timer continues")
                .setItems(labels, (dialog, which) -> {
                    activeDialog = null;

                    if (!canAct()) {
                        return;
                    }

                    GuessQuestion question = available.get(which);
                    boolean answer = round.ask(question);

                    lastAnswer = question.text
                            + "\nAnswer: " + (answer ? "YES" : "NO");

                    render();
                })
                .setNegativeButton("Cancel", null)
                .create();

        activeDialog.show();
    }

    private void confirmGuess() {
        if (!canAct()) {
            return;
        }

        String selectedId = board.getSelectedId();

        if (selectedId == null || !round.isRemaining(selectedId)) {
            return;
        }

        GuessCharacter selected =
                GuessCharacterBank.findById(selectedId);

        closeDialog();

        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Guess " + selected.name + "?")
                .setMessage(
                        "An incorrect guess uses one attempt. "
                                + "The timer continues."
                )
                .setPositiveButton("Make guess", (dialog, which) -> {
                    activeDialog = null;

                    if (!canAct()) {
                        return;
                    }

                    GuessRound.GuessResult result =
                            round.guess(selectedId);

                    board.clearSelection();

                    if (result == GuessRound.GuessResult.CORRECT) {
                        finishRound("You found the secret character!");
                    } else if (result
                            == GuessRound.GuessResult.OUT_OF_GUESSES) {
                        finishRound("No guesses remaining.");
                    } else {
                        lastAnswer = selected.name
                                + " is not the secret character. "
                                + "Keep investigating!";
                        render();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        activeDialog.show();
    }

    private boolean canAct() {
        if (!running || paused || gameOver) {
            return false;
        }

        captureTime();

        if (timeLeft <= 0) {
            finishRound("Time is up!");
            return false;
        }

        return true;
    }

    private void render() {
        board.setInteractionEnabled(!paused && !gameOver);

        scoreView.setText("Score: " + round.getScore());

        progressView.setText(
                round.getRemainingCount() + " remaining • "
                        + round.getQuestionsAsked() + " questions • "
                        + round.getGuessesLeft() + " guesses left"
        );

        answerView.setText(lastAnswer);
        pauseButton.setEnabled(!gameOver);

        for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
            categoriesLayout.getChildAt(i)
                    .setEnabled(!paused && !gameOver);
        }

        renderSelection();
        renderTimer();
    }

    private void renderSelection() {
        String selectedId = board.getSelectedId();

        boolean enabled = selectedId != null && !paused && !gameOver;

        guessButton.setEnabled(enabled);
        guessButton.setAlpha(enabled ? 1f : 0.45f);

        if (selectedId == null) {
            selectedView.setText(
                    "Tap a remaining character to prepare your guess"
            );
        } else {
            selectedView.setText(
                    "Selected: "
                            + GuessCharacterBank.findById(selectedId).name
            );
        }
    }

    private void renderTimer() {
        long seconds = (timeLeft + 999) / 1000;
        timerView.setText("TIME: " + seconds + "s");
    }

    private void startClock() {
        if (running || paused || gameOver) {
            return;
        }

        if (timeLeft <= 0) {
            finishRound("Time is up!");
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
            finishRound("Time is up!");
            return;
        }

        paused = true;
        render();
        showPauseDialog();
    }

    private void showPauseDialog() {
        closeDialog();

        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Game paused")
                .setMessage(
                        "Ask questions to narrow the board, then guess "
                                + "the secret character before time runs out."
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
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .create();

        activeDialog.show();
    }

    private void finishRound(String message) {
        if (gameOver) {
            return;
        }

        stopClock();
        gameOver = true;
        paused = false;

        resultMessage = message;
        lastAnswer = message + "\nSecret character: "
                + round.getSecretName();

        GameDatabaseHelper.getInstance(getApplicationContext())
                .saveResult(
                        roundId,
                        "GUESS",
                        true,
                        0,
                        round.getScore(),
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
                .setTitle(round.hasWon() ? "Victory!" : "Round finished")
                .setMessage(
                        resultMessage
                                + "\n\nSecret: " + round.getSecretName()
                                + "\nScore: " + round.getScore()
                                + "\nQuestions: " + round.getQuestionsAsked()
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

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
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
        outState.putBoolean("gameOver", gameOver);
        outState.putString("lastAnswer", lastAnswer);
        outState.putString("resultMessage", resultMessage);
        outState.putString("selectedId", board.getSelectedId());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        closeDialog();
        super.onDestroy();
    }
}