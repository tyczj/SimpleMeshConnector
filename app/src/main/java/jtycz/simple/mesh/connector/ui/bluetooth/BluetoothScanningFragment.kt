package jtycz.simple.mesh.connector.ui.bluetooth

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import jtycz.simple.mesh.connector.R

class BluetoothScanningFragment : androidx.fragment.app.Fragment() {

    companion object {
        fun newInstance() = BluetoothScanningFragment()
    }

    private lateinit var viewModel: BluetoothScanningViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view:View = inflater.inflate(R.layout.main_fragment, container, false)



        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BluetoothScanningViewModel::class.java)

        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build()
    }


}

