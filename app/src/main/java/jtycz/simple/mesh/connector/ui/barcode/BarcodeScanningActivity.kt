package jtycz.simple.mesh.connector.ui.barcode

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.otaliastudios.cameraview.*
import jtycz.simple.mesh.connector.R
import kotlinx.android.synthetic.main.camera_view.*
import com.google.android.gms.common.util.IOUtils.toByteArray
import java.io.ByteArrayOutputStream


/**
 * Using a 3rd party camera library for interfacing with the camera because the camera API is stupid complex
 * and unnecessary to show for this example, you can find the library used here
 * https://github.com/natario1/CameraView
 */
class BarcodeScanningActivity : AppCompatActivity(){

    private lateinit var detector:FirebaseVisionBarcodeDetector

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_view)

        val options:FirebaseVisionBarcodeDetectorOptions = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_ALL_FORMATS) //For some reason the barcode detector only find the data matrix in this format
            .build()

        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)

        camera.setLifecycleOwner(this)
        camera.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(jpeg: ByteArray?) {
                val bitmap = jpeg?.size?.let { BitmapFactory.decodeByteArray(jpeg, 0, it) }
                bitmap?.let { checkForBarcode(it) }
                imageView.setImageBitmap(bitmap)
            }

        })

        //kick off the picture taking
        val handler = Handler()
        handler.postDelayed({camera.captureSnapshot()},1000)

    }

    override fun onDestroy() {
        super.onDestroy()
        camera.destroy()
    }

    private fun checkForBarcode(bitmap: Bitmap){
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.detectInImage(image).addOnSuccessListener {
            Log.d("BarcodeScanningActivity","Barcodes found: ${it.size}")
            if(!it.isEmpty()){
                val intent = Intent()
                intent.putExtra("barcode",it.first().rawValue)
                setResult(Activity.RESULT_OK,intent)
                finish()
            }else{
                //no barcode found, take another picture
                camera.captureSnapshot()
            }
        }
    }
}