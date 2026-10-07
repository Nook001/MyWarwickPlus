package uk.ac.warwick.plus.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(entities = {EventEntity.class, SyncEntity.class, CourseworkEntity.class, FeedEntry.class, FeedMeta.class}, version = 5, exportSchema = true)
public abstract class TimetableDatabase extends RoomDatabase {
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE events ADD COLUMN moduleName TEXT NOT NULL DEFAULT ''");
        }
    };
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS coursework (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL, url TEXT NOT NULL, dueMillis INTEGER NOT NULL)");
        }
    };
    public abstract TimetableDao timetable();
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE sync_state ADD COLUMN email TEXT NOT NULL DEFAULT ''");
        }
    };
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS feed_entries (feed INTEGER NOT NULL, id TEXT NOT NULL, title TEXT NOT NULL, text TEXT NOT NULL, url TEXT NOT NULL, provider TEXT NOT NULL, type TEXT NOT NULL, dateMillis INTEGER NOT NULL, html INTEGER NOT NULL, moduleCode TEXT NOT NULL, academicYear TEXT NOT NULL, announcementCount INTEGER NOT NULL, evaluationCount INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(feed,id))");
            database.execSQL("CREATE TABLE IF NOT EXISTS feed_meta (feed INTEGER NOT NULL PRIMARY KEY, description TEXT NOT NULL, url TEXT NOT NULL, hasMore INTEGER NOT NULL, webReadMillis INTEGER NOT NULL)");
        }
    };
    public static final Migration[] MIGRATIONS = {MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5};
}
