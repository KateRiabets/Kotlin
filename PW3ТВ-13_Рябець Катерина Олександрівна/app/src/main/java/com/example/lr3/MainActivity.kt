package com.example.lr3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lr3.ui.theme.LR3Theme
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.PI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LR3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    EnergyCalculatorApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun EnergyCalculatorApp(modifier: Modifier = Modifier) {
    var dailyPower by remember { mutableStateOf("5") }  // Середньодобова потужність Pc
    var currentStdDev by remember { mutableStateOf("1") } // Поточне sigma1
    var futureStdDev by remember { mutableStateOf("0.25") } // Майбутнє sigma2
    var energyCost by remember { mutableStateOf("7") } // Вартість електроенергії V

    var errorMessage by remember { mutableStateOf("") } // Поле для виведення помилок

    var W1 by remember { mutableStateOf(0.0) }
    var W2 by remember { mutableStateOf(0.0) }
    var profitBefore by remember { mutableStateOf(0.0) }
    var penaltyBefore by remember { mutableStateOf(0.0) }
    var finalProfitBefore by remember { mutableStateOf(0.0) }

    var W3 by remember { mutableStateOf(0.0) }
    var W4 by remember { mutableStateOf(0.0) }
    var profitAfter by remember { mutableStateOf(0.0) }
    var penaltyAfter by remember { mutableStateOf(0.0) }
    var finalProfitAfter by remember { mutableStateOf(0.0) }

    Column(modifier = modifier.padding(16.dp)) {
        TextField(
            value = dailyPower,
            onValueChange = { dailyPower = it },
            label = { Text("Середньодобова потужність (Pc), МВт") },
            modifier = Modifier.fillMaxWidth(),
            isError = dailyPower.isBlank() || dailyPower.toDoubleOrNull() == null
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = currentStdDev,
            onValueChange = { currentStdDev = it },
            label = { Text("Поточне середньоквадратичне відхилення (σ1)") },
            modifier = Modifier.fillMaxWidth(),
            isError = currentStdDev.isBlank() || currentStdDev.toDoubleOrNull() == null
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = futureStdDev,
            onValueChange = { futureStdDev = it },
            label = { Text("Майбутнє середньоквадратичне відхилення (σ2)") },
            modifier = Modifier.fillMaxWidth(),
            isError = futureStdDev.isBlank() || futureStdDev.toDoubleOrNull() == null
        )
        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = energyCost,
            onValueChange = { energyCost = it },
            label = { Text("Вартість електроенергії (V), грн") },
            modifier = Modifier.fillMaxWidth(),
            isError = energyCost.isBlank() || energyCost.toDoubleOrNull() == null
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Button(onClick = {
            // Перевірка, чи поля не порожні та чи введені числа
            if (dailyPower.isBlank() || currentStdDev.isBlank() || futureStdDev.isBlank() || energyCost.isBlank()) {
                errorMessage = "Всі поля мають бути заповнені!"
                return@Button
            }

            // Перетворення введених дани в числа
            val Pc = dailyPower.toDoubleOrNull()
            val sigma1 = currentStdDev.toDoubleOrNull()
            val sigma2 = futureStdDev.toDoubleOrNull()
            val V = energyCost.toDoubleOrNull()

            if (Pc == null ||sigma1 == null || sigma2 == null || V == null) {
                errorMessage = "Будь ласка, введіть правильні числові значення!"
                return@Button
            }

            errorMessage = "" // Очищуємо повідомлення про помилки

            val P_lower = Pc - sigma2 // Нижня межа
            val P_upper = Pc + sigma2 // Верхня межа

            // Розрахунки до вдосконалення системи
            val deltaW1 = integrateNormalDistribution(Pc, sigma1, P_lower, P_upper) // Інтегрування
            W1 = Pc * 24 * deltaW1 // Енергія без небалансів
            profitBefore = W1 * V // Прибуток від  енергії
            W2 = Pc * 24 * (1 - deltaW1) // Енергія з небалансами
            penaltyBefore = W2 * V // Штраф за небаланси
            finalProfitBefore = profitBefore - penaltyBefore // Загальний прибуток до вдосконалення

            // Розрахунки після вдосконалення системи
            val deltaW2 = integrateNormalDistribution(Pc, sigma2, P_lower, P_upper) // Інтегрування
            W3 = Pc * 24 * deltaW2 // Енергія без небалансів
            profitAfter = W3 * V // Прибуток від  енергії
            W4 = Pc * 24 * (1 - deltaW2) // Енергія з небалансами
            penaltyAfter = W4 * V // Штраф за небаланси
            finalProfitAfter = profitAfter - penaltyAfter // Загальний прибуток після вдосконалення
        }) {
            Text("Розрахувати")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Виведення результатів до вдосконалення системи
        Text("До вдосконалення системи:")
        Text("Частка енергії без небалансів: ${"%.2f".format(W1)} МВт·год")
        Text("Прибуток: ${"%.2f".format(profitBefore)} тис. грн")
        Text("Штраф: ${"%.2f".format(penaltyBefore)} тис. грн")
        Text("Загальний прибуток: ${"%.2f".format(finalProfitBefore)} тис. грн")
        Spacer(modifier = Modifier.height(8.dp))

        // Виведення результатів після вдосконалення системи
        Text("Після вдосконалення системи:")
        Text("Частка енергії без небалансів: ${"%.2f".format(W3)} МВт·год")
        Text("Прибуток: ${"%.2f".format(profitAfter)} тис. грн")
        Text("Штраф: ${"%.2f".format(penaltyAfter)} тис. грн")
        Text("Загальний прибуток: ${"%.2f".format(finalProfitAfter)} тис. грн")
    }
}

// Чисельне інтегрування
fun integrateNormalDistribution(Pc: Double, stdDev: Double, P_lower: Double, P_upper: Double): Double {
    val n = 1000 // Кількість кроків для інтегрування
    val step = (P_upper - P_lower) / n
    var area = 0.0

    for (i in 0 until n) {
        val x1 = P_lower + i * step
        val x2 = P_lower + (i + 1) * step
        val y1 = normalDistribution(x1, Pc, stdDev)
        val y2 = normalDistribution(x2, Pc, stdDev)
        area += 0.5 * (y1 + y2) * step // Метод трапецій
    }

    return area
}

// Функція нормального розподілу
fun normalDistribution(p: Double, Pc: Double, stdDev: Double): Double {
    return (1 / (stdDev * sqrt(2 * PI))) * exp(-((p - Pc).pow(2)) / (2 * stdDev.pow(2)))
}

@Preview(showBackground = true)
@Composable
fun EnergyCalculatorAppPreview() {
    LR3Theme {
        EnergyCalculatorApp()
    }
}
