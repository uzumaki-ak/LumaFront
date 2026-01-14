package com.edgelight.flashcam

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgelight.flashcam.service.EdgeLightService
import com.edgelight.flashcam.ui.theme.EdgeLightFlashTheme
import com.edgelight.flashcam.utils.PermissionManager

/**
 * MainActivity - Main UI screen
 *
 * PURPOSE: Control panel for EdgeLight
 * - Start/Stop service
 * - Request permissions
 * - Show service status
 * - Basic settings (coming soon)
 *
 * This is what the user sees when they open the app
 */
class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private var serviceRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission manager
        permissionManager = PermissionManager(this)

        setContent {
            EdgeLightFlashTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    /**
     * Main UI Screen
     * Shows status and controls for EdgeLight service
     */
    @Composable
    fun MainScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title
            Text(
                text = "EdgeLight Flash",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status indicator
            Text(
                text = if (serviceRunning) "🟢 Service Running" else "🔴 Service Stopped",
                style = MaterialTheme.typography.titleMedium,
                color = if (serviceRunning)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main control button
            Button(
                onClick = { toggleService() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (serviceRunning) "Stop Service" else "Start Service",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permissions button
            OutlinedButton(
                onClick = { requestAllPermissions() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Grant Permissions",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info text
            Text(
                text = "EdgeLight adds face-tracking screen flash to ALL apps.\n\n" +
                        "Works with WhatsApp, Telegram, Instagram, and more!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    /**
     * Toggles the EdgeLight service on/off
     */
    private fun toggleService() {
        if (serviceRunning) {
            // Stop service
            stopService(Intent(this, EdgeLightService::class.java))
            serviceRunning = false
        } else {
            // Check permissions first
            if (permissionManager.hasAllPermissions()) {
                // Start service
                val intent = Intent(this, EdgeLightService::class.java)
                startForegroundService(intent)
                serviceRunning = true
            } else {
                // Request permissions if not granted
                requestAllPermissions()
            }
        }
    }

    /**
     * Requests all required permissions
     */
    private fun requestAllPermissions() {
        permissionManager.requestAllPermissions()
    }
}