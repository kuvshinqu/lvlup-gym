package com.levelup.gym.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun kindToString(kind: ExerciseKind): String = kind.name
    @TypeConverter fun stringToKind(value: String): ExerciseKind = ExerciseKind.valueOf(value)
}

@Database(
    entities = [UserEntity::class, ExerciseEntity::class, WorkoutEntity::class, SetEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        fun create(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, "gym.db").build()
    }
}
