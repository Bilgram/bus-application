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
import android.widget.TextView
import android.widget.ProgressBar
import org.json.JSONArray
import JSON.TripClass
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import com.beust.klaxon.Klaxon
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.fixedRateTimer
import java.lang.Long.MAX_VALUE
import java.text.SimpleDateFormat
import java.util.concurrent.Future
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    var locationManager: LocationManager? = null
    var currentTrip :Trip? = null
    var mApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getLocation()
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CALENDAR), 1)

        val progressBar = this.progressOverview
        updateProgressBar(progressBar)
        doAsync {
            currentTrip = calculatePath()
            val trip = updateUI(currentTrip!!)
            doTrip(trip)

        }




        mApiClient = GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mApiClient?.connect();
    }

    private fun doTrip(trip: Trip?) {

    }

    private fun updateUI(trip: Trip): Trip? {
        val locationField = this.location
        var fifteenMinNotifcation = false
        var zeroMinNotfication = false
        var commitedTrip:Trip? = null
        val timeField = this.time
        val busField = this.time
        var from = SimpleDateFormat("hh:mm").parse(trip.Leg.first().Origin.time)
        var minutesToBus = (from.time - getCurrentTime()) / 60000
        var bus = trip.Leg.filter { l->l.type != "WALK"}.first()
        val fixedRateTimer = fixedRateTimer(name = "UpdateUI", initialDelay = 100, period = 100) {
            if (commitedTrip == null) {
                from = SimpleDateFormat("hh:mm").parse(trip.Leg.first().Origin.time)
                minutesToBus = (from.time - getCurrentTime()) / 60000
                locationField.setText(trip.Leg.first().name + "\n " + trip.Leg.first().Destination.name)
                bus = trip.Leg.filter { l->l.type != "WALK"}.first()
                runOnUiThread() {
                    timeField.setText(minutesToBus.toString())
                    busField.setText(bus.name)
                }
                if (minutesToBus >= 15 && !fifteenMinNotifcation) {
                    fifteenMinNotifcation = true
                    sendNotification("gå", "om 15 min")
                    commitedTrip = trip
                }

            }
            else {
                minutesToBus = (from.time - getCurrentTime()) / 60000
                runOnUiThread() { timeField.setText(minutesToBus.toString()) }
                if (minutesToBus >= 0 && !zeroMinNotfication) {
                    zeroMinNotfication = true
                    sendNotification("gå", "nu")
                    cancel()
                }
            }
        }
        fixedRateTimer.run{}
        //nok rigtig rigtig grim møde at gøre det på
        while (!zeroMinNotfication){

        }
        return commitedTrip

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
        val customLocation = "Aalborg Busterminal"
        val destCordinates = getXYCordinates(customLocation)
        var adress = ""
        try {
            val startLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val geoCoder = Geocoder(this, Locale.getDefault())
            adress = geoCoder.getFromLocation(startLocation!!.latitude, startLocation.longitude, 1)[0].getAddressLine(0)
        } catch (ex: SecurityException) {
            //sørger for at det ikke crasher og locationlistenerens call burde gøre det irelevant
            adress = "selmalagerløfsvej 300"
            Log.d("myTag", "Security Exception, no location available");
        }
        val startCordinates = getXYCordinates(adress)
        //result mangler time og date og så den søger efter arrival time todo når vi kan læse fra kalender
        val result = URL("http://xmlopen.rejseplanen.dk/bin/rest.exe/trip?" +
                "originCoordName=" + adress.toString() + "&originCoordX=" + startCordinates[0] + "&originCoordY=" + startCordinates[1] +
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
        val to = SimpleDateFormat("hh:mm").parse(currentHourMin)
        return to.time


    }

    private fun extractTripInfo(pathInfo: String): Trip? {
        var bestTime = MAX_VALUE
        val reader = Klaxon().parse<TripClass>(pathInfo)
        val tripMetrics = "first"
        if (reader != null) {
            val triplist = reader.TripList
            val trips = triplist.Trip
            val tripping = trips.first().Leg
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
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 10f, locationListener);
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

    fun updateProgressBar(progressBar: ProgressBar) {
        runOnUiThread {
            val drawable = progressBar.progressDrawable
            drawable.colorFilter = createColor()
            progressBar.setProgressDrawable(drawable)
            progressBar.progress = 50
        }
    }

    fun createColor(): PorterDuffColorFilter {
        val color = android.graphics.Color.argb(255, 153, 153, 153)
        val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        return colorFilter
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentTrip = calculatePath()
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




