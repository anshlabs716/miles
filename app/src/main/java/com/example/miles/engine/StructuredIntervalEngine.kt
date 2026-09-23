package com.example.miles.engine

import com.example.miles.data.model.IntervalType
import com.example.miles.data.model.WorkoutInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class IntervalPhaseConfig(
    val name: String = "Custom HIIT Intervals",
    val warmupSeconds: Int = 300,
    val workSeconds: Int = 90,
    val recoverySeconds: Int = 60,
    val cooldownSeconds: Int = 300,
    val repetitions: Int = 6,
    val targetWorkPaceSecPerKm: Double? = 270.0 // 4:30 min/km
)

class StructuredIntervalEngine {

    private val _currentPhase = MutableStateFlow<WorkoutInterval?>(null)
    val currentPhase: StateFlow<WorkoutInterval?> = _currentPhase.asStateFlow()

    private val _currentIntervalIndex = MutableStateFlow(0)
    val currentIntervalIndex: StateFlow<Int> = _currentIntervalIndex.asStateFlow()

    private val _remainingPhaseSeconds = MutableStateFlow(0)
    val remainingPhaseSeconds: StateFlow<Int> = _remainingPhaseSeconds.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private var activeIntervals: List<WorkoutInterval> = emptyList()

    fun buildIntervalList(config: IntervalPhaseConfig): List<WorkoutInterval> {
        val list = mutableListOf<WorkoutInterval>()

        // 1. Warm-up Phase
        if (config.warmupSeconds > 0) {
            list.add(
                WorkoutInterval(
                    type = IntervalType.WARMUP,
                    durationSeconds = config.warmupSeconds,
                    instruction = "Warm Up: Steady light jog / brisk walk (${config.warmupSeconds / 60}m)"
                )
            )
        }

        // 2. Work & Recovery Repetition Loops
        for (rep in 1..config.repetitions.coerceAtLeast(1)) {
            list.add(
                WorkoutInterval(
                    type = IntervalType.RUN,
                    durationSeconds = config.workSeconds,
                    targetPaceSecPerKm = config.targetWorkPaceSecPerKm,
                    instruction = "Work (Rep $rep/${config.repetitions}): High Intensity Effort for ${config.workSeconds}s"
                )
            )
            if (rep < config.repetitions || config.recoverySeconds > 0) {
                list.add(
                    WorkoutInterval(
                        type = IntervalType.WALK,
                        durationSeconds = config.recoverySeconds,
                        instruction = "Recovery (Rep $rep/${config.repetitions}): Catch your breath (${config.recoverySeconds}s)"
                    )
                )
            }
        }

        // 3. Cool-down Phase
        if (config.cooldownSeconds > 0) {
            list.add(
                WorkoutInterval(
                    type = IntervalType.COOLDOWN,
                    durationSeconds = config.cooldownSeconds,
                    instruction = "Cool Down: Gentle stroll and stretch (${config.cooldownSeconds / 60}m)"
                )
            )
        }

        return list
    }

    fun startPlan(intervals: List<WorkoutInterval>) {
        if (intervals.isEmpty()) return
        activeIntervals = intervals
        _currentIntervalIndex.value = 0
        _isCompleted.value = false
        val first = intervals.first()
        _currentPhase.value = first
        _remainingPhaseSeconds.value = first.durationSeconds
    }

    /**
     * Ticks one second. Returns true if an interval boundary was crossed.
     */
    fun tickSecond(): Boolean {
        if (_isCompleted.value || activeIntervals.isEmpty()) return false

        val remaining = _remainingPhaseSeconds.value - 1
        if (remaining > 0) {
            _remainingPhaseSeconds.value = remaining
            return false
        }

        // Phase finished, advance to next
        val nextIdx = _currentIntervalIndex.value + 1
        if (nextIdx < activeIntervals.size) {
            _currentIntervalIndex.value = nextIdx
            val nextInterval = activeIntervals[nextIdx]
            _currentPhase.value = nextInterval
            _remainingPhaseSeconds.value = nextInterval.durationSeconds
            return true
        } else {
            _isCompleted.value = true
            _currentPhase.value = null
            _remainingPhaseSeconds.value = 0
            return true
        }
    }

    fun skipCurrentInterval() {
        if (_isCompleted.value || activeIntervals.isEmpty()) return
        val nextIdx = _currentIntervalIndex.value + 1
        if (nextIdx < activeIntervals.size) {
            _currentIntervalIndex.value = nextIdx
            val nextInterval = activeIntervals[nextIdx]
            _currentPhase.value = nextInterval
            _remainingPhaseSeconds.value = nextInterval.durationSeconds
        } else {
            _isCompleted.value = true
            _currentPhase.value = null
            _remainingPhaseSeconds.value = 0
        }
    }

    fun addExtraSeconds(seconds: Int = 30) {
        if (!_isCompleted.value) {
            _remainingPhaseSeconds.value += seconds
        }
    }
}
