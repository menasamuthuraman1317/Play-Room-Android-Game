package com.example.myapplication;

import android.os.Bundle;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;

public final class EscapeRound {

    public static final int SIZE = 5;
    public static final int CELL_COUNT = SIZE * SIZE;
    public static final int START = 0;
    public static final int EXIT = CELL_COUNT - 1;

    public enum MoveResult {
        BLOCKED,
        MOVED,
        ESCAPED,
        ALREADY_FINISHED
    }

    private final boolean[] walls;
    private final int minimumMoves;

    private int position = START;
    private int moves;

    public EscapeRound() {
        walls = generateMaze();
        minimumMoves = shortestPath(walls);
    }

    private EscapeRound(boolean[] walls, int position, int moves) {
        this.walls = walls.clone();
        this.minimumMoves = shortestPath(this.walls);
        this.position = position;
        this.moves = moves;
    }

    public MoveResult move(int rowChange, int columnChange) {
        if (hasEscaped()) {
            return MoveResult.ALREADY_FINISHED;
        }

        // Only one step up, down, left, or right is allowed.
        if (Math.abs(rowChange) + Math.abs(columnChange) != 1) {
            return MoveResult.BLOCKED;
        }

        int row = position / SIZE + rowChange;
        int column = position % SIZE + columnChange;

        if (row < 0 || row >= SIZE || column < 0 || column >= SIZE) {
            return MoveResult.BLOCKED;
        }

        int destination = row * SIZE + column;

        if (walls[destination]) {
            return MoveResult.BLOCKED;
        }

        position = destination;
        moves++;

        return hasEscaped()
                ? MoveResult.ESCAPED
                : MoveResult.MOVED;
    }

    public boolean isWall(int index) {
        checkIndex(index);
        return walls[index];
    }

    public int getPosition() {
        return position;
    }

    public int getMoves() {
        return moves;
    }

    public int getMinimumMoves() {
        return minimumMoves;
    }

    public boolean hasEscaped() {
        return position == EXIT;
    }

    private static boolean[] generateMaze() {
        Random random = new Random();

        for (int attempt = 0; attempt < 100; attempt++) {
            boolean[] candidate = new boolean[CELL_COUNT];
            int wallCount = 0;

            for (int i = 1; i < EXIT; i++) {
                candidate[i] = random.nextFloat() < 0.3f;

                if (candidate[i]) {
                    wallCount++;
                }
            }

            // Accept only mazes with obstacles and a valid exit route.
            if (wallCount >= 5 && shortestPath(candidate) >= 0) {
                return candidate;
            }
        }

        // Guaranteed playable fallback.
        boolean[] fallback = new boolean[CELL_COUNT];
        Arrays.fill(fallback, true);

        // Open the left column and the bottom row.
        for (int row = 0; row < SIZE; row++) {
            fallback[row * SIZE] = false;
        }

        for (int column = 0; column < SIZE; column++) {
            fallback[(SIZE - 1) * SIZE + column] = false;
        }

        return fallback;
    }

    private static int shortestPath(boolean[] maze) {
        int[] distance = new int[CELL_COUNT];
        Arrays.fill(distance, -1);

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(START);
        distance[START] = 0;

        int[] rowChanges = {-1, 1, 0, 0};
        int[] columnChanges = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            int current = queue.removeFirst();

            if (current == EXIT) {
                return distance[current];
            }

            int row = current / SIZE;
            int column = current % SIZE;

            for (int direction = 0; direction < 4; direction++) {
                int nextRow = row + rowChanges[direction];
                int nextColumn = column + columnChanges[direction];

                if (nextRow < 0 || nextRow >= SIZE
                        || nextColumn < 0 || nextColumn >= SIZE) {
                    continue;
                }

                int next = nextRow * SIZE + nextColumn;

                if (!maze[next] && distance[next] == -1) {
                    distance[next] = distance[current] + 1;
                    queue.addLast(next);
                }
            }
        }

        return -1;
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= CELL_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid maze cell: " + index
            );
        }
    }

    public Bundle saveState() {
        Bundle state = new Bundle();

        state.putBooleanArray("walls", walls.clone());
        state.putInt("position", position);
        state.putInt("moves", moves);

        return state;
    }

    public static EscapeRound restoreState(Bundle state) {
        if (state == null) {
            throw new IllegalArgumentException("Maze state is missing.");
        }

        boolean[] walls = state.getBooleanArray("walls");
        int position = state.getInt("position", START);
        int moves = state.getInt("moves", 0);

        if (walls == null || walls.length != CELL_COUNT) {
            throw new IllegalArgumentException("Invalid maze size.");
        }

        checkIndex(position);

        if (walls[START] || walls[EXIT] || walls[position]
                || moves < 0 || shortestPath(walls) < 0) {
            throw new IllegalArgumentException("Invalid maze state.");
        }

        return new EscapeRound(walls, position, moves);
    }
}