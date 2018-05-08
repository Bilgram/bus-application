package d803.busplanning

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_input.*
import org.jetbrains.anko.startActivityForResult

class InputActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        this.start.setOnClickListener{
            val intent = Intent(this@InputActivity, MainActivity::class.java)
            val dest = (this.destination.text).toString()
            intent.putExtra("destination", dest)
            startActivity(intent)
        }
    }
}
