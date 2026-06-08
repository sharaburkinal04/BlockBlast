package com.example.blockblast

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blockblast.ui.theme.BlockBlastTheme

class SettingsActivity : ComponentActivity() {

    private var isChangingActivity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(applicationContext)
            )
            val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
            val currentLocale = if (currentLanguage == "en") java.util.Locale("en") else java.util.Locale("ru")

            CompositionLocalProvider(LocalLocale provides currentLocale) {
                BlockBlastTheme {
                    SettingsScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isChangingActivity = false
        val prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        val musicVolume = prefs.getFloat("music_volume", 0.5f)
        SoundManager.startBackgroundMusic(applicationContext, R.raw.background_music, musicVolume)
    }

    override fun onPause() {
        super.onPause()
        if (!isChangingActivity) {
            SoundManager.pauseBackgroundMusic()
        }
    }

    override fun onBackPressed() {
        isChangingActivity = true
        super.onBackPressed()
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext)
    )
    val musicVolume by settingsViewModel.musicVolume.collectAsState()
    val soundVolume by settingsViewModel.soundVolume.collectAsState()
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val designWidth = 360f
    val scale = screenWidthDp / designWidth

    var languageDropdownExpanded by remember { mutableStateOf(false) }

    val backgroundRes = if (isDarkTheme) R.drawable.settings_background_night else R.drawable.settingsbackground
    val productSansFont = FontFamily(Font(R.font.productsans))

    val verticalPadding = (24 * scale).dp
    val horizontalPadding = (24 * scale).dp
    val glassBoxCornerRadius = (24 * scale).dp
    val glassBoxPadding = (16 * scale).dp
    val spacingMedium = (16 * scale).dp
    val spacingSmall = (8 * scale).dp
    val textFontSize = (18 * scale).sp
    val iconSize = (28 * scale).dp

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlassSettingsBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = glassBoxCornerRadius
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(glassBoxPadding),
                    verticalArrangement = Arrangement.spacedBy(spacingMedium)
                ) {
                    SettingsSliderRow(
                        label = localizedString(R.string.music),
                        value = musicVolume,
                        onValueChange = { newVolume ->
                            settingsViewModel.setMusicVolume(newVolume)
                            SoundManager.updateMusicVolume(newVolume)
                        },
                        font = productSansFont,
                        scale = scale
                    )
                    SettingsSliderRow(
                        label = localizedString(R.string.sound),
                        value = soundVolume,
                        onValueChange = { newVolume ->
                            settingsViewModel.setSoundVolume(newVolume)
                            SoundManager.updateSoundVolume(newVolume)
                        },
                        font = productSansFont,
                        scale = scale
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacingMedium))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedString(R.string.language),
                        fontSize = textFontSize,
                        fontFamily = productSansFont,
                        color = Color.White,
                        modifier = Modifier.padding(start = 18.dp)
                    )
                    Box {
                        Button(
                            onClick = { languageDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x66FFFFFF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Text(
                                when (currentLanguage) {
                                    "ru" -> localizedString(R.string.russian)
                                    else -> localizedString(R.string.english)
                                },
                                fontFamily = productSansFont,
                                color = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = languageDropdownExpanded,
                            onDismissRequest = { languageDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(localizedString(R.string.russian), fontFamily = productSansFont, color = Color(0xFF31467A)) },
                                onClick = {
                                    settingsViewModel.setLanguage("ru")
                                    languageDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(localizedString(R.string.english), fontFamily = productSansFont, color = Color(0xFF31467A)) },
                                onClick = {
                                    settingsViewModel.setLanguage("en")
                                    languageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacingMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = localizedString(R.string.theme),
                        fontSize = textFontSize,
                        fontFamily = productSansFont,
                        color = Color.White,
                        modifier = Modifier.padding(start = 18.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacingSmall)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Brightness4,
                            contentDescription = "Dark theme",
                            tint = Color(0x66FFFFFF),
                            modifier = Modifier.size(iconSize)
                        )
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { checked ->
                                settingsViewModel.setDarkTheme(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White.copy(alpha = 0.5f),
                                checkedTrackColor = Color.White.copy(alpha = 0.5f),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        Icon(
                            imageVector = Icons.Outlined.WbSunny,
                            contentDescription = "Light theme",
                            tint = Color(0x66FFFFFF),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSettingsBox(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    spotColor = Color.Black.copy(alpha = 0.05f),
                    ambientColor = Color.Black.copy(alpha = 0.03f)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect.createBlurEffect(
                            25f, 25f, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.20f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )

        Box {
            content()
        }
    }
}

@Composable
fun SettingsSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    font: FontFamily,
    scale: Float
) {
    val fontSize = (18 * scale).sp
    val labelWidth = (90 * scale).dp
    val sliderHeight = (48 * scale).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(sliderHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontFamily = font,
            color = Color.White,
            modifier = Modifier.width(labelWidth)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}