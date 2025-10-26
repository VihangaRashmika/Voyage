package com.example.voyage_v2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.voyage_v2.ui.theme.Voyage_V2Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompt_screen)

        val btnExplore: Button = findViewById(R.id.btnExplore)
        btnExplore.setOnClickListener {
            // Go to Sign In activity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

    }
}
