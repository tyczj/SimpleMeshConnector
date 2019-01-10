package jtycz.simple.mesh.connector.ui.barcode

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jtycz.simple.mesh.connector.R

import kotlinx.android.synthetic.main.activity_barcode_scanning2.*

class BarcodeScanningActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_barcode_scanning2)
        setSupportActionBar(toolbar)
    }

}
