package com.example.lr6

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lr6.ui.theme.LR6Theme
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LR6Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ElectricPowerCalculator()
                }
            }
        }
    }
}
val coefficientTable = listOf(
    listOf(8.00, 5.33, 4.00, 2.67, 2.00, 1.60, 1.33, 1.14, 1.00),
    listOf(6.22, 4.33, 3.06, 2.45, 1.98, 1.60, 1.33, 1.14, 1.00),
    listOf(4.66, 2.89, 2.31, 1.74, 1.45, 1.34, 1.22, 1.14, 1.00),
    listOf(3.24, 2.35, 1.91, 1.47, 1.25, 1.21, 1.12, 1.06, 1.00),
    listOf(2.84, 2.09, 1.72, 1.35, 1.16, 1.16, 1.08, 1.03, 1.00),
    listOf(2.64, 1.96, 1.62, 1.28, 1.14, 1.13, 1.06, 1.01, 1.00),
    listOf(2.49, 1.86, 1.54, 1.23, 1.12, 1.10, 1.04, 1.00, 1.00),
    listOf(2.37, 1.78, 1.48, 1.19, 1.10, 1.08, 1.02, 1.00, 1.00),
    listOf(2.27, 1.71, 1.43, 1.16, 1.09, 1.07, 1.01, 1.00, 1.00),
    listOf(2.18, 1.65, 1.39, 1.13, 1.07, 1.05, 1.00, 1.00, 1.00),
    listOf(2.04, 1.56, 1.32, 1.08, 1.05, 1.03, 1.00, 1.00, 1.00),
    listOf(1.94, 1.49, 1.27, 1.05, 1.02, 1.00, 1.00, 1.00, 1.00),
    listOf(1.85, 1.43, 1.23, 1.02, 1.00, 1.00, 1.00, 1.00, 1.00),
    listOf(1.78, 1.39, 1.19, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    listOf(1.72, 1.35, 1.16, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    listOf(1.60, 1.27, 1.10, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    listOf(1.51, 1.21, 1.05, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    listOf(1.44, 1.16, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    listOf(1.40, 1.13, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    listOf(1.30, 1.07, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    listOf(1.25, 1.03, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00),
    listOf(1.16, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00)
)

val rowHeaders = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 25) //  n_e
val colHeaders = listOf(0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8)//k_v

val secondTable = listOf(
    listOf(8.00, 5.33, 4.00, 2.67, 2.00, 1.60, 1.33, 1.14, 1.14),
    listOf(5.01, 3.44, 2.69, 1.90, 1.52, 1.24, 1.11, 1.00, 1.00),
    listOf(2.40, 2.17, 1.80, 1.42, 1.23, 1.14, 1.08, 1.00, 1.00),
    listOf(2.28, 1.73, 1.46, 1.19, 1.06, 1.04, 0.97, 0.94, 0.94),
    listOf(1.31, 1.20, 1.00, 0.96, 0.95, 0.94, 0.93, 0.91, 0.91),
    listOf(1.10, 0.97, 0.91, 0.91, 0.90, 0.90, 0.90, 0.90, 0.90),
    listOf(0.80, 0.80, 0.80, 0.85, 0.85, 0.85, 0.85, 0.85, 0.85),
    listOf(0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.85, 0.85, 0.85),
    listOf(0.65, 0.65, 0.65, 0.70, 0.70, 0.70, 0.75, 0.80, 0.80) // Для n_e > 50
)

val secondRowHeaders = listOf(1, 2, 3, 4, 5, 6, 9, 10, 50) // Останнє для  n_e > 50
val secondColHeaders = listOf(0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8) // Останнє для  k_v >= 0.7


fun findInTable(     n_e: Int,                     // Ефективна кількість ЕП
                     k_v: Double,                  // Груповий коефіцієнт використання
                     table: List<List<Double>>,    // Таблиця значень
                     rowHeaders: List<Int>,        // Заголовки рядків (значення n_e)
                     colHeaders: List<Double>      // Заголовки стовпців (значення k_v)
): Double {
// Обробка випадку, якщо n_e більше за максимальне значення у rowHeaders
    val rowIndex = if (n_e > rowHeaders.last()) {
        rowHeaders.size - 1  // Використовуємо останній рядок таблиці
    } else {
        rowHeaders.indexOfFirst { it >= n_e }  // Знаходимо перший рядок, де значення >= n_e
    }
    val rowPrevIndex = if (rowIndex > 0) rowIndex - 1 else rowIndex  // Попередній рядок

    // Обробка випадку, якщо k_v більше або дорівнює максимальному значенню у colHeaders
    val colIndex = if (k_v >= colHeaders.last()) {
        colHeaders.size - 1 // Використовуємо останній стовпець таблиці
    } else {
        colHeaders.indexOfFirst { it >= k_v }  // Знаходимо перший стовпець, де значення >= k_v
    }
    val colPrevIndex = if (colIndex > 0) colIndex - 1 else colIndex // Попередній стовпець

    // Перевірка, чи є точний збіг у таблиці
    if (n_e <= rowHeaders.last() && rowHeaders[rowIndex] == n_e && k_v <= colHeaders.last() && colHeaders[colIndex] == k_v) {
        return table[rowIndex][colIndex] // Якщо знайдено точне значення, повертаємо його
    }
    val rowLower = rowHeaders[rowPrevIndex]// Нижня межа по n_e
    val rowUpper = rowHeaders[rowIndex]// Верхня межа по n_e
    val rowFraction = if (rowUpper != rowLower) (n_e - rowLower).toDouble() / (rowUpper - rowLower) else 0.0
    val colLower = colHeaders[colPrevIndex]// Нижня межа по k_v
    val colUpper = colHeaders[colIndex]// Верхня межа по k_v
    val colFraction = if (colUpper != colLower) (k_v - colLower) / (colUpper - colLower) else 0.0

    // Отримання значення з таблиці для 4-х сусідніх точок
    val valueLowerLower = table[rowPrevIndex][colPrevIndex]
    val valueLowerUpper = table[rowPrevIndex][colIndex]
    val valueUpperLower = table[rowIndex][colPrevIndex]
    val valueUpperUpper = table[rowIndex][colIndex]


    val interpolatedValue =
        valueLowerLower * (1 - rowFraction) * (1 - colFraction) +
                valueLowerUpper * (1 - rowFraction) * colFraction +
                valueUpperLower * rowFraction * (1 - colFraction) +
                valueUpperUpper * rowFraction * colFraction

    return interpolatedValue
}




@Composable
fun ElectricPowerCalculator() {
    val equipmentList = listOf(
        "Шліфувальний верстат", "Свердлильний верстат", "Фигувальний верстат",
        "Циркулярна пила", "Прес", "Полірувальнйи верстат", "Фрезерний верстат",
        "Вентилятор", "Зварювальний трансформатор", "Сушильна шафа"
    )
    val defaultValues = mapOf(
        "Шліфувальний верстат" to EquipmentParams(
            eta = "0.92", cosPhi = "0.9", uH = "0.38", n = "4", pH = "20", kv = "0.15", tgPhi = "1.33"
        ),
        "Свердлильний верстат" to EquipmentParams(
            eta = "0.92", cosPhi = "0.9", uH = "0.38", n = "2", pH = "14", kv = "0.12", tgPhi = "1.00"
        ),
        "Фигувальний верстат" to EquipmentParams(
            eta = "0.92", cosPhi = "0.9", uH = "0.38", n = "4", pH = "42", kv = "0.15", tgPhi = "1.33"
        ),
        "Циркулярна пила" to EquipmentParams(
            eta = "0.92", cosPhi = "0.9", uH = "0.38", n = "1", pH = "36", kv = "0.3", tgPhi = "1.52"
        ),
        "Прес" to EquipmentParams(
            eta = "0.92", cosPhi = "0.9", uH = "0.38", n = "1", pH = "20", kv = "0.5", tgPhi = "0.75"
        ),
        "Полірувальнйи верстат" to EquipmentParams(
            eta = "0.92", cosPhi = "0.9", uH = "0.38", n = "1", pH = "40", kv = "0.2", tgPhi = "1.00"
        ),
        "Фрезерний верстат" to EquipmentParams(
            eta = "0.92", cosPhi = "0.9", uH = "0.38", n = "2", pH = "32", kv = "0.2", tgPhi = "1.00"
        ),
        "Вентилятор" to EquipmentParams(
            eta = "0.92", cosPhi = "0.9", uH = "0.38", n = "1", pH = "20", kv = "0.65", tgPhi = "0.75"
        ),
        "Зварювальний трансформатор" to EquipmentParams(
            eta = "0.92", cosPhi = "0.9", uH = "0.38", n = "2", pH = "100", kv = "0.2", tgPhi = "3.00"
        ),
        "Сушильна шафа" to EquipmentParams(
            eta = "0.92", cosPhi = "1.0", uH = "0.38", n = "2", pH = "120", kv = "0.8", tgPhi = "0"
        )
    )

    val fields = remember {
        equipmentList.associateWith { mutableStateOf(defaultValues[it] ?: EquipmentParams()) }
    }

    val workshopParams = remember {
        mutableStateOf(
            WorkshopParams(
                n = "81",
                totalPower = "2330",
                weightedPower = "752",
                weightedPowerTg = "657",
                squaredPower = "96399"
            )
        )
    }

    val scrollState = rememberScrollState()
    var results by remember { mutableStateOf<Map<String, CalculatedResults>>(emptyMap()) }
    val equipmentResults = remember { mutableMapOf<String, EquipmentResults>() }
    var groupKv by remember { mutableStateOf(0.0) }
    var n_e by remember { mutableStateOf(0.0) }
    var k_r by remember { mutableStateOf(0.0) }
    var P_p by remember { mutableStateOf(0.0) }
    var Q_p by remember { mutableStateOf(0.0) }
    var S_p by remember { mutableStateOf(0.0) }
    var I_p by remember { mutableStateOf(0.0) }

    var groupKv_Workshop by remember { mutableStateOf(0.0) }
    var n_e_Workshop by remember { mutableStateOf(0.0) }
    var k_r_Workshop by remember { mutableStateOf(0.0) }
    var P_p_Workshop by remember { mutableStateOf(0.0) }
    var Q_p_Workshop by remember { mutableStateOf(0.0) }
    var S_p_Workshop by remember { mutableStateOf(0.0) }
    var I_p_Workshop by remember { mutableStateOf(0.0) }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Введіть параметри для кожного ЕП",
            style = MaterialTheme.typography.titleLarge
        )

        // Поля введення параметрів для кажного ЕП
        fields.forEach { (equipmentName, paramsState) ->
            EquipmentForm(
                equipmentName = equipmentName,
                params = paramsState
            )
        }


//"ВЕСЬ ЦЕХ"
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ВЕСЬ ЦЕХ",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = workshopParams.value.n,
            onValueChange = { workshopParams.value = workshopParams.value.copy(n = it) },
            label = { Text("Кількість ЕП (n), шт") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = workshopParams.value.totalPower,
            onValueChange = { workshopParams.value = workshopParams.value.copy(totalPower = it) },
            label = { Text("n * Pн (кВт)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = workshopParams.value.weightedPower,
            onValueChange = { workshopParams.value = workshopParams.value.copy(weightedPower = it) },
            label = { Text("n * Pн * kв (кВт)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = workshopParams.value.weightedPowerTg,
            onValueChange = { workshopParams.value = workshopParams.value.copy(weightedPowerTg = it) },
            label = { Text("n * Pн * kв * tgPhi (кВт)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = workshopParams.value.squaredPower,
            onValueChange = { workshopParams.value = workshopParams.value.copy(squaredPower = it) },
            label = { Text("n * Pн^2 (кВт²)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Очистка результатів
            equipmentResults.clear()

            // Змінні для ∑
            var sumTotalPower = 0.0 // ∑ n * P_H
            var sumWeightedPower = 0.0 // ∑ n * P_H * k_v
            var sumSquaredPower = 0.0 // ∑ n * P_H^2
            var sumWeightedPowerTg = 0.0// ∑(n * P_H * k_v*tgPhi)
            //Розрахунки для кожного ЕП
            fields.forEach { (equipmentName, paramsState) ->
                val params = paramsState.value
                // Отримання параметрів
                val n = params.n.toDoubleOrNull() ?: 0.0
                val pH = params.pH.toDoubleOrNull() ?: 0.0
                val kv = params.kv.toDoubleOrNull() ?: 0.0
                val uH = params.uH.toDoubleOrNull() ?: 1.0
                val cosPhi = params.cosPhi.toDoubleOrNull() ?: 1.0
                val tgPhi = params.tgPhi.toDoubleOrNull() ?: 1.0
                val eta = params.eta.toDoubleOrNull() ?: 1.0
                // Розрахунок n * P_H
                val totalPower = calculateTotalPower(n, pH)
                // Розрахунок n * P_H * k_v
                val weightedPower = calculateWeightedPower(n, pH, kv)
                // Розрахунок I_p
                val current = calculateCurrent(totalPower, uH, cosPhi, eta)
                // Розрахунок n * P_H^2
                val squaredPower = calculateSquaredPower(n, pH)
                val weightedPowerTg = calculateWeightedPowerTg(n, pH,kv,tgPhi)
                // Розрахунок сум
                if (equipmentName != "Зварювальний трансформатор" && equipmentName != "Сушильна шафа") {
                    sumTotalPower += totalPower
                    sumWeightedPower += weightedPower
                    sumSquaredPower += squaredPower
                    sumWeightedPowerTg+=weightedPowerTg
                }
                // Збереження результатів для кожного обладнання
                equipmentResults[equipmentName] = EquipmentResults(
                    totalPower = totalPower,
                    current = current,
                    weightedPower = weightedPower,
                    squaredPower = squaredPower,
                    weightedPowerTg=weightedPowerTg
                )
            }

            // 4.1 Груповий коефіцієнт використання
            groupKv = if (sumTotalPower > 0) sumWeightedPower / sumTotalPower else 0.0

            // 4.2 Ефективна кількість ЕП:
            n_e = if (sumSquaredPower > 0) {
                (sumTotalPower * sumTotalPower) / sumSquaredPower
            } else {
                0.0
            }

            //4.3
            val rounded_n_e = n_e.toInt() // Округлення n_e для пошуку по таблиці
             k_r = findInTable(rounded_n_e, (groupKv * 10).roundToInt() / 10.0,coefficientTable, rowHeaders, colHeaders)

            //4.4
            P_p=calculateActive(k_r,sumWeightedPower)

            //4.5
            Q_p=calculateReactive(1.00, sumWeightedPowerTg)

            //4.6
            S_p= calculateFullPower(P_p,Q_p)

            //4.7
            I_p=calculateGrupStrum(P_p,0.38)


            // "ВЕСЬ ЦЕХ"
            val workshopValues = workshopParams.value
            val workshopTotalPower = workshopValues.totalPower.toDoubleOrNull() ?: 0.0 // ∑(n * P_H)
            val workshopWeightedPower = workshopValues.weightedPower.toDoubleOrNull() ?: 0.0 // ∑(n * P_H * k_v)
            val workshopSquaredPower = workshopValues.squaredPower.toDoubleOrNull() ?: 0.0 // ∑(n * P_H^2)
            val weightedPowerTg = workshopValues.weightedPowerTg.toDoubleOrNull() ?: 0.0 // ∑(n * P_H * k_v*tgPhi)
            //6.1
            groupKv_Workshop=workshopWeightedPower/workshopTotalPower

            //6.2
            n_e_Workshop=workshopTotalPower.pow(2.0)/workshopSquaredPower

            val rounded_n_e_Workshop = n_e_Workshop.toInt()
            //6.3
            k_r_Workshop = findInTable(rounded_n_e_Workshop, (groupKv_Workshop * 10).roundToInt() / 10.0,
                secondTable, secondRowHeaders, secondColHeaders)

            //6.4
            P_p_Workshop=calculateActive((k_r_Workshop * 10).roundToInt() / 10.0,workshopWeightedPower)

            //6.5
            Q_p_Workshop=calculateReactive((k_r_Workshop * 10).roundToInt() / 10.0,weightedPowerTg)

            //6.6
            S_p_Workshop= calculateFullPower(P_p_Workshop,Q_p_Workshop)

            //6.7
            I_p_Workshop=calculateGrupStrum(P_p_Workshop,0.38)

            // Конвертація в формат для виведення
            results = equipmentResults.mapValues { (_, result) ->
                CalculatedResults(
                    totalPower = String.format("%.2f", result.totalPower),
                    current = String.format("%.2f", result.current),
                    weightedPower = String.format("%.2f", result.weightedPower),
                    squaredPower = String.format("%.2f", result.squaredPower),
                    weightedPowerTg = String.format("%.2f", result.weightedPowerTg)

                )
            }
        }) {
            Text("Розрахувати")
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Отображение результатов расчётов
        results.forEach { (equipmentName, calculations) ->
            Text(text = "$equipmentName:", style = MaterialTheme.typography.titleMedium)
            Text(text = "n * Pн = ${calculations.totalPower} кВт")
            Text(text = "n * Pн * kв = ${calculations.weightedPower} кВт")
            Text(text = "n * Pн * kв * tgPhi = ${calculations.weightedPowerTg} кВт")
            Text(text = "Ip = ${calculations.current} А")
            Spacer(modifier = Modifier.height(8.dp))
        }

// Отображение группового коэффициента
        Text(
            text = "Груповий коефіцієнт використання: ${String.format("%.4f", groupKv)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Ефективна кількіст: ${String.format("%.4f", n_e)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Розрахунковий коефіцієнт активної потужності: ${String.format("%.4f", k_r)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Розрахункове активне навантаження: ${String.format("%.4f", P_p)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Розрахункове реактивне навантаження: ${String.format("%.4f", Q_p)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Повна потужність: ${String.format("%.4f", S_p)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Розрахунковий груповий струм: ${String.format("%.4f", I_p)}",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Коефіцієнт використання цеху в цілому: ${String.format("%.4f", groupKv_Workshop)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Ефективна кількість ЕП цеху в цілому: ${String.format("%.4f", n_e_Workshop)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Розрахунковий коефіцієнт активної потужності: ${String.format("%.1f", k_r_Workshop)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Розрахункове активне навантаження на шинах 0,38 кВ: ${String.format("%.1f", P_p_Workshop)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Розрахункове реактивне навантаження на шинах 0,38 кВ ${String.format("%.1f", Q_p_Workshop)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Повна потужність на шинах 0,38 кВ: ${String.format("%.1f", S_p_Workshop)}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Розрахунковий груповий струм на шинах 0,38 кВ: ${String.format("%.2f", I_p_Workshop)}",
            style = MaterialTheme.typography.titleLarge
        )

    }
}
//3.1
fun calculateTotalPower(n: Double, pH: Double): Double {
    return n * pH
}
//3.2
fun calculateCurrent(totalPower: Double, uH: Double, cosPhi: Double, eta: Double): Double {
    return if (uH > 0 && cosPhi > 0 && eta > 0) {
        totalPower / (sqrt(3.0) * uH * cosPhi * eta)
    } else {
        0.0
    }
}
fun calculateWeightedPower(n: Double, pH: Double, kv: Double): Double {
    return n * pH * kv
}

//4.2
fun calculateSquaredPower(n: Double, pH: Double): Double {
    return n * pH * pH
}

//4.4
fun calculateActive(k_r:Double,sumWeightedPower: Double): Double {
    return k_r*sumWeightedPower
}

//4.5
fun calculateWeightedPowerTg(n: Double, pH: Double, kv: Double, tgPhi: Double): Double {
    return n * pH * kv* tgPhi
}
fun calculateReactive(ser_koef:Double, SumWeightedPowerTg: Double): Double {
    return ser_koef*SumWeightedPowerTg
}
//4.6
fun calculateFullPower(pp: Double, qp: Double): Double {
    return sqrt(pp * pp + qp * qp)
}

//4.7
fun calculateGrupStrum(P_p: Double, U_h: Double): Double {
    return P_p/U_h
}

data class EquipmentResults(
    val totalPower: Double = 0.0, // n * P_H
    val current: Double = 0.0, // I_p
    val weightedPower: Double = 0.0, // n * P_H * k_v
    val squaredPower: Double = 0.0, //// n * P_H^2
    val weightedPowerTg: Double = 0.0  //n * P_H * k_v *tgPhi


)

data class CalculatedResults(
    val totalPower: String = "", // n * P_H
    val current: String = "", // I_p
    val weightedPower: String = "", // n * P_H * k_v
    val squaredPower:String = "",  // n * P_H^2
    val weightedPowerTg: String = ""  //n * P_H * k_v *tgPhi

)

data class WorkshopParams(
    val n: String = "",
    val totalPower: String = "",
    val weightedPower: String = "",
    val weightedPowerTg: String = "",
    val squaredPower: String = ""
)



@Composable
fun EquipmentForm(equipmentName: String, params: MutableState<EquipmentParams>) {
    val paramValues = params.value
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = equipmentName, style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = paramValues.eta,
            onValueChange = { params.value = paramValues.copy(eta = it) },
            label = { Text("Номінальне значення коефіцієнта корисної дії (ηн)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paramValues.cosPhi,
            onValueChange = { params.value = paramValues.copy(cosPhi = it) },
            label = { Text("Коефіцієнт потужності навантаження (cos φ)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paramValues.uH,
            onValueChange = { params.value = paramValues.copy(uH = it) },
            label = { Text("Напруга навантаження (Uн) в кВ") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paramValues.n,
            onValueChange = { params.value = paramValues.copy(n = it) },
            label = { Text("Кількість ЕП (n) шт") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paramValues.pH,
            onValueChange = { params.value = paramValues.copy(pH = it) },
            label = { Text("Номінальна потужність (Pн) в кВт") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paramValues.kv,
            onValueChange = { params.value = paramValues.copy(kv = it) },
            label = { Text("Коефіцієнт використання (КВ)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = paramValues.tgPhi,
            onValueChange = { params.value = paramValues.copy(tgPhi = it) },
            label = { Text("Коефіцієнт реактивної потужності (tg φ)") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Дані для кожного ЕП
data class EquipmentParams(
    val eta: String = "",
    val cosPhi: String = "",
    val uH: String = "",
    val n: String = "",
    val pH: String = "",
    val kv: String = "",
    val tgPhi: String = ""
)




@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    LR6Theme {
        ElectricPowerCalculator()
    }
}
