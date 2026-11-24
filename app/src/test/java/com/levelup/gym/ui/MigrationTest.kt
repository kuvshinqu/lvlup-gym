package com.levelup.gym.ui

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.levelup.gym.data.GymDatabase
import com.levelup.gym.data.MIGRATION_1_2
import com.levelup.gym.data.MIGRATION_2_3
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Upgrading from v0.1 must keep every logged workout and set. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Test fun upgradeFromV1KeepsData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = context.getDatabasePath("migration-test.db").apply { parentFile?.mkdirs(); delete() }

        // Build the exact v0.1 schema from Room's exported 1.json.
        val schema = JSONObject(File("schemas/com.levelup.gym.data.GymDatabase/1.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val idx = e.optJSONArray("indices") ?: continue
                for (j in 0 until idx.length()) db.execSQL(idx.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO users (id, name, createdAt) VALUES (1, 'Hero', 0)")
            db.execSQL("INSERT INTO exercises (id, userId, name, muscleGroup, kind) VALUES (1, 1, 'Bench Press', 'Chest', 'STRENGTH')")
            db.execSQL("INSERT INTO workouts (id, userId, startedAt, finishedAt, bonusXp, planDayId) VALUES (1, 1, 0, 10, 50, 'upper_a')")
            db.execSQL("INSERT INTO sets (id, workoutId, exerciseId, reps, weightKg, durationSec, distanceM, xp, isPersonalRecord, createdAt) VALUES (1, 1, 1, 8, 60.0, 0, 0.0, 27, 0, 5)")
            db.version = 1
        }

        // Open with v2 + migration; Room validates the resulting schema on open.
        val room = Room.databaseBuilder(context, GymDatabase::class.java, file.absolutePath)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).allowMainThreadQueries().build()
        val dao = room.dao()
        assertEquals(77, dao.totalXp(1).first())
        dao.insertBodyWeight(com.levelup.gym.data.BodyWeightEntity(userId = 1, weightKg = 80.5))
        assertEquals(80.5, dao.latestBodyWeight(1).first()!!.weightKg, 0.001)
        dao.insertChest(com.levelup.gym.data.ChestEntity(userId = 1, tier = "WOODEN", source = "workout"))
        assertEquals(1, dao.chests(1).first().size)
        room.close()
    }
}
