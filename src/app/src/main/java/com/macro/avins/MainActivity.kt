package com.macro.avins

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.macro.avins.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val Tag: String = "avins"

    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccel: Sensor
    private lateinit var mGyro: Sensor

    private lateinit var mBackgroundThread: HandlerThread
    private lateinit var mBackgroundHandler: Handler
    private lateinit var mCameraManager: CameraManager
    private lateinit var mCameraId: String
    private lateinit var mCameraDevice: CameraDevice
    private lateinit var mCaptureRequestBuilder: CaptureRequest.Builder
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mImageReader: ImageReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
    }

    override fun onResume() {
        super.onResume()

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED)!!
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED)!!
        mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_UI);

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceView.holder.addCallback(mSurfaceCallback);

        mBackgroundThread = HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = Handler(mBackgroundThread.getLooper());
    }

    override fun onPause() {
        super.onPause()

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
        } catch (e: InterruptedException) {
            e.printStackTrace();
        }
    }
    private val mSurfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            StartCamera(mSurfaceView.width, mSurfaceView.height);
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
        }
    }

    val mRequestCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureProgressed(session: CameraCaptureSession,
        request: CaptureRequest,
        partialResult: CaptureResult) {
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult) {
            val timeStamp = result[CaptureResult.SENSOR_TIMESTAMP]!!
            val mScreenBitmap = Bitmap.createBitmap(mSurfaceView.width, mSurfaceView.height, Bitmap.Config.ARGB_8888);
            Log.d(Tag, "request complete: $timeStamp")
            super.onCaptureCompleted(session, request, result)
            PixelCopy.request(mSurfaceView, mScreenBitmap,
                { copyResult ->
                    if (PixelCopy.SUCCESS == copyResult) {
                        Log.d(Tag,"SUCCESS ");
                    }
                }, mBackgroundHandler);
        }
    }

    val mCaptureCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {}

        override fun onConfigured(session: CameraCaptureSession) {

            session.setRepeatingRequest(
                mCaptureRequestBuilder.build(),
                mRequestCallback,
                mBackgroundHandler
            )
        }
    }

    private var mCameraCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            mCaptureRequestBuilder =
                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            mCaptureRequestBuilder.addTarget(mSurfaceView.holder.surface)
//            mCaptureRequestBuilder.addTarget(mImageReader.surface)

            mCameraDevice.createCaptureSession(
                listOf(mSurfaceView.holder.surface),
                mCaptureCallback,
                mBackgroundHandler
            )
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            mCameraDevice.close()
        }
    }

    private val mOnImageAvailableListener =
        ImageReader.OnImageAvailableListener { Log.d(Tag, "image read") }

    private fun StartCamera(width: Int, height: Int) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this,"Lacking privileges to access camera service, please request permission first",Toast.LENGTH_SHORT).show();
            Log.e(Tag,"Lacking privileges to access camera service, please request permission first");
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 123);
            return;
        }

        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        mImageReader = ImageReader.newInstance(width, height,  PixelFormat.RGBA_8888, 3)
//        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler)

        try {
            if (mCameraManager.cameraIdList.isEmpty()) {
                return
            }
            mCameraId = mCameraManager.cameraIdList[0]

            val characteristics = mCameraManager.getCameraCharacteristics(mCameraId)
            val fpsRange = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            Log.d(Tag, "fps: " + (fpsRange?.get(0) ?: 0))

            mCameraManager.openCamera(
                mCameraId,
                mCameraCallback,
                mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onSensorChanged(event: SensorEvent) {
        val buffer = StringBuffer()
        if (event.sensor == mAccel) {
            buffer.append("acc: ")
        }
        else if(event.sensor == mGyro) {
            buffer.append("gyr: ")
        }
        buffer.append(String.format("%s-%.2f,%.2f,%.2f",event.timestamp.toString(), event.values[0], event.values[1], event.values[2]))
        Log.d(Tag, buffer.toString())

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * A native method that is implemented by the 'avins' native library,
     * which is packaged with this application.
     */
    private external fun stringFromJNI(): String

    companion object {
        init {
            System.loadLibrary("avins")
        }
    }
}