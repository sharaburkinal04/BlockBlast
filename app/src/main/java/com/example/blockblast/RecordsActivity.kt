package com.example.blockblast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blockblast.data.ScoreData

class RecordsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(applicationContext)
            )
            val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
            val currentLocale = if (currentLanguage == "en") java.util.Locale("en") else java.util.Locale("ru")
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

            val prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
            val musicVolume = prefs.getFloat("music_volume", 0.5f)
            SoundManager.startBackgroundMusic(applicationContext, R.raw.background_music, musicVolume)

            CompositionLocalProvider(LocalLocale provides currentLocale) {
                RecordsScreen(isDarkTheme = isDarkTheme)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        SoundManager.pauseBackgroundMusic()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        val musicVolume = prefs.getFloat("music_volume", 0.5f)
        SoundManager.startBackgroundMusic(applicationContext, R.raw.background_music, musicVolume)
    }
}

@Composable
fun RecordsScreen(isDarkTheme: Boolean) {
    val context = LocalContext.current
    val productSansFont = FontFamily(Font(R.font.productsans))
    var records by remember { mutableStateOf<List<ScoreData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val backgroundRes = if (isDarkTheme) R.drawable.game_background_night else R.drawable.gameoverbackground

    LaunchedEffect(Unit) {
        isLoading = true
        FirebaseManager.getTopScores().collect { scores ->
            records = scores
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = localizedString(R.string.records_title),
                fontSize = 28.sp,
                fontFamily = productSansFont,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(records) { index, record ->
                        RecordItem(
                            rank = index + 1,
                            record = record,
                            font = productSansFont,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordItem(rank: Int, record: ScoreData, font: FontFamily, isDarkTheme: Boolean) {
    val isAtLeastAPI31 = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    val bgColor = if (isDarkTheme) {
        Color(0x66272F94)
    } else {
        Color.White.copy(alpha = if (isAtLeastAPI31) 0.25f else 0.4f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isAtLeastAPI31) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            20f, 20f, android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(bgColor)
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontFamily = font,
                modifier = Modifier.width(30.dp)
            )

            Text(
                text = record.playerName,
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = font,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${record.score}",
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = font
            )
        }
    }
}