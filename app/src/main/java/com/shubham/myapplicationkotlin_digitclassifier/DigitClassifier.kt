package com.shubham.myapplicationkotlin_digitclassifier

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.AccessControlContext
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DigitClassifier (private val context: Context){
    private var interpreter:Interpreter?=null
    var isInitialized = false
    private set

    private var executorService:ExecutorService = Executors.newCachedThreadPool()

    private var inputImageHeight:Int=0
    private var inputImageWidth:Int=0
    private var modelInputSize:Int=0

    fun initialize():Task<Void>{
        val task = TaskCompletionSource<Void>()
        executorService.execute{
            try {
                initializeInterpreter()
                task.setResult(null)
            }
            catch (e:IOException)
            {
                task.setException(e)
            }
        }
        return task.task
    }

    private fun initializeInterpreter() {
        val assetManager: AssetManager = context.assets
        val model = loadModelFile(assetManager, "minst.tflite")
        var options = Interpreter.Options()
        options.setUseNNAPI(true)
        var interpreter = Interpreter(model, options)
        val inputShape:IntArray = interpreter.getInputTensor(0).shape()
        inputImageHeight=inputShape[2]
        inputImageWidth=inputShape[1]
        modelInputSize=FLOAT_TYPE * inputImageWidth*inputImageHeight*PIXEL_VALUE
        this.interpreter = interpreter
        isInitialized = true
    }
    fun classifyAsyn(bitmap: Bitmap):Task<String>{
        val task = TaskCompletionSource<String>()
        executorService.execute{
            val result = classify(bitmap)
            task.setResult(result)

        }
        return task.task
    }
    private fun convertBitmaptoByteBuffer(bitmap: Bitmap):ByteBuffer
    {
        var byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        var pixels = IntArray(inputImageHeight*inputImageWidth)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width,bitmap.height)
        for (pixelvalue in pixels){
            var r = (pixelvalue shr 16 and 0xFF)
            var b = (pixelvalue  and 0xFF)
            var g = (pixelvalue shr 8 and 0xFF)

            val normalizedpixel = (r+g+b)/3.0f/255.0f
            byteBuffer.putFloat(normalizedpixel)
        }
        return byteBuffer
    }

    private fun classify(bitmap: Bitmap):String{
        check(isInitialized){
            "TF lite is not initialized"
        }
        val resizeImage:Bitmap=Bitmap.createScaledBitmap(
            bitmap, inputImageWidth, inputImageHeight, true
        )
        val  byteBuffer = convertBitmaptoByteBuffer(resizeImage)

        val output = Array(1){FloatArray(OUTPUT_CLASS)}

        interpreter?.run(byteBuffer, output)
        val result = output[0]
        val maxindex = result.indices.maxBy { result[it] } ?: -1
        val resultString = "prediction result: %d\nConfidence:%2f".format(maxindex, result[maxindex])
        return resultString

    }

    fun close()
    {
        executorService.execute{
            interpreter?.close()
        }
    }

    private fun loadModelFile(assetManager: AssetManager, filename:String):ByteBuffer{
        val fileDescriptor:AssetFileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel:FileChannel=inputStream.channel
        val startoffset:Long = fileDescriptor.startOffset
        var declaredLength:Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength)
    }
    companion object{
        private const val OUTPUT_CLASS =10
        private const val PIXEL_VALUE = 1
        private const val FLOAT_TYPE = 4
    }


}