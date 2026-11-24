package com.levelup.gym.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2 adds body weight weigh-ins. Keeps everything logged with v0.1. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `body_weights` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `loggedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_body_weights_userId` ON `body_weights` (`userId`)")
    }
}

/** v3 adds loot chests and equipped gear. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        CHESTS_SQL.forEach(db::execSQL)
    }
}

internal val CHESTS_SQL = listOf(
    "CREATE TABLE IF NOT EXISTS `chests` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `tier` TEXT NOT NULL, `source` TEXT NOT NULL, `earnedAt` INTEGER NOT NULL, `openedAt` INTEGER, `itemKey` TEXT, `duplicate` INTEGER NOT NULL, `cost` INTEGER NOT NULL, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    "CREATE INDEX IF NOT EXISTS `index_chests_userId` ON `chests` (`userId`)",
    "CREATE TABLE IF NOT EXISTS `equipped` (`userId` INTEGER NOT NULL, `slot` TEXT NOT NULL, `itemKey` TEXT NOT NULL, PRIMARY KEY(`userId`, `slot`), FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
)

class Converters {
    @TypeConverter fun kindToString(kind: ExerciseKind): String = kind.name
    @TypeConverter fun stringToKind(value: String): ExerciseKind = ExerciseKind.valueOf(value)
}

@Database(
    entities = [UserEntity::class, ExerciseEntity::class, WorkoutEntity::class, SetEntity::class, BodyWeightEntity::class, ChestEntity::class, EquippedEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        fun create(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, "gym.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
