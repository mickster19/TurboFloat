package com.turbofloat.app

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import java.io.OutputStream

class ScreenCaptureActivity : Activity() {

    companion object {
        private const val REQ_CODE_CAPTURE = 7001
        private var activeProjection: MediaProjection? = null
        private var activeRecorder: MediaRecorder? = null
        private var activeVirtualDisplay: VirtualDisplay? = null
        private var isCurrentlyRecording: Boolean = false
        private var currentMode: String = "SCREENSHOT"

        fun isRecording(): Boolean = isCurrentlyRecording

        fun stopRecordingIfActive(context: Context) {
            try {
                activeRecorder?.stop()
            } catch (_: Exception) {
            }
            try {
                activeRecorder?.release()
            } catch (_: Exception) {
            }
            try {
                activeVirtualDisplay?.release()
            } catch (_: Exception) {
            }
            try {
                activeProjection?.stop()
            } catch (_: Exception) {
            }
            activeRecorder = null
            activeVirtualDisplay = null
            activeProjection = null
            isCurrentlyRecording = false
            Toast.makeText(context, "Screen recording saved to Movies/TurboFloat", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentMode = intent?.getStringExtra("CAPTURE_MODE") ?: "SCREENSHOT"

        if (currentMode == "RECORD" && isCurrentlyRecording) {
            stopRecordingIfActive(this)
            finish()
            return
        }

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (mgr == null) {
            Toast.makeText(this, "Screen capture unavailable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            startActivityForResult(mgr.createScreenCaptureIntent(), REQ_CODE_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "Capture error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CODE_CAPTURE) {
            finish()
            return
        }
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "Permission cancelled", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = mgr.getMediaProjection(resultCode, data)
        if (projection == null) {
            Toast.makeText(this, "Could not start projection", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (currentMode == "RECORD") {
            startScreenRecord(projection)
        } else {
            takeInstantScreenshot(projection)
        }
    }

    private fun takeInstantScreenshot(projection: MediaProjection) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val virtualDisplay = projection.createVirtualDisplay(
            "TurboFloatScreenshot",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val image = imageReader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val rawBitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    rawBitmap.copyPixelsFromBuffer(buffer)
                    val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
                    rawBitmap.recycle()
                    image.close()

                    saveScreenshotToGallery(cropped)
                    cropped.recycle()
                } else {
                    Toast.makeText(this, "Could not capture frame", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Screenshot error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                try {
                    virtualDisplay.release()
                } catch (_: Exception) {
                }
                try {
                    imageReader.close()
                } catch (_: Exception) {
                }
                try {
                    projection.stop()
                } catch (_: Exception) {
                }
                finish()
            }
        }, 250L)
    }

    private fun saveScreenshotToGallery(bitmap: Bitmap) {
        val filename = "TurboFloat_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TurboFloat")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            var out: OutputStream? = null
            try {
                out = contentResolver.openOutputStream(uri)
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                }
                Toast.makeText(this, "Screenshot saved: Pictures/TurboFloat/$filename", Toast.LENGTH_SHORT).show()
            } finally {
                try {
                    out?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun startScreenRecord(projection: MediaProjection) {
        try {
            val metrics = resources.displayMetrics
            val width = (metrics.widthPixels / 2).let { if (it % 2 == 0) it else it - 1 }.coerceAtLeast(320)
            val height = (metrics.heightPixels / 2).let { if (it % 2 == 0) it else it - 1 }.coerceAtLeast(320)

            val filename = "TurboFloat_Rec_${System.currentTimeMillis()}.mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/TurboFloat")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val videoUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Cannot create video file")

            val pfd = contentResolver.openFileDescriptor(videoUri, "rw")
                ?: throw IllegalStateException("Cannot open video descriptor")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(3_500_000)
                setOutputFile(pfd.fileDescriptor)
                prepare()
                start()
            }

            val vDisplay = projection.createVirtualDisplay(
                "TurboFloatRecorder",
                width,
                height,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder.surface,
                null,
                null
            )

            activeProjection = projection
            activeRecorder = recorder
            activeVirtualDisplay = vDisplay
            isCurrentlyRecording = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                contentResolver.update(videoUri, values, null, null)
            }

            Toast.makeText(this, "Screen recording started", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Record start error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
