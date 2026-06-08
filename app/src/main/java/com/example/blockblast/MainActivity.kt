package com.example.blockblast

import android.content.Context
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
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

class MainActivity : ComponentActivity() {

    var isChangingActivity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(applicationContext)
            )
            val musicVolume by settingsViewModel.musicVolume.collectAsState()
            val soundVolume by settingsViewModel.soundVolume.collectAsState()
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

            LaunchedEffect(Unit) {
                SoundManager.init(applicationContext, musicVolume, soundVolume)
                SoundManager.startBackgroundMusic(applicationContext, R.raw.background_music, musicVolume)
            }

            val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
            val currentLocale = if (currentLanguage == "en") java.util.Locale("en") else java.util.Locale("ru")

            CompositionLocalProvider(LocalLocale provides currentLocale) {
                BlockBlastTheme(darkTheme = isDarkTheme) {
                    StartScreen()
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
}

@Composable
fun NameInputDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Введите ваш никнейм") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Никнейм") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun StartScreen() {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext)
    )
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingGameStart by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val designWidth = 360f
    val scale = screenWidthDp / designWidth

    val buttonWidth = (268 * scale).dp
    val buttonHeight = (70 * scale).dp
    val verticalOffset = (131 * scale).dp
    val fontSize = (21 * scale).sp
    val iconSize = (28 * scale).dp
    val spacing = (40 * scale).dp

    val backgroundRes = if (isDarkTheme) R.drawable.main_background_night else R.drawable.background_main
    val productSansFont = FontFamily(Font(R.font.productsans))

    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("user_name", null)

    if (showNameDialog) {
        NameInputDialog(
            onConfirm = { name ->
                prefs.edit().putString("user_name", name).apply()
                showNameDialog = false
                if (pendingGameStart) {
                    (context as? MainActivity)?.isChangingActivity = true
                    context.startActivity(Intent(context, GameActivity::class.java))
                }
            },
            onDismiss = {
                showNameDialog = false
                pendingGameStart = false
            }
        )
    }

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
                .wrapContentSize()
                .offset(y = verticalOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isDarkTheme) {
                GlassButtonDark(
                    text = localizedString(R.string.start_game),
                    onClick = {
                        if (userName.isNullOrEmpty()) {
                            showNameDialog = true
                            pendingGameStart = true
                        } else {
                            (context as? MainActivity)?.isChangingActivity = true
                            context.startActivity(Intent(context, GameActivity::class.java))
                        }
                    },
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                    endIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF3F4FBF),
                            modifier = Modifier.size(iconSize)
                        )
                    },
                    fontSize = fontSize,
                    fontFamily = productSansFont,
                    gradientColors = listOf(
                        Color(0xFFCFEFFF),
                        Color(0xFFB7E3FF),
                        Color(0xFF8FC7FF)
                    ),
                    borderColor = Color(0xFFDDF6FF),
                    textColor = Color(0xFF3F4FBF)
                )

                Spacer(modifier = Modifier.height(spacing))

                GlassButtonDark(
                    text = localizedString(R.string.settings),
                    onClick = {
                        (context as? MainActivity)?.isChangingActivity = true
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                    startIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = Color(0xFFEAF4FF),
                            modifier = Modifier.size(iconSize)
                        )
                    },
                    fontSize = fontSize,
                    fontFamily = productSansFont,
                    gradientColors = listOf(
                        Color(0xFF3C63E6),
                        Color(0xFF2E4FD0),
                        Color(0xFF1D2FA8)
                    ),
                    borderColor = Color(0xFF5EA9FF),
                    textColor = Color(0xFFEAF4FF)
                )
            } else {
                GlassButtonLight(
                    text = localizedString(R.string.start_game),
                    onClick = {
                        if (userName.isNullOrEmpty()) {
                            showNameDialog = true
                            pendingGameStart = true
                        } else {
                            (context as? MainActivity)?.isChangingActivity = true
                            context.startActivity(Intent(context, GameActivity::class.java))
                        }
                    },
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                    endIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(iconSize)
                        )
                    },
                    fontSize = fontSize,
                    fontFamily = productSansFont,
                    isBrighter = true,
                    textColor = Color(0xFF5F8AB8)
                )

                Spacer(modifier = Modifier.height(spacing))

                GlassButtonLight(
                    text = localizedString(R.string.settings),
                    onClick = {
                        (context as? MainActivity)?.isChangingActivity = true
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                    startIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(iconSize)
                        )
                    },
                    fontSize = fontSize,
                    fontFamily = productSansFont,
                    isBrighter = false,
                    textColor = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassButtonLight(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    startIcon: @Composable (() -> Unit)? = null,
    endIcon: @Composable (() -> Unit)? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 21.sp,
    isBrighter: Boolean = false,
    fontFamily: FontFamily,
    textColor: Color = Color(0xFF31467A)
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "scale"
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "backgroundAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    val baseBgColors = if (isBrighter) {
        listOf(0.55f, 0.45f)
    } else {
        listOf(0.20f, 0.15f)
    }

    val bgColors = baseBgColors.map { alpha ->
        val newAlpha = alpha * backgroundAlpha
        Color.White.copy(alpha = newAlpha.coerceIn(0f, 1f))
    }

    val baseModifier = modifier
        .graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale
        }
        .shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(999.dp),
            spotColor = Color.Black.copy(alpha = 0.05f),
            ambientColor = Color.Black.copy(alpha = 0.03f)
        )
        .clip(RoundedCornerShape(999.dp))
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    pressed = event.changes.any { it.pressed }
                }
            }
        }

    Box(modifier = baseModifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect.createBlurEffect(
                            35f, 35f, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(
                    brush = Brush.verticalGradient(colors = bgColors),
                    shape = RoundedCornerShape(999.dp)
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            startIcon?.let {
                Box(modifier = Modifier.size(28.dp)) { it() }
            } ?: Spacer(modifier = Modifier.width(28.dp))

            Text(
                text = text,
                fontSize = fontSize,
                fontFamily = fontFamily,
                color = textColor,
                letterSpacing = 0.sp
            )

            endIcon?.let {
                Box(modifier = Modifier.size(28.dp)) { it() }
            } ?: Spacer(modifier = Modifier.width(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassButtonDark(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    startIcon: @Composable (() -> Unit)? = null,
    endIcon: @Composable (() -> Unit)? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 21.sp,
    fontFamily: FontFamily,
    gradientColors: List<Color>,
    borderColor: Color,
    textColor: Color
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "scale"
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (pressed) 1.1f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "backgroundAlpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    val animatedGradientColors = gradientColors.map { color ->
        color.copy(
            red = (color.red * backgroundAlpha).coerceIn(0f, 1f),
            green = (color.green * backgroundAlpha).coerceIn(0f, 1f),
            blue = (color.blue * backgroundAlpha).coerceIn(0f, 1f)
        )
    }

    val baseModifier = modifier
        .graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale
        }
        .shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(999.dp),
            spotColor = Color.Black.copy(alpha = 0.05f),
            ambientColor = Color.Black.copy(alpha = 0.03f)
        )
        .clip(RoundedCornerShape(999.dp))
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    pressed = event.changes.any { it.pressed }
                }
            }
        }

    Box(modifier = baseModifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect.createBlurEffect(
                            35f, 35f, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(
                    brush = Brush.verticalGradient(colors = animatedGradientColors),
                    shape = RoundedCornerShape(999.dp)
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(borderColor.copy(alpha = 0.6f), borderColor.copy(alpha = 0.3f))
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            startIcon?.let {
                Box(modifier = Modifier.size(28.dp)) { it() }
            } ?: Spacer(modifier = Modifier.width(28.dp))

            Text(
                text = text,
                fontSize = fontSize,
                fontFamily = fontFamily,
                color = textColor,
                letterSpacing = 0.sp
            )

            endIcon?.let {
                Box(modifier = Modifier.size(28.dp)) { it() }
            } ?: Spacer(modifier = Modifier.width(28.dp))
        }
    }
}