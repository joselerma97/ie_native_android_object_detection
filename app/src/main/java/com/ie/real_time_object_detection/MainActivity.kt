package com.ie.real_time_object_detection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.FileUtils
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ie.real_time_object_detection.ml.AutoModel1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class MainActivity : AppCompatActivity() {

    lateinit var labels: List<String>
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitMap: Bitmap
    lateinit var imageView: ImageView
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraView: TextureView
    lateinit var cameraManager: CameraManager
    lateinit var model: AutoModel1
    lateinit var floattingButton: FloatingActionButton


    var colors = listOf(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        floattingButton = findViewById(R.id.goCustom)
        floattingButton.setOnClickListener {
            val intent = Intent(this, CustomModel::class.java)
            startActivity(intent)
        }

        getPermission()

        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(
            300,300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = AutoModel1.newInstance(this)

        val handlerThread = HandlerThread("HandlerVideoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        cameraView = findViewById(R.id.cameraView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        imageView = findViewById(R.id.imageView)

        cameraView.surfaceTextureListener = object: TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                openCamara()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean = false

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitMap = cameraView.bitmap!!

                // Creates inputs for reference.
                var image = TensorImage.fromBitmap(bitMap)
                image = imageProcessor.process(image)
                // Runs model inference and gets result.
                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                val mutable = bitMap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)

                val h = mutable.height
                val w = mutable.width
                paint.textSize = h/80f
                paint.strokeWidth = h/350f
                var x = 0
                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
                    if(fl > 0.5){
                        paint.color = colors[index]
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(RectF(locations[x + 1] *w, locations[x] *h, locations[x + 3] *w, locations[x + 2] *h), paint)
                        paint.style = Paint.Style.FILL
                        canvas.drawText(labels[classes[index].toInt()] +" "+fl.toString(), locations[x+1] *w, locations[x] *h, paint)
                    }
                }

                imageView.setImageBitmap(mutable)


            }

        }

    }

    @SuppressLint("MissingPermission")
    private fun openCamara(){
        cameraManager.openCamera(cameraManager.cameraIdList[0],
        object: CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                val surfaceTexture = cameraView.surfaceTexture
                val surface = Surface(surfaceTexture)

                val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null)
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }

                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {
            }

            override fun onError(p0: CameraDevice, p1: Int) {
            }

        }, handler)
    }

    private fun getPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            getPermission()
        }else{

        }

    }

    @Override
    override fun onDestroy() {
        // Releases model resources if no longer used.
        super.onDestroy()
        model.close()
    }

}