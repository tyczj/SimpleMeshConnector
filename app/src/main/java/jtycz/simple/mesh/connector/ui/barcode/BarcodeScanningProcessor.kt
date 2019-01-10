package jtycz.simple.mesh.connector.ui.barcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import java.io.IOException

class BarcodeScanningProcessor : VisionProcessorBase<List<FirebaseVisionBarcode>>() {

    val foundBarcodes: LiveData<List<FirebaseVisionBarcode>>
        get() = mutableFoundBarcodes

    private val mutableFoundBarcodes = MutableLiveData<List<FirebaseVisionBarcode>>()

    private val detector: FirebaseVisionBarcodeDetector

    init {
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_DATA_MATRIX)
            .build()
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {

        }

    }

    override fun detectInImage(image: FirebaseVisionImage): Task<List<FirebaseVisionBarcode>> {
        return detector.detectInImage(image)
    }

    override fun onSuccess(
        barcodes: List<FirebaseVisionBarcode>,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay) {

        graphicOverlay.clear()
        for (i in barcodes.indices) {
//            val barcode = barcodes[i]
//            val barcodeGraphic = BarcodeGraphic(graphicOverlay, barcode)
//            graphicOverlay.add(barcodeGraphic)
        }

//        mutableFoundBarcodes.setOnMainThread(barcodes)
    }

    override fun onFailure(e: Exception) {

    }
}