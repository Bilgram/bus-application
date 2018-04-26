package d803.busplanning

data class ActivityReader
(
        val activityType: String,
        val values: FloatArray,
        val time: Long)