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
import org.json.JSONArray
import JSON.TripClass
import android.app.SharedElementCallback
import android.content.SharedPreferences
import android.os.CountDownTimer
import android.os.Handler
import android.os.PersistableBundle
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuItem
import com.beust.klaxon.Klaxon
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.NonCancellable.cancel
import kotlinx.coroutines.experimental.android.UI
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.fixedRateTimer
import java.lang.Long.MAX_VALUE
import java.lang.Math.abs
import java.lang.NullPointerException
import java.nio.file.Files.size
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    var locationManager: LocationManager? = null
    var mApiClient: GoogleApiClient? = null

    override fun onStart() {
        super.onStart()
        setContentView(R.layout.activity_main)
        getLocation()
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CALENDAR), 1)

        mApiClient = GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient?.connect();

        val destination = intent.getStringExtra("destination")
        println(destination)
    }

    private fun updateOverview(trip: Trip?) {//Dangerous when less than three elements
        this.walkBegin.setText(trip!!.Leg[0].Origin.time)
        this.busBegin.setText(trip.Leg[1].Origin.time)
        this.walkBeginLast.setText(trip.Leg[2].Origin.time)
        this.destination.setText(trip.Leg[2].Destination.time)
    }

    private fun asyncAPICalls() {
        val firstUpdate = async(CommonPool) {
            calculatePath()
        }
        launch(CommonPool) {
            doTrip(firstUpdate.await())
        }
    }

    private fun updateTime(time: Long) {
        this.time.setText(time.toString())
    }

    private fun updateActivity(leg: Leg) {
        if (leg.name == "til fods") {
            val ss = SpannableString("Gå til \n" + leg.Destination.name)
            ss.setSpan(RelativeSizeSpan(2f), 0, 6, 0)
            this.activity.setText(ss)
        } else {
            val ss = SpannableString("Stå af ved \n" + leg.Destination.name)
            ss.setSpan(RelativeSizeSpan(2f), 0, 10, 0)
            this.activity.setText(ss)
        }
    }

    private fun updateBus(trip: Trip?) {
        val bus = trip!!.Leg.filter { l -> l.type != "WALK" }
        this.bus.setText(bus.first().name)
    }

    private fun getTime(time: String): Long {
        val from: Date = SimpleDateFormat("HH:mm").parse(time)
        return (from.time - getCurrentTime()) / 60000
    }

    suspend fun handleActivityDetection(): Boolean {
        var missedBus = true
        for (time in 3 downTo 0) {
            if (determineActivity() == "In Vehicle") {
                missedBus = false
            }
            delay(60000)
        }
        return missedBus
    }

    private fun handleNotification(time: Long, location: String) {
        if (time == 15L) {
            sendNotification("15 minutter til du skal gå", location)
        } else if (time == 0L) {
            sendNotification("Gå nu til", location)
        }
    }

    suspend fun firstTripPart(trip: Trip?): Trip{
        val firstLeg = trip!!.Leg.first()
        updateOverview(trip)
        updateBus(trip)
        updateActivity(firstLeg)
        for (time in getTime(firstLeg.Origin.time) downTo 0) {
            handleNotification(time, firstLeg.Destination.name)
            updateTime(time)
            delay(60000)
        }
        trip.Leg = trip.Leg.drop(1)
        return trip
    }

    private fun doTrip(trip: Trip?) {
        var startLocation: Location? = null
        var stop = false
        try {
            startLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available");
        }
        launch(UI) {
            val secondTripPart = firstTripPart(trip)
            var onBus = true
            for (leg: Leg in secondTripPart.Leg) {
                try {
                    val locationCheck = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if ((abs(startLocation!!.distanceTo(locationCheck)) >= 50)) {
                        break
                    }
                } catch
                (ex: SecurityException) {
                    Log.d("myTag", "Security Exception, no location available");
                }

                val job = launch(UI) {
                    updateActivity(leg)
                    for (time in getTime(leg.Destination.time) downTo 0) {
                        if (stop == true){
                            cancel()
                        }else{
                            updateTime(time)
                            delay(60000)
                        }
                    }
                }

                if (onBus) {
                    val findNewRoute = async(CommonPool) {
                        handleActivityDetection()
                    }
                    if (findNewRoute.await()) {
                        sendNotification("Nåede ikke bussen", "Det virker til du ikke nåde bussen.. Finder ny rute")
                        stop = true
                        asyncAPICalls()
                        cancel()
                        break
                    } else {
                        onBus = false
                    }
                }
                job.join() //Wait for job(coroutine) to finish
            }
        }
    }

    private fun determineActivity(): String {
        val preferences: SharedPreferences = getSharedPreferences("ActivityRecognition", Context.MODE_PRIVATE)
        return preferences.getString("Activity", "default")
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
        val customLocation = intent.getStringExtra("destination")//"Aalborg busterminal"
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
            try {
                var bestTrip = (trips.filter { l -> l.Leg.count() == 3 }).first()
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
            } catch (ex: NullPointerException) {
                Log.d("Null pointer", "No walk-bus-walk trip available");
            }
        }
        return null
    }

    private fun getXYCordinates(place: String): List<String> {
        val result = URL("http://xmlopen.rejseplanen.dk/bin/rest.exe/location?input=" + place + "&format=json").readText()
        val reader = JSONObject(result)
        val locationlist = reader.getJSONObject("LocationList")
        val keys = locationlist.keys()
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




