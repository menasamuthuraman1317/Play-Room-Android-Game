package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.cardMathRush).setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    GameSetupActivity.class
            );

            intent.putExtra(
                    GameSetupActivity.EXTRA_GAME_ID,
                    "MATH"
            );

            startActivity(intent);
        });
        findViewById(R.id.cardGuessPerson)
                .setOnClickListener(v -> openGame("GUESS"));

        findViewById(R.id.cardMemoryDetective)
                .setOnClickListener(v -> openGame("MEMORY"));

        findViewById(R.id.cardColourClash)
                .setOnClickListener(v -> openGame("COLOUR"));

        findViewById(R.id.cardEscapeGrid)
                .setOnClickListener(v -> openGame("ESCAPE"));

        findViewById(R.id.btnLeaderboard)
                .setOnClickListener(v -> {
                    Intent intent = new Intent(
                            MainActivity.this,
                            LeaderboardActivity.class
                    );
                    startActivity(intent);
                });
    }

    private void openGame(String gameId) {
        Intent intent = new Intent(
                MainActivity.this, GameSetupActivity.class);

        intent.putExtra(GameSetupActivity.EXTRA_GAME_ID, gameId);
        startActivity(intent);
    }
}