package cz.vasabi.myiot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.math.round
import kotlin.random.Random

data class Reading(val timestamp: Instant, val value: Float)


@Composable
fun Chart(
    readings: List<Reading>,
    timeMilis: Long? = null,
    color: Color = Color.Green,
    textColor: Color = MaterialTheme.colorScheme.tertiary,
    modifier: Modifier = Modifier
) {
    val nOfValueSteps = 10
    val nOfTimestampsSteps = 10

    // FIXME
    val maxTimestamp = Instant.now().toEpochMilli()

    val reads = if (timeMilis != null) readings.filter {
        it.timestamp.toEpochMilli() > maxTimestamp - timeMilis
    } else readings

    val density = LocalDensity.current
    val textPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize = density.run { 12.sp.toPx() }
            this.color = textColor.toArgb()
        }
    }

    val maxValue = reads.maxByOrNull { it.value }?.value ?: 0f
    val minValue = reads.minByOrNull { it.value }?.value ?: 0f

    val minTimestamp =
        if (timeMilis == null) (reads.minBy { it.timestamp.toEpochMilli() }.timestamp.toEpochMilli()) else Instant.now()
            .toEpochMilli() - timeMilis

    val valueStep = (maxValue - minValue) / nOfValueSteps
    val timeStep = (maxTimestamp - minTimestamp) / nOfTimestampsSteps

    val topOffset = 50f
    val bottomOffset = 50f
    val rightOffset = 50f
    val leftOffset = 100f

    var lastTap: Offset? by remember {
        mutableStateOf(null)
    }
    Canvas(modifier = modifier
        // .background(Color.DarkGray)
        .pointerInput(Unit) {
            detectTapGestures {
                lastTap = it
            }
        }) {
        fun valueToY(value: Float): Float {
            return size.height - ((value - minValue) * ((size.height - (topOffset + bottomOffset)) / (maxValue - minValue)) + topOffset)
        }

        fun valueToX(value: Long): Float {
            return (value - minTimestamp) * ((size.width - (rightOffset + leftOffset)) / (maxTimestamp - minTimestamp)) + leftOffset
        }

        repeat(nOfValueSteps + 1) {
            val value = maxValue - it * valueStep
            // drawContext.canvas.nativeCanvas.drawCircle(leftOffset, valueToY(value), 10f, android.graphics.Paint())
            drawContext.canvas.nativeCanvas.drawText(
                "${round(value)}",
                10f,
                valueToY(value),
                textPaint
            )
        }
        repeat(nOfTimestampsSteps + 1) {
            val value = minTimestamp + (it * timeStep)
            //drawContext.canvas.nativeCanvas.drawCircle(valueToX(value), size.height-bottomOffset, 10f, android.graphics.Paint())
            drawContext.canvas.nativeCanvas.drawText(
                "${
                    round(
                        (Instant.now().toEpochMilli() - value) / 1000f
                    )
                }s", valueToX(value), size.height - 10f, textPaint
            )
        }

        val path = Path()
        reads.firstOrNull()?.let {
            val aX = valueToX(it.timestamp.toEpochMilli())
            val aY = valueToY(it.value)
            path.moveTo(aX, aY)
        }
        reads.zipWithNext { a, b ->
            val aX = valueToX(a.timestamp.toEpochMilli())
            val bX = valueToX(b.timestamp.toEpochMilli())
            val aY = valueToY(a.value)
            val bY = valueToY(b.value)
            drawContext.canvas.nativeCanvas.drawCircle(aX, aY, 5f, android.graphics.Paint())
            drawContext.canvas.nativeCanvas.drawCircle(bX, bY, 5f, android.graphics.Paint())
            // drawContext.canvas.nativeCanvas.drawLine(aX, aY, bX, bY, android.graphics.Paint())
            path.quadraticBezierTo(aX, aY, (aX + bX) / 2, (aY + bY) / 2)
        }

        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Preview
@Composable
fun previewChart() {
    val readings: SnapshotStateList<Reading> = remember {
        mutableStateListOf(Reading(Instant.now(), Random.nextInt(5, 200).toFloat()))
    }

    var step = 5

    LaunchedEffect(null) {
        while (true) {
            readings.add(Reading(Instant.now(), step.toFloat()))
            if (Random.nextBoolean()) {
                if (Random.nextBoolean()) {
                    step += Random.nextInt(1, 5)
                } else {
                    step -= Random.nextInt(1, 5)
                }
            }
            delay(40)
        }
    }
    Chart(readings, 4000)
}