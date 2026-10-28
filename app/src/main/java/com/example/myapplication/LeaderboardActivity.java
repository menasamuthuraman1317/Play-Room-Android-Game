package com.example.myapplication;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private final String[] gameNames = {
            "Math Rush",
            "Guess the Person",
            "Memory Detective",
            "Colour Clash",
            "Escape the Grid"
    };

    private final String[] gameIds = {
            "MATH",
            "GUESS",
            "MEMORY",
            "COLOUR",
            "ESCAPE"
    };

    private Spinner gameSpinner;
    private RadioGroup modeGroup;
    private TextView tvSummary;
    private TextView tvStatus;
    private LinearLayout resultsLayout;

    private boolean survival = true;
    private int selectedGame = 0;

    // Prevent an older database response from replacing a newer filter.
    private int requestNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        gameSpinner = findViewById(R.id.spinnerLeaderboardGame);
        modeGroup = findViewById(R.id.rgLeaderboardMode);
        tvSummary = findViewById(R.id.tvLeaderboardSummary);
        tvStatus = findViewById(R.id.tvLeaderboardStatus);
        resultsLayout = findViewById(R.id.layoutLeaderboardResults);

        if (savedInstanceState != null) {
            selectedGame = savedInstanceState.getInt("selectedGame", 0);
            survival = savedInstanceState.getBoolean("survival", true);
        }

        selectedGame = Math.max(
                0, Math.min(selectedGame, gameIds.length - 1));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                gameNames
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        gameSpinner.setAdapter(adapter);
        gameSpinner.setSelection(selectedGame);

        modeGroup.check(survival
                ? R.id.rbLeaderboardSurvival
                : R.id.rbLeaderboardDuel);

        gameSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        selectedGame = position;
                        refreshResults();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            survival = checkedId == R.id.rbLeaderboardSurvival;
            refreshResults();
        });

        findViewById(R.id.btnLeaderboardBack)
                .setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshResults();
    }

    private void refreshResults() {
        final int currentRequest = ++requestNumber;
        final boolean requestedSurvival = survival;
        final String requestedGameName = gameNames[selectedGame];

        resultsLayout.removeAllViews();
        tvStatus.setText("Loading results…");

        tvSummary.setText(requestedSurvival
                ? requestedGameName + " — High scores"
                : requestedGameName + " — Recent duels");

        GameDatabaseHelper.getInstance(this).loadResults(
                gameIds[selectedGame],
                requestedSurvival,
                (results, success) -> {
                    if (isFinishing() || isDestroyed()
                            || currentRequest != requestNumber) {
                        return;
                    }

                    if (!success) {
                        tvStatus.setText(
                                "Could not load results. "
                                        + "Select a game or mode to try again.");
                        return;
                    }

                    showResults(results, requestedSurvival);
                }
        );
    }

    private void showResults(
            List<GameDatabaseHelper.GameResult> results,
            boolean showSurvival) {

        resultsLayout.removeAllViews();

        if (results.isEmpty()) {
            tvStatus.setText(
                    "No completed rounds yet for this game and mode.");
            return;
        }

        if (showSurvival) {
            tvStatus.setText(
                    "Best score: " + results.get(0).playerTwoScore
                            + " points • Showing "
                            + results.size() + " results");
        } else {
            tvStatus.setText(
                    "Showing " + results.size()
                            + " recent matches • Newest first");
        }

        DateFormat dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
        );

        int rank = 0;
        int previousScore = Integer.MIN_VALUE;

        for (int i = 0; i < results.size(); i++) {
            GameDatabaseHelper.GameResult result = results.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(20), dp(16), dp(20), dp(16));
            card.setBackgroundResource(R.drawable.bg_game_card);

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            cardParams.bottomMargin = dp(12);
            card.setLayoutParams(cardParams);

            String heading;
            String details;

            if (showSurvival) {
                if (result.playerTwoScore != previousScore) {
                    rank = i + 1;
                    previousScore = result.playerTwoScore;
                }

                heading = "#" + rank + "  •  "
                        + result.playerTwoScore + " points";

                details = "Survival";
            } else {
                if (result.playerOneScore == result.playerTwoScore) {
                    heading = "Draw";
                } else {
                    heading = result.playerOneScore > result.playerTwoScore
                            ? "Player 1 won"
                            : "Player 2 won";
                }

                details = "Player 1: " + result.playerOneScore
                        + "  •  Player 2: " + result.playerTwoScore;
            }

            TextView title = makeText(heading, 20, true);
            TextView scoreDetails = makeText(details, 15, false);

            TextView date = makeText(
                    dateFormat.format(new Date(result.completedAt)),
                    13,
                    false
            );

            scoreDetails.setPadding(0, dp(8), 0, 0);
            date.setPadding(0, dp(8), 0, 0);

            card.addView(title);
            card.addView(scoreDetails);
            card.addView(date);

            resultsLayout.addView(card);
        }
    }

    private TextView makeText(String text, int size, boolean bold) {
        TextView view = new TextView(this);

        view.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(
                this, R.color.game_text));

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("selectedGame", selectedGame);
        outState.putBoolean("survival", survival);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        requestNumber++;
        super.onDestroy();
    }
}