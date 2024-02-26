package com.ie.real_time_object_detection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ie.real_time_object_detection.ml.PenNotebookBottle
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp

import org.tensorflow.lite.support.image.TensorImage
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class CustomModel : AppCompatActivity() {

    lateinit var labels: List<String>
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitMap: Bitmap
    lateinit var imageView: ImageView
    lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
    lateinit var cameraView: TextureView
    lateinit var cameraManager: CameraManager
    lateinit var model: PenNotebookBottle
    lateinit var floatingActionButton: FloatingActionButton

    var colors = listOf(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)

    private val viewModel: ScoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_model)

        floatingActionButton = findViewById(R.id.goHome)
        floatingActionButton.isVisible = false
        floatingActionButton.setOnClickListener {
            PredictionsDialog.newInstance(viewModel.name!!, viewModel.score!!).show(supportFragmentManager,
                System.currentTimeMillis().toString())
        }

        getPermission()

        labels = FileUtil.loadLabels(this, "pen_notebook_bottle.txt")
        imageProcessor = ImageProcessor.Builder().add(
            ResizeOp(
            640,640, ResizeOp.ResizeMethod.BILINEAR)
        ).build()
        model = PenNotebookBottle.newInstance(this)

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

                val mutable = bitMap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)

                val h = mutable.height
                val w = mutable.width
                paint.textSize = h/40f
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

                        viewModel.name = labels[classes[index].toInt()]
                        viewModel.score = fl
                    }
                }

                imageView.setImageBitmap(mutable)
                floatingActionButton.isVisible = viewModel.hasNameAndScore()
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

    fun assetFilePath(context: Context, asset: String): String {
        val file = File(context.filesDir, asset)

        try {
            val inpStream: InputStream = context.assets.open(asset)
            try {
                val outStream = FileOutputStream(file, false)
                val buffer = ByteArray(4 * 1024)
                var read: Int

                while (true) {
                    read = inpStream.read(buffer)
                    if (read == -1) {
                        break
                    }
                    outStream.write(buffer, 0, read)
                }
                outStream.flush()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

}


