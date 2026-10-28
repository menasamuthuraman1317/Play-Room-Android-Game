package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GuessBoard {

    public interface SelectionListener {
        void onSelected(String characterId);
    }

    private final GridLayout grid;
    private final SelectionListener listener;

    private final Map<String, View> cards = new LinkedHashMap<>();

    private GuessRound round;
    private String selectedId;
    private boolean interactionEnabled = true;

    public GuessBoard(
            GridLayout grid,
            SelectionListener listener
    ) {
        this.grid = grid;
        this.listener = listener;

        createCards();

        grid.addOnLayoutChangeListener(
                (view, left, top, right, bottom,
                 oldLeft, oldTop, oldRight, oldBottom) -> {

                    int width = right - left;
                    int previousWidth = oldRight - oldLeft;

                    if (width > 0 && width != previousWidth) {
                        resizeCards(width);
                    }
                }
        );

        grid.post(() -> {
            if (grid.getWidth() > 0) {
                resizeCards(grid.getWidth());
            }
        });
    }

    private void createCards() {
        grid.removeAllViews();

        LayoutInflater inflater =
                LayoutInflater.from(grid.getContext());

        for (GuessCharacter character : GuessCharacterBank.getAll()) {
            View card = inflater.inflate(
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

            // Treat the whole card as one accessible button.
            card.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_YES
            );

            name.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            );

            card.findViewById(R.id.tvGuessCharacterStatus)
                    .setImportantForAccessibility(
                            View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    );

            card.setOnClickListener(v -> {
                if (!interactionEnabled || round == null
                        || round.isFinished()
                        || !round.isRemaining(character.id)) {
                    return;
                }

                selectedId = character.id;
                render();

                if (listener != null) {
                    listener.onSelected(selectedId);
                }
            });

            cards.put(character.id, card);
            grid.addView(card);
        }

        render();
    }

    public void setRound(GuessRound round) {
        this.round = round;
        selectedId = null;
        render();
    }

    public void setInteractionEnabled(boolean enabled) {
        interactionEnabled = enabled;
        render();
    }

    public String getSelectedId() {
        return selectedId;
    }

    public void restoreSelection(String characterId) {
        selectedId = characterId;
        render();
    }

    public void clearSelection() {
        selectedId = null;
        render();
    }

    public void render() {
        if (selectedId != null
                && (round == null || !round.isRemaining(selectedId))) {
            selectedId = null;
        }

        for (GuessCharacter character : GuessCharacterBank.getAll()) {
            View card = cards.get(character.id);

            boolean remaining = round != null
                    && round.isRemaining(character.id);

            boolean selected = remaining
                    && character.id.equals(selectedId);

            boolean enabled = remaining
                    && interactionEnabled
                    && !round.isFinished();

            card.setSelected(selected);
            card.setEnabled(enabled);
            card.setAlpha(remaining ? 1f : 0.3f);

            TextView status =
                    card.findViewById(R.id.tvGuessCharacterStatus);

            String gender = character.gender
                    == GuessCharacter.Gender.MAN ? "Man" : "Woman";

            if (!remaining) {
                status.setText("ELIMINATED");
            } else if (selected) {
                status.setText("SELECTED");
            } else {
                status.setText(gender);
            }

            card.setContentDescription(
                    character.name + ", " + gender + ", "
                            + (!remaining ? "eliminated"
                            : selected ? "selected for guess"
                            : "available to guess")
            );
        }
    }

    private void resizeCards(int availableWidth) {
        int columns = Math.max(
                2,
                Math.min(6, availableWidth / dp(108))
        );

        int margin = dp(4);
        int width = availableWidth / columns - margin * 2;

        grid.removeAllViews();
        grid.setColumnCount(columns);

        int position = 0;

        for (View card : cards.values()) {
            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams(
                            GridLayout.spec(position / columns),
                            GridLayout.spec(position % columns)
                    );

            params.width = width;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(margin, margin, margin, margin);

            grid.addView(card, params);
            position++;
        }
    }

    private int dp(int value) {
        return Math.round(
                value * grid.getResources()
                        .getDisplayMetrics().density
        );
    }
}