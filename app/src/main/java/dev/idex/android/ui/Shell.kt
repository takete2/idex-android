package dev.idex.android.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.idex.android.data.AppLanguage
import dev.idex.android.ui.theme.DexColors
import dev.idex.android.ui.theme.LocalDexFonts

@Composable
fun PixelText(
    text: String,
    language: AppLanguage,
    size: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
) {
    val fonts = LocalDexFonts.current
    val cjk = language != AppLanguage.En
    val fontSize: TextUnit = (if (cjk) (size + 6).coerceAtLeast(12) else size).sp
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = fonts.forLanguage(language),
        fontSize = fontSize,
        lineHeight = (fontSize.value * if (cjk) 1.35f else 1.7f).sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun ShellBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(DexColors.RedBright, DexColors.Red, DexColors.RedDeep),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 8.dp.toPx()
            var y = 0f
            while (y < size.height) {
                var x = 0f
                while (x < size.width) {
                    val bright = ((x * 7 + y * 13).toInt() % 9) == 0
                    drawRect(
                        color = Color.White.copy(alpha = if (bright) 0.10f else 0.025f),
                        topLeft = Offset(x, y),
                        size = Size(1.5f, 1.5f),
                    )
                    x += step
                }
                y += step
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(9.dp)
                .border(4.dp, Color.Black.copy(alpha = 0.35f), RoundedCornerShape(28.dp)),
        )
        content()
    }
}

@Composable
fun Screw(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(12.dp)
            .shadow(1.dp, CircleShape)
            .background(
                Brush.linearGradient(listOf(Color.Gray, Color.Black.copy(alpha = 0.8f))),
                CircleShape,
            )
            .border(0.6.dp, Color.White.copy(alpha = 0.3f), CircleShape),
    )
}

@Composable
fun Indicator(color: Color, lit: Boolean, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(
        modifier
            .size(size)
            .shadow(if (lit) 4.dp else 0.dp, CircleShape, ambientColor = color, spotColor = color)
            .background(
                Brush.radialGradient(
                    if (lit) listOf(Color.White.copy(0.95f), color, color.copy(0.45f))
                    else listOf(color.copy(0.35f), color.copy(0.18f), Color.Black.copy(0.65f)),
                ),
                CircleShape,
            )
            .border(2.dp, Color.Black.copy(0.65f), CircleShape),
    )
}

@Composable
fun Lens(lit: Boolean, pulsing: Boolean = false, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val reduceMotion = remember {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    val glow by animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = tween(durationMillis = if (lit) 160 else 280),
        label = "lens-glow",
    )
    val throb = if (lit && pulsing && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "lens-pulse")
        val level by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(420),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "lens-throb",
        )
        level
    } else {
        1f
    }
    val intensity = glow * throb
    Box(
        modifier
            .size(70.dp)
            .shadow(
                elevation = (16f * intensity).dp,
                shape = CircleShape,
                ambientColor = Color(0xFF7EFFFF),
                spotColor = Color(0xFF08D4FF),
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color.White, Color(0xFF808080), Color(0xFFD9D9D9))), CircleShape)
                .padding(6.dp)
                .background(Color(0xFF222222), CircleShape)
                .padding(4.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            lerp(Color(0xFF266374), Color(0xFFC7FFFF), intensity),
                            lerp(Color(0xFF053B4F), Color(0xFF08D4FF), intensity),
                            lerp(Color(0xFF031A30), Color(0xFF0566A6), intensity),
                        ),
                    ),
                    CircleShape,
                ),
        )
        if (intensity > 0.05f) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 14.dp)
                    .size(width = 22.dp, height = 10.dp)
                    .graphicsLayer { rotationZ = -40f; alpha = 0.35f + 0.55f * intensity }
                    .background(Color.White, CircleShape),
            )
        }
    }
}

@Composable
fun HardwareButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = DexColors.Charcoal,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .offset(y = if (pressed && enabled) 3.dp else 0.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .shadow(if (pressed) 0.dp else 3.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.9f), color)))
            .border(2.dp, Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
fun LcdPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .shadow(1.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(DexColors.Charcoal)
            .border(2.dp, Color.Black.copy(0.65f), RoundedCornerShape(10.dp))
            .padding(7.dp),
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFD5E4B8), DexColors.Lcd, DexColors.LcdDeep))),
        ) {
            content()
            ScanLines()
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Black.copy(0.18f)),
            )
        }
    }
}

@Composable
fun ScanLines(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val step = 3.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawRect(Color.Black.copy(0.07f), topLeft = Offset(0f, y), size = Size(size.width, 1f))
            y += step
        }
    }
}

@Composable
fun RadarGrid(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val step = 18.dp.toPx()
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                drawRect(DexColors.Ink.copy(0.18f), Offset(x, y), Size(1.5f, 1.5f))
                x += step
            }
            y += step
        }
        for (insetDp in listOf(36f, 68f)) {
            val inset = insetDp.dp.toPx()
            if (size.minDimension > inset * 2) {
                drawOval(
                    DexColors.Ink.copy(0.10f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(1.5f),
                )
            }
        }
    }
}

@Composable
fun Reticle(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize().padding(28.dp)) {
        val length = 16.dp.toPx()
        val path = Path()
        val corners = listOf(
            Offset(0f, 0f) to Offset(1f, 1f),
            Offset(size.width, 0f) to Offset(-1f, 1f),
            Offset(0f, size.height) to Offset(1f, -1f),
            Offset(size.width, size.height) to Offset(-1f, -1f),
        )
        corners.forEach { (origin, direction) ->
            path.moveTo(origin.x, origin.y + direction.y * length)
            path.lineTo(origin.x, origin.y)
            path.lineTo(origin.x + direction.x * length, origin.y)
        }
        drawPath(path, DexColors.Ink.copy(0.65f), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun SpeakerGrille(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(6) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black.copy(0.65f)),
            )
        }
    }
}

@Composable
fun HingeBar(vertical: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            if (vertical) {
                Brush.horizontalGradient(
                    listOf(Color(0xFF4A0509), DexColors.Red, Color(0xFFE63B3B), DexColors.Red, Color.Black.copy(0.7f)),
                )
            } else {
                Brush.verticalGradient(
                    listOf(Color(0xFF4A0509), DexColors.Red, Color(0xFFE63B3B), DexColors.Red, Color.Black.copy(0.7f)),
                )
            },
        ),
    )
}

@Composable
fun Wordmark() {
    Text(
        "iDEX",
        color = DexColors.Wordmark,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        fontSize = 28.sp,
    )
}

@Composable
fun ViewfinderBezel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFF8A8A8A), Color(0xFFBFBFBF))),
            )
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(DexColors.Charcoal)
                .padding(8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
                    .background(DexColors.Lcd),
            ) {
                content()
                ScanLines()
            }
        }
        Column(Modifier.fillMaxSize().padding(4.dp)) {
            Row(Modifier.fillMaxSize().weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                Screw()
                Screw()
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Screw()
                Screw()
            }
        }
    }
}
