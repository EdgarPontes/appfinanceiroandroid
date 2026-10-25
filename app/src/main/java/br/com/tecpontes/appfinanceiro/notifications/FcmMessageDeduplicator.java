package br.com.tecpontes.appfinanceiro.notifications;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;

/** Durable FCM idempotency store, independent of the app's destructive Room migration policy. */
public final class FcmMessageDeduplicator extends SQLiteOpenHelper {
    private static final String DATABASE = "fcm_delivery_state.db";
    private static final int VERSION = 1;
    private static volatile FcmMessageDeduplicator instance;

    public static FcmMessageDeduplicator get(@NonNull Context context) {
        if (instance == null) {
            synchronized (FcmMessageDeduplicator.class) {
                if (instance == null) instance = new FcmMessageDeduplicator(context.getApplicationContext());
            }
        }
        return instance;
    }

    private FcmMessageDeduplicator(Context context) {
        // Deduplication belongs to this physical installation and must not be restored
        // from an Android backup onto another device/account association.
        super(context, new java.io.File(context.getNoBackupFilesDir(), DATABASE).getAbsolutePath(),
                null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE delivered_fcm_logs (log_id TEXT PRIMARY KEY NOT NULL, delivered_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX delivered_fcm_logs_time_idx ON delivered_fcm_logs(delivered_at)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public synchronized boolean wasDelivered(@NonNull String logId) {
        try (Cursor cursor = getReadableDatabase().query(
                "delivered_fcm_logs", new String[]{"log_id"}, "log_id = ?",
                new String[]{logId}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    public synchronized void markDelivered(@NonNull String logId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("log_id", logId);
        values.put("delivered_at", System.currentTimeMillis());
        db.insertWithOnConflict("delivered_fcm_logs", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        // Keep enough history to cover FCM TTL/retries while bounding on-device storage.
        db.delete("delivered_fcm_logs", "delivered_at < ?",
                new String[]{String.valueOf(System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)});
    }
}
