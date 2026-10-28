package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class MemoryBoard {

    public interface CardListener {
        void onCardTapped(int index);
    }

    private final GridLayout grid;
    private final CardListener listener;

    private final List<TextView> cardViews = new ArrayList<>();

    private final boolean compact;
    private MemoryRound round;
    private boolean interactionEnabled = true;

    public MemoryBoard(GridLayout grid, CardListener listener) {
        this(grid, listener, false);
    }

    public MemoryBoard(
            GridLayout grid,
            CardListener listener,
            boolean compact
    ) {
        this.grid = grid;
        this.listener = listener;
        this.compact = compact;

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
    }

    public void setRound(MemoryRound round) {
        if (round == null) {
            throw new IllegalArgumentException(
                    "A memory round is required."
            );
        }

        this.round = round;

        grid.removeAllViews();
        cardViews.clear();

        LayoutInflater inflater =
                LayoutInflater.from(grid.getContext());

        for (int i = 0; i < round.getCardCount(); i++) {
            final int index = i;

            TextView card = (TextView) inflater.inflate(
                    R.layout.item_memory_card,
                    grid,
                    false
            );

            card.setOnClickListener(v -> {
                if (!interactionEnabled
                        || this.round.isComplete()
                        || this.round.hasPendingMismatch()
                        || this.round.isFaceUp(index)) {
                    return;
                }

                if (listener != null) {
                    listener.onCardTapped(index);
                }
            });

            cardViews.add(card);
            grid.addView(card);
        }

        render();

        grid.post(() -> {
            if (grid.getWidth() > 0) {
                resizeCards(grid.getWidth());
            }
        });
    }

    public void setInteractionEnabled(boolean enabled) {
        interactionEnabled = enabled;
        render();
    }

    public void render() {
        if (round == null) {
            return;
        }

        for (int i = 0; i < cardViews.size(); i++) {
            TextView card = cardViews.get(i);

            boolean matched = round.isMatched(i);
            boolean faceUp = round.isFaceUp(i);

            boolean enabled = interactionEnabled
                    && !round.isComplete()
                    && !round.hasPendingMismatch()
                    && !faceUp;

            card.setActivated(matched);
            card.setSelected(faceUp);
            card.setEnabled(enabled);

            card.setText(faceUp ? round.getSymbol(i) : "?");

            card.setTextColor(
                    faceUp ? Color.rgb(23, 54, 93) : Color.WHITE
            );

            String description = "Card " + (i + 1);

            if (matched) {
                description += ", matched, " + round.getSymbol(i);
            } else if (faceUp) {
                description += ", revealed, " + round.getSymbol(i);
            } else {
                description += ", hidden, tap to reveal";
            }

            card.setContentDescription(description);
        }
    }

    private void resizeCards(int availableWidth) {
        if (cardViews.isEmpty()) {
            return;
        }

        int columns = compact
                ? 4
                : (availableWidth >= dp(480) ? 4 : 3);

        int margin = dp(compact ? 3 : 5);

        int cardWidth = availableWidth / columns - margin * 2;

        int cardHeight = compact
                ? dp(48)
                : Math.max(dp(72), Math.min(dp(112), cardWidth));

        grid.removeAllViews();
        grid.setColumnCount(columns);

        for (int i = 0; i < cardViews.size(); i++) {
            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams(
                            GridLayout.spec(i / columns),
                            GridLayout.spec(i % columns)
                    );

            params.width = cardWidth;
            params.height = cardHeight;
            params.setMargins(margin, margin, margin, margin);

            grid.addView(cardViews.get(i), params);
        }
    }

    private int dp(int value) {
        return Math.round(
                value * grid.getResources()
                        .getDisplayMetrics().density
        );
    }
}