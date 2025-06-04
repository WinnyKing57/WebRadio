package com.example.webradioapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.webradioapp.R
import com.example.webradioapp.services.StreamingService

class HomeFragment : Fragment() {

    private lateinit var etStreamUrl: EditText
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button

    // A sample stream URL for quick testing
    private val defaultStreamUrl = "https://stream.radioparadise.com/flac"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        etStreamUrl = view.findViewById(R.id.et_stream_url)
        btnPlay = view.findViewById(R.id.btn_play)
        btnPause = view.findViewById(R.id.btn_pause)

        // Set default URL for easier testing
        etStreamUrl.setText(defaultStreamUrl)

        btnPlay.setOnClickListener {
            val streamUrl = etStreamUrl.text.toString()
            if (streamUrl.isNotEmpty()) {
                val serviceIntent = Intent(activity, StreamingService::class.java).apply {
                    action = StreamingService.ACTION_PLAY
                    putExtra(StreamingService.EXTRA_STREAM_URL, streamUrl)
                }
                activity?.startService(serviceIntent)
            }
        }

        btnPause.setOnClickListener {
            val serviceIntent = Intent(activity, StreamingService::class.java).apply {
                action = StreamingService.ACTION_PAUSE
            }
            activity?.startService(serviceIntent)
        }

        return view
    }
}
