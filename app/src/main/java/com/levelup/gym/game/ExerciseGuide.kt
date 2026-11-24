package com.levelup.gym.game

/**
 * Pictures and short form notes for the starter exercises.
 * Photos come from Free Exercise DB (github.com/yuhonas/free-exercise-db, public domain / Unlicense)
 * and live in assets/exercises/<slug>_0.jpg (start) and <slug>_1.jpg (finish).
 */
data class ExerciseGuide(
    val imageSlug: String,
    val cues: List<String>,
    val avoid: String,
    /** Done with your own body; a weight entry means added weight (vest, belt, dumbbell). */
    val bodyweight: Boolean = false,
    /** Reps are counted as seconds (planks). */
    val timed: Boolean = false,
) {
    val startImage get() = "exercises/${imageSlug}_0.jpg"
    val finishImage get() = "exercises/${imageSlug}_1.jpg"
}

object ExerciseGuides {
    private val guides = mapOf(
        "Bench Press" to ExerciseGuide(
            "bench_press",
            listOf(
                "Eyes under the bar, feet flat, shoulder blades squeezed back and down.",
                "Grip a bit wider than shoulders, wrists stacked over elbows.",
                "Lower under control to the middle of your chest, elbows about 45° from your body.",
                "Press up and slightly back toward your face, keep your butt on the bench.",
            ),
            "Bouncing the bar off your chest or flaring elbows straight out to the sides.",
        ),
        "Incline Dumbbell Press" to ExerciseGuide(
            "incline_dumbbell_press",
            listOf(
                "Set the bench to 30-45°, kick the dumbbells up from your knees.",
                "Shoulder blades back, dumbbells over your upper chest.",
                "Lower until your elbows are just below the bench line, then press up and slightly in.",
            ),
            "Setting the bench too steep, which turns it into a shoulder press.",
        ),
        "Push-up" to ExerciseGuide(
            "push_up",
            listOf(
                "Hands just wider than shoulders, body in one straight line from head to heels.",
                "Brace your abs and squeeze your glutes.",
                "Lower until your chest is a fist from the floor, elbows about 45° back.",
                "Push the floor away until your arms are straight.",
            ),
            "Letting the hips sag or poking the head forward.",
            bodyweight = true,
        ),
        "Barbell Row" to ExerciseGuide(
            "barbell_row",
            listOf(
                "Hinge forward to about 45°, slight knee bend, back flat.",
                "Grip just outside your knees, arms long.",
                "Pull the bar to your lower ribs, driving elbows back.",
                "Lower under control without standing up.",
            ),
            "Jerking the weight up with your lower back or rounding your spine.",
        ),
        "Pull-up" to ExerciseGuide(
            "pull_up",
            listOf(
                "Hang with hands a bit wider than shoulders, palms facing away.",
                "Pull shoulders down first, then drive elbows toward your ribs.",
                "Get your chin over the bar without kicking.",
                "Lower all the way to straight arms each rep.",
            ),
            "Half reps that never reach full hang, or swinging for momentum.",
            bodyweight = true,
        ),
        "Lat Pulldown" to ExerciseGuide(
            "lat_pulldown",
            listOf(
                "Thighs locked under the pad, grip a bit wider than shoulders.",
                "Lean back slightly, chest up.",
                "Pull the bar to your upper chest, elbows down and back.",
                "Let it rise slowly until your arms are straight.",
            ),
            "Pulling the bar behind your neck or leaning way back to yank it down.",
        ),
        "Seated Cable Row" to ExerciseGuide(
            "seated_cable_row",
            listOf(
                "Sit tall, knees slightly bent, chest up.",
                "Pull the handle to your belly, squeezing shoulder blades together.",
                "Return slowly, letting your shoulders reach forward a little.",
            ),
            "Rocking your torso back and forth to move the weight.",
        ),
        "Deadlift" to ExerciseGuide(
            "deadlift",
            listOf(
                "Bar over mid-foot, feet hip-width, grip just outside your legs.",
                "Push your hips back, shins to the bar, chest up, back flat.",
                "Take the slack out of the bar, then push the floor away.",
                "Stand tall with hips and knees locked, then lower along your legs.",
            ),
            "Rounding the lower back or letting the bar drift away from your body.",
        ),
        "Squat" to ExerciseGuide(
            "squat",
            listOf(
                "Bar on your upper back, feet shoulder-width, toes slightly out.",
                "Big breath and brace, then sit down between your heels.",
                "Knees track over your toes; go to at least thighs parallel.",
                "Drive up through the whole foot, chest staying up.",
            ),
            "Knees caving in, heels lifting, or cutting depth short.",
        ),
        "Romanian Deadlift" to ExerciseGuide(
            "romanian_deadlift",
            listOf(
                "Stand tall holding the bar, soft knees.",
                "Push your hips straight back, sliding the bar down your thighs.",
                "Stop when you feel a strong hamstring stretch, usually mid-shin.",
                "Squeeze glutes to stand back up.",
            ),
            "Bending the knees into a squat or rounding the back to reach lower.",
        ),
        "Leg Press" to ExerciseGuide(
            "leg_press",
            listOf(
                "Feet shoulder-width in the middle of the platform.",
                "Lower until knees are about 90°, keeping your lower back on the pad.",
                "Press through your heels without locking your knees hard at the top.",
            ),
            "Going so deep that your hips curl off the seat.",
        ),
        "Bulgarian Split Squat" to ExerciseGuide(
            "bulgarian_split_squat",
            listOf(
                "Back foot on a bench, front foot far enough forward to drop straight down.",
                "Torso tall or slightly forward, dumbbells at your sides.",
                "Lower until the back knee nearly touches the floor.",
                "Drive up through the front heel. Do all reps, then switch legs.",
            ),
            "Front foot too close to the bench, which pushes the knee far past the toes.",
        ),
        "Leg Curl" to ExerciseGuide(
            "leg_curl",
            listOf(
                "Pad just above your heels, knees just off the bench edge.",
                "Curl your heels toward your glutes, hips pressed down.",
                "Lower slowly to almost straight legs.",
            ),
            "Lifting the hips to swing the weight up.",
        ),
        "Leg Extension" to ExerciseGuide(
            "leg_extension",
            listOf(
                "Knees lined up with the machine's pivot, pad on lower shins.",
                "Straighten your legs and squeeze the quads for a second.",
                "Lower under control.",
            ),
            "Kicking the weight up fast and letting it crash down.",
        ),
        "Calf Raise" to ExerciseGuide(
            "calf_raise",
            listOf(
                "Balls of your feet on the edge, heels hanging.",
                "Lower your heels for a deep stretch and pause.",
                "Rise as high as you can onto your toes and hold for a second.",
            ),
            "Short bouncy reps with no stretch or pause.",
        ),
        "Overhead Press" to ExerciseGuide(
            "overhead_press",
            listOf(
                "Bar on your front shoulders, grip just outside shoulders.",
                "Squeeze glutes and abs so your ribs stay down.",
                "Press straight up, moving your head back then through once the bar passes.",
                "Finish with the bar over your mid-foot, arms locked.",
            ),
            "Leaning back into a big arch to push the weight.",
        ),
        "Dumbbell Shoulder Press" to ExerciseGuide(
            "dumbbell_shoulder_press",
            listOf(
                "Sit with your back on an upright bench, dumbbells at ear height.",
                "Press up until arms are straight, dumbbells close but not clanking.",
                "Lower back to ear height under control.",
            ),
            "Arching off the bench or stopping the reps halfway down.",
        ),
        "Lateral Raise" to ExerciseGuide(
            "lateral_raise",
            listOf(
                "Stand tall, light dumbbells at your sides, slight bend in the elbows.",
                "Raise your arms out to the sides to shoulder height, leading with the elbows.",
                "Lower slowly.",
            ),
            "Going too heavy and swinging with your body or shrugging up.",
        ),
        "Bicep Curl" to ExerciseGuide(
            "bicep_curl",
            listOf(
                "Stand tall, elbows pinned at your sides, palms forward.",
                "Curl up until the dumbbells reach your shoulders.",
                "Lower all the way to straight arms.",
            ),
            "Swinging the torso or letting the elbows drift forward.",
        ),
        "Hammer Curl" to ExerciseGuide(
            "hammer_curl",
            listOf(
                "Palms face each other the whole time.",
                "Elbows stay at your sides as you curl up.",
                "Lower slowly to straight arms.",
            ),
            "Using momentum from your hips.",
        ),
        "Tricep Pushdown" to ExerciseGuide(
            "tricep_pushdown",
            listOf(
                "Stand close to the cable, elbows tucked at your sides.",
                "Push down until your arms are straight, squeeze the triceps.",
                "Let the bar rise to about chest height, elbows staying put.",
            ),
            "Elbows flaring out or leaning over the bar to push with body weight.",
        ),
        "Overhead Tricep Extension" to ExerciseGuide(
            "overhead_tricep_extension",
            listOf(
                "Face away from the cable (or hold one dumbbell overhead), elbows pointing forward.",
                "Lower the weight behind your head for a deep stretch.",
                "Straighten your arms without moving your elbows.",
            ),
            "Elbows flaring wide or arching the lower back.",
        ),
        "Plank (reps = seconds)" to ExerciseGuide(
            "plank",
            listOf(
                "Forearms under your shoulders, body in a straight line.",
                "Squeeze glutes and pull your belly button in.",
                "Breathe steadily. Log the seconds you held as reps.",
            ),
            "Hips sagging toward the floor or piked up high.",
            bodyweight = true, timed = true,
        ),
        "Hanging Leg Raise" to ExerciseGuide(
            "hanging_leg_raise",
            listOf(
                "Hang from the bar with straight arms, shoulders active.",
                "Curl your pelvis up as you raise your legs (bent knees is easier).",
                "Lower slowly without swinging.",
            ),
            "Swinging to get the legs up instead of using your abs.",
            bodyweight = true,
        ),
        "Running" to ExerciseGuide(
            "running",
            listOf(
                "Stay relaxed and tall, arms swinging front to back.",
                "Land under your hips with short, quick steps.",
                "For easy runs keep a pace where you could still talk.",
            ),
            "Overstriding and landing hard on your heels in front of you.",
        ),
        "Cycling" to ExerciseGuide(
            "cycling",
            listOf(
                "Seat height so your knee is slightly bent at the bottom of the pedal stroke.",
                "Keep your upper body relaxed and your knees tracking straight.",
                "Pick a resistance you can hold for the whole session.",
            ),
            "A seat that's too low, which strains the knees.",
        ),
        "Rowing Machine" to ExerciseGuide(
            "rowing_machine",
            listOf(
                "Order on the way out: legs, then lean back slightly, then arms.",
                "Order on the way back: arms, then body, then legs.",
                "Most of the power comes from your legs.",
            ),
            "Pulling mostly with the arms or bending the knees before the handle passes them.",
        ),
        "Incline Walk" to ExerciseGuide(
            "incline_walk",
            listOf(
                "Set an incline of 5-12% and a brisk walking pace.",
                "Stand tall and swing your arms naturally.",
                "Let go of the rails so your legs do the work.",
            ),
            "Hanging onto the handrails, which cancels most of the effort.",
        ),
        "Elliptical" to ExerciseGuide(
            "elliptical",
            listOf(
                "Stand tall with your weight through your heels.",
                "Push and pull the handles to involve your upper body.",
                "Use resistance so it doesn't feel like free-spinning.",
            ),
            "Leaning on the handles or slouching forward.",
        ),
    )

    fun forName(name: String): ExerciseGuide? = guides[name]
}
