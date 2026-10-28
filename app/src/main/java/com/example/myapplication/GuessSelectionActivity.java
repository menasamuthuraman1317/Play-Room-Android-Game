package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedHashMap;
import java.util.Map;

public class GuessSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_PLAYER_NUMBER = "GUESS_PLAYER_NUMBER";
    public static final String EXTRA_CHARACTER_ID = "GUESS_CHARACTER_ID";

    private static final String STATE_SELECTED_ID = "SELECTED_CHARACTER";

    private final Map<String, View> characterCards =
            new LinkedHashMap<>();

    private GridLayout grid;
    private TextView selectionStatus;
    private Button confirmButton;

    private String selectedId;
    private boolean confirmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guess_selection);

        grid = findViewById(R.id.gridGuessSelection);
        selectionStatus = findViewById(R.id.tvGuessSelectionStatus);
        confirmButton = findViewById(R.id.btnGuessConfirm);

        TextView title = findViewById(R.id.tvGuessSelectionTitle);
        TextView instructions =
                findViewById(R.id.tvGuessSelectionInstructions);

        int playerNumber = getIntent().getIntExtra(
                EXTRA_PLAYER_NUMBER, 1
        );

        title.setText("PLAYER " + playerNumber + ": CHOOSE");

        instructions.setText(
                "Choose your secret character while the other player "
                        + "looks away. Then tap CONFIRM."
        );

        if (savedInstanceState != null) {
            selectedId = savedInstanceState.getString(
                    STATE_SELECTED_ID
            );
        }

        createCharacterCards();
        updateSelection();

        // Adjust column count when the available width changes.
        grid.addOnLayoutChangeListener(
                (view, left, top, right, bottom,
                 oldLeft, oldTop, oldRight, oldBottom) -> {

                    int width = right - left;
                    int oldWidth = oldRight - oldLeft;

                    if (width > 0 && width != oldWidth) {
                        resizeCards(width);
                    }
                }
        );

        confirmButton.setOnClickListener(v -> confirmSelection());

        findViewById(R.id.btnGuessSelectionBack)
                .setOnClickListener(v -> {
                    setResult(RESULT_CANCELED);
                    finish();
                });
    }

    private void createCharacterCards() {
        grid.removeAllViews();
        characterCards.clear();

        for (GuessCharacter character : GuessCharacterBank.getAll()) {
            View card = getLayoutInflater().inflate(
                    R.layout.item_guess_character,
                    grid,
                    false
            );

            ImageView portrait =
                    card.findViewById(R.id.ivGuessPortrait);

            TextView name =
                    card.findViewById(R.id.tvGuessCharacterName);

            portrait.setImageDrawable(
                    new GuessPortraitDrawable(character)
            );

            name.setText(character.name);

            // The whole card is one accessible selection target.
            card.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_YES
            );

            portrait.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            );

            name.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            );

            card.findViewById(R.id.tvGuessCharacterStatus)
                    .setImportantForAccessibility(
                            View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    );

            card.setOnClickListener(v -> {
                selectedId = character.id;
                updateSelection();
            });

            characterCards.put(character.id, card);
            grid.addView(card);
        }
    }

    private void resizeCards(int availableWidth) {
        int columns = Math.max(
                2,
                Math.min(6, availableWidth / dp(108))
        );

        int spacing = dp(4);
        int cardWidth = availableWidth / columns - spacing * 2;

        // Clear old column positions before changing column count.
        grid.removeAllViews();
        grid.setColumnCount(columns);

        int position = 0;

        for (View card : characterCards.values()) {
            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams(
                            GridLayout.spec(position / columns),
                            GridLayout.spec(position % columns)
                    );

            params.width = cardWidth;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(spacing, spacing, spacing, spacing);

            grid.addView(card, params);
            position++;
        }
    }

    private void updateSelection() {
        boolean hasSelection = selectedId != null
                && characterCards.containsKey(selectedId);

        for (GuessCharacter character : GuessCharacterBank.getAll()) {
            View card = characterCards.get(character.id);

            if (card == null) {
                continue;
            }

            boolean selected = character.id.equals(selectedId);
            card.setSelected(selected);

            TextView status =
                    card.findViewById(R.id.tvGuessCharacterStatus);

            status.setText(selected ? "SELECTED" : "");

            String gender = character.gender
                    == GuessCharacter.Gender.MAN ? "Man" : "Woman";

            card.setContentDescription(
                    character.name + ", " + gender
                            + (selected ? ", selected" : ", tap to select")
            );
        }

        confirmButton.setEnabled(hasSelection);
        confirmButton.setAlpha(hasSelection ? 1f : 0.45f);

        if (hasSelection) {
            GuessCharacter selected =
                    GuessCharacterBank.findById(selectedId);

            selectionStatus.setText(
                    selected.name + " selected — ready to confirm"
            );
        } else {
            selectionStatus.setText("Tap a character to select");
        }
    }

    private void confirmSelection() {
        if (confirmed || selectedId == null
                || !characterCards.containsKey(selectedId)) {
            return;
        }

        confirmed = true;
        confirmButton.setEnabled(false);

        Intent result = new Intent();
        result.putExtra(EXTRA_CHARACTER_ID, selectedId);

        setResult(RESULT_OK, result);
        finish();
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_SELECTED_ID, selectedId);
        super.onSaveInstanceState(outState);
    }
}