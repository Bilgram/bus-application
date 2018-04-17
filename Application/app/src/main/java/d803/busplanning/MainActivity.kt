package d803.busplanning
import JSON.Destination
import JSON.Trip
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import java.net.URL
import android.util.Log
import android.view.View
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
import org.json.JSONArray
import JSON.TripClass
import com.beust.klaxon.Klaxon
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.fixedRateTimer
import java.lang.Long.MAX_VALUE
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    var locationManager:LocationManager?=null

    var mApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getLocation()
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CALENDAR), 1)
        val timeField = findViewById<TextView>(R.id.time)
        val locationField = findViewById<TextView>(R.id.location)
        calculatePath(locationField, timeField)


        mApiClient = GoogleApiClient.Builder(this)
        .addApi(ActivityRecognition.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build();

        mApiClient?.connect();
    }

    override fun onConnected(bundle: Bundle?) {
        val intent = Intent(this, ActivityDetection::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val per = ActivityRecognition.getClient(this)
        per.requestActivityUpdates(0, pendingIntent)
    }

    override fun onConnectionSuspended(i: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun calculatePath(locationField: TextView, timeField: TextView) {
        val customLocation = "Aalborg Busterminal"
        thread {
            val destCordinates = getXYCordinates(customLocation)
            var adress = ""
            try {
                val startLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val geoCoder = Geocoder(this, Locale.getDefault())
                adress = geoCoder.getFromLocation(startLocation!!.latitude, startLocation.longitude, 1)[0].getAddressLine(0)
            } catch (ex: SecurityException) {
                Log.d("myTag", "Security Exception, no location available");
            }
            val startCordinates = getXYCordinates(adress)
            //result mangler time og date og så den søger efter arrival time todo når vi kan læse fra kalender
            val result = URL("http://xmlopen.rejseplanen.dk/bin/rest.exe/trip?" +
                    "originCoordName="+adress.toString()+"&originCoordX=" + startCordinates[0] + "&originCoordY=" + startCordinates[1] +
                    "&destCoordName=" + customLocation + "&destCoordX=" + destCordinates[0] + "&destCoordY=" + destCordinates[1] + "&format=json\n").readText()

            runOnUiThread {
                val busField = findViewById<TextView>(R.id.bus)
                val tripInfo = extractTripInfo(result)
                val from = SimpleDateFormat("hh:mm").parse(tripInfo!!.Leg.first().Origin.time)
                val current = java.util.Calendar.getInstance()
                //val result = (current.time - from.time)/1000
                //val min = (result/60).toInt().toString()
                //val seconds = (result % 60).toString()
                val bus = tripInfo.Leg.filter { l->l.type != "WALK"}.first()
                busField.setText(bus.name)
                locationField.setText(tripInfo!!.Leg.first().Destination.name)
                timeField.setText(tripInfo.Leg.first().Destination.time)
                }

        }.start()
    }

    private fun extractTripInfo(pathInfo: String): Trip? {
        var bestTime = MAX_VALUE
        val reader = Klaxon().parse<TripClass>(pathInfo)
        if (reader != null) {

            val triplist = reader.TripList
            val trips = triplist.Trip
            val tripping = trips.first().Leg
            var bestTrip = trips.first()
            for (trip in trips){

                if (trip.getDuration() < bestTime){
                    bestTime = trip.getDuration()
                    bestTrip = trip
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
        val notification = Notification.Builder(this@MainActivity)
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
            sendNotification(location.latitude.toString(), location.toString())
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




