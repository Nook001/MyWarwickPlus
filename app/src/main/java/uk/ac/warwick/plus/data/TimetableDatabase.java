package uk.ac.warwick.plus.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(entities = {EventEntity.class, SyncEntity.class}, version = 2, exportSchema = true)
public abstract class TimetableDatabase extends RoomDatabase {
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE events ADD COLUMN moduleName TEXT NOT NULL DEFAULT ''");
        }
    };
    public abstract TimetableDao timetable();
}
