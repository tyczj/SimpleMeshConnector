package jtycz.simple.mesh.connector.ui.barcode

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import jtycz.simple.mesh.connector.R
import android.util.SparseIntArray
import android.view.*
import androidx.annotation.NonNull
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata


class BarcodeScanningFragment : Fragment(),ImageReader.OnImageAvailableListener {

    //Camera state: Showing camera preview.
    private val STATE_PREVIEW = 0

    //Camera state: Waiting for the focus to be locked.
    private val STATE_WAITING_LOCK = 1

    //Camera state: Waiting for the exposure to be precapture state.
    private val STATE_WAITING_PRECAPTURE = 2

    //Camera state: Waiting for the exposure state to be something other than precapture.
    private val STATE_WAITING_NON_PRECAPTURE = 3

    //Camera state: Picture was taken.
    private val STATE_PICTURE_TAKEN = 4

    companion object {
        fun newInstance() = BarcodeScanningFragment()
    }

    private val ORIENTATIONS = SparseIntArray()
    private lateinit var viewModel: BarcodeScanningViewModel
    private lateinit var detector:FirebaseVisionBarcodeDetector
    private lateinit var cameraManager:CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader:ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val sessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            // The camera is already closed
            if (cameraDevice == null) {
                return
            }
            // When the session is ready, we start capture.
            captureSession = cameraCaptureSession
            triggerImageCapture()
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
//            Log.w(TAG, "Failed to configure camera")
        }
    }

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(@NonNull cameraDevice: CameraDevice) {
            this@BarcodeScanningFragment.cameraDevice = cameraDevice
            this@BarcodeScanningFragment.cameraDevice?.createCaptureSession(listOf<Surface>(imageReader!!.surface),sessionCallback,null)
        }

        override fun onDisconnected(@NonNull cameraDevice: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(@NonNull cameraDevice: CameraDevice, i: Int) {
            cameraDevice.close()
        }

        override fun onClosed(@NonNull cameraDevice: CameraDevice) {
            this@BarcodeScanningFragment.cameraDevice = null
        }

    }

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(session: CameraCaptureSession,request: CaptureRequest,partialResult: CaptureResult) {
//            Log.d(TAG, "Partial result")
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,request: CaptureRequest,result: TotalCaptureResult) {
//            session.close()
//            mCaptureSession = null
//            Log.d(TAG, "CaptureSession closed")
        }
    }

    private fun triggerImageCapture() {
        try {
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            captureSession!!.capture(captureBuilder.build(), captureCallback, null)
        } catch (cae: CameraAccessException) {

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view: View = inflater.inflate(R.layout.camera_view, container, false)



        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(BarcodeScanningViewModel::class.java)

        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)

        val options:FirebaseVisionBarcodeDetectorOptions = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build()

        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)

        backgroundThread = HandlerThread("BackgroundThread")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread?.looper)
        backgroundHandler?.post(initializeOnBackground)
    }

    override fun onImageAvailable(reader: ImageReader?) {
        reader?.acquireNextImage().use {
            if (it != null){
                val image = FirebaseVisionImage.fromMediaImage(it,getRotationCompensation(cameraDevice!!.id,activity!!,context!!))

                detector.detectInImage(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes){
                            val rawValue = barcode.rawValue
                        }
                    }
                    .addOnFailureListener{}
            }
        }
    }

    @SuppressLint("MissingPermission")
    private val initializeOnBackground = Runnable {
        cameraManager = activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        imageReader = ImageReader.newInstance(480, 360, ImageFormat.YUV_420_888, 1)
        imageReader?.setOnImageAvailableListener(this, backgroundHandler)

        try {
            val camIds = cameraManager.cameraIdList

            if(ContextCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                cameraManager.openCamera(camIds[0], stateCallback, backgroundHandler)
            }
        } catch (e: CameraAccessException) {

        }
    }

    private fun getRotationCompensation(cameraId: String, activity: Activity, context: Context): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = activity.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // On most devices, the sensor orientation is 90 degrees, but for some
        // devices it is 270 degrees. For devices with a sensor orientation of
        // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
        val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360

        // Return the corresponding FirebaseVisionImageMetadata rotation value.
        val result: Int

        when (rotationCompensation) {
            0 -> result = FirebaseVisionImageMetadata.ROTATION_0
            90 -> result = FirebaseVisionImageMetadata.ROTATION_90
            180 -> result = FirebaseVisionImageMetadata.ROTATION_180
            270 -> result = FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                result = FirebaseVisionImageMetadata.ROTATION_0
            }
        }
        return result
    }
}