package uk.ac.warwick.plus.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(entities = {EventEntity.class, SyncEntity.class, CourseworkEntity.class}, version = 3, exportSchema = true)
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
}
