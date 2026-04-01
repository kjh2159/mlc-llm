package ai.mlc.mlcchat

// This clock profile is used for IGNITE.
data class ClockProfile(
    val gpuClockPrefill: Int? = null,
    val gpuClockDecode: Int? = null,
    val ramClockPrefill: Int? = null,
    val ramClockDecode: Int? = null,
    val phasePause: Int? = 0
)
