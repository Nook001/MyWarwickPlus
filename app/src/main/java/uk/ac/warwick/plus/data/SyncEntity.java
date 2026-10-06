package uk.ac.warwick.plus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_state")
public class SyncEntity {
    @PrimaryKey public int id = 1;
    @NonNull public String userCode = "";
    @NonNull public String displayName = "";
    @NonNull @ColumnInfo(defaultValue = "''") public String email = "";
    public long syncedAt;
}
