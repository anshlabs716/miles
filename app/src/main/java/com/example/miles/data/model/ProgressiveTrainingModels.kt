package com.example.miles.data.model

enum class IntervalType(val label: String, val colorHex: Long) {
    WARMUP("Warm Up", 0xFFFFB300),
    RUN("Run / Sprint", 0xFF00E5FF),
    WALK("Walk / Recovery", 0xFF00E676),
    COOLDOWN("Cool Down", 0xFFB388FF),
    REST("Rest", 0xFF9E9E9E)
}

data class WorkoutInterval(
    val type: IntervalType,
    val durationSeconds: Int,
    val targetPaceSecPerKm: Double? = null,
    val instruction: String = ""
) {
    val durationMinutes: Int get() = durationSeconds / 60
    val durationRemainingSeconds: Int get() = durationSeconds % 60
}

data class ProgressiveWorkoutDay(
    val dayNumber: Int,
    val title: String,
    val description: String,
    val targetDistanceKm: Float? = null,
    val intervals: List<WorkoutInterval>,
    val isCheatOrRestDay: Boolean = false,
    val isShortEasyDay: Boolean = false
) {
    val totalDurationSeconds: Int get() = intervals.sumOf { it.durationSeconds }
    val totalDurationMinutes: Int get() = (totalDurationSeconds + 59) / 60
}

data class ProgressiveWorkoutWeek(
    val weekNumber: Int,
    val focusTitle: String,
    val description: String,
    val days: List<ProgressiveWorkoutDay>
)

data class ProgressivePlan(
    val id: String,
    val title: String,
    val subtitle: String,
    val difficulty: String, // Beginner, Intermediate, Advanced
    val durationWeeks: Int,
    val iconName: String,
    val weeks: List<ProgressiveWorkoutWeek>
)

object ProgressiveTrainingPrograms {

    // 1. Couch to 5K (C25K) Progressive Plan (8 Weeks)
    val COUCH_TO_5K = ProgressivePlan(
        id = "c25k",
        title = "Couch to 5K (C25K)",
        subtitle = "Zero to 30 continuous minutes of running in 8 weeks",
        difficulty = "Beginner",
        durationWeeks = 8,
        iconName = "DirectionsRun",
        weeks = listOf(
            ProgressiveWorkoutWeek(
                weekNumber = 1,
                focusTitle = "Foundation Building",
                description = "Alternating 60s gentle runs with 90s recovery walks.",
                days = listOf(
                    ProgressiveWorkoutDay(
                        dayNumber = 1,
                        title = "Week 1 - Day 1: Run/Walk Intervals",
                        description = "5 min warmup, 8x (60s run / 90s walk), 5 min cooldown",
                        intervals = buildIntervals(warmup = 300, runs = 8, runSec = 60, walkSec = 90, cooldown = 300)
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 2,
                        title = "Week 1 - Day 2: Consistency Run",
                        description = "5 min warmup, 8x (60s run / 90s walk), 5 min cooldown",
                        intervals = buildIntervals(warmup = 300, runs = 8, runSec = 60, walkSec = 90, cooldown = 300)
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 3,
                        title = "Week 1 - Day 3: Week 1 Graduation",
                        description = "5 min warmup, 8x (60s run / 90s walk), 5 min cooldown",
                        intervals = buildIntervals(warmup = 300, runs = 8, runSec = 60, walkSec = 90, cooldown = 300)
                    )
                )
            ),
            ProgressiveWorkoutWeek(
                weekNumber = 2,
                focusTitle = "Extending the Intervals",
                description = "Increasing run duration to 90 seconds.",
                days = listOf(
                    ProgressiveWorkoutDay(
                        dayNumber = 1,
                        title = "Week 2 - Day 1: 90s Running Repeats",
                        description = "5 min warmup, 6x (90s run / 120s walk), 5 min cooldown",
                        intervals = buildIntervals(warmup = 300, runs = 6, runSec = 90, walkSec = 120, cooldown = 300)
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 2,
                        title = "Week 2 - Day 2: Aerobic Rhythm",
                        description = "5 min warmup, 6x (90s run / 120s walk), 5 min cooldown",
                        intervals = buildIntervals(warmup = 300, runs = 6, runSec = 90, walkSec = 120, cooldown = 300)
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 3,
                        title = "Week 2 - Day 3: Sustained Momentum",
                        description = "5 min warmup, 6x (90s run / 120s walk), 5 min cooldown",
                        intervals = buildIntervals(warmup = 300, runs = 6, runSec = 90, walkSec = 120, cooldown = 300)
                    )
                )
            ),
            ProgressiveWorkoutWeek(
                weekNumber = 3,
                focusTitle = "Breaking the 3-Minute Barrier",
                description = "Pushing to 3 minutes of continuous running.",
                days = listOf(
                    ProgressiveWorkoutDay(
                        dayNumber = 1,
                        title = "Week 3 - Day 1: 3-Min Run Intervals",
                        description = "Warmup, 2x (90s run / 90s walk / 3m run / 3m walk), Cooldown",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Brisk 5 min warmup walk"),
                            WorkoutInterval(IntervalType.RUN, 90, instruction = "Run 90s"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3 minutes!"),
                            WorkoutInterval(IntervalType.WALK, 180, instruction = "Walk 3 minutes"),
                            WorkoutInterval(IntervalType.RUN, 90, instruction = "Run 90s"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3 minutes!"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "5 min cooldown walk")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 2,
                        title = "Week 3 - Day 2: Cardio Progression",
                        description = "Repeat Week 3 intervals with focus on relaxed shoulders",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Brisk 5 min warmup walk"),
                            WorkoutInterval(IntervalType.RUN, 90, instruction = "Run 90s"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3 minutes"),
                            WorkoutInterval(IntervalType.WALK, 180, instruction = "Walk 3 minutes"),
                            WorkoutInterval(IntervalType.RUN, 90, instruction = "Run 90s"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3 minutes"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "5 min cooldown walk")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 3,
                        title = "Week 3 - Day 3: Confidence Builder",
                        description = "Repeat Week 3 intervals with even pacing",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Brisk 5 min warmup walk"),
                            WorkoutInterval(IntervalType.RUN, 90, instruction = "Run 90s"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3 minutes"),
                            WorkoutInterval(IntervalType.WALK, 180, instruction = "Walk 3 minutes"),
                            WorkoutInterval(IntervalType.RUN, 90, instruction = "Run 90s"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3 minutes"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "5 min cooldown walk")
                        )
                    )
                )
            ),
            ProgressiveWorkoutWeek(
                weekNumber = 4,
                focusTitle = "The 5-Minute Run",
                description = "Building stamina with 5-minute sustained running.",
                days = listOf(
                    ProgressiveWorkoutDay(
                        dayNumber = 1,
                        title = "Week 4 - Day 1: 5-Minute Blocks",
                        description = "3m run / 90s walk / 5m run / 2.5m walk / 3m run / 90s walk / 5m run",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup walk 5m"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3m"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.WALK, 150, instruction = "Walk 2.5m"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3m"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 2,
                        title = "Week 4 - Day 2: Repeat Day",
                        description = "Repeat Week 4 progression smoothly",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup walk 5m"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3m"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.WALK, 150, instruction = "Walk 2.5m"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3m"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 3,
                        title = "Week 4 - Day 3: Pacing Mastery",
                        description = "Steady cadence on both 5-minute blocks",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup walk 5m"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3m"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.WALK, 150, instruction = "Walk 2.5m"),
                            WorkoutInterval(IntervalType.RUN, 180, instruction = "Run 3m"),
                            WorkoutInterval(IntervalType.WALK, 90, instruction = "Walk 90s"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    )
                )
            ),
            ProgressiveWorkoutWeek(
                weekNumber = 5,
                focusTitle = "The Breakthrough 20-Minute Run",
                description = "Progressing to 8-minute blocks and your first 20-minute continuous run.",
                days = listOf(
                    ProgressiveWorkoutDay(
                        dayNumber = 1,
                        title = "Week 5 - Day 1: 5m Run x 3",
                        description = "3x (5m run / 3m walk)",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.WALK, 180, instruction = "Walk 3m"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.WALK, 180, instruction = "Walk 3m"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 2,
                        title = "Week 5 - Day 2: 8m Run x 2",
                        description = "2x (8m run / 5m walk)",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 480, instruction = "Run 8m"),
                            WorkoutInterval(IntervalType.WALK, 300, instruction = "Walk 5m"),
                            WorkoutInterval(IntervalType.RUN, 480, instruction = "Run 8m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 3,
                        title = "Week 5 - Day 3: Continuous 20m Run!",
                        description = "Warmup, continuous 20 min run without walking, Cooldown",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup walk 5m"),
                            WorkoutInterval(IntervalType.RUN, 1200, instruction = "Continuous 20m Run! Find your groove."),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Victory cooldown walk 5m")
                        )
                    )
                )
            ),
            ProgressiveWorkoutWeek(
                weekNumber = 6,
                focusTitle = "Consolidating Distance",
                description = "22-minute sustained running.",
                days = listOf(
                    ProgressiveWorkoutDay(
                        dayNumber = 1,
                        title = "Week 6 - Day 1: Pyramid Interval",
                        description = "5m run, 3m walk, 8m run, 3m walk, 5m run",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.WALK, 180, instruction = "Walk 3m"),
                            WorkoutInterval(IntervalType.RUN, 480, instruction = "Run 8m"),
                            WorkoutInterval(IntervalType.WALK, 180, instruction = "Walk 3m"),
                            WorkoutInterval(IntervalType.RUN, 300, instruction = "Run 5m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 2,
                        title = "Week 6 - Day 2: 10m Run x 2",
                        description = "2x (10m run / 3m walk)",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 600, instruction = "Run 10m"),
                            WorkoutInterval(IntervalType.WALK, 180, instruction = "Walk 3m"),
                            WorkoutInterval(IntervalType.RUN, 600, instruction = "Run 10m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 3,
                        title = "Week 6 - Day 3: Continuous 22m Run",
                        description = "Continuous 22-minute run",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 1320, instruction = "Run 22m continuous"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    )
                )
            ),
            ProgressiveWorkoutWeek(
                weekNumber = 7,
                focusTitle = "The 25-Minute Threshold",
                description = "Three continuous 25-minute runs.",
                days = listOf(
                    ProgressiveWorkoutDay(
                        dayNumber = 1,
                        title = "Week 7 - Day 1: 25m Run",
                        description = "5m warmup walk, 25m continuous run, 5m cooldown",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 1500, instruction = "Run 25m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 2,
                        title = "Week 7 - Day 2: 25m Run",
                        description = "5m warmup walk, 25m continuous run, 5m cooldown",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 1500, instruction = "Run 25m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 3,
                        title = "Week 7 - Day 3: 25m Run",
                        description = "5m warmup walk, 25m continuous run, 5m cooldown",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 1500, instruction = "Run 25m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    )
                )
            ),
            ProgressiveWorkoutWeek(
                weekNumber = 8,
                focusTitle = "5K Graduation: Continuous 30 Minutes!",
                description = "Reaching the ultimate goal: 30 minutes of running without stopping!",
                days = listOf(
                    ProgressiveWorkoutDay(
                        dayNumber = 1,
                        title = "Week 8 - Day 1: 28m Run",
                        description = "Warmup, 28 min run, Cooldown",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 1680, instruction = "Run 28m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 2,
                        title = "Week 8 - Day 2: 28m Run",
                        description = "Warmup, 28 min run, Cooldown",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 1680, instruction = "Run 28m"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown 5m")
                        )
                    ),
                    ProgressiveWorkoutDay(
                        dayNumber = 3,
                        title = "Week 8 - Day 3: 5K FINISHER (30m Run) 🏆",
                        description = "Continuous 30 minutes of running! You are a 5K runner!",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 300, instruction = "Warmup 5m"),
                            WorkoutInterval(IntervalType.RUN, 1800, instruction = "5K Continuous 30-Minute Run! Run tall!"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Celebration cooldown walk 5m")
                        )
                    )
                )
            )
        )
    )

    // 2. Short Easy Day & Active Recovery Workouts
    val SHORT_EASY_DAYS = listOf(
        ProgressiveWorkoutDay(
            dayNumber = 1,
            title = "10-Min Gentle Flush Walk",
            description = "Active recovery walk to flush lactic acid, relax tight muscles, and protect daily streak.",
            targetDistanceKm = 1.0f,
            isShortEasyDay = true,
            intervals = listOf(
                WorkoutInterval(IntervalType.WALK, 600, instruction = "Easy relaxing recovery walk (10 min)")
            )
        ),
        ProgressiveWorkoutDay(
            dayNumber = 2,
            title = "15-Min Aerobic Recovery Jog",
            description = "Zone 1 ultra-gentle conversational recovery jog. Keep effort very low.",
            targetDistanceKm = 1.8f,
            isShortEasyDay = true,
            intervals = listOf(
                WorkoutInterval(IntervalType.WARMUP, 180, instruction = "Easy warmup walk 3m"),
                WorkoutInterval(IntervalType.RUN, 600, instruction = "Ultra-gentle recovery jog 10m (Zone 1)"),
                WorkoutInterval(IntervalType.COOLDOWN, 120, instruction = "Cooldown walk 2m")
            )
        ),
        ProgressiveWorkoutDay(
            dayNumber = 3,
            title = "12-Min Post-Rest Shakeout",
            description = "Shake out stiffness after a rest/cheat day with light walking and relaxed 30s pickups.",
            targetDistanceKm = 1.4f,
            isShortEasyDay = true,
            intervals = listOf(
                WorkoutInterval(IntervalType.WALK, 300, instruction = "Easy walk 5m"),
                WorkoutInterval(IntervalType.RUN, 30, instruction = "Light stride 30s"),
                WorkoutInterval(IntervalType.WALK, 90, instruction = "Recovery walk 90s"),
                WorkoutInterval(IntervalType.RUN, 30, instruction = "Light stride 30s"),
                WorkoutInterval(IntervalType.WALK, 90, instruction = "Recovery walk 90s"),
                WorkoutInterval(IntervalType.COOLDOWN, 180, instruction = "Cooldown walk 3m")
            )
        )
    )

    // 3. HIIT Sprint & Speed Overload (4 Weeks)
    val SPRINT_PROGRESSION = ProgressivePlan(
        id = "sprint_hiit",
        title = "Speed & Interval Overload",
        subtitle = "Build anaerobic power, VO2 max, and leg turnover",
        difficulty = "Intermediate",
        durationWeeks = 4,
        iconName = "Speed",
        weeks = listOf(
            ProgressiveWorkoutWeek(
                weekNumber = 1,
                focusTitle = "Introduction to Strides",
                description = "4x 20s progressive accelerations.",
                days = listOf(
                    ProgressiveWorkoutDay(
                        dayNumber = 1,
                        title = "Week 1 - Day 1: 4x 20s Strides",
                        description = "10m jog warmup, 4x (20s sprint / 60s walk), 5m cooldown",
                        intervals = listOf(
                            WorkoutInterval(IntervalType.WARMUP, 600, instruction = "Warmup jog 10m"),
                            WorkoutInterval(IntervalType.RUN, 20, instruction = "Sprint 20s!"),
                            WorkoutInterval(IntervalType.WALK, 60, instruction = "Walk recovery 60s"),
                            WorkoutInterval(IntervalType.RUN, 20, instruction = "Sprint 20s!"),
                            WorkoutInterval(IntervalType.WALK, 60, instruction = "Walk recovery 60s"),
                            WorkoutInterval(IntervalType.RUN, 20, instruction = "Sprint 20s!"),
                            WorkoutInterval(IntervalType.WALK, 60, instruction = "Walk recovery 60s"),
                            WorkoutInterval(IntervalType.RUN, 20, instruction = "Sprint 20s!"),
                            WorkoutInterval(IntervalType.COOLDOWN, 300, instruction = "Cooldown walk 5m")
                        )
                    )
                )
            )
        )
    )

    private fun buildIntervals(
        warmup: Int,
        runs: Int,
        runSec: Int,
        walkSec: Int,
        cooldown: Int
    ): List<WorkoutInterval> {
        val list = mutableListOf<WorkoutInterval>()
        list.add(WorkoutInterval(IntervalType.WARMUP, warmup, instruction = "Brisk warmup walk ${warmup / 60}m"))
        for (i in 1..runs) {
            list.add(WorkoutInterval(IntervalType.RUN, runSec, instruction = "Run ($i/$runs): ${runSec}s"))
            if (i < runs) {
                list.add(WorkoutInterval(IntervalType.WALK, walkSec, instruction = "Walk recovery ($i/$runs): ${walkSec}s"))
            }
        }
        list.add(WorkoutInterval(IntervalType.COOLDOWN, cooldown, instruction = "Cooldown walk ${cooldown / 60}m"))
        return list
    }
}
