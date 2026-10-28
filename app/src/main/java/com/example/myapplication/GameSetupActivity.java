package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class GameSetupActivity extends AppCompatActivity {

    public static final String EXTRA_GAME_ID = "GAME_ID";

    private String gameId;
    private boolean isSurvival = true;

    private TextView tvModeDescription;

    private String survivalDescription;
    private String duelDescription;
    private String survivalInstructions;
    private String duelInstructions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_setup);

        gameId = getIntent().getStringExtra(EXTRA_GAME_ID);
        if (gameId == null) {
            gameId = "MATH";
        }

        android.widget.ImageView gameArtwork =
                findViewById(R.id.ivGameArtwork);
        if ("MATH".equals(gameId)) {
            gameArtwork.setImageResource(R.drawable.art_math_rush);
            gameArtwork.setVisibility(android.view.View.VISIBLE);

        } else if ("GUESS".equals(gameId)) {
            gameArtwork.setImageResource(R.drawable.art_guess_person);
            gameArtwork.setVisibility(android.view.View.VISIBLE);

        } else if ("MEMORY".equals(gameId)) {
            gameArtwork.setImageResource(R.drawable.art_memory_detective);
            gameArtwork.setVisibility(android.view.View.VISIBLE);

        } else if ("COLOUR".equals(gameId)) {
            gameArtwork.setImageResource(R.drawable.art_colour_clash);
            gameArtwork.setVisibility(android.view.View.VISIBLE);

        } else if ("ESCAPE".equals(gameId)) {
            gameArtwork.setImageResource(R.drawable.art_escape_grid);
            gameArtwork.setVisibility(android.view.View.VISIBLE);

        } else {
            gameArtwork.setVisibility(android.view.View.GONE);
        }

        TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        TextView tvGameTitle = findViewById(R.id.tvGameTitle);
        TextView tvGameTagline = findViewById(R.id.tvGameTagline);
        TextView tvGameSkills = findViewById(R.id.tvGameSkills);
        TextView tvGameDifficulty = findViewById(R.id.tvGameDifficulty);

        tvModeDescription = findViewById(R.id.tvModeDescription);

        RadioGroup rgGameMode = findViewById(R.id.rgGameMode);
        Button btnPlay = findViewById(R.id.btnPlay);
        Button btnHowToPlay = findViewById(R.id.btnHowToPlay);

        String title;
        String tagline;
        String skills;

        switch (gameId) {
            case "GUESS":
                title = "Guess the Person";
                tagline = "Ask questions. Uncover the mystery!";
                skills = "Observation, deduction and reasoning";

                survivalDescription =
                        "Find the secret character in 90 seconds. You have 3 guesses.";

                duelDescription =
                        "Choose characters privately, then take turns finding "
                                + "your opponent's secret character.";

                survivalInstructions =
                        "YOUR GOAL\n"
                                + "Find the randomly chosen secret character.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Choose a question category, such as Hair or Accessories.\n"
                                + "2. Select a question. The game answers using the secret "
                                + "character's appearance.\n"
                                + "3. Characters that do not match are eliminated automatically.\n"
                                + "4. Select a remaining character and use the guess button "
                                + "to submit your answer.\n\n"
                                + "TIME AND ATTEMPTS\n"
                                + "You have 90 seconds and 3 guesses. A wrong guess removes "
                                + "that character and uses one attempt.\n\n"
                                + "WINNING AND SCORE\n"
                                + "Guess correctly before time or attempts run out. "
                                + "Use fewer questions and wrong guesses for a higher score.";

                duelInstructions =
                        "YOUR GOAL\n"
                                + "Discover your opponent's secret character first.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Each player privately selects a character and confirms.\n"
                                + "2. Pass the phone only when prompted.\n"
                                + "3. On your turn, ask one appearance question or make a guess.\n"
                                + "4. The answer refers to your opponent's character. "
                                + "Nonmatching candidates are eliminated automatically.\n"
                                + "5. Follow the handover prompt for the next turn.\n\n"
                                + "TIME AND ATTEMPTS\n"
                                + "Both players share 120 seconds of playing time. "
                                + "Each player has 3 guesses. The clock pauses for "
                                + "answer review and handover.\n\n"
                                + "WINNING\n"
                                + "A correct guess wins. Using all 3 guesses incorrectly "
                                + "loses the match. If time expires without a winner, "
                                + "the match is a draw.";
                break;

            case "MEMORY":
                title = "Memory Detective";
                tagline = "Flip cards. Find the matching pairs!";
                skills = "Memory, attention and observation";

                survivalDescription =
                        "Find all 6 pairs before time runs out. Matches add time!";

                duelDescription =
                        "Race to find pairs. A found pair opens on both boards, "
                                + "but only its finder earns points.";

                survivalInstructions =
                        "YOUR GOAL\n"
                                + "Find all 6 matching pairs.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Tap a hidden card to reveal its picture.\n"
                                + "2. Tap another card and look for the same picture.\n"
                                + "3. Matching cards stay open. Different cards turn "
                                + "face down again.\n"
                                + "4. Remember their positions and keep finding pairs.\n\n"
                                + "TIME AND POINTS\n"
                                + "Start with 45 seconds.\n"
                                + "Matching pair: +10 points and +5 seconds.\n"
                                + "Wrong pair: -3 seconds.\n\n"
                                + "WINNING\n"
                                + "Reveal all pairs before time expires. "
                                + "A completed board earns extra points for remaining time.";

                duelInstructions =
                        "YOUR GOAL\n"
                                + "Find more pairs than your opponent.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Sit at opposite ends of the phone.\n"
                                + "2. Both players have the same card arrangement.\n"
                                + "3. Flip two cards on your side to find a pair.\n"
                                + "4. A found pair opens automatically on BOTH boards. "
                                + "Only the player who finds it earns the points.\n\n"
                                + "TIME AND POINTS\n"
                                + "Both players share 60 seconds.\n"
                                + "Matching pair: +10 points to its finder.\n"
                                + "Wrong pair: -3 seconds from the shared clock.\n\n"
                                + "WINNING\n"
                                + "The match ends when all pairs are found or time expires. "
                                + "The higher score wins. Equal scores mean a draw.";
                break;

            case "COLOUR":
                title = "Colour Clash";
                tagline = "Ignore the word. Match the ink!";
                skills = "Focus, attention and quick decisions";

                survivalDescription =
                        "Choose the ink colour. Correct answers earn points and extra time.";

                duelDescription =
                        "Race to identify the ink colour. The first correct answer scores.";

                survivalInstructions =
                        "YOUR GOAL\n"
                                + "Score by identifying the colour of the letters.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Look at the ink colour of the displayed word.\n"
                                + "2. Ignore what the word says.\n"
                                + "3. Tap the option matching the ink colour.\n\n"
                                + "EXAMPLE\n"
                                + "If BLUE is written in red letters, choose RED.\n\n"
                                + "TIME AND POINTS\n"
                                + "Start with 30 seconds.\n"
                                + "Correct answer: +10 points and +2 seconds.\n"
                                + "Wrong answer: -3 seconds.\n\n"
                                + "Keep scoring until the clock reaches zero.";

                duelInstructions =
                        "YOUR GOAL\n"
                                + "Score more points than your opponent.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Sit at opposite ends of the phone.\n"
                                + "2. Both players see the same word and ink colour.\n"
                                + "3. Choose the INK colour, not the word's meaning.\n"
                                + "4. The first correct answer earns 10 points.\n\n"
                                + "TIME AND PENALTIES\n"
                                + "Both players share 60 seconds.\n"
                                + "A wrong answer removes 3 seconds. The other player "
                                + "can still answer that question.\n"
                                + "If both players answer incorrectly, a new word appears.\n\n"
                                + "WINNING\n"
                                + "The higher score at timeout wins. Equal scores mean a draw.";
                break;

            case "ESCAPE":
                title = "Escape the Grid";
                tagline = "Find a path. Beat the clock!";
                skills = "Planning, navigation and problem solving";

                survivalDescription =
                        "Navigate the maze with the arrow buttons and reach the exit "
                                + "within 45 seconds.";

                duelDescription =
                        "Take turns navigating the same maze. "
                                + "Efficient moves and remaining time determine your score.";

                survivalInstructions =
                        "YOUR GOAL\n"
                                + "Move your player from START to EXIT.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Look for an open route through the maze.\n"
                                + "2. Use the arrow buttons to move up, down, left or right.\n"
                                + "3. Move through open cells and avoid walls.\n\n"
                                + "TIME AND PENALTIES\n"
                                + "You have 45 seconds.\n"
                                + "Trying to move into a wall or outside the grid "
                                + "costs 2 seconds.\n\n"
                                + "WINNING AND SCORE\n"
                                + "Reach the exit before time expires. "
                                + "Fewer unnecessary moves and more remaining time "
                                + "give a higher score. Timeout earns 0 points.";

                duelInstructions =
                        "YOUR GOAL\n"
                                + "Earn the higher score on the same maze.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Player 1 navigates the maze first.\n"
                                + "2. Pass the phone when prompted.\n"
                                + "3. Player 2 starts the same maze from the beginning.\n"
                                + "4. Use the arrow buttons to reach the exit.\n\n"
                                + "TIME AND PENALTIES\n"
                                + "Each player gets their own 45-second timer.\n"
                                + "A blocked move costs 2 seconds.\n\n"
                                + "WINNING AND SCORE\n"
                                + "Finishing with fewer unnecessary moves and more "
                                + "remaining time earns a higher score. Timeout earns 0.\n"
                                + "After both turns, the higher score wins. "
                                + "Equal scores mean a draw.";
                break;

            case "MATH":
            default:
                gameId = "MATH";
                title = "Math Rush";
                tagline = "Think fast. Solve more!";
                skills = "Mental maths, speed and accuracy";

                survivalDescription =
                        "Solve addition, subtraction, multiplication and division. "
                                + "Correct answers keep you playing longer.";

                duelDescription =
                        "Face your opponent across the phone. "
                                + "The first correct answer earns points.";

                survivalInstructions =
                        "YOUR GOAL\n"
                                + "Solve as many calculations as possible.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Read the calculation.\n"
                                + "2. Tap the correct answer from the options.\n"
                                + "3. Questions include +, -, × and ÷. "
                                + "Division answers are whole numbers.\n\n"
                                + "TIME AND POINTS\n"
                                + "Start with 30 seconds.\n"
                                + "Correct answer: +10 points and +5 seconds.\n"
                                + "Wrong answer: -5 seconds.\n\n"
                                + "Keep scoring until time expires. "
                                + "Calculations become harder as your score increases.";

                duelInstructions =
                        "YOUR GOAL\n"
                                + "Score more points than your opponent.\n\n"
                                + "HOW TO PLAY\n"
                                + "1. Sit at opposite ends of the phone.\n"
                                + "2. Both players see the same calculation.\n"
                                + "3. The first correct answer earns 10 points.\n"
                                + "4. Questions include +, -, × and ÷.\n\n"
                                + "TIME AND PENALTIES\n"
                                + "Both players share 60 seconds.\n"
                                + "A wrong answer removes 5 seconds. The other player "
                                + "can still answer that question.\n"
                                + "If both answer incorrectly, a new question appears.\n\n"
                                + "WINNING\n"
                                + "The higher score at timeout wins. Equal scores mean a draw.";
                break;
        }
        tvGameTitle.setText(title);
        tvGameTagline.setText(tagline);
        tvGameSkills.setText("Skills: " + skills);
        tvGameDifficulty.setText("Difficulty: Progressive");

        if (savedInstanceState != null) {
            isSurvival = savedInstanceState.getBoolean("SURVIVAL", true);
        }

        rgGameMode.check(isSurvival
                ? R.id.rbSurvival : R.id.rbDuel);

        updateModeDescription();

        rgGameMode.setOnCheckedChangeListener((group, checkedId) -> {
            isSurvival = checkedId == R.id.rbSurvival;
            updateModeDescription();
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnHowToPlay.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle(isSurvival
                                ? "How to Play — Survival"
                                : "How to Play — Duel")
                        .setMessage(isSurvival
                                ? survivalInstructions : duelInstructions)
                        .setPositiveButton("Got it", null)
                        .show()
        );

        // Temporary feedback until the gameplay activities are added.
        btnPlay.setOnClickListener(v -> {
            if ("MATH".equals(gameId)) {

                Intent intent = new Intent(
                        GameSetupActivity.this,
                        MathRushActivity.class
                );

                intent.putExtra(
                        MathRushActivity.EXTRA_SURVIVAL,
                        isSurvival
                );

                startActivity(intent);

            } else if ("GUESS".equals(gameId)) {

                Intent intent = new Intent(
                        GameSetupActivity.this,
                        isSurvival
                                ? GuessGameActivity.class
                                : GuessDuelActivity.class
                );

                startActivity(intent);

            } else if ("MEMORY".equals(gameId)) {

                Intent intent = new Intent(
                        GameSetupActivity.this,
                        isSurvival
                                ? MemoryGameActivity.class
                                : MemoryDuelActivity.class
                );

                startActivity(intent);

            } else if ("COLOUR".equals(gameId)) {

                Intent intent = new Intent(
                        GameSetupActivity.this,
                        ColourGameActivity.class
                );

                intent.putExtra(
                        ColourGameActivity.EXTRA_SURVIVAL,
                        isSurvival
                );

                startActivity(intent);

            } else if ("ESCAPE".equals(gameId)) {

                Intent intent = new Intent(
                        GameSetupActivity.this,
                        isSurvival
                                ? EscapeGameActivity.class
                                : EscapeDuelActivity.class
                );

                startActivity(intent);

            } else {

                new AlertDialog.Builder(GameSetupActivity.this)
                        .setTitle("Coming next")
                        .setMessage(
                                "This game's selected mode is still being built."
                        )
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void updateModeDescription() {
        tvModeDescription.setText(isSurvival
                ? survivalDescription : duelDescription);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("SURVIVAL", isSurvival);
        super.onSaveInstanceState(outState);
    }
}