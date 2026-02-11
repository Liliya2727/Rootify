package com.aby.rootify

import android.os.Bundle
import com.aby.rootify.utils.RootifyLog
import androidx.core.view.WindowCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugins.GeneratedPluginRegistrant

private const val TAG = "Rootify.MainActivity"

/// Production-grade MainActivity for Rootify system tool.
/// Restored to native behavior without refresh rate overrides.
class MainActivity: FlutterActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RootifyLog.init(this)
        RootifyLog.i("MainActivity: onCreate - Native initialization")
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        // super call handles GeneratedPluginRegistrant automatically in modern Flutter
        super.configureFlutterEngine(flutterEngine)
        
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            RootifyLog.d("Applied edge-to-edge via WindowCompat")
        } catch (e: Exception) {
            RootifyLog.w("Failed to apply window insets: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
