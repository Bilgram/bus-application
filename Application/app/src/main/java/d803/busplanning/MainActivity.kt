package d803.busplanning

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
import android.location.LocationManager
import android.location.Location
import android.location.LocationListener
import android.support.design.widget.TextInputLayout
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.fixedRateTimer

class MainActivity : AppCompatActivity() {
    var locationManager:LocationManager?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_CALENDAR),1)
        val tripButton = findViewById<Button>(R.id.button)
        val inputField = findViewById<TextView>(R.id.textView)
        tripButton.text = "not yet started"
        getLocation()
        val intent= Intent(this, SecondaryActivity::class.java)
        intent.putExtra("key",2)
        //startActivity(intent)
        tripButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                calculatePath(tripButton,inputField)
                //getLocation()
                }
        })
    }

    fun calculatePath(tripButton: Button, location: TextView){
        tripButton.text ="Started"
        Thread(){
            //tripButton.text = "resulted hahaha"
            val result = URL("http://xmlopen.rejseplanen.dk/bin/rest.exe/location?input="+location.text+"&format=json").readText()
            val reader= JSONObject(result)
            val locationlist = reader.getJSONObject("LocationList")
            //val coordLocations = locationlist.getJSONObject(Coo)
            val test = locationlist.get("CoordLocation")
            var name = ""
            var x = ""
            var y =""

            if (test is JSONArray) {
                val coordLocation = locationlist.getJSONArray("CoordLocation")
                val getResult = coordLocation.getJSONObject(0)
                name = getResult.getString("name")
                x = getResult.getString("x")
                y = getResult.getString("y")

            }
            else{
                val getResult = locationlist.getJSONObject("CoordLocation")
                name = getResult.getString("name")
                x = getResult.getString("x")
                y = getResult.getString("y")
            }


            //val something = reader.getJSONObject("locationslist")
            runOnUiThread(){
                tripButton.text = name+x+y+location.text
            }
        }.start()


    }
    fun getLocation(){
        if (locationManager == null)
            locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            // Request location updates
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener);
        } catch(ex: SecurityException) {
            Log.d("myTag", "Security Exception, no location available");
        }
    }

    override fun onStop() {
        super.onStop()
        var title = "hej"
        val fixedRateTimer = fixedRateTimer(name="kappa",initialDelay = 100, period = 100){
            //sendNotification(title,"Body")
            title = title +"j"
        }



        }
    fun sendNotification(title: String, body: String){
        val intent = Intent()
        val pendingIntent = PendingIntent.getActivity(this@MainActivity,0,intent,0)
        val notification = Notification.Builder(this@MainActivity)
                .setTicker("")
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.notification_template_icon_bg)
                .setContentIntent(pendingIntent).notification
        notification.flags =Notification.FLAG_AUTO_CANCEL
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0,notification)

    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            sendNotification(location.latitude.toString(),location.toString())
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


