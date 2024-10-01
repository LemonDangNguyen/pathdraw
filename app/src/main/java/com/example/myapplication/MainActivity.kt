package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sử dụng view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Xử lý sự kiện nút Reset
        binding.resetButton.setOnClickListener {
            binding.drawingView.resetCanvas()
        }
    }
}