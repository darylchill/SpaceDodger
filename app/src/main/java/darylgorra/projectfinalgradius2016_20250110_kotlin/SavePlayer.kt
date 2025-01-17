package darylgorra.projectfinalgradius2016_20250110_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SavePlayer : AppCompatActivity() {

    private lateinit var txtname: EditText
    private lateinit var txtTime: TextView
    private lateinit var btnSave: Button
    private lateinit var btnView: Button
    private lateinit var btnReturn: Button

    private var PlayTime: String? = null
    var myDB: DatabaseHelper? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_save_player)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_save_player)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtname = findViewById<View>(R.id.txtName) as EditText
        txtTime = findViewById<View>(R.id.time) as TextView
        btnSave = findViewById<View>(R.id.Save) as Button
        btnView = findViewById<View>(R.id.btnView) as Button
        btnReturn = findViewById<View>(R.id.Return) as Button

        val extras = intent.extras
        PlayTime = extras!!.getString("TIME")

        txtTime.text = PlayTime

        myDB = DatabaseHelper(this)


        addData()
        viewAll()
        returnStart()
    }

    private fun returnStart() {
        btnReturn.setOnClickListener {
            startActivity(
                Intent(
                    applicationContext,
                    StartMenu::class.java
                )
            )
        }
    }

    private fun addData() {
        btnSave.setOnClickListener {
            var isInserted = myDB!!.insertData(txtname.text.toString(), txtTime.text.toString())
            if (true.also { isInserted = it }) {
                Toast.makeText(applicationContext, "Data Inserted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Data not Inserted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun viewAll() {
        btnView.setOnClickListener {
            val result = myDB!!.getAllData()
            if (result.count == 0) {
                showMessage("Error", "Nothing found!")
                return@setOnClickListener
            }
            val buffer = StringBuffer()
            while (result.moveToNext()) {
                buffer.append("ID :").append(result.getString(0)).append("\n")
                buffer.append("Name :").append(result.getString(1)).append("\n")
                buffer.append("Time :").append(result.getString(2)).append("\n")
            }

            //Show all data
            showMessage("Data", buffer.toString())
        }
    }

    private fun showMessage(title: String?, message: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.show()
    }
}