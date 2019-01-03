package jtycz.simple.mesh.connector

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import jtycz.simple.mesh.connector.ui.barcode.BarcodeScanningActivity
import jtycz.simple.mesh.connector.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    private val BARCODE_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }

    private fun startBarcodeScanningActivity(){
        val intent = Intent(this,BarcodeScanningActivity::class.java)
        startActivityForResult(intent,BARCODE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == BARCODE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            //TODO go and connect to device
        }
    }

}
