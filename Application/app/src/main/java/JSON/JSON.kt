package JSON


data class JSON(
		val TripList: TripList
)

data class TripList(
		val noNamespaceSchemaLocation: String,
		val Trip: List<Trip>
)

data class Trip(
		val Leg: List<Leg>
)

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
