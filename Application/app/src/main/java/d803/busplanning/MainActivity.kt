package d803.busplanning

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

//    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
//        super.onCreate(savedInstanceState, persistentState)
//        setContentView(R.layout.activity_main)
//        getLocation()
//        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CALENDAR), 1)
//
//        mApiClient = GoogleApiClient.Builder(this)
//                .addApi(ActivityRecognition.API)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .build();
//        mApiClient?.connect();
//
//        val destination = intent.getStringExtra("Destination")
//        println(destination)
//    }

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

    // handle button activities
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

    private fun updateUI(trip: Trip?) {
        var activityType = trip!!.Leg.first().name
        if(activityType == "til fods"){
            var ss = SpannableString("Gå til \n" + trip.Leg.first().Destination.name)
            ss.setSpan(RelativeSizeSpan(2f), 0, 6, 0)
            this.activity.setText(ss)
            if(trip.Leg.count() != 1){ //In order to set bus field on first run
                val bus = trip.Leg.filter { l -> l.type != "WALK" }
                this.bus.setText(bus.first().name)
            }else{
                this.bus.setText(" ")
            }
        }
        else{
            var ss = SpannableString("Stå af ved \n" + trip.Leg.first().Destination.name)
            ss.setSpan(RelativeSizeSpan(2f), 0, 10, 0)
            this.activity.setText(ss)
            val bus = trip.Leg.filter { l -> l.type != "WALK" }
            this.bus.setText(bus.first().name)
        }
    }

    private fun getTime(time: String): Long {
        val from: Date = SimpleDateFormat("HH:mm").parse(time)
        return (from.time - getCurrentTime()) / 60000
    }

    private fun doTrip(trip: Trip?) {
        var startLocation: Location? = null
        try {
            startLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available");
        }
        var location = trip!!.Leg.first().Destination.name
        var tripStarted = false
        var time :Long= 0
        launch(UI) {
            updateUI(trip)
            updateOverview(trip)
            while (!trip.Leg.isEmpty()) {
                try {
                    val locationCheck = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if ((abs(startLocation!!.distanceTo(locationCheck)) >= 50)) {
                        break
                    }
                } catch
                (ex: SecurityException) {
                    Log.d("myTag", "Security Exception, no location available");
                }
                if (!tripStarted){
                    time = getTime(trip.Leg.first().Origin.time)
                    updateTime(time)
                } else{
                    time = getTime(trip.Leg.first().Destination.time)
                    updateTime(time)
                }
                if (time == 15L) {
                    sendNotification("15 minutter til at skulle gå til", location)
                } else if (time <= 1L && handleActivityDetection() == "In Vehicle") {
                    //Giver tom trip ved sidste element
                    updateUI(trip)
                    if (tripStarted) {
                        trip.Leg = trip.Leg.drop(1)
                    } else {
                        sendNotification("Gå nu til", location)
                        tripStarted = true
                    }
                }
                delay(60000)
            }
            cancel()
        }
    }

    private fun handleActivityDetection(): String {
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
            }catch (ex: NullPointerException) {
                Log.d("Null pointer", "No walk-bus-walk trip available");
            }
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




