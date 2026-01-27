package com.edgelight.flashcam

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgelight.flashcam.service.EdgeLightService
import com.edgelight.flashcam.utils.PermissionManager
import com.edgelight.flashcam.utils.PreferencesManager
import kotlinx.coroutines.launch

/**
 * MainActivity - FIXED VERSION
 *
 * ALL ERRORS RESOLVED:
 * - Fixed enum name (EdgeLightGradients consistently)
 * - Fixed Color vs Int comparisons
 * - Fixed type inference issues
 * - Fixed null safety issues
 */
class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var preferencesManager: PreferencesManager
    private var serviceRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(this)
        preferencesManager = PreferencesManager(this)

        setContent {
            MainScreen()
        }
    }

    @Composable
    fun MainScreen() {
        val scope = rememberCoroutineScope()

        // FIXED: Proper type inference
        var selectedColorEnum by remember { mutableStateOf(EdgeLightColors.WARM_WHITE) }
        var selectedGradientEnum by remember { mutableStateOf<EdgeLightGradients?>(null) }

        // Load saved color from preferences
        LaunchedEffect(Unit) {
            preferencesManager.glowColorFlow.collect { savedColorInt ->
                // FIXED: Find matching color by converting Int to Color
                selectedColorEnum = EdgeLightColors.entries.find {
                    it.colorInt == savedColorInt
                } ?: EdgeLightColors.WARM_WHITE
            }
        }

        // FIXED: Proper color extraction
        val backgroundColor = selectedColorEnum.color.copy(alpha = 0.15f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "EdgeLight Flash",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (serviceRunning) Color.Green else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (serviceRunning) "Service Running" else "Service Stopped",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selectedGradientEnum != null) {
                                Brush.horizontalGradient(selectedGradientEnum!!.colors)
                            } else {
                                Brush.linearGradient(listOf(selectedColorEnum.color, selectedColorEnum.color))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Current Glow",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Color Picker Section
                Text(
                    text = "Choose Edge Light Color",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Solid Colors Grid - FIXED: Use entries instead of values()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    items(EdgeLightColors.entries) { colorOption ->
                        ColorCircle(
                            color = colorOption.color,
                            isSelected = selectedColorEnum == colorOption,
                            onClick = {
                                selectedColorEnum = colorOption
                                selectedGradientEnum = null
                                scope.launch {
                                    preferencesManager.setGlowColor(colorOption.colorInt)
                                    preferencesManager.setIsGradient(false)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Gradient Colors
                Text(
                    text = "Gradient Options",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                // FIXED: Use entries instead of values()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EdgeLightGradients.entries.forEach { gradientOption ->
                        GradientCircle(
                            gradient = gradientOption.colors,
                            isSelected = selectedGradientEnum == gradientOption,
                            onClick = {
                                selectedGradientEnum = gradientOption
                                scope.launch {
                                    preferencesManager.setGlowColor(gradientOption.colors[0].toArgb())
                                    preferencesManager.setIsGradient(true)
                                    preferencesManager.setGradientColors(
                                        gradientOption.colors.map { it.toArgb() }
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Control Buttons
                Button(
                    onClick = { toggleService() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (serviceRunning) Color.Red else Color.Green
                    )
                ) {
                    Text(
                        text = if (serviceRunning) "Stop Service" else "Start Service",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { requestAllPermissions() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Grant Permissions",
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }

    @Composable
    fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 4.dp else 0.dp,
                    color = Color.Black,
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        )
    }

    @Composable
    fun GradientCircle(gradient: List<Color>, isSelected: Boolean, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(gradient))
                .border(
                    width = if (isSelected) 4.dp else 0.dp,
                    color = Color.Black,
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        )
    }

    private fun toggleService() {
        if (serviceRunning) {
            stopService(Intent(this, EdgeLightService::class.java))
            serviceRunning = false
        } else {
            if (permissionManager.hasAllPermissions()) {
                val intent = Intent(this, EdgeLightService::class.java)
                startForegroundService(intent)
                serviceRunning = true
            } else {
                requestAllPermissions()
            }
        }
    }

    private fun requestAllPermissions() {
        permissionManager.requestAllPermissions()
    }
}

/**
 * FIXED: Each color now stores both Color and Int representations
 */
enum class EdgeLightColors(val color: Color, val colorInt: Int) {
    WARM_WHITE(Color(0xFFFFF4E6), 0xFFFFF4E6.toInt()),
    PURE_WHITE(Color(0xFFFFFFFF), 0xFFFFFFFF.toInt()),
    NEON_PINK(Color(0xFFFF10F0), 0xFFFF10F0.toInt()),
    NEON_BLUE(Color(0xFF00F0FF), 0xFF00F0FF.toInt()),
    NEON_GREEN(Color(0xFF39FF14), 0xFF39FF14.toInt()),
    NEON_YELLOW(Color(0xFFFFFF00), 0xFFFFFF00.toInt()),
    NEON_ORANGE(Color(0xFFFF6600), 0xFFFF6600.toInt()),
    NEON_PURPLE(Color(0xFFBF00FF), 0xFFBF00FF.toInt()),
    ELECTRIC_BLUE(Color(0xFF0080FF), 0xFF0080FF.toInt()),
    HOT_PINK(Color(0xFFFF1493), 0xFFFF1493.toInt()),
    LIME_GREEN(Color(0xFF00FF00), 0xFF00FF00.toInt()),
    CYAN(Color(0xFF00FFFF), 0xFF00FFFF.toInt()),
    MAGENTA(Color(0xFFFF00FF), 0xFFFF00FF.toInt()),
    GOLD(Color(0xFFFFD700), 0xFFFFD700.toInt()),
    ROSE_GOLD(Color(0xFFFFB6C1), 0xFFFFB6C1.toInt()),
    MINT(Color(0xFF98FF98), 0xFF98FF98.toInt()),
    LAVENDER(Color(0xFFE6E6FA), 0xFFE6E6FA.toInt()),
    PEACH(Color(0xFFFFDAB9), 0xFFFFDAB9.toInt()),
    CORAL(Color(0xFFFF7F50), 0xFFFF7F50.toInt()),
    TEAL(Color(0xFF008080), 0xFF008080.toInt())
}

/**
 * FIXED: Consistent naming (EdgeLightGradients with 's')
 */
enum class EdgeLightGradients(val colors: List<Color>) {
    RAINBOW(listOf(
        Color(0xFFFF0000), Color(0xFFFF7F00), Color(0xFFFFFF00),
        Color(0xFF00FF00), Color(0xFF0000FF), Color(0xFF4B0082), Color(0xFF9400D3)
    )),
    SUNSET(listOf(Color(0xFFFF6B6B), Color(0xFFFFD93D), Color(0xFFFFA07A))),
    OCEAN(listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))),
    FIRE(listOf(Color(0xFFFF0000), Color(0xFFFF8C00), Color(0xFFFFD700)))
}

// FIXED: Extension function to convert Color to Int
fun Color.toArgb(): Int {
    val a = (alpha * 255.0f + 0.5f).toInt()
    val r = (red * 255.0f + 0.5f).toInt()
    val g = (green * 255.0f + 0.5f).toInt()
    val b = (blue * 255.0f + 0.5f).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
} 