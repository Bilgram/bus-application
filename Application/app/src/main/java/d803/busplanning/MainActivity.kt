package d803.busplanning

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.net.URL
import android.R.attr.button
import android.util.Log
import android.view.View
import android.os.AsyncTask.execute
import java.io.File
import android.support.v4.app.ActivityCompat
import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Timer
import android.content.pm.PackageManager
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.app.NotificationChannel
import android.graphics.Color
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MainActivity : AppCompatActivity() {
    //private var locationmanager : locationmanager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_CALENDAR),1)
        var tripButton = findViewById<Button>(R.id.button)
        tripButton.text = "not yet started"

        val intent= Intent(this, SecondaryActivity::class.java)
        intent.putExtra("key",2)
        //startActivity(intent)
        tripButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                calculatePath(tripButton)
                }
        })
    }

    fun calculatePath(tripButton: Button){
        tripButton.text ="Started"
        Thread(){
            //tripButton.text = "resulted hahaha"

            val result = URL("http://xmlopen.rejseplanen.dk/bin/rest.exe/location?input=erikholmsparken 178a&format=json")
            //val result mangler .readtext men virker ikke for some reason
            //val reader: JSONObject = JSONObject(result.readText())
            //val something = reader.getJSONObject("locationslist")
            runOnUiThread(){
                tripButton.text = "done"
            }
        }.start()


    }

    override fun onStop() {
        super.onStop()
        var title = "hej"
        val fixedRateTimer = fixedRateTimer(name="kappa",initialDelay = 100, period = 100){
            sendNotification(title,"Body")
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


    /** base url http://xmlopen.rejseplanen.dk/bin/rest.exe
    url format
    http://<baseurl>/trip?originId=8600626&destCoordX=<xInteger>&
    destCoordY=<yInteger>&destCoordName=<NameOfDestination>&date=
    19.09.10&time=07:02&useBus=0*/


        

}


