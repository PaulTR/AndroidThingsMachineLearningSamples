package com.ptrprograms.machinelearningdemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.android.gms.vision.face.Landmark
import com.ptrprograms.machinelearningdemo.TextRecognition.TextRecognitionActivity
import com.ptrprograms.machinelearningdemo.assistant.AssistantActivity
import com.ptrprograms.machinelearningdemo.barcodereader.BarcodeReaderActivity
import com.ptrprograms.machinelearningdemo.facedetection.FaceDetectionActivity
import com.ptrprograms.machinelearningdemo.labels.LabelsActivity
import com.ptrprograms.machinelearningdemo.landmarks.LandmarkDetectionActivity
import com.ptrprograms.machinelearningdemo.tensorflow.TensorFlowActivity

import java.util.ArrayList

class MainActivity : Activity(), MachineLearningRecyclerViewAdapter.ItemClickListener {

    lateinit var recyclerView: RecyclerView
    lateinit var adapter: MachineLearningRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)


        val options = ArrayList<String>()
        options.add("Text Recognition")
        options.add("Face Detection")
        options.add("Barcode Reader")
        options.add("Image Labeling")
        options.add("Landmark Recognition")
        options.add("TensorFlow")
        options.add("Assistant")

        recyclerView = findViewById(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MachineLearningRecyclerViewAdapter(this, options)
        adapter.setClickListener(this)

        recyclerView.adapter = adapter

    }

    override fun onItemClick(view: View, position: Int) {
        Toast.makeText(this, adapter.getItem(position), Toast.LENGTH_SHORT ).show()

        when(adapter.getItem(position)) {
            "Text Recognition" -> {
                val activityIntent = Intent(applicationContext, TextRecognitionActivity::class.java)
                startActivity(activityIntent)
            }
            "Face Detection" -> {
                val activityIntent = Intent(applicationContext, FaceDetectionActivity::class.java)
                startActivity(activityIntent)
            }
            "Barcode Reader" -> {
                val activityIntent = Intent(applicationContext, BarcodeReaderActivity::class.java)
                startActivity(activityIntent)
            }
            "Image Labeling" -> {
                val activityIntent = Intent(applicationContext, LabelsActivity::class.java)
                startActivity(activityIntent)
            }
            "Landmark Recognition" -> {
                val activityIntent = Intent(applicationContext, LandmarkDetectionActivity::class.java)
                startActivity(activityIntent)
            }
            "TensorFlow" -> {
                val activityIntent = Intent(applicationContext, TensorFlowActivity::class.java)
                startActivity(activityIntent)
            }
            "Assistant" -> {
                val activityIntent = Intent(applicationContext, AssistantActivity::class.java)
                startActivity(activityIntent)
            }
        }
    }
}
