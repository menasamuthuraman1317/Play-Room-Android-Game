package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GuessDuelSetupActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_ONE_SECRET =
            "PLAYER_ONE_SECRET";

    public static final String EXTRA_PLAYER_TWO_SECRET =
            "PLAYER_TWO_SECRET";

    private static final int REQUEST_PLAYER_ONE = 401;
    private static final int REQUEST_PLAYER_TWO = 402;

    private String playerOneSecret;
    private boolean selectionOpen;

    private TextView title;
    private TextView message;
    private Button readyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guess_handoff);

        title = findViewById(R.id.tvGuessHandoffTitle);
        message = findViewById(R.id.tvGuessHandoffMessage);
        readyButton = findViewById(R.id.btnGuessHandoffReady);

        if (savedInstanceState != null) {
            playerOneSecret = savedInstanceState.getString(
                    "playerOneSecret"
            );

            selectionOpen = savedInstanceState.getBoolean(
                    "selectionOpen", false
            );
        }

        readyButton.setOnClickListener(v -> openSelection());

        findViewById(R.id.btnGuessHandoffExit)
                .setOnClickListener(v -> {
                    setResult(RESULT_CANCELED);
                    finish();
                });

        renderHandoff();
    }

    private void renderHandoff() {
        int player = playerOneSecret == null ? 1 : 2;

        title.setText("PASS TO PLAYER " + player);

        message.setText(
                "Player " + player
                        + ", choose your secret character on the next screen."
                        + "\n\nThe other player must look away."
                        + "\n\nDo not reveal your selection."
        );

        readyButton.setText("PLAYER " + player + " IS READY");
        readyButton.setEnabled(!selectionOpen);
    }

    private void openSelection() {
        if (selectionOpen) {
            return;
        }

        int player = playerOneSecret == null ? 1 : 2;

        selectionOpen = true;
        readyButton.setEnabled(false);

        Intent intent = new Intent(
                this,
                GuessSelectionActivity.class
        );

        intent.putExtra(
                GuessSelectionActivity.EXTRA_PLAYER_NUMBER,
                player
        );

        startActivityForResult(
                intent,
                player == 1 ? REQUEST_PLAYER_ONE : REQUEST_PLAYER_TWO
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_PLAYER_ONE
                && requestCode != REQUEST_PLAYER_TWO) {
            return;
        }

        selectionOpen = false;

        // Leaving character selection cancels the whole setup.
        if (resultCode != RESULT_OK || data == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        String selectedId = data.getStringExtra(
                GuessSelectionActivity.EXTRA_CHARACTER_ID
        );

        if (!isValidCharacter(selectedId)) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        if (requestCode == REQUEST_PLAYER_ONE) {
            playerOneSecret = selectedId;
            renderHandoff();
            return;
        }

        if (!isValidCharacter(playerOneSecret)) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Intent result = new Intent();

        result.putExtra(
                EXTRA_PLAYER_ONE_SECRET,
                playerOneSecret
        );

        result.putExtra(
                EXTRA_PLAYER_TWO_SECRET,
                selectedId
        );

        setResult(RESULT_OK, result);
        finish();
    }

    private boolean isValidCharacter(String id) {
        if (id == null) {
            return false;
        }

        for (GuessCharacter character : GuessCharacterBank.getAll()) {
            if (character.id.equals(id)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("playerOneSecret", playerOneSecret);
        outState.putBoolean("selectionOpen", selectionOpen);
        super.onSaveInstanceState(outState);
    }
}