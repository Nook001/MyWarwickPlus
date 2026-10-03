package uk.ac.warwick.plus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "feed_meta")
public class FeedMeta {
    @PrimaryKey public int feed;
    @NonNull public String description = "";
    @NonNull public String url = "";
    public boolean hasMore;
    public long webReadMillis;
}
