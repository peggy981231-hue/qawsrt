package com.example.mynewgyroquiz

import android.content.Context
import android.hardware.*
import android.os.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import kotlin.math.absoluteValue

// ==============================
// 題目資料結構
// ==============================
data class Question(
    val title: String,
    val options: List<String>,
    val answer: Int
)

// ==============================
// 主畫面
// ==============================
@Composable
fun GameScreen() {

    // ===== 畫面狀態 =====
    var screen by rememberSaveable { mutableStateOf(0) } // 0開始 / 1遊戲 / 2結果
    var index by rememberSaveable { mutableStateOf(0) }
    var correctCount by rememberSaveable { mutableStateOf(0) }

    // ===== 游標控制 =====
    var cursor by remember { mutableStateOf(Offset.Zero) }
    var base by remember { mutableStateOf(Offset.Zero) }
    var last by remember { mutableStateOf(Offset.Zero) }
    var screenSize by remember { mutableStateOf(Offset.Zero) }

    // ===== Hover 判定 =====
    var hoverIndex by rememberSaveable { mutableStateOf(-1) }
    var hoverTime by rememberSaveable { mutableStateOf(0L) }

    // ===== 答題狀態 =====
    var selectedIndex by rememberSaveable { mutableStateOf(-1) }
    var showResult by rememberSaveable { mutableStateOf(false) }
    var lock by rememberSaveable { mutableStateOf(false) }

    // ===== 題庫 =====
    val questions = listOf(
        Question("人體最大的器官是？", listOf("心臟", "肝臟", "皮膚", "肺"), 2),
        Question("人體最硬的部位是？", listOf("指甲", "牙齒琺瑯質", "骨頭", "頭髮"), 1),
        Question("章魚有幾顆心臟？", listOf("1", "2", "3", "4"), 2),
        Question("世界上跑最快的陸地動物是？", listOf("獵豹", "獅子", "馬", "袋鼠"), 0)
    )

    // ===== 系統工具 =====
    val context = LocalContext.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val handler = remember { Handler(Looper.getMainLooper()) }

    // ===== 工具函數 =====
    fun resetSelect() {
        hoverIndex = -1
        hoverTime = 0L
        selectedIndex = -1
        showResult = false
        lock = false
    }

    fun resetCursor() {
        cursor = Offset.Zero
    }

    // ==============================
    // 感測器控制（傾斜）
    // ==============================
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {

                last = Offset(e.values[0], e.values[1])

                if (showResult || lock) return

                val dx = -(e.values[0] - base.x) * 10
                val dy = (e.values[1] - base.y) * 10

                cursor = Offset(
                    (cursor.x + dx).coerceIn(-screenSize.x / 3, screenSize.x / 3),
                    (cursor.y + dy).coerceIn(-screenSize.y / 3, screenSize.y / 3)
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // ==============================
    // UI 主容器
    // ==============================
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged {
                screenSize = Offset(it.width.toFloat(), it.height.toFloat())
            }
    ) {

        // ==============================
        // 0️⃣ 開始畫面
        // ==============================
        if (screen == 0) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("作者 : 沛芸", color = Color.White)
                Text("手機傾斜答題遊戲", color = Color.White)
                Text("題數：${questions.size} / 總分：100", color = Color.Red)
                Text("Gyro Quiz", color = Color.White, fontSize = 28.sp)

                Spacer(Modifier.height(20.dp))

                Button(onClick = {
                    correctCount = 0
                    index = 0
                    resetCursor()
                    resetSelect()
                    screen = 1
                }) {
                    Text("開始")
                }
            }
        }

        // ==============================
        // 1️⃣ 遊戲畫面
        // ==============================
        if (screen == 1) {

            val q = questions[index]

            val distance = 110.dp
            val size = 140.dp

            val positions = listOf(
                Offset(-1f, -1f),
                Offset(1f, -1f),
                Offset(-1f, 1f),
                Offset(1f, 1f)
            )

            // 題目
            Text(
                q.title,
                color = Color.White,
                fontSize = 28.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
            )

            // ===== Hover 判定 =====
            val newHover = positions.indexOfFirst {
                (cursor.x - it.x * distance.value * 3).absoluteValue < size.value * 1.5 &&
                        (cursor.y - it.y * distance.value * 3).absoluteValue < size.value * 1.5
            }

            if (newHover != hoverIndex) {
                hoverIndex = newHover
                hoverTime = System.currentTimeMillis()
            }

            // ===== 停留時間 =====
            val progress = if (hoverIndex != -1)
                ((System.currentTimeMillis() - hoverTime) / 1000f).coerceIn(0f, 1f)
            else 0f

            // ===== 自動選擇 =====
            if (progress >= 1f && !showResult && !lock) {

                selectedIndex = hoverIndex
                showResult = true

                if (selectedIndex == q.answer) {
                    correctCount++
                } else {
                    lock = true
                }

                handler.postDelayed({
                    resetCursor()
                    resetSelect()

                    if (index < questions.lastIndex) index++
                    else screen = 2

                }, if (selectedIndex == q.answer) 1000 else 2500)
            }

            // ===== 選項 =====
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                for (i in 0..3) {

                    val color = when {
                        showResult && i == q.answer -> Color.Green
                        showResult && i == selectedIndex -> Color.Red
                        !showResult && hoverIndex == i -> Color.Cyan
                        else -> Color.Blue
                    }

                    Box(
                        Modifier
                            .offset(
                                if (positions[i].x < 0) -distance else distance,
                                if (positions[i].y < 0) -distance else distance
                            )
                            .size(size)
                            .background(color, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(q.options[i], color = Color.White)
                    }
                }
            }

            // ===== 游標 =====
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(cursor.x.toInt(), cursor.y.toInt()) }
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.Green,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        style = Stroke(4.dp.toPx())
                    )
                }

                Box(
                    Modifier
                        .size(24.dp)
                        .background(Color.Yellow, CircleShape)
                )
            }

            // ===== 答題結果 =====
            if (showResult) {
                Text(
                    if (selectedIndex == q.answer) "答對！" else "答錯！",
                    color = if (selectedIndex == q.answer) Color.Green else Color.Red,
                    fontSize = 28.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ===== 校正 =====
            Button(
                onClick = {
                    base = last
                    resetCursor()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            ) {
                Text("校正中心")
            }
        }

        // ==============================
        // 2️⃣ 結果畫面
        // ==============================
        if (screen == 2) {

            val score = (correctCount.toFloat() / questions.size * 100).toInt()

            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("分數：$score", color = Color.White, fontSize = 30.sp)
                Text("答對：$correctCount / ${questions.size}", color = Color.Gray)

                Spacer(Modifier.height(20.dp))

                Button(onClick = { screen = 0 }) {
                    Text("再玩一次")
                }
            }
        }
    }
}