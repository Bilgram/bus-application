package d803.busplanning

import JSON.Leg
import JSON.Trip
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import java.net.URL
import android.util.Log
import android.support.v4.app.ActivityCompat
import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.LocationManager
import android.location.Location
import android.location.LocationListener
import android.widget.ProgressBar
import org.json.JSONArray
import JSON.TripClass
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Handler
import android.text.TextUtils.isEmpty
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.beust.klaxon.Klaxon
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.NonCancellable.cancel
import kotlinx.coroutines.experimental.android.UI
import org.jetbrains.anko.act
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.abs
import java.lang.Long.MAX_VALUE
import java.lang.Math.abs
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    var locationManager: LocationManager? = null
    var mApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getLocation()
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CALENDAR), 1)

        asyncAPICalls()


        mApiClient = GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient?.connect();
    }

    // Used for overview button
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.overview -> {
            val intent = Intent(this, OverviewActivity::class.java)
            val trip: Trip? = calculatePath()
            intent.putExtra("trip", trip) //hjælp, måske skal hele klassen serializaes. Et andet gæt på crash kan være fordi trip er tom, da vi ikke venter på den(Tror dette er meget sandsynligt)
            startActivity(intent)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun updateOverview(trip: Trip?) {//Dangerous when less than three elements
        this.walkBegin.setText(trip!!.Leg[0].Origin.time)
        this.busBegin.setText(trip.Leg[1].Origin.time)
        this.walkBeginLast.setText(trip.Leg[2].Origin.time)
        this.destination.setText(trip.Leg[2].Destination.time)
    }

    // handle button activities
    private fun asyncAPICalls() {
        val firstUpdate = async(CommonPool) {
            calculatePath()
        }
        launch(UI) {
            doTrip(firstUpdate.await())
            updateUI(firstUpdate.await())
            updateOverview(firstUpdate.await())
        }

    }


    private fun updateTime(time: Long) {
        this.time.setText(time.toString())
    }

    private fun updateUI(trip: Trip?) {

        var activityType = this.activity.setText(trip!!.Leg.first().name)
        var compaireType = "til fods"
        if(activityType.equals(compaireType)){
            this.activity.setText("Gå til")
        }
        if ((trip.Leg.first().type == "WALK") && (trip.Leg.size == 1)) {
            this.bus.setText(" ")
        } else {
            val bus = trip.Leg.filter { l -> l.type != "WALK" }
            this.bus.setText(bus.first().name)
        }
        this.location.setText(trip.Leg.first().Destination.name)
    }

    private fun getTime(leg: Leg?): Long {
        val from: Date = SimpleDateFormat("HH:mm").parse(leg!!.Origin.time)
        return (from.time - getCurrentTime()) / 60000
    }

    private fun doTrip(trip: Trip?) {
        var startLocation: Location? = null
        try {
            startLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available");
        }

        var tripTime = trip!!.getDuration()
        var location = trip.Leg.first().Destination.name
        launch(UI) {
            for (i in tripTime downTo 0) {
                try {
                    val locationCheck = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if ((abs(startLocation!!.distanceTo(locationCheck)) >= 50)) {
                        break
                    }
                } catch
                (ex: SecurityException) {
                    Log.d("myTag", "Security Exception, no location available");

                }
                val time = getTime(trip.Leg.first())
                updateTime(time)
                if (time.equals(15)) {
                    sendNotification("15 minutter til at skulle gå", location)
                }
                if (time.equals(0)) {
                    sendNotification("Gå nu til", location)
                }
                if (time <= 150) {
                    // giver tom trip ved sidste element
                    trip.Leg = trip.Leg.drop(1)
                    if (trip.Leg.isEmpty()) {
                        break
                    }
                    updateUI(trip)
                }
                delay(60000)
            }
            cancel()
        }
    }

    override fun onConnected(bundle: Bundle?) {
        val intent = Intent(this, ActivityDetection::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val client = ActivityRecognition.getClient(this)
        client.requestActivityUpdates(0, pendingIntent)
    }

    override fun onConnectionSuspended(i: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun calculatePath(): Trip? {
        val customLocation = "Aalborg busterminal"
        val destCordinates = getXYCordinates(customLocation)
        var address = ""
        try {
            val startLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val geoCoder = Geocoder(this, Locale.getDefault())
            address = geoCoder.getFromLocation(startLocation!!.latitude, startLocation.longitude, 1)[0].getAddressLine(0)
        } catch (ex: SecurityException) {
            address = "Selma Lagerløfsvej 300"
            Log.d("myTag", "Security Exception, no location available");
        }
        val startCordinates = getXYCordinates(address)
        //result mangler time og date og så den søger efter arrival time todo når vi kan læse fra kalender
        val result = URL("http://xmlopen.rejseplanen.dk/bin/rest.exe/trip?" +
                "originCoordName=" + address.toString() + "&originCoordX=" + startCordinates[0] + "&originCoordY=" + startCordinates[1] +
                "&destCoordName=" + customLocation + "&destCoordX=" + destCordinates[0] + "&destCoordY=" + destCordinates[1] + "&format=json\n").readText()
        val tripInfo = extractTripInfo(result)
        return tripInfo
    }

    private fun getCurrentTime(): Long {
        val current = java.util.Calendar.getInstance().getTime()
        var currentHourMin = ""
        if (current.minutes < 10) {
            currentHourMin = current.hours.toString() + ":" + "0" + current.minutes.toString()
        } else {
            currentHourMin = current.hours.toString() + ":" + current.minutes.toString()
        }
        val to = SimpleDateFormat("HH:mm").parse(currentHourMin)
        return to.time
    }


    private fun extractTripInfo(pathInfo: String): Trip? {
        var bestTime = MAX_VALUE
        val reader = Klaxon().parse<TripClass>(pathInfo)
        val tripMetrics = "first"
        if (reader != null) {
            val triplist = reader.TripList
            val trips = triplist.Trip
            var bestTrip = trips.first()

            if (tripMetrics == "first") {
                return bestTrip
            } else if (tripMetrics == "fastest") {
                for (trip in trips) {
                    if (trip.getDuration() < bestTime) {
                        bestTime = trip.getDuration()
                        bestTrip = trip
                    }
                }
            }
            return bestTrip
        }
        return null
    }

    private fun getXYCordinates(place: String): List<String> {
        val result = URL("http://xmlopen.rejseplanen.dk/bin/rest.exe/location?input=" + place + "&format=json").readText()
        val reader = JSONObject(result)
        val locationlist = reader.getJSONObject("LocationList")
        var keys = locationlist.keys()
        //rigtig grim måde at gøre det på måske
        keys.next()//nonamespaceshemalocation
        val str = keys.next()//coordlocation eller stoplocation
        val test = locationlist.get(str)
        if (test is JSONArray) {
            val coordLocation = locationlist.getJSONArray(str)
            val getResult = coordLocation.getJSONObject(0)
            val x = getResult.getString("x")
            val y = getResult.getString("y")
            return listOf<String>(x, y)
        }
        val getResult = locationlist.getJSONObject(str)
        val x = getResult.getString("x")
        val y = getResult.getString("y")
        return listOf<String>(x, y)
    }

    private fun getLocation() {
        if (locationManager == null)
            locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            // Request location updates
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 50f, locationListener);
        } catch (ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available");
        }
    }

    override fun onStop() {
        super.onStop()
        var title = "hej"
        val fixedRateTimer = fixedRateTimer(name = "kappa", initialDelay = 100, period = 100) {
            //sendNotification(title,"Body")
            title = title + "j"
        }
    }

    fun sendNotification(title: String, body: String) {
        val intent = Intent()
        val pendingIntent = PendingIntent.getActivity(this@MainActivity, 0, intent, 0)
        val notification = Notification.Builder(this@MainActivity) //https://stackoverflow.com/questions/45462666/notificationcompat-builder-deprecated-in-android-o?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                .setTicker("")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.notification_template_icon_bg)
                .setContentIntent(pendingIntent).notification
        notification.flags = Notification.FLAG_AUTO_CANCEL
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            asyncAPICalls()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    /** base url http://xmlopen.rejseplanen.dk/bin/rest.exe
    url format
    http://<baseurl>/trip?originId=8600626&destCoordX=<xInteger>&
    destCoordY=<yInteger>&destCoordName=<NameOfDestination>&date=
    19.09.10&time=07:02&useBus=0*/
}




