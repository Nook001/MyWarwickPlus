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
    @Query("SELECT * FROM sync_state ORDER BY id") public abstract List<SyncEntity> states();
    @Query("SELECT * FROM feed_entries WHERE feed = :feed ORDER BY position, id") public abstract List<FeedEntry> feedEntries(int feed);
    @Query("SELECT * FROM feed_meta WHERE feed = :feed") public abstract FeedMeta feedMeta(int feed);
    @Query("SELECT * FROM sync_state WHERE id = :feed") public abstract SyncEntity feedState(int feed);
    @Query("DELETE FROM feed_entries WHERE feed = :feed") public abstract void deleteFeedEntries(int feed);
    @Query("DELETE FROM feed_meta WHERE feed = :feed") public abstract void deleteFeedMeta(int feed);
    @Query("DELETE FROM sync_state WHERE id = :feed") public abstract void deleteFeedState(int feed);
    @Query("DELETE FROM feed_entries") public abstract void deleteFeeds();
    @Query("DELETE FROM feed_meta") public abstract void deleteFeedMetas();
    @Query("DELETE FROM sync_state WHERE id > 2") public abstract void deleteFeedStates();
    @Insert public abstract void insertFeedEntries(List<FeedEntry> entries);
    @Insert public abstract void insertFeedMeta(FeedMeta meta);

    @Transaction public void replaceFeed(List<FeedEntry> entries, FeedMeta meta, SyncEntity state) {
        if (state.id != meta.feed) throw new IllegalArgumentException("Feed state does not match");
        deleteFeedEntries(meta.feed); deleteFeedMeta(meta.feed); deleteFeedState(meta.feed);
        insertFeedEntries(entries); insertFeedMeta(meta); insertState(state);
    }

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
        deleteFeeds();
        deleteFeedMetas();
        deleteFeedStates();
    }
    @Transaction public void replaceAccount(SyncEntity state) {
        if (state.id != 6) throw new IllegalArgumentException("Account state does not match");
        deleteFeedState(6); insertState(state);
    }
}
