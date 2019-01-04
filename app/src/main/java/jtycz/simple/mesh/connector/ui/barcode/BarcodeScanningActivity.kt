package jtycz.simple.mesh.connector.ui.barcode

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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

/**
 * Using a 3rd party camera library for using interfacing with the camera because the camera API is stupid complex
 * and unnecessary to show for this example, you can find the library used here
 * https://github.com/natario1/CameraView
 */
class BarcodeScanningActivity : AppCompatActivity(),FrameProcessor {

    private lateinit var detector:FirebaseVisionBarcodeDetector

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_view)

        val options:FirebaseVisionBarcodeDetectorOptions = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_DATA_MATRIX)
            .build()

        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)

        camera.setLifecycleOwner(this)
        camera.addFrameProcessor(this)
        camera.sessionType = SessionType.PICTURE
        camera.facing = Facing.BACK
        camera.flash = Flash.OFF
    }

    override fun onResume() {
        super.onResume()
        camera.start()
    }

    override fun onPause() {
        super.onPause()
        camera.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera.destroy()
    }

    override fun process(frame: Frame) {
        val metadata = FirebaseVisionImageMetadata.Builder()
            .setWidth(480)
            .setHeight(360)
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(FirebaseVisionImageMetadata.ROTATION_0) // always assuming portrait
            .build()

        val image = FirebaseVisionImage.fromByteArray(frame.data, metadata)

        val result = detector.detectInImage(image)
            .addOnSuccessListener { barcodes ->
                val barcode = barcodes.first()
                val intent = Intent()
                intent.putExtra("barcode",barcode.rawValue)
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener {

            }
    }

}