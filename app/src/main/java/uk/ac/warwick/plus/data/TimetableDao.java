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
    @Query("DELETE FROM events")
    public abstract void deleteEvents();
    @Query("DELETE FROM sync_state")
    public abstract void deleteState();
    @Insert public abstract void insertEvents(List<EventEntity> events);
    @Insert public abstract void insertState(SyncEntity state);

    @Transaction public void replace(List<EventEntity> events, SyncEntity state) {
        clear();
        insertEvents(events);
        insertState(state);
    }
    @Transaction public void clear() {
        deleteEvents();
        deleteState();
    }
}
