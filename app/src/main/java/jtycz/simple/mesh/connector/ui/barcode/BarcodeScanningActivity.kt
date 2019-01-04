package jtycz.simple.mesh.connector.ui.barcode

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.otaliastudios.cameraview.*
import jtycz.simple.mesh.connector.R
import kotlinx.android.synthetic.main.camera_view.*

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
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_DATA_MATRIX)
            .build()

        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)

//        camera.setLifecycleOwner(this)
        camera.addCameraListener(object : CameraListener(){

            override fun onPictureTaken(result: PictureResult) {
                val bitmap = result.size.let { BitmapFactory.decodeByteArray(result.data, 0, result.data.size) }
                bitmap?.let { checkForBarcode(it) }
            }

        })
        camera.mode = Mode.PICTURE
        camera.facing = Facing.BACK
        camera.flash = Flash.OFF

        //kick off the picture taking
        camera.takePictureSnapshot()
    }

    override fun onResume() {
        super.onResume()
        camera.open()
    }

    override fun onPause() {
        super.onPause()
        camera.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera.destroy()
    }

    private fun checkForBarcode(bitmap: Bitmap){
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.detectInImage(image).addOnSuccessListener {
            if(!it.isEmpty()){
                val intent = Intent()
                intent.putExtra("barcode",it.first().rawValue)
                setResult(Activity.RESULT_OK,intent)
                finish()
            }else{
                //no barcode found, take another picture
                camera.takePictureSnapshot()
            }
        }
    }

}