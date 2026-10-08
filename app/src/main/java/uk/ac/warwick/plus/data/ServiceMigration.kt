package uk.ac.warwick.plus.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Public schema 5 is retained; only new service tables are added. */
val ServiceMigration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS service_summaries (resource INTEGER NOT NULL, id TEXT NOT NULL, callout TEXT NOT NULL, text TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(resource, id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS campus_events (id TEXT NOT NULL, source TEXT NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, url TEXT NOT NULL, location TEXT NOT NULL, startMillis INTEGER NOT NULL, endMillis INTEGER NOT NULL, allDay INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS service_meta (resource INTEGER NOT NULL, description TEXT NOT NULL, url TEXT NOT NULL, PRIMARY KEY(resource))")
    }
}
