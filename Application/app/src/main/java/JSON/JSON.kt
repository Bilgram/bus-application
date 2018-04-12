package JSON
import java.text.SimpleDateFormat
import java.util.Date;

data class TripClass(
		val TripList: TripList
)



data class TripList(
		val noNamespaceSchemaLocation: String,
		val Trip: List<Trip>
)

data class Trip(
		val Leg: List<Leg>
) {
	fun getDuration(): Long {
		val seconds = 1000
		val to = SimpleDateFormat("hh:mm").parse(Leg.last().Destination.time)
		val from = SimpleDateFormat("hh:mm").parse(Leg.first().Origin.time)
		val result = to.time - from.time
		return result/seconds
	}
}

data class Leg(
		val name: String,
		val type: String,
		val Origin: Origin,
		val Destination: Destination,
		val Notes: Notes
)

data class Destination(
		val name: String,
		val type: String,
		val time: String,
		val date: String

)

data class Origin(
		val name: String,
		val type: String,
		val time: String,
		val date: String
)

data class Notes(
		val text: String
)
