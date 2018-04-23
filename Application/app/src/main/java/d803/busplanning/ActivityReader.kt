package d803.busplanning

class ActivityReader(
    //The type of a activity
    val ActivityType: String,
    //The value of how confident the google API is of a specific task
    val values: FloatArray,
    // The time for when the confident was read
    val time: Long)
