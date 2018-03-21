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
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat



class MainActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_CALENDAR),1)
        var tripButton = findViewById<Button>(R.id.button)


        tripButton.text = "not yet started"
        tripButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                calculatePath(tripButton)

                }


        })
    }

    fun calculatePath(tripButton: Button){
        tripButton.text ="Started"
        Thread(){
            tripButton.text = "resulted hahaha"
            val result = URL("http://xmlopen.rejseplanen.dk/bin/rest.exe/location?input=erikholmsparken 178a\n")
            // val result mangler .readtext men virker ikke for some reason
            tripButton.text = "backup sladderhank"
        }.start()

    }
    /** base url http://xmlopen.rejseplanen.dk/bin/rest.exe
    url format
    http://<baseurl>/trip?originId=8600626&destCoordX=<xInteger>&
    destCoordY=<yInteger>&destCoordName=<NameOfDestination>&date=
    19.09.10&time=07:02&useBus=0*/


        

}

private operator fun Int.invoke(function: () -> Unit) {}
