package com.macro.avins

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.macro.avins.databinding.ActivityMainBinding
import java.util.Arrays


class MainActivity : AppCompatActivity(), SensorEventListener, TextureView.SurfaceTextureListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccel: Sensor
    private lateinit var mGyro: Sensor

    private lateinit var mBackgroundThread: HandlerThread
    private lateinit var mBackgroundHandler: Handler
    private lateinit var mCameraManager: CameraManager
    private lateinit var mCameraId: String
    private lateinit var mCameraDevice: CameraDevice
    private lateinit var mCaptureRequestBuilder: CaptureRequest.Builder
    private lateinit var mImageReader: ImageReader
    private lateinit var mTextureView: TextureView

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

        mTextureView = findViewById(R.id.textureView);
        mTextureView.surfaceTextureListener = this;

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

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        StartCamera(width, height);
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return true;
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    val mOnImageAvailableListener = ImageReader.OnImageAvailableListener() {
        fun onImageAvailable(reader: ImageReader) {
            Log.e("imagereader", "onImageAvailable");
            reader.acquireLatestImage()?.close()
        }
    }

    val mCaptureCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {}

        override fun onConfigured(session: CameraCaptureSession) {

            session.setRepeatingRequest(
                mCaptureRequestBuilder.build(),
                null,
                mBackgroundHandler
            )
        }
    }

    var mCameraCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            mCaptureRequestBuilder =
                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            var previewTexture = mTextureView.getSurfaceTexture();
            previewTexture?.setDefaultBufferSize(mTextureView.width, mTextureView.height);
            var previewSurface = Surface(previewTexture);
            mCaptureRequestBuilder.addTarget(previewSurface)
            mCaptureRequestBuilder.addTarget(mImageReader.surface)

            mCameraDevice.createCaptureSession(
                Arrays.asList(mImageReader.surface, previewSurface),
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

    private fun StartCamera(width: Int, height: Int) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this,"Lacking privileges to access camera service, please request permission first",Toast.LENGTH_SHORT).show();
            Log.e("customCarmeraActivity.openCamera","Lacking privileges to access camera service, please request permission first");
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 123);
            return;
        }

        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler)

        try {
            if (mCameraManager.cameraIdList.isEmpty()) {
                return
            }
            mCameraId = mCameraManager.cameraIdList[0]

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
//        Log.i("sensor: ", buffer.toString())

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("Not yet implemented", "")
    }

    /**
     * A native method that is implemented by the 'avins' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'avins' library on application startup.
        init {
            System.loadLibrary("avins")
        }
    }
}