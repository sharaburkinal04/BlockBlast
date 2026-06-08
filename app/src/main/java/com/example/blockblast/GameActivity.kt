package com.example.blockblast

import android.app.Activity
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blockblast.model.Piece
import com.example.blockblast.ui.theme.BlockBlastTheme
import com.example.blockblast.viewmodel.GameViewModel
import com.example.blockblast.viewmodel.GameViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class GameActivity : ComponentActivity() {

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
                    GameScreen()
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

    fun triggerGameOver(score: Int) {
        isChangingActivity = true
        val intent = Intent(this, GameOverActivity::class.java).apply {
            putExtra("SCORE", score)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun TexturedBlock(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.texture_block),
        contentDescription = "Block",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(LocalContext.current.applicationContext)
    )
) {
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(LocalContext.current.applicationContext)
    )
    val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()
    val soundVolume by settingsViewModel.soundVolume.collectAsState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val score by viewModel.score.collectAsState()
    val board by viewModel.board.collectAsState()
    val boardColors by viewModel.boardColors.collectAsState()
    val availablePieces by viewModel.availablePieces.collectAsState()
    val isGameOver by viewModel.isGameOver.collectAsState()

    val context = LocalContext.current
    val comfortaaFont = FontFamily(Font(R.font.productsans))

    var selectedPiece by remember { mutableStateOf<Piece?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var previewRow by remember { mutableStateOf(-1) }
    var previewCol by remember { mutableStateOf(-1) }
    var ghostOffset by remember { mutableStateOf(Offset.Zero) }
    var boardPosition by remember { mutableStateOf(Offset.Zero) }
    var boardSize by remember { mutableStateOf(Offset.Zero) }

    val scrollState = rememberScrollState()

    var waveProgress by remember { mutableStateOf(0f) }
    var waveCenterPx by remember { mutableStateOf(Offset.Zero) }
    var isWaveAnimating by remember { mutableStateOf(false) }
    var shakeOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        viewModel.waveEvent.collect { event ->
            if (boardSize != Offset.Zero && boardPosition != Offset.Zero) {
                val cellSize = boardSize / 8f
                val centerX = boardPosition.x + event.centerCol * cellSize.x + cellSize.x / 2
                val centerY = boardPosition.y + event.centerRow * cellSize.y + cellSize.y / 2
                waveCenterPx = Offset(centerX, centerY)

                scope.launch {
                    isWaveAnimating = true
                    waveProgress = 0f
                    for (i in 0..5) {
                        shakeOffset = Offset(
                            with(density) { Random.nextInt(-3, 3).dp.toPx() },
                            with(density) { Random.nextInt(-3, 3).dp.toPx() }
                        )
                        delay(20)
                    }
                    shakeOffset = Offset.Zero
                    val anim = Animatable(0f, Float.VectorConverter)
                    anim.animateTo(1f, animationSpec = tween(durationMillis = 600, easing = LinearEasing))
                    waveProgress = anim.value
                    delay(600)
                    isWaveAnimating = false
                    waveProgress = 0f
                }
            }
        }
    }

    fun resetDrag() {
        isDragging = false
        selectedPiece = null
        previewRow = -1
        previewCol = -1
        ghostOffset = Offset.Zero
    }

    fun tryPlacePiece(row: Int, col: Int): Boolean {
        if (selectedPiece != null && viewModel.placeSelectedPiece(row, col)) {
            resetDrag()
            return true
        }
        resetDrag()
        return false
    }

    LaunchedEffect(isGameOver) {
        if (isGameOver) {
            val intent = Intent(context, GameOverActivity::class.java).apply {
                putExtra("SCORE", score)
            }
            context.startActivity(intent)
            (context as? Activity)?.finish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val backgroundRes = if (isDarkTheme) R.drawable.game_background_night else R.drawable.gameact_background
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = "Game Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    state = scrollState,
                    enabled = !isDragging
                )
                .padding(16.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            if (isDragging && selectedPiece != null) {
                                val change = event.changes.firstOrNull() ?: continue
                                when (event.type) {
                                    PointerEventType.Move -> {
                                        ghostOffset = change.position
                                        val relativeX = change.position.x - boardPosition.x
                                        val relativeY = change.position.y - boardPosition.y
                                        if (relativeX in 0f..boardSize.x && relativeY in 0f..boardSize.y) {
                                            val cellSize = boardSize.x / 8
                                            val col = (relativeX / cellSize).toInt().coerceIn(0, 7)
                                            val row = (relativeY / cellSize).toInt().coerceIn(0, 7)
                                            if (viewModel.canPlacePiece(board, selectedPiece!!, row, col)) {
                                                previewRow = row
                                                previewCol = col
                                            } else {
                                                previewRow = -1
                                                previewCol = -1
                                            }
                                        } else {
                                            previewRow = -1
                                            previewCol = -1
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        if (previewRow != -1 && previewCol != -1) {
                                            tryPlacePiece(previewRow, previewCol)
                                        } else {
                                            resetDrag()
                                        }
                                        change.consume()
                                    }
                                    PointerEventType.Exit -> {
                                        resetDrag()
                                        change.consume()
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassScoreCard(
                score = score,
                font = comfortaaFont,
                shakeOffset = shakeOffset,
                isWaveAnimating = isWaveAnimating
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onGloballyPositioned { coords ->
                        boardPosition = coords.positionInRoot()
                        boardSize = Offset(coords.size.width.toFloat(), coords.size.height.toFloat())
                    }
            ) {
                GameBoard(
                    board = board,
                    boardColors = boardColors,
                    previewPiece = if (previewRow != -1 && previewCol != -1) selectedPiece else null,
                    previewRow = previewRow,
                    previewCol = previewCol,
                    onCellClick = { row, col ->
                        if (selectedPiece != null && !isDragging) {
                            if (viewModel.placeSelectedPiece(row, col)) {
                                resetDrag()
                            }
                        }
                    },
                    shakeOffset = shakeOffset,
                    isWaveAnimating = isWaveAnimating,
                    density = density
                )

                if (isWaveAnimating && waveProgress > 0f && waveCenterPx != Offset.Zero) {
                    WaveOverlay(
                        waveCenter = waveCenterPx,
                        waveProgress = waveProgress,
                        boardSize = boardSize,
                        density = density
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                availablePieces.forEachIndexed { index, piece ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                    ) {
                        DraggablePieceStarter(
                            piece = piece,
                            isSelected = selectedPiece == piece,
                            onDragStart = { touchPosition ->
                                if (!isDragging) {
                                    SoundManager.playSound(context, R.raw.bubble, soundVolume)
                                    selectedPiece = piece
                                    viewModel.selectPiece(piece)
                                    isDragging = true
                                    ghostOffset = touchPosition
                                    previewRow = -1
                                    previewCol = -1
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isDragging && selectedPiece != null) {
            val ghostX = ghostOffset.x - 50
            val ghostY = ghostOffset.y - 50
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(ghostX.roundToInt(), ghostY.roundToInt())
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(100.dp)
                        .alpha(0.7f)
                ) {
                    PieceGhost(piece = selectedPiece!!)
                }
            }
        }
    }
}

@Composable
fun DraggablePieceStarter(
    piece: Piece,
    isSelected: Boolean,
    onDragStart: (Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(piece) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        if (event.type == PointerEventType.Press && !isSelected) {
                            onDragStart(change.position)
                            change.consume()
                            while (true) {
                                val releaseEvent = awaitPointerEvent(pass = PointerEventPass.Initial)
                                if (releaseEvent.type == PointerEventType.Release) {
                                    break
                                }
                            }
                        }
                    }
                }
            }
    ) {
        val isAtLeastAPI31 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val topAlpha = if (isAtLeastAPI31) 0.35f else 0.55f
        val bottomAlpha = if (isAtLeastAPI31) 0.20f else 0.40f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = Color.Black.copy(alpha = 0.05f),
                    ambientColor = Color.Black.copy(alpha = 0.03f)
                )
                .clip(RoundedCornerShape(12.dp))
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
                            Color.White.copy(alpha = topAlpha),
                            Color.White.copy(alpha = bottomAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isAtLeastAPI31) 0.4f else 0.6f),
                            Color.White.copy(alpha = if (isAtLeastAPI31) 0.1f else 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                for (i in piece.shape.indices) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        for (j in piece.shape[i].indices) {
                            val hasBlock = piece.shape[i][j] == 1
                            if (hasBlock) {
                                TexturedBlock(modifier = Modifier.size(28.dp))
                            } else {
                                Box(modifier = Modifier.size(28.dp).padding(1.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassScoreCard(
    score: Int,
    font: FontFamily,
    shakeOffset: Offset,
    isWaveAnimating: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer {
                if (isWaveAnimating) {
                    translationX = shakeOffset.x
                    translationY = shakeOffset.y
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    .width(100.dp)
                    .height(44.dp),
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

@Composable
fun WaveOverlay(
    waveCenter: Offset,
    waveProgress: Float,
    boardSize: Offset,
    density: Density
) {
    val maxRadius = max(boardSize.x, boardSize.y) * 1.0f
    val currentRadius = waveProgress * maxRadius
    val alpha = (1f - waveProgress * 0.6f).coerceIn(0.2f, 1f)
    val strokeWidthPx = with(density) { (32.dp).toPx() } * (1f - waveProgress * 0.7f)
    val finalStrokeWidth = strokeWidthPx.coerceAtLeast(with(density) { 4.dp.toPx() })

    Box(modifier = Modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        renderEffect = RenderEffect.createBlurEffect(
                            20f, 20f, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
            ) {
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.5f),
                    radius = currentRadius + with(density) { 16.dp.toPx() },
                    center = waveCenter,
                    style = Stroke(width = finalStrokeWidth * 2f)
                )
            }
        }

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.3f),
                radius = currentRadius + with(density) { 8.dp.toPx() },
                center = waveCenter,
                style = Stroke(width = finalStrokeWidth * 1.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = currentRadius,
                center = waveCenter,
                style = Stroke(width = finalStrokeWidth)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.8f),
                radius = currentRadius * 0.8f,
                center = waveCenter,
                style = Stroke(width = finalStrokeWidth * 0.7f)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.6f),
                radius = currentRadius * 0.4f,
                center = waveCenter,
                style = Stroke(width = finalStrokeWidth * 0.5f)
            )
        }
    }
}

@Composable
fun PieceGhost(piece: Piece) {
    val blockSize = 28.dp
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        for (i in piece.shape.indices) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (j in piece.shape[i].indices) {
                    val hasBlock = piece.shape[i][j] == 1
                    if (hasBlock) {
                        Box(
                            modifier = Modifier
                                .size(blockSize)
                                .padding(1.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    } else {
                        Box(modifier = Modifier.size(blockSize).padding(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoard(
    board: Array<IntArray>,
    boardColors: Array<IntArray>,
    previewPiece: Piece?,
    previewRow: Int,
    previewCol: Int,
    onCellClick: (row: Int, col: Int) -> Unit,
    shakeOffset: Offset,
    isWaveAnimating: Boolean,
    density: Density
) {
    val boardBgColor = Color(0xFF23445B)
    val borderColor = Color(0xFFBAD8FF)
    val borderWidth = 5.dp
    val cornerRadius = 16.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val shadowColor = Color.Black.copy(alpha = 0.3f)
                val shadowOffsetX = with(density) { (-4).dp.toPx() }
                val shadowOffsetY = with(density) { 3.dp.toPx() }
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply { color = shadowColor }
                    canvas.save()
                    canvas.translate(shadowOffsetX, shadowOffsetY)
                    canvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        with(density) { cornerRadius.toPx() }, with(density) { cornerRadius.toPx() },
                        paint
                    )
                    canvas.restore()
                }
            }
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        with(density) { cornerRadius.toPx() },
                        with(density) { cornerRadius.toPx() }
                    ),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { borderWidth.toPx() })
                )
            }
            .padding(borderWidth)
            .graphicsLayer {
                if (isWaveAnimating) {
                    translationX = shakeOffset.x
                    translationY = shakeOffset.y
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(boardBgColor, shape = RoundedCornerShape(cornerRadius))
                .padding(8.dp)
        ) {
            for (row in 0 until 8) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    for (col in 0 until 8) {
                        val isPreview = previewPiece != null &&
                                previewRow in 0..7 && previewCol in 0..7 &&
                                row in previewRow until previewRow + previewPiece.height &&
                                col in previewCol until previewCol + previewPiece.width &&
                                previewPiece.shape[row - previewRow][col - previewCol] == 1

                        val isFilled = board[row][col] == 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .clickable { onCellClick(row, col) }
                        ) {
                            when {
                                isFilled -> {
                                    TexturedBlock(modifier = Modifier.fillMaxSize())
                                }
                                isPreview -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = Color(0x88FFFFFF),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = Color(0xFF1E1E2A),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}