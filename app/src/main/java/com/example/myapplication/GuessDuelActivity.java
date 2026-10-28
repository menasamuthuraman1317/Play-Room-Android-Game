package com.example.myapplication;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
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
import java.util.UUID;

public class GuessDuelActivity extends AppCompatActivity {

    private static final int REQUEST_SETUP = 501;
    private static final long START_TIME = 120000L;

    private static final String SETUP = "SETUP";
    private static final String HANDOFF = "HANDOFF";
    private static final String BOARD = "BOARD";
    private static final String RESULT = "RESULT";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private GuessDuelMatch match;
    private GuessBoard board;

    private TextView timerView;
    private TextView scoreView;
    private TextView progressView;
    private TextView answerView;
    private TextView selectedView;

    private Button guessButton;
    private Button nextButton;
    private LinearLayout categoriesLayout;

    private AlertDialog activeDialog;

    private String phase = SETUP;
    private String roundId;
    private String selectedId;

    private long timeLeft = START_TIME;
    private long deadline;

    private boolean running;
    private boolean setupOpen;
    private boolean resultSaved;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running || !BOARD.equals(phase)) {
                return;
            }

            captureTime();
            renderTimer();

            if (timeLeft <= 0) {
                match.finishOnTimeout();
                finishMatch();
            } else {
                handler.postDelayed(this, 100);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            phase = savedInstanceState.getString("phase", SETUP);
            roundId = savedInstanceState.getString("roundId");
            selectedId = savedInstanceState.getString("selectedId");
            timeLeft = savedInstanceState.getLong("timeLeft", START_TIME);
            setupOpen = savedInstanceState.getBoolean("setupOpen");
            resultSaved = savedInstanceState.getBoolean("resultSaved");

            Bundle matchState = savedInstanceState.getBundle("match");

            if (matchState != null) {
                match = GuessDuelMatch.restoreState(matchState);
            }
        }

        // Always restore an unfinished match behind a privacy screen.
        if (match != null && !match.isFinished()) {
            phase = HANDOFF;
        }

        showHandoff();
    }

    private void launchSetup() {
        if (setupOpen) {
            return;
        }

        setupOpen = true;

        startActivityForResult(
                new Intent(this, GuessDuelSetupActivity.class),
                REQUEST_SETUP
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_SETUP) {
            return;
        }

        setupOpen = false;

        if (resultCode != RESULT_OK || data == null) {
            finish();
            return;
        }

        String first = data.getStringExtra(
                GuessDuelSetupActivity.EXTRA_PLAYER_ONE_SECRET
        );

        String second = data.getStringExtra(
                GuessDuelSetupActivity.EXTRA_PLAYER_TWO_SECRET
        );

        if (first == null || second == null) {
            finish();
            return;
        }

        match = new GuessDuelMatch(first, second);
        roundId = UUID.randomUUID().toString();

        timeLeft = START_TIME;
        resultSaved = false;
        selectedId = null;
        phase = HANDOFF;

        showHandoff();
    }

    private void showHandoff() {
        closeDialog();
        board = null;

        setContentView(R.layout.activity_guess_handoff);

        TextView title = findViewById(R.id.tvGuessHandoffTitle);
        TextView message = findViewById(R.id.tvGuessHandoffMessage);
        Button ready = findViewById(R.id.btnGuessHandoffReady);
        Button menu = findViewById(R.id.btnGuessHandoffExit);

        menu.setText("MATCH MENU");
        menu.setOnClickListener(v -> showMenu());

        if (match == null) {
            title.setText("GET READY");
            message.setText("Both players will choose a secret character.");
            ready.setEnabled(false);
            return;
        }

        if (match.isFinished()) {
            title.setText("MATCH FINISHED");
            message.setText("View the result or start a new match.");
            ready.setText("VIEW RESULT");
            ready.setOnClickListener(v -> showResult());
            return;
        }

        int nextPlayer = match.isWaitingForNextTurn()
                ? match.getNextPlayer()
                : match.getCurrentPlayer();

        title.setText("PASS TO PLAYER " + nextPlayer);

        message.setText(
                "Only Player " + nextPlayer
                        + " should view the next screen."
                        + "\n\nThe other player should look away."
                        + "\n\nThe shared timer is paused."
        );

        ready.setText("PLAYER " + nextPlayer + " IS READY");

        ready.setOnClickListener(v -> {
            if (match.isWaitingForNextTurn()) {
                match.beginNextTurn();
                selectedId = null;
            }

            phase = BOARD;
            showBoard();
            startClock();
        });
    }

    private void showBoard() {
        setContentView(R.layout.activity_guess_game);

        timerView = findViewById(R.id.tvGuessTimer);
        scoreView = findViewById(R.id.tvGuessScore);
        progressView = findViewById(R.id.tvGuessProgress);
        answerView = findViewById(R.id.tvGuessLastAnswer);
        selectedView = findViewById(R.id.tvGuessSelectedCandidate);

        guessButton = findViewById(R.id.btnGuessSubmit);
        nextButton = findViewById(R.id.btnGuessNextTurn);
        categoriesLayout = findViewById(R.id.layoutGuessCategories);

        TextView title = findViewById(R.id.tvGuessGameTitle);
        TextView turn = findViewById(R.id.tvGuessTurn);

        title.setText("GUESS THE PERSON • DUEL");
        turn.setText(
                "Player " + match.getCurrentPlayer()
                        + ": find your opponent's character"
        );

        GridLayout grid = findViewById(R.id.gridGuessGame);

        board = new GuessBoard(grid, id -> {
            selectedId = id;
            renderSelection();
        });

        board.setRound(match.getCurrentRound());
        board.restoreSelection(selectedId);

        createCategories();

        guessButton.setOnClickListener(v -> confirmGuess());

        nextButton.setOnClickListener(v -> {
            if (!match.isWaitingForNextTurn()) {
                return;
            }

            stopClock();
            selectedId = null;
            phase = HANDOFF;
            showHandoff();
        });

        findViewById(R.id.btnGuessPause).setOnClickListener(v -> {
            stopClock();

            if (timeLeft <= 0) {
                match.finishOnTimeout();
                finishMatch();
                return;
            }

            phase = HANDOFF;
            showHandoff();
            showMenu();
        });

        renderBoard();
    }

    private void createCategories() {
        categoriesLayout.removeAllViews();

        for (GuessQuestion.Category category
                : GuessQuestion.Category.values()) {

            AppCompatButton button = new AppCompatButton(this);

            button.setText(category.label);
            button.setAllCaps(false);
            button.setTextColor(Color.WHITE);
            button.setBackgroundTintList(
                    ColorStateList.valueOf(Color.rgb(18, 86, 160))
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            params.setMargins(dp(4), 0, dp(4), 0);
            button.setLayoutParams(params);
            button.setMinHeight(dp(48));

            button.setOnClickListener(v -> showQuestions(category));
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
            if (!match.getCurrentRound().hasAsked(question)) {
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

                    match.ask(available.get(which));
                    stopClock();
                    clearSelection();
                    renderBoard();
                })
                .setNegativeButton("Cancel", null)
                .create();

        activeDialog.show();
    }

    private void confirmGuess() {
        if (!canAct() || selectedId == null) {
            return;
        }

        final String candidateId = selectedId;
        GuessCharacter candidate =
                GuessCharacterBank.findById(candidateId);

        closeDialog();

        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Guess " + candidate.name + "?")
                .setMessage(
                        "An incorrect guess uses one attempt "
                                + "and ends your turn."
                                + "\n\nThe timer continues."
                )
                .setPositiveButton("Make guess", (dialog, which) -> {
                    activeDialog = null;

                    if (!canAct()) {
                        return;
                    }

                    match.guess(candidateId);
                    stopClock();
                    clearSelection();

                    if (match.isFinished()) {
                        finishMatch();
                    } else {
                        renderBoard();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        activeDialog.show();
    }

    private void clearSelection() {
        selectedId = null;

        if (board != null) {
            board.clearSelection();
        }
    }

    private boolean canAct() {
        if (!BOARD.equals(phase) || !running
                || match == null || match.isFinished()
                || match.isWaitingForNextTurn()) {
            return false;
        }

        captureTime();

        if (timeLeft <= 0) {
            match.finishOnTimeout();
            finishMatch();
            return false;
        }

        return true;
    }

    private void renderBoard() {
        GuessRound currentRound = match.getCurrentRound();

        boolean waiting = match.isWaitingForNextTurn();
        boolean enabled = !waiting && !match.isFinished();

        board.setInteractionEnabled(enabled);
        selectedId = board.getSelectedId();

        scoreView.setText(
                "P1: " + match.getScore(1)
                        + " • P2: " + match.getScore(2)
        );

        progressView.setText(
                currentRound.getRemainingCount() + " remaining • "
                        + currentRound.getQuestionsAsked() + " questions • "
                        + currentRound.getGuessesLeft() + " guesses left"
        );

        answerView.setText(
                match.getLastMessage()
                        + (waiting
                        ? "\n\nTimer paused. Tap PASS PHONE when ready."
                        : "")
        );

        for (int i = 0; i < categoriesLayout.getChildCount(); i++) {
            categoriesLayout.getChildAt(i).setEnabled(enabled);
        }

        guessButton.setVisibility(waiting ? View.GONE : View.VISIBLE);
        nextButton.setVisibility(waiting ? View.VISIBLE : View.GONE);

        nextButton.setText(
                "PASS PHONE TO PLAYER " + match.getNextPlayer()
        );

        renderSelection();
        renderTimer();
    }

    private void renderSelection() {
        boolean enabled = selectedId != null
                && !match.isFinished()
                && !match.isWaitingForNextTurn();

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
        if (timerView != null && BOARD.equals(phase)) {
            timerView.setText(
                    "SHARED TIME: " + ((timeLeft + 999) / 1000) + "s"
            );
        }
    }

    private void startClock() {
        if (running || !BOARD.equals(phase)
                || match == null || match.isFinished()
                || match.isWaitingForNextTurn()) {
            return;
        }

        if (timeLeft <= 0) {
            match.finishOnTimeout();
            finishMatch();
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

    private void finishMatch() {
        stopClock();
        phase = RESULT;

        if (!resultSaved) {
            resultSaved = true;

            GameDatabaseHelper.getInstance(getApplicationContext())
                    .saveResult(
                            roundId,
                            "GUESS",
                            false,
                            match.getScore(1),
                            match.getScore(2),
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

    private void showResult() {
        if (match == null || !match.isFinished()) {
            return;
        }

        closeDialog();

        String title = match.getWinner() == GuessDuelMatch.DRAW
                ? "Match drawn!"
                : "Player " + match.getWinner() + " wins!";

        activeDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(
                        match.getLastMessage()
                                + "\n\nPlayer 1 score: " + match.getScore(1)
                                + "\nPlayer 2 score: " + match.getScore(2)
                                + "\n\nPlayer 1 chose: "
                                + match.getRound(2).getSecretName()
                                + "\nPlayer 2 chose: "
                                + match.getRound(1).getSecretName()
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

    private void showMenu() {
        closeDialog();

        activeDialog = new AlertDialog.Builder(this)
                .setTitle("Duel menu")
                .setMessage(
                        "The timer is paused."
                                + "\n\nRestarting requires both players "
                                + "to choose their characters again."
                )
                .setPositiveButton("Continue", (dialog, which) -> {
                    activeDialog = null;

                    if (match != null && match.isFinished()) {
                        showResult();
                    }
                })
                .setNeutralButton(
                        "Restart",
                        (dialog, which) -> restartMatch()
                )
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .create();

        activeDialog.show();
    }

    private void restartMatch() {
        stopClock();
        closeDialog();

        match = null;
        board = null;
        selectedId = null;
        roundId = null;

        timeLeft = START_TIME;
        resultSaved = false;
        setupOpen = false;
        phase = SETUP;

        showHandoff();
        launchSetup();
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

        if (isFinishing()) {
            return;
        }

        if (match == null) {
            launchSetup();
        } else if (match.isFinished()) {
            finishMatch();
        } else if (BOARD.equals(phase)) {
            startClock();
        }
    }

    @Override
    protected void onPause() {
        stopClock();
        closeDialog();

        if (BOARD.equals(phase)
                && match != null && !match.isFinished()) {

            if (board != null) {
                selectedId = board.getSelectedId();
            }

            phase = HANDOFF;
            showHandoff();
        }

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (match == null) {
            finish();
            return;
        }

        if (match.isFinished()) {
            finish();
            return;
        }

        stopClock();
        phase = HANDOFF;
        showHandoff();
        showMenu();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        captureTime();

        if (board != null && BOARD.equals(phase)) {
            selectedId = board.getSelectedId();
        }

        outState.putString(
                "phase",
                BOARD.equals(phase) ? HANDOFF : phase
        );

        outState.putString("roundId", roundId);
        outState.putString("selectedId", selectedId);
        outState.putLong("timeLeft", timeLeft);
        outState.putBoolean("setupOpen", setupOpen);
        outState.putBoolean("resultSaved", resultSaved);

        if (match != null) {
            outState.putBundle("match", match.saveState());
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