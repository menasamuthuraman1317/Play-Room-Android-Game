package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class EscapeBoard {

    private final GridLayout grid;
    private final List<TextView> cells = new ArrayList<>();

    private EscapeRound round;

    public EscapeBoard(GridLayout grid) {
        this.grid = grid;

        grid.removeAllViews();
        grid.setColumnCount(EscapeRound.SIZE);

        LayoutInflater inflater =
                LayoutInflater.from(grid.getContext());

        for (int i = 0; i < EscapeRound.CELL_COUNT; i++) {
            TextView cell = (TextView) inflater.inflate(
                    R.layout.item_escape_cell,
                    grid,
                    false
            );

            cells.add(cell);
            grid.addView(cell);
        }

        grid.addOnLayoutChangeListener(
                (view, left, top, right, bottom,
                 oldLeft, oldTop, oldRight, oldBottom) -> {

                    int width = right - left;
                    int oldWidth = oldRight - oldLeft;

                    if (width > 0 && width != oldWidth) {
                        resizeCells(width);
                    }
                }
        );

        grid.post(() -> {
            if (grid.getWidth() > 0) {
                resizeCells(grid.getWidth());
            }
        });
    }

    public void setRound(EscapeRound round) {
        if (round == null) {
            throw new IllegalArgumentException(
                    "An escape round is required."
            );
        }

        this.round = round;
        render();
    }

    public void render() {
        if (round == null) {
            return;
        }

        for (int i = 0; i < cells.size(); i++) {
            TextView cell = cells.get(i);

            boolean wall = round.isWall(i);
            boolean exit = i == EscapeRound.EXIT;
            boolean player = i == round.getPosition();

            cell.setSelected(wall);
            cell.setActivated(exit);

            if (player) {
                cell.setText("●");
                cell.setTextSize(28);
                cell.setTextColor(Color.rgb(249, 115, 22));

            } else if (exit) {
                cell.setText("EXIT");
                cell.setTextSize(12);
                cell.setTextColor(Color.rgb(21, 128, 61));

            } else if (i == EscapeRound.START) {
                cell.setText("START");
                cell.setTextSize(10);
                cell.setTextColor(Color.rgb(23, 54, 93));

            } else {
                cell.setText("");
            }

            int row = i / EscapeRound.SIZE + 1;
            int column = i % EscapeRound.SIZE + 1;

            String description = "Row " + row + ", column " + column;

            if (player) {
                description += ", player";
            }

            if (exit) {
                description += ", exit";
            } else if (wall) {
                description += ", wall";
            } else if (i == EscapeRound.START) {
                description += ", start";
            } else {
                description += ", open path";
            }

            cell.setContentDescription(description);
        }
    }

    private void resizeCells(int availableWidth) {
        int margin = dp(3);

        int cellSize = Math.max(
                1,
                availableWidth / EscapeRound.SIZE - margin * 2
        );

        // Keep the maze compact on large screens.
        int cellHeight = Math.min(cellSize, dp(64));

        grid.removeAllViews();

        for (int i = 0; i < cells.size(); i++) {
            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams(
                            GridLayout.spec(i / EscapeRound.SIZE),
                            GridLayout.spec(i % EscapeRound.SIZE)
                    );

            params.width = cellSize;
            params.height = cellHeight;
            params.setMargins(margin, margin, margin, margin);

            grid.addView(cells.get(i), params);
        }
    }

    private int dp(int value) {
        return Math.round(
                value * grid.getResources()
                        .getDisplayMetrics().density
        );
    }
}