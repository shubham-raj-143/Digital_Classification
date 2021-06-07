package com.shubham.myapplicationkotlin_digitclassifier

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import com.divyanshu.draw.widget.DrawView

class MainActivity : AppCompatActivity() {
    private var drawView:DrawView?=null
    private var clearButton:Button?=null
    private var predictionTextView : TextView?=null
    private  var digitClassifier = DigitClassifier(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawView = findViewById(R.id.draw_view)
        drawView?.setStrokeWidth(70.0f)
        drawView?.setColor(Color.WHITE)
        drawView?.setBackgroundColor(Color.BLACK)

        clearButton = findViewById(R.id.clr_btn)
        predictionTextView = findViewById(R.id.prediction_text)

        clearButton?.setOnClickListener{
            drawView?.clearCanvas()
            predictionTextView?.text = getString(R.string.prediction_place_holder)
        }

        drawView?.setOnTouchListener { v, event ->
            drawView?.onTouchEvent(event)
            if(event.action == MotionEvent.ACTION_UP){
                classifyDrawing()
            }
            true
        }
        digitClassifier.initialize().addOnFailureListener{e-> Log.e(TAG, "onCreate: error", e)}
    }

    override fun onDestroy() {
        digitClassifier.close()
        super.onDestroy()
    }

    private fun classifyDrawing()
    {
        val bitmap = drawView?.getBitmap()
        if((bitmap!=null) && (digitClassifier.isInitialized))
        {
            digitClassifier.classifyAsyn(bitmap).addOnSuccessListener { resultText->
                predictionTextView?.text = resultText
            }.addOnFailureListener{e->
                predictionTextView?.text = getString(R.string.classification_error)
            }
        }

    }
    companion object
    {
        private val TAG = "MainActivity"
    }

}