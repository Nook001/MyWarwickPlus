package uk.ac.warwick.plus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public abstract class TimetableDao {
    @Query("SELECT * FROM events ORDER BY startMillis, id")
    public abstract List<EventEntity> events();
    @Query("SELECT * FROM sync_state WHERE id = 1")
    public abstract SyncEntity state();
    @Query("SELECT * FROM coursework ORDER BY dueMillis, id")
    public abstract List<CourseworkEntity> coursework();
    @Query("SELECT * FROM sync_state WHERE id = 2")
    public abstract SyncEntity courseworkState();
    @Query("DELETE FROM coursework")
    public abstract void deleteCoursework();
    @Query("DELETE FROM sync_state WHERE id = 2")
    public abstract void deleteCourseworkState();
    @Query("DELETE FROM events")
    public abstract void deleteEvents();
    @Query("DELETE FROM sync_state WHERE id = 1")
    public abstract void deleteState();
    @Insert public abstract void insertEvents(List<EventEntity> events);
    @Insert public abstract void insertState(SyncEntity state);
    @Insert public abstract void insertCoursework(List<CourseworkEntity> entries);

    @Transaction public void replace(List<EventEntity> events, SyncEntity state) {
        deleteEvents();
        deleteState();
        insertEvents(events);
        insertState(state);
    }
    @Transaction public void replaceCoursework(List<CourseworkEntity> entries, SyncEntity state) {
        deleteCoursework();
        deleteCourseworkState();
        insertCoursework(entries);
        insertState(state);
    }
    @Transaction public void clear() {
        deleteEvents();
        deleteState();
        deleteCoursework();
        deleteCourseworkState();
    }
}
