package com.example.blockblast

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.blur
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

class GameOverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val score = intent.getIntExtra("SCORE", 0)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(applicationContext)
            )
            val musicVolume by settingsViewModel.musicVolume.collectAsState()

            LaunchedEffect(Unit) {
                SoundManager.playSound(applicationContext, R.raw.game_over, musicVolume)
            }

            val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
            val currentLocale = if (currentLanguage == "en") java.util.Locale("en") else java.util.Locale("ru")

            CompositionLocalProvider(LocalLocale provides currentLocale) {
                BlockBlastTheme {
                    GameOverScreen(score = score)
                }
            }
        }
    }
}

@Composable
fun GameOverScreen(score: Int) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(context.applicationContext)
    )
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val designWidth = 360f
    val screenScale = screenWidthDp / designWidth

    val scoreCardWidth = 345.dp
    val scoreCardHeight = 67.dp
    val scoreCardVerticalOffset = (-66).dp
    val gameOverFontSize = 68.sp

    val buttonsVerticalOffset = 53.dp
    val buttonWidth = (260 * screenScale).dp
    val buttonHeight = (68 * screenScale).dp
    val buttonsSpacing = 34.dp

    val backgroundRes = if (isDarkTheme) R.drawable.game_background_night else R.drawable.gameoverbackground
    val productSansFont = FontFamily(Font(R.font.productsans))
    val nunitoFont = FontFamily(Font(R.font.nunito_extrabold))

    val glowBlurRadius = 12.dp
    val glowAlpha = 0.4f

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Game Over Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.offset(y = scoreCardVerticalOffset)
            ) {
                GlassScoreCard(
                    score = score,
                    font = productSansFont,
                    width = scoreCardWidth,
                    height = scoreCardHeight,
                    screenScale = screenScale
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    Text(
                        text = "GAME",
                        fontSize = gameOverFontSize,
                        fontFamily = nunitoFont,
                        color = Color.White.copy(alpha = glowAlpha),
                        modifier = Modifier.blur(glowBlurRadius)
                    )
                    Text(
                        text = "GAME",
                        fontSize = gameOverFontSize,
                        fontFamily = nunitoFont,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(0.dp))
                Box {
                    Text(
                        text = "OVER",
                        fontSize = gameOverFontSize,
                        fontFamily = nunitoFont,
                        color = Color.White.copy(alpha = glowAlpha),
                        modifier = Modifier.blur(glowBlurRadius)
                    )
                    Text(
                        text = "OVER",
                        fontSize = gameOverFontSize,
                        fontFamily = nunitoFont,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.offset(y = buttonsVerticalOffset),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                GlassButtonGameOver(
                    text = localizedString(R.string.play_again),
                    onClick = {
                        context.startActivity(Intent(context, GameActivity::class.java))
                        (context as? GameOverActivity)?.finish()
                    },
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                    endIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = if (isDarkTheme) Color(0xFF3F4FBF) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size((28 * screenScale).dp)
                        )
                    },
                    fontSize = (21 * screenScale).sp,
                    isBrighter = true,
                    fontFamily = productSansFont,
                    isDarkTheme = isDarkTheme
                )

                Spacer(modifier = Modifier.height(buttonsSpacing))

                GlassButtonGameOver(
                    text = localizedString(R.string.records),
                    onClick = {
                        context.startActivity(Intent(context, RecordsActivity::class.java))
                    },
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                    fontSize = (21 * screenScale).sp,
                    isBrighter = false,
                    fontFamily = productSansFont,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
fun GlassScoreCard(
    score: Int,
    font: FontFamily,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    screenScale: Float
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color.Black.copy(alpha = 0.05f),
                    ambientColor = Color.Black.copy(alpha = 0.03f)
                )
                .clip(RoundedCornerShape(16.dp))
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
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localizedString(R.string.score),
                fontSize = 20.sp,
                fontFamily = font,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Card(
                modifier = Modifier
                    .width((100 * screenScale).dp)
                    .height((44 * screenScale).dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "$score",
                        fontSize = 25.sp,
                        fontFamily = font,
                        color = Color.White,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassButtonGameOver(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    startIcon: @Composable (() -> Unit)? = null,
    endIcon: @Composable (() -> Unit)? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 21.sp,
    isBrighter: Boolean = false,
    fontFamily: FontFamily,
    isDarkTheme: Boolean
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

    val (gradientColors, borderColor, textColor) = if (isDarkTheme) {
        if (isBrighter) {
            Triple(
                listOf(Color(0xFFCFEFFF), Color(0xFFB7E3FF), Color(0xFF8FC7FF)),
                Color(0xFFDDF6FF),
                Color(0xFF3F4FBF)
            )
        } else {
            Triple(
                listOf(Color(0xFF3C63E6), Color(0xFF2E4FD0), Color(0xFF1D2FA8)),
                Color(0xFF5EA9FF),
                Color(0xFFEAF4FF)
            )
        }
    } else {
        if (isBrighter) {
            Triple(
                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.45f)),
                Color.White.copy(alpha = 0.3f),
                Color(0xFF5F8AB8)
            )
        } else {
            Triple(
                listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.15f)),
                Color.White.copy(alpha = 0.1f),
                Color.White
            )
        }
    }

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