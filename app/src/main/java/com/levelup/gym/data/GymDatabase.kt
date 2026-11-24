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

class Converters {
    @TypeConverter fun kindToString(kind: ExerciseKind): String = kind.name
    @TypeConverter fun stringToKind(value: String): ExerciseKind = ExerciseKind.valueOf(value)
}

@Database(
    entities = [UserEntity::class, ExerciseEntity::class, WorkoutEntity::class, SetEntity::class, BodyWeightEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        fun create(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, "gym.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
