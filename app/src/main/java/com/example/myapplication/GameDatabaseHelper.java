package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "playroom.db";
    private static final int DATABASE_VERSION = 1;

    private static GameDatabaseHelper instance;

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface SaveCallback {
        void onComplete(boolean success);
    }

    public static synchronized GameDatabaseHelper getInstance(
            Context context) {

        if (instance == null) {
            instance = new GameDatabaseHelper(
                    context.getApplicationContext());
        }

        return instance;
    }

    private GameDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE game_results ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "round_id TEXT NOT NULL UNIQUE, "
                        + "game_id TEXT NOT NULL, "
                        + "mode TEXT NOT NULL "
                        + "CHECK(mode IN ('SURVIVAL', 'DUEL')), "
                        + "player_one_score INTEGER NOT NULL DEFAULT 0, "
                        + "player_two_score INTEGER NOT NULL DEFAULT 0, "
                        + "completed_at INTEGER NOT NULL"
                        + ")"
        );

        db.execSQL(
                "CREATE INDEX index_results_game_mode "
                        + "ON game_results(game_id, mode)"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        // Add migrations here when the database version changes.
        // Do not delete existing player records.
        throw new IllegalStateException(
                "Database migration required from "
                        + oldVersion + " to " + newVersion);
    }

    public void saveResult(
            String roundId,
            String gameId,
            boolean survival,
            int playerOneScore,
            int playerTwoScore,
            long completedAt,
            SaveCallback callback) {

        databaseExecutor.execute(() -> {
            boolean success;

            try {
                SQLiteDatabase db = getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put("round_id", roundId);
                values.put("game_id", gameId);
                values.put("mode", survival ? "SURVIVAL" : "DUEL");
                values.put("player_one_score", playerOneScore);
                values.put("player_two_score", playerTwoScore);
                values.put("completed_at", completedAt);

                // Re-saving the same round updates its record.
                // It does not create a duplicate leaderboard entry.
                db.insertWithOnConflict(
                        "game_results",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                );

                // Verify that the round was stored successfully.
                try (android.database.Cursor cursor = db.query(
                        "game_results",
                        new String[]{"round_id"},
                        "round_id = ?",
                        new String[]{roundId},
                        null,
                        null,
                        null)) {

                    success = cursor.moveToFirst();
                }

            } catch (Exception exception) {
                android.util.Log.e(
                        "GameDatabase",
                        "Could not save game result",
                        exception
                );

                success = false;
            }

            final boolean saved = success;

            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(saved));
            }
        });
    }
    public static class GameResult {

        public final int playerOneScore;
        public final int playerTwoScore;
        public final long completedAt;

        public GameResult(
                int playerOneScore,
                int playerTwoScore,
                long completedAt) {

            this.playerOneScore = playerOneScore;
            this.playerTwoScore = playerTwoScore;
            this.completedAt = completedAt;
        }
    }

    public interface ResultsCallback {
        void onComplete(
                java.util.List<GameResult> results,
                boolean success);
    }

    public void loadResults(
            String gameId,
            boolean survival,
            ResultsCallback callback) {

        databaseExecutor.execute(() -> {
            java.util.List<GameResult> results =
                    new java.util.ArrayList<>();

            boolean success = true;

            String mode = survival ? "SURVIVAL" : "DUEL";

            // Solo scores are stored in player_two_score.
            String sortOrder = survival
                    ? "player_two_score DESC, completed_at DESC, id DESC"
                    : "completed_at DESC, id DESC";

            try {
                SQLiteDatabase db = getReadableDatabase();

                try (android.database.Cursor cursor = db.query(
                        "game_results",
                        new String[]{
                                "player_one_score",
                                "player_two_score",
                                "completed_at"
                        },
                        "game_id = ? AND mode = ?",
                        new String[]{gameId, mode},
                        null,
                        null,
                        sortOrder,
                        "50")) {

                    while (cursor.moveToNext()) {
                        results.add(new GameResult(
                                cursor.getInt(0),
                                cursor.getInt(1),
                                cursor.getLong(2)
                        ));
                    }
                }

            } catch (Exception exception) {
                android.util.Log.e(
                        "GameDatabase",
                        "Could not load game results",
                        exception
                );

                results.clear();
                success = false;
            }

            final boolean loaded = success;

            mainHandler.post(() ->
                    callback.onComplete(results, loaded));
        });
    }
}