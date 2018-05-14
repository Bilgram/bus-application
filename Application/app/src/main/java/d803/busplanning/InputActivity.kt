package d803.busplanning

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_input.*

class InputActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        this.start.setOnClickListener{
            val intent = Intent(this@InputActivity, MainActivity::class.java)
            val dest = (this.destination.text).toString()
            val time = (this.time.text).toString()
            intent.putExtra("time",time)
            intent.putExtra("destination", dest)
            intent.putExtra("arrival","1")
            startActivity(intent)
        }
    }
}
