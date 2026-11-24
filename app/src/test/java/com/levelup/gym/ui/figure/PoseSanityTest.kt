package com.levelup.gym.ui.figure

import org.junit.Assert.assertTrue
import org.junit.Test

/** Catches broken legs: on floor exercises no knee may sink to the floor. */
class PoseSanityTest {
    @Test fun kneesStayOffTheFloor() {
        val bad = mutableListOf<String>()
        ExerciseArts.names.forEach { name ->
            val art = ExerciseArts.forName(name)!!
            listOf("start" to art.start, "finish" to art.finish).forEach { (which, pose) ->
                val sk = Skeleton(pose, art.frontView)
                listOf(sk.legNear to sk.footNear, sk.legFar to sk.footFar).forEach { (leg, foot) ->
                    // a knee points the same way as the toes; pointing the other way means the leg bends backwards
                    if (!art.frontView) {
                        val mid = leg.start.lerp(leg.end, 0.5f)
                        val off = leg.joint - mid
                        val toes = (foot - leg.end).norm()
                        val forward = off.x * toes.x + off.y * toes.y
                        val rearFootOnBench = name == "Bulgarian Split Squat" && foot == sk.footFar
                        if (forward < -1.5f && !rearFootOnBench) bad += "$name $which: knee bends backwards ($forward)"
                    }
                    if (art.floor && leg.joint.y > 88f && name != "Plank (reps = seconds)" && name != "Push-up") bad += "$name $which: knee at y=${leg.joint.y}"
                }
            }
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }
}
