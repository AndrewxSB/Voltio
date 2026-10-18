package com.voltio.app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

private val Charge = Color(0xFF2EE6A6)
private val Drain = Color(0xFFFF8A3D)
private val Muted = Color(0xFF8A94A8)

data class Reading(
    val mA: Int, val volt: Float, val tempC: Float, val level: Int,
    val status: Int, val plugged: Int, val health: Int, val mAh: Int,
) {
    val charging get() = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val watts get() = volt * abs(mA) / 1000f
}

fun readBattery(ctx: Context): Reading {
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val i: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    // Algunos móviles devuelven µA y otros mA
    val mag = if (abs(raw) > 20000) abs(raw) / 1000 else abs(raw)
    val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
    val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    return Reading(
        mA = if (charging) mag else -mag,
        volt = (i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f,
        tempC = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f,
        level = level * 100 / scale.coerceAtLeast(1),
        status = status,
        plugged = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0,
        health = i?.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) ?: 0,
        mAh = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000,
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var r by remember { mutableStateOf(readBattery(ctx)) }
    val history = remember { mutableStateListOf<Int>() }

    LaunchedEffect(Unit) {
        while (true) {
            r = readBattery(ctx)
            history.add(r.mA)
            if (history.size > 90) history.removeAt(0)
            delay(1000)
        }
    }

    val accent = if (r.charging) Charge else Drain
    val stateText = when (r.status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Cargando"
        BatteryManager.BATTERY_STATUS_FULL -> "Completa"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descargando"
        else -> "Sin cargar"
    }
    val source = when {
        r.plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> "Inalámbrica"
        r.plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> "Cargador"
        r.plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> "USB"
        else -> "Batería"
    }
    val health = when (r.health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Buena"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Sobrecalentada"
        BatteryManager.BATTERY_HEALTH_COLD -> "Fría"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobrevoltaje"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dañada"
        else -> "Desconocida"
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF05080F), Color(0xFF0D1426), Color(0xFF05080F)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("VOLTIO", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)

            Gauge(r.mA, accent, stateText, source)

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Glass(Modifier.weight(1f)) { Stat("Voltaje", "%.2f".format(r.volt), "V") }
                Glass(Modifier.weight(1f)) { Stat("Potencia", "%.1f".format(r.watts), "W") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Glass(Modifier.weight(1f)) { Stat("Temperatura", "%.1f".format(r.tempC), "°C") }
                Glass(Modifier.weight(1f)) { Stat("Capacidad", "${r.mAh}", "mAh") }
            }

            Glass(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Nivel", color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("${r.level}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Color(0x1FFFFFFF))) {
                    Box(
                        Modifier.fillMaxWidth(r.level / 100f).fillMaxHeight().clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = .5f), accent)))
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("Salud: $health", color = Muted, fontSize = 13.sp)
            }

            Glass(Modifier.fillMaxWidth()) {
                Text("Corriente en vivo", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Chart(history, accent)
                if (history.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("Media", history.average().toInt())
                        MiniStat("Mín", history.min())
                        MiniStat("Máx", history.max())
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun Gauge(mA: Int, color: Color, state: String, source: String) {
    val frac by animateFloatAsState((abs(mA) / 5000f).coerceIn(0f, 1f), tween(700), label = "gauge")
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 24.dp.toPx()
            val d = size.minDimension - sw
            val tl = Offset(sw / 2, sw / 2)
            val sz = Size(d, d)
            drawArc(Color(0x1FFFFFFF), 135f, 270f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            if (frac > 0.005f) {
                drawArc(color.copy(alpha = .22f), 135f, 270f * frac, false, tl, sz,
                    style = Stroke(sw * 1.7f, cap = StrokeCap.Round))
                drawArc(color, 135f, 270f * frac, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                (if (mA > 0) "+" else if (mA < 0) "-" else "") + abs(mA),
                color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Bold,
            )
            Text("mA", color = Muted, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(state, color = color, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(source, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
fun Glass(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.clip(shape).background(Color(0x14FFFFFF)).border(1.dp, Color(0x1AFFFFFF), shape).padding(18.dp),
        content = content,
    )
}

@Composable
fun Stat(label: String, value: String, unit: String) {
    Text(label, color = Muted, fontSize = 13.sp)
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Text(unit, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
fun MiniStat(label: String, v: Int) {
    Column {
        Text(label, color = Muted, fontSize = 12.sp)
        Text("$v mA", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Chart(data: List<Int>, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        if (data.size < 2) return@Canvas
        var lo = data.min().toFloat()
        var hi = data.max().toFloat()
        if (hi - lo < 100f) { val m = (hi + lo) / 2; lo = m - 50f; hi = m + 50f }
        val w = size.width
        val h = size.height
        val line = Path()
        data.forEachIndexed { i, v ->
            val x = i / (data.size - 1f) * w
            val y = h - (v - lo) / (hi - lo) * h
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(line); lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = .35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
