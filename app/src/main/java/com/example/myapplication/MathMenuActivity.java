package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MathMenuActivity extends AppCompatActivity {

    private TextView bestView;
    private TextView duelView;
    private TextView statusView;

    private int requestNumber;
    private int pendingRequests;
    private boolean loadFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_menu);

        getWindow().setStatusBarColor(Color.rgb(67, 16, 95));
        getWindow().setNavigationBarColor(Color.rgb(57, 7, 80));

        bestView = findViewById(R.id.tvMathMenuBest);
        duelView = findViewById(R.id.tvMathMenuDuel);
        statusView = findViewById(R.id.tvMathMenuStatsStatus);

        findViewById(R.id.btnMathMenuBack)
                .setOnClickListener(v -> finish());

        findViewById(R.id.btnMathMenuSurvival)
                .setOnClickListener(v -> startGame(true));

        findViewById(R.id.btnMathMenuDuel)
                .setOnClickListener(v -> startGame(false));

        findViewById(R.id.btnMathMenuLeaderboard)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(
                            MathMenuActivity.this,
                            LeaderboardActivity.class
                    );

                    startActivity(intent);
                });

        findViewById(R.id.btnMathMenuHelp)
                .setOnClickListener(v -> showHelp());

        // Allows retrying if loading the records fails.
        statusView.setOnClickListener(v -> loadRecords());
    }

    private void startGame(boolean survival) {
        Intent intent = new Intent(
                MathMenuActivity.this,
                MathRushActivity.class
        );

        intent.putExtra(
                MathRushActivity.EXTRA_SURVIVAL,
                survival
        );

        startActivity(intent);
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle("How to play Math Rush")
                .setMessage(
                        "SURVIVAL"
                                + "\nStart with 30 seconds."
                                + "\nCorrect answer: 10 points and +5 seconds."
                                + "\nWrong answer: -5 seconds."
                                + "\nQuestions become harder as you score."
                                + "\n\nDUEL"
                                + "\nTwo players share a 60-second timer."
                                + "\nPlayer 1 faces the opposite end."
                                + "\nFirst correct answer earns 10 points."
                                + "\nA wrong answer removes 5 shared seconds."
                                + "\nThe opponent can still try that question."
                                + "\n\nCompleted results are saved on this device."
                )
                .setPositiveButton("Let's play", null)
                .show();
    }

    private void loadRecords() {
        final int request = ++requestNumber;

        pendingRequests = 2;
        loadFailed = false;

        bestView.setText("—");
        duelView.setText("—");
        statusView.setText("Loading saved results…");

        GameDatabaseHelper database =
                GameDatabaseHelper.getInstance(
                        getApplicationContext()
                );

        database.loadResults(
                "MATH",
                true,
                (results, success) -> {
                    if (!isCurrentRequest(request)) {
                        return;
                    }

                    if (success) {
                        if (results.isEmpty()) {
                            bestView.setText("0");
                        } else {
                            bestView.setText(
                                    String.valueOf(
                                            results.get(0).playerTwoScore
                                    )
                            );
                        }
                    } else {
                        loadFailed = true;
                    }

                    finishLoading();
                }
        );

        database.loadResults(
                "MATH",
                false,
                (results, success) -> {
                    if (!isCurrentRequest(request)) {
                        return;
                    }

                    if (success) {
                        if (results.isEmpty()) {
                            duelView.setText("—");
                        } else {
                            GameDatabaseHelper.GameResult latest =
                                    results.get(0);

                            duelView.setText(
                                    latest.playerOneScore
                                            + " : "
                                            + latest.playerTwoScore
                            );
                        }
                    } else {
                        loadFailed = true;
                    }

                    finishLoading();
                }
        );
    }

    private boolean isCurrentRequest(int request) {
        return request == requestNumber
                && !isFinishing()
                && !isDestroyed();
    }

    private void finishLoading() {
        pendingRequests--;

        if (pendingRequests > 0) {
            return;
        }

        if (loadFailed) {
            statusView.setText(
                    "Some records could not load. Tap here to retry."
            );
        } else {
            statusView.setText(
                    "Saved on this device • Duel: Player 1 : Player 2"
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords();
    }

    @Override
    protected void onDestroy() {
        requestNumber++;
        super.onDestroy();
    }
}