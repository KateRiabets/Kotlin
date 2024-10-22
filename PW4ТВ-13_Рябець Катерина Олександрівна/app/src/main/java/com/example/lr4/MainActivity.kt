package com.example.lr4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.lr4.ui.theme.LR4Theme
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.InputStream
import kotlin.math.sqrt
import kotlin.math.pow
import android.util.Log
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LR4Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val cableData = loadCableData("pue.json")
                    val economicDensityData = loadEconomicDensityData("economic_density.json")
                    TaskSwitcherScreen(
                        modifier = Modifier.padding(innerPadding),
                        cableData = cableData,
                        economicDensityData = economicDensityData
                    )
                }
            }
        }
    }
    // Завантаження JSON-файла кабелів
    private fun loadCableData(filename: String): List<CableData> {
        return try {
            val jsonString: String
            assets.open(filename).use { inputStream: InputStream ->
                jsonString = inputStream.bufferedReader().use { it.readText() }
            }
            // Десеріалізація даних з JSON
            Log.d("CableFinder", "JSON content: $jsonString") // Перевірка JSON
            Json.decodeFromString<List<CableData>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("CableFinder", "Error loading JSON: ${e.message}")
            emptyList() // При помілці порожній список
        }
    }

    // Завантаження JSON-файла з економічною густиною струму
    private fun loadEconomicDensityData(filename: String): List<EconomicDensityData> {
        return try {
            val jsonString: String
            assets.open(filename).use { inputStream: InputStream ->
                jsonString = inputStream.bufferedReader().use { it.readText() }
            }
            // // Десеріалізація даних з JSON
            Log.d("CableFinder", "Economic density JSON content: $jsonString") // Перевірка JSON
            Json.decodeFromString<List<EconomicDensityData>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("CableFinder", "Error loading JSON: ${e.message}")
            emptyList() // При помілці порожній список
        }
    }
}

@Composable
fun TaskSwitcherScreen(
    modifier: Modifier = Modifier,
    cableData: List<CableData>,
    economicDensityData: List<EconomicDensityData>
) {
    var selectedTask by remember { mutableStateOf(1) }  // Стан для запам'ятовування обраного завдання

    Column(modifier = modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { selectedTask = 1 }) {
                Text("Завдання 1-2")
            }
            Button(onClick = { selectedTask = 2 }) {
                Text("Задание 3")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        //Для вибору та відображення екранів відповідно до обраного завдання
        when (selectedTask) {
            1 -> CableCalculatorScreen( // Відображення 1-2 завдань
                cableData = cableData, // Передача даних кабелів
                economicDensityData = economicDensityData// Передача даних  економічної густини
            )
            2 -> Task3Screen() //Відображення 3 завдання
        }
    }
}



@Serializable
data class Cable(
    val sech: Int,
    @SerialName("up_to_3_kV") val upTo3kV: String?,
    @SerialName("6_kV") val sixKV: String?,
    @SerialName("10_kV") val tenKV: String?
)

@Serializable
data class CableData(
    val conductor: String,
    val insulation: String,
    val sheath: String,
    val cables: List<Cable>
)

@Serializable
data class EconomicDensityData(
    val conductor: String,
    val insulation: String,
    val coefficients: Map<String, Double>
)

fun findSuitableCable(
    impa: Int, // Розрахунковий струм для післяаварійного режиму
    voltage: Double, // Напруга
    cablesData: List<CableData> //Табличка для пошуку
): CableData? {
    for (cableData in cablesData) {// Прохід через список кабельних даних
        for (cable in cableData.cables) {  // Прохід через кожен кабель у списку кабелів всередині cableData
            // Перевірка напруги і отримання відповідного значення струму
            val current = when (voltage) {
                3.0 -> cable.upTo3kV?.toIntOrNull() // Перетворення в Int
                6.0 -> cable.sixKV?.toIntOrNull()   // Перетворення в Int
                10.0 -> cable.tenKV?.toIntOrNull()  // Перетворення в Int
                else -> null // Якщо в таблиці нема значень для такої напруги
            }

            // Якщо струм  і  значення impa знайдено
            if (current != null && impa == current) {
                return cableData
            }
        }
    }
    return null
}


// Пошук коефіцієнта економічної густини
fun findEconomicCurrentDensity(
    conductor: String, // Жили
    insulation: String, // Обмотка
    timeTm: Double,
    economicDensityData: List<EconomicDensityData> //Дані з таблиці
): Double? {
    for (densityData in economicDensityData) {// Перебір усіх записів
        if (densityData.conductor == conductor && densityData.insulation == insulation) {
            // Визначення діапазоун часу і повернення відповідний коефіцієнт
            return when (timeTm) {
                in 1000.0..3000.0 -> densityData.coefficients["1000_to_3000"]
                in 3000.0..5000.0 -> densityData.coefficients["3000_to_5000"]
                else -> densityData.coefficients["5000_plus"]
            }
        }
    }
    return null // Возвращаем null, если коэффициент не найден
}

// Пошук кабелю за термічною стійкістю
fun findClosestSech(
    cableData: CableData,
    thermalStability: Double,
    currentVoltage: Double // Поточна напруга
): Pair<Cable?, Double>? { // Pair для пари кабель-напруга
    // Пошук точного співпадіння
    val exactMatch = cableData.cables.find { it.sech.toDouble() == thermalStability }
    if (exactMatch != null) {
        //Повернення результату з поточною напругою в якості номінальної
        return Pair(exactMatch, currentVoltage)
    }

    // Корегування вибору кабеля з огляду на термічну стійкість
    val closestBiggerCable = cableData.cables.filter { it.sech >= thermalStability }
        .minByOrNull { it.sech }

    //Впзначення номінальної напруги, якщо довелося корегувати
    if (closestBiggerCable != null) {
        val newVoltage = when (currentVoltage) {
            10.0 -> 6.0
            6.0 -> 3.0
            else -> currentVoltage
        }

        // Повернення результату з новою номінальною напругою
        return Pair(closestBiggerCable, newVoltage)
    }
    return null
}


//Отримання термічного коефіцієнту в залежності від типу ізоляції
fun getThermalCoefficient(insulation: String): Double {
    return when (insulation.lowercase()) {
        "paper", "паперова" -> 92.0 // Паперові кабелі
        "plastic", "пластмасова" -> 75.0 // Пластмасові кабелі
        "rubber", "гумова" -> 65.0 // Гумові кабелі
        else -> 92.0
    }
}


// Основні розрахунки 1 завдання
fun calculateResultsWithDensity(
    currentIk: Double,
    timeTf: Double,
    powerSm: Double,
    voltage: Double,
    timeTm: Double,
    powerKZ: Double,
    cableData: List<CableData>,
    economicDensityData: List<EconomicDensityData>,
): String {
    val im = (powerSm / 2) / (sqrt(3.0) * voltage) // Струм для нормального режиму
    val impa = (2 * im).toInt() //Струм для післяаварійного режиму

    // Пошук кабеля за І_м.па та напругою
    val suitableCable = findSuitableCable(
        impa=impa,
        voltage = voltage,
        cablesData = cableData
    )

    // Якщо обрали кабель, шукаємо економічну густину струму
    return if (suitableCable != null) {
        val economicDensity = findEconomicCurrentDensity(
            conductor = suitableCable.conductor,
            insulation = suitableCable.insulation,
            timeTm = timeTm,
            economicDensityData = economicDensityData
        )

        if (economicDensity != null) { //Якщо знайшли економічну густину

            val sek = im / economicDensity // Економічний переріз
            // Вибір термічного коефіцієнта на основі типу ізоляції
            val thermalCoefficient = getThermalCoefficient(suitableCable.insulation)
            val thermalStability = currentIk * sqrt(timeTf) / thermalCoefficient //Термічна стійкість


            // Пошук перерізу та номінальної напруги за термічною стійкістю
            val closestCableResult = findClosestSech(suitableCable, thermalStability, voltage)

            // ПЕРЕВІРКА ЗАВДАННЯ 2
            return if (closestCableResult != null) { // Якщо знайдено переріз та ном. напругу
                val (closestCable, foundVoltage) = closestCableResult

                val uSn = if (voltage == 10.0) 10.5 else 6.3  // Середня номінальна напруга точки, в якій виникає КЗ
                val ukPercent = if (voltage == 10.0) 10.5 else 6.3  // Напруга короткого замикання трансформатора
                val sNomT = if (foundVoltage == 10.0) 10.5 else 6.3  // Номінальна потужність трансформатора

                // Опори елементів заступної схеми
                val xc = uSn * uSn / powerKZ*10
                val xt = (ukPercent / 100) * (uSn * uSn) / sNomT

                // Сумарний опір для точки К1
                val xSum = xc + xt

                // Початкове діюче значення струму трифазного КЗ
                val ip0 = uSn / (sqrt(3.0) * xSum)

                """
                Номінальний струм (Iном): $im А
                Струм після аварії (Iм.па): $impa А
                Термічна стійкість (s): $thermalStability мм^2
                Підходящий кабель: провідник - ${suitableCable.conductor}, ізоляція - ${suitableCable.insulation}, оболонка - ${suitableCable.sheath}
                Економічна густина струму: $economicDensity A/мм²
                Економічний переріз: $sek мм^2
                Переріз жил кабеля - ${closestCable?.sech ?: "Не знайдено"} мм², Номінальна напруга: $foundVoltage кВ
                
                Перевірка:
                U_с.н. = $uSn кВ
                U_к%. = $ukPercent %
                S_ном.т = $sNomT МВА
                Xc = $xc Ом
                Xt = $xt Ом
                Сумарний опір XΣ = $xSum Ом
                Початкове значення струму трифазного КЗ Iп0 = $ip0 кА
                """.trimIndent()
            } else {
                "Неможливо знайти підходящу секцію для значення термічної стійкості $thermalStability мм²."
            }
        } else {
            """
                Номінальний струм (Iном): ${(powerSm / 2) / (sqrt(3.0) * voltage)} А
                Струм після аварії (Iм.па): ${(2 * (powerSm / 2) / (sqrt(3.0) * voltage)).toInt()} А
                Термічна стійкість (s): ${currentIk * sqrt(timeTf) / 92.0} мм^2
                Підходящий кабель: провідник - ${suitableCable.conductor}, ізоляція - ${suitableCable.insulation}, оболонка - ${suitableCable.sheath}
                Не вдалося знайти економічну густину струму
            """.trimIndent()
        }
    } else {
        """
            Струм після аварії (Iм.па): ${(2 * (powerSm / 2) / (sqrt(3.0) * voltage)).toInt()} А
            Підходящий кабель не знайдено
        """.trimIndent()
    }
}



@Composable
fun CableCalculatorScreen(
    modifier: Modifier = Modifier,
    cableData: List<CableData>,
    economicDensityData: List<EconomicDensityData>
) {
    var currentIk by remember { mutableStateOf("2500") }  // Струм КЗ
    var timeTf by remember { mutableStateOf("2.5") }     // Фіктивний час вимикання
    var powerSm by remember { mutableStateOf("1300") }    // Sm
    var timeTm by remember { mutableStateOf("4000") }     // Кількість годин
    var voltage by remember { mutableStateOf("10") }    // Напруга
    var powerKZ by remember { mutableStateOf("2000") }    // Потужність ТП
    var result by remember { mutableStateOf("Результати будуть тут") }   // Поле для результату


    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(scrollState) // Прокрутка
    ) {
        Text(text = "Розрахунок кабелю", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Поля для введення даних
        OutlinedTextField(value = currentIk, onValueChange = { currentIk = it }, label = { Text("Струм КЗ, Iк (кА)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = timeTf, onValueChange = { timeTf = it }, label = { Text("Фіктивний час, tф (с)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = powerSm, onValueChange = { powerSm = it }, label = { Text("Розрахункове навантаження, Sм (кВА)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = timeTm, onValueChange = { timeTm = it }, label = { Text("Кількість годин, Tм (год)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = voltage, onValueChange = { voltage = it }, label = { Text("Напруга (кВ)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = powerKZ, onValueChange = { powerKZ = it }, label = { Text("Потужність КЗ (МВ*А)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка для розрахунків
        Button(onClick = {
            val calculatedResult = calculateResultsWithDensity(
                currentIk = currentIk.toDoubleOrNull() ?: 0.0,
                timeTf = timeTf.toDoubleOrNull() ?: 0.0,
                powerSm = powerSm.toDoubleOrNull() ?: 0.0,
                voltage = voltage.toDoubleOrNull() ?: 0.0,
                timeTm = timeTm.toDoubleOrNull() ?: 0.0,
                powerKZ=powerKZ.toDoubleOrNull()?: 0.0,
                cableData = cableData,
                economicDensityData = economicDensityData,

            )
            result = calculatedResult
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Розрахувати")
        }

        // Виведення результату
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = result, style = MaterialTheme.typography.bodyLarge)

    }
}

@Composable
fun Task3Screen(
    modifier: Modifier = Modifier
) {
    var uKmax by remember { mutableStateOf("11.1") }  // Uк.max
    var uVn by remember { mutableStateOf("115") }    // Uв.н
    var uNn by remember { mutableStateOf("11") }    // Uв.н
    var sNomT by remember { mutableStateOf("6.3") }  // Sном.т
    var rc_n by remember { mutableStateOf("10.65") }    // Rс.н
    var xc_n by remember { mutableStateOf("24.02") }    // Xс.н
    var rc_min by remember { mutableStateOf("34.88") }  // Rс.min
    var xc_min by remember { mutableStateOf("65.68") }  // Xс.min
    var r0 by remember { mutableStateOf("0.64") }   //  R0
    var x0 by remember { mutableStateOf("0.363") }   // X0
    //дані відхідної лінії
    var section1_2 by remember { mutableStateOf("0.2") }
    var section2_3 by remember { mutableStateOf("0.35") }
    var section4_5 by remember { mutableStateOf("0.2") }
    var section5_6 by remember { mutableStateOf("0.6") }
    var section6_7 by remember { mutableStateOf("2.0") }
    var section7_8 by remember { mutableStateOf("2.55") }
    var section8_9 by remember { mutableStateOf("3.37") }
    var section9_10 by remember { mutableStateOf("3.1") }

    var result by remember { mutableStateOf("Результати будуть тут") }

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(text = "Розрахунок опорів та параметрів", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Поля для ввода данных
        OutlinedTextField(
            value = uKmax,
            onValueChange = { uKmax = it },
            label = { Text("Uк.max (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uVn,
            onValueChange = { uVn = it },
            label = { Text("Uв.н (кВ)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uNn,
            onValueChange = { uNn = it },
            label = { Text("Uн.н (кВ)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = sNomT,
            onValueChange = { sNomT = it },
            label = { Text("Sном.т (кВА)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = rc_n,
            onValueChange = { rc_n = it },
            label = { Text("Rс.н (Ом)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = xc_n,
            onValueChange = { xc_n = it },
            label = { Text("Xс.н (Ом)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = rc_min,
            onValueChange = { rc_min = it },
            label = { Text("Rс.min (Ом)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = xc_min,
            onValueChange = { xc_min = it },
            label = { Text("Xс.min (Ом)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = r0,
            onValueChange = { r0 = it },
            label = { Text("R0 (Ом/км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = x0,
            onValueChange = { x0 = it },
            label = { Text("X0 (Ом/км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = section1_2,
            onValueChange = { section1_2 = it },
            label = { Text("Довжина ділянки 1-2 (км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = section2_3,
            onValueChange = { section2_3 = it },
            label = { Text("Довжина ділянки 2-3 (км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = section4_5,
            onValueChange = { section4_5 = it },
            label = { Text("Довжина ділянки 4-5 (км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = section5_6,
            onValueChange = { section5_6 = it },
            label = { Text("Довжина ділянки 5-6 (км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = section6_7,
            onValueChange = { section6_7 = it },
            label = { Text("Довжина ділянки 6-7 (км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = section7_8,
            onValueChange = { section7_8 = it },
            label = { Text("Довжина ділянки 7-8 (км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = section8_9,
            onValueChange = { section8_9 = it },
            label = { Text("Довжина ділянки 8-9 (км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = section9_10,
            onValueChange = { section9_10 = it },
            label = { Text("Довжина ділянки 9-10 (км)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        //Розрахувати
        Button(onClick = {
            result = calculateMain(
                uKmax.toDoubleOrNull() ?: 0.0,
                uVn.toDoubleOrNull() ?: 0.0,
                uNn.toDoubleOrNull() ?: 0.0,
                sNomT.toDoubleOrNull() ?: 0.0,
                rc_n.toDoubleOrNull() ?: 0.0,
                xc_n.toDoubleOrNull() ?: 0.0,
                rc_min.toDoubleOrNull() ?: 0.0,
                xc_min.toDoubleOrNull() ?: 0.0,
                r0.toDoubleOrNull() ?: 0.0,
                x0.toDoubleOrNull() ?: 0.0,
                section1_2.toDoubleOrNull() ?: 0.0,
                section2_3.toDoubleOrNull() ?: 0.0,
                section4_5.toDoubleOrNull() ?: 0.0,
                section5_6.toDoubleOrNull() ?: 0.0,
                section6_7.toDoubleOrNull() ?: 0.0,
                section7_8.toDoubleOrNull() ?: 0.0,
                section8_9.toDoubleOrNull() ?: 0.0,
                section9_10.toDoubleOrNull() ?: 0.0
            )
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Розрахувати")
        }

        // Виведення результату
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = result, style = MaterialTheme.typography.bodyLarge)
    }
}

fun calculateMain(
    uKmax: Double,
    uVn: Double,
    uNn: Double,
    sNomT: Double,
    rc_n: Double,
    xc_n: Double,
    rc_min: Double,
    xc_min: Double,
    r0: Double,
    x0: Double,
    section1_2: Double,
    section2_3: Double,
    section4_5: Double,
    section5_6: Double,
    section6_7: Double,
    section7_8: Double,
    section8_9: Double,
    section9_10: Double

): String {
    // Розрахунок  Xт - реактанс
    val xt = calculateXtValue(uKmax, uVn, sNomT)

    // Zш и Zш.min - опори на шинах 10 кВ (норм. та мін. режими)
    val (xSh, zSh, xSh_min, zSh_min) = calculateZshValues(rc_n, xc_n, rc_min, xc_min, xt)

    // Струми трифазного та двофазного КЗ на шинах 10 кВ(норм. та мін. режими)
    val (i3_sh,i2_sh,i3_sh_min,i2_sh_min) = calculateI(uVn, zSh, zSh_min)

    // Коефіцієнт приведення
    val k_pr=calculateK(uVn,uNn)

    // Zш.н и Zш.н.min- Опори на шинах 10 кВ (норм. та мін. режими)
    val(rShN,xShN,zSh_N,rSh_N_min,xSh_N_min,zSh_N_min)=calculateZshNValues(rc_n,xSh,rc_min,xSh_min,k_pr)

    // Дійсні струми трифазного та двофазного КЗ на шинах 10 кВ(норм. та мін. режими)
    val (i3_sh_n,i2_sh_n,i3_sh_n_min,i2_sh_n_min) = calculateI(uNn, zSh_N, zSh_N_min)

    // Довжина відрізку
    val totalLength = section1_2 + section2_3  + section4_5 + section5_6 + section6_7 + section7_8 + section8_9 + section9_10
    val Rl = totalLength * r0 // резистанс
    val Xl = totalLength * x0 // реактанс

        //Опори в точці 10(норм. та мін. режими)
    val(r_sum_n,x_sum_n,z_sum_n,r_sum_n_min,x_sum_n_min,z_sum_n_min)=calculateZsumN(Rl, Xl,rShN,xShN,rSh_N_min,xSh_N_min)

    // Струми трифазного та двофазного КЗ в точці 10(норм. та мін. режими)
    val (i3_l_n,i2_l_n,i3_l_n_min,i2_l_n_min) = calculateI(uNn, z_sum_n, z_sum_n_min)

    return """
        Реактивний опір трансформатора: XТ = ${"%.2f".format(xt)} Ом
        
        Xш = ${"%.2f".format(xSh)} Ом
        Zш = ${"%.2f".format(zSh)} Ом
        Xш.min = ${"%.2f".format(xSh_min)} Ом
        Zш.min = ${"%.2f".format(zSh_min)} Ом
        
        Струми КЗ у нормальному режимі:
        I(3)ш = ${"%.2f".format(i3_sh)} А
        I(2)ш = ${"%.2f".format(i2_sh)} А
        Струми КЗ у мінімальному режимі:
        I(3)ш.min = ${"%.2f".format(i3_sh_min)} А
        I(2)ш.min = ${"%.2f".format(i2_sh_min)} А
        
        Коефіцієнт приведення:
        k_р = ${"%.3f".format(k_pr)}
        
        Нормальний режим:
        Rш.н = ${"%.2f".format(rShN)} Ом
        Xш.н = ${"%.2f".format(xShN)} Ом
        Zш.н = ${"%.2f".format(zSh_N)} Ом

        Мінімальний режим:
        Rш.н.мін = ${"%.2f".format(rSh_N_min)} Ом
        Xш.н.мін = ${"%.2f".format(xSh_N_min)} Ом
        Zш.н.мін = ${"%.2f".format(zSh_N_min)} Ом
        
        Струми КЗ у нормальному режимі:
        I(3)ш = ${"%.2f".format(i3_sh_n)} А
        I(2)ш = ${"%.2f".format(i2_sh_n)} А
        Струми КЗ у мінімальному режимі:
        I(3)ш.min = ${"%.2f".format(i3_sh_n_min)} А
        I(2)ш.min = ${"%.2f".format(i2_sh_n_min)} А
        
        Сумарна довжина: $totalLength км
        Сумарний опір: Rл = $Rl Ом
        Сумарний реактивний опір: Xл = $Xl Ом
        
        Нормальний режим:
        RΣ.н = ${"%.2f".format(r_sum_n)} Ом
        XΣ.н = ${"%.2f".format(x_sum_n)} Ом
        ZΣ.н = ${"%.2f".format(z_sum_n)} Ом

        Мінімальний режим:
        RΣ.н.мін = ${"%.2f".format(r_sum_n_min)} Ом
        XΣ.н.мін = ${"%.2f".format(x_sum_n_min)} Ом
        ZΣ.н.мін = ${"%.2f".format(z_sum_n_min)} Ом
        
        
         Струми КЗ у нормальному режимі:
         I(3)ш = ${"%.2f".format(i3_l_n)} А
         I(2)ш = ${"%.2f".format(i2_l_n)} А
         Струми КЗ у мінімальному режимі:
         I(3)ш.min = ${"%.2f".format(i3_l_n_min)} А
         I(2)ш.min = ${"%.2f".format(i2_l_n_min)} А
    """.trimIndent()
}

fun calculateZsumN(
    Rl: Double,
    Xl: Double,
    rShN: Double,
    xShN: Double,
    rSh_N_min: Double,
    xSh_N_min: Double

): Six<Double, Double, Double, Double, Double, Double> {
    val r_sum_n =  Rl + rShN
    val  x_sum_n  = Xl + xShN
    val z_sum_n =  sqrt(r_sum_n.pow(2) + x_sum_n.pow(2))

    val r_sum_n_min =  Rl + rSh_N_min
    val  x_sum_n_min  = Xl + xSh_N_min
    val z_sum_n_min =  sqrt(r_sum_n_min.pow(2) + x_sum_n_min.pow(2))

    return Six(r_sum_n, x_sum_n, z_sum_n, r_sum_n_min,x_sum_n_min,z_sum_n_min)
}






// Розрахунок реактивного опору трансформатора (XТ)
fun calculateXtValue(uKmax: Double, uVn: Double, sNomT: Double): Double {
    return if (uKmax == 0.0 || uVn == 0.0 || sNomT == 0.0) {
        0.0
    } else {
        (uKmax * uVn.pow(2)) / (100 * sNomT)
    }
}

// Розрахунок Zш и Zш.min
fun calculateZshValues(
    rc_n: Double,
    xc_n: Double,
    rc_min: Double,
    xc_min: Double,
    xt: Double
): Four<Double, Double, Double, Double> {
    val xSh = xc_n + xt
    val zSh = sqrt(rc_n.pow(2) + xSh.pow(2))

    val xSh_min = xc_min + xt
    val zSh_min = sqrt(rc_min.pow(2) + xSh_min.pow(2))

    return Four(xSh, zSh, xSh_min, zSh_min)
}

fun calculateZshNValues(
    rc_n: Double,
    xSh: Double,
    rc_min: Double,
    xSh_min: Double,
    k_pr: Double

): Six<Double, Double, Double, Double, Double, Double> {
    val rShN =  rc_n * k_pr
    val xShN = xSh * k_pr
    val zSh_N =  sqrt(rShN.pow(2) + xShN.pow(2))

    val rSh_N_min = rc_min * k_pr
    val xSh_N_min = xSh_min * k_pr
    val zSh_N_min = sqrt(rSh_N_min.pow(2) + xSh_N_min.pow(2))

    return Six(rShN, xShN, zSh_N, rSh_N_min,xSh_N_min,zSh_N_min)
}


// Вспомогательная структура для возвращения нескольких значений
data class Four<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
data class Six<A, B, C, D, E,F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

// Розрахунок струму КЗ
fun calculateI(uVn: Double, zSh: Double, zSh_min: Double)
: Four<Double, Double, Double, Double>  {

    // Струм трифазного короткого замикання в нормальному режимі
    val i3_sh = (uVn * 1000) / (sqrt(3.0) * zSh)
    // Струм двофазного короткого замикання в нормальному режимі
    val i2_sh = i3_sh * (sqrt(3.0) / 2)

    // Струм трифазного короткого замикання в мінімальному режимі
    val i3_sh_min = (uVn * 1000) / (sqrt(3.0) * zSh_min)
    // Струм двофазного короткого замикання в мінімальному режимі
    val i2_sh_min = i3_sh_min * (sqrt(3.0) / 2)

    return  Four (i3_sh,i2_sh,i3_sh_min,i2_sh_min )
}


fun calculateK (uVn: Double,uNn: Double):Double{
    return if (uVn == 0.0 || uNn == 0.0 ) {
        0.0
    }else{
        uNn.pow(2)/uVn.pow(2)
    }

}

@Preview(showBackground = true)
@Composable
fun CableCalculatorScreenPreview() {
    LR4Theme {
        TaskSwitcherScreen(cableData = emptyList(), economicDensityData = emptyList())
    }
}
