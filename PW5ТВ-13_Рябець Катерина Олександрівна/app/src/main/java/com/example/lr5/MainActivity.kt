package com.example.lr5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lr5.ui.theme.LR5Theme
import android.util.Log
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment

data class Equipment(
    val name: String,
    val omega: Double, // Частота відмов ω
    val tB: Double,    // Середній час відновлення t_b (год.)
    val mu: Double,    // Частота ремонтів µ
    val tP: Double,    // Тривалість планового ремонту t_p (год.)
    val requiresLength: Boolean = false, // Чи потрібне введення довжини
    val requiresQuantity: Boolean = false // Чи потрібен вибір кількості
)

val equipmentData = listOf(
    // Перша таблиця
    Equipment("ПЛ-110 кВ", 0.007, 10.0, 0.167, 35.0, requiresLength = true),
    Equipment("ПЛ-35 кВ", 0.02, 8.0, 0.167, 35.0, requiresLength = true),
    Equipment("ПЛ-10 кВ", 0.02, 10.0, 0.167, 35.0, requiresLength = true),
    Equipment("КЛ-10 кВ (траншея)", 0.03, 44.0, 1.0, 9.0, requiresLength = true),
    Equipment("КЛ-10 кВ (кабельний канал)", 0.005, 17.5, 1.0, 9.0, requiresLength = true),


    // Друга таблиця
    Equipment("Збірні шини 10 кВ", 0.03, 2.0, 0.167, 5.0, requiresQuantity = true),
    Equipment("Т-110 кВ", 0.015, 100.0, 1.0, 43.0),
    Equipment("Т-35 кВ", 0.02, 80.0, 1.0, 28.0),
    Equipment("Т-10 кВ (кабельна мережа)", 0.005, 60.0, 0.5, 10.0),
    Equipment("Т-10 кВ (повітряна мережа)", 0.05, 60.0, 0.5, 10.0),
    Equipment("В-110 кВ (елегазовий)", 0.01, 30.0, 0.1, 30.0),
    Equipment("В-10 кВ (малооливний)", 0.02, 15.0, 0.33, 15.0),
    Equipment("В-10 кВ (вакуумний)", 0.01, 15.0, 0.33, 15.0),
    Equipment("АВ-0,38 кВ", 0.05, 4.0, 0.33, 10.0),
    Equipment("ЕД 6-10 кВ", 0.1, 160.0, 0.5, 0.0), // без t_p
    Equipment("ЕД 0,38 кВ", 0.1, 50.0, 0.5, 0.0) // без t_p
)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LR5Theme {
                MainScreen()
            }
        }
    }
}

@Composable
fun ReliabilityCalculator(modifier: Modifier = Modifier) {
    // Вибране обладнання
    var selectedEquipment by remember { mutableStateOf<Equipment?>(null) }
    // Список доданого в одноколову систему обладнання
    val addedEquipmentSingle = remember { mutableStateListOf<Pair<Equipment, Double>>() }
    // Список доданого в двоколову систему обладнання
    val addedEquipmentDouble = remember { mutableStateListOf<Pair<Equipment, Double>>() }
    // Секційний вимикач, вибраний для двоколової системи
    var switchEquipment by remember { mutableStateOf<Equipment?>(null) }
    // Режим: true - двоколова система, false - одноколова
    var isDoubleLineMode by remember { mutableStateOf(false) }
    // Поля вводу для довжини або кількості
    var inputLength by remember { mutableStateOf("") }
    var inputQuantity by remember { mutableStateOf("") }
    // Результати розрахунків для одноколової системи
    var resultSingleLine by remember { mutableStateOf("") }
    // Результати розрахунків для двоколової системи
    var resultDoubleLine by remember { mutableStateOf("") }
    // Висновок про надійність
    var resultConclusion by remember { mutableStateOf("") }
    // Повідомлення про помилку
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (isDoubleLineMode) "Двоколова система" else "Одноколова система",
            style = MaterialTheme.typography.titleMedium
        )

        if (!isDoubleLineMode) {
            // Для одноколової системи доступне все обладнання
            DropdownMenuEquipment(
                selectedEquipment = selectedEquipment,
                onEquipmentSelected = { selected ->
                    selectedEquipment = selected
                }
            )
        } else {
            // Для двоколової системи доступні лише секційні вимикачі
            DropdownMenuEquipment(
                selectedEquipment = switchEquipment,
                onEquipmentSelected = { selected ->
                    switchEquipment = selected
                },
                filter = { it.name.startsWith("В-") } // Фільтр: лише вимикачі
            )
        }
        // Додатковий ввід довжини або кількості
        if (!isDoubleLineMode) {
            selectedEquipment?.let { equipment ->
                if (equipment.requiresLength) {
                    OutlinedTextField(
                        value = inputLength,
                        onValueChange = { inputLength = it },
                        label = { Text("Довжина (км)") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (equipment.requiresQuantity) {
                    OutlinedTextField(
                        value = inputQuantity,
                        onValueChange = { inputQuantity = it },
                        label = { Text("Кількість") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Кнопка для додавання обладнання
        if (!isDoubleLineMode) { // Активна лише в режимі одноколової системи
            Button(
                onClick = {
                    //Якщо користувач обрав обладнання, воно додається до списку addedEquipmentSingle
                    selectedEquipment?.let { equipment ->
                        //Множник, який залежить від того, чи потрібне введення довжини (requiresLength)
                        // або кількості (requiresQuantity).
                        val multiplier = if (equipment.requiresLength) {
                            inputLength.toDoubleOrNull() ?: 1.0
                        } else if (equipment.requiresQuantity) {
                            inputQuantity.toDoubleOrNull() ?: 1.0
                        } else {
                            1.0
                        }
                        addedEquipmentSingle.add(equipment to multiplier)
                        inputLength = ""
                        inputQuantity = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedEquipment != null
            ) {
                Text("Додати обладнання")
            }
        }



        // Список доданого обладнання в залежності від вибраного режиму
        val currentEquipment = if (!isDoubleLineMode) addedEquipmentSingle else addedEquipmentDouble
        // Перевіряємо, чи є додане обладнання в одноколовій або в двоколовій системі
        // та  чи обрано секційний вимикач в двоколовій
        if (currentEquipment.isNotEmpty() || (isDoubleLineMode && switchEquipment != null)) {
            Text("Додане обладнання:")
            for ((item, multiplier) in currentEquipment) {// Прохід по кожному елементу з currentEquipment
                val description = if (item.requiresLength) "Довжина: $multiplier км" else if (item.requiresQuantity)
                    "Кількість: $multiplier" else if (isDoubleLineMode) "2 шт. " else " "
                Text("- ${item.name} $description")
            }
            if (isDoubleLineMode && switchEquipment != null) { // Якщо це двоколова система і вибрано секційний вимикач
                Text("- ${switchEquipment!!.name} (використовується як секційний вимикач)")
            }
        }


        // Перемикання між одноколовою і двоколовою
        Button(
            onClick = {
                if (addedEquipmentSingle.isEmpty()) {
                    errorMessage = "Заповніть одноколову систему перед переходом до двоколової!"
                } else {
                    isDoubleLineMode = !isDoubleLineMode
                    errorMessage = ""
                    if (isDoubleLineMode) {
                        // Автоматичне копіювання обладнання
                        addedEquipmentDouble.clear()
                        addedEquipmentDouble.addAll(addedEquipmentSingle.map { it.first to it.second * 2 })
                    } else {
                        switchEquipment = null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isDoubleLineMode) "Перейти до одноколової системи" else "Перейти до двоколової системи")
        }


        // Кнопка для розрахунку
        if (isDoubleLineMode) {
            Button(
                onClick = {
                    if (addedEquipmentSingle.isEmpty() || addedEquipmentDouble.isEmpty() || switchEquipment == null) {
                        errorMessage = "Заповніть обидві системи та виберіть секційний вимикач!"
                    } else {
                        errorMessage = ""

                        // Розрахунок для одноколової системи
                        val singleOmega = addedEquipmentSingle.sumOf { it.first.omega * it.second }
                        val singleTB = addedEquipmentSingle.sumOf { it.first.tB * it.first.omega * it.second } / singleOmega
                        val singleKa = singleOmega * (singleTB / 8760)
                        val singleKp = if (addedEquipmentSingle.isNotEmpty()) {
                            val maxTp = addedEquipmentSingle
                                .filter { it.first in equipmentData.drop(5) } // Тільки елементи з другої таблиці
                                .maxOfOrNull { it.first.tP * it.second } // Максимальне t_п враховуючи множники
                            maxTp?.let { 1.2 * it / 8760 } ?: 0.0 // Якщо maxTp знайдено, обчислюємо; інакше повертаємо 0.0
                        } else {
                            0.0
                        }

                        // Розрахунок для двоколової системи
                        val omegaDk = 2 * singleOmega * (singleKa + singleKp)
                        val switchOmega = switchEquipment?.omega ?: 0.0
                        val omegaDs = omegaDk + switchOmega

                        // Висновки
                        resultSingleLine = """
                            Одноколова система:
                            Частота відмов: %.6f
                            Середній час відновлення: %.2f год.
                            Коеф. аварійного простою: %.6f
                            Коеф. планового простою: %.6f
                        """.trimIndent().format(
                            singleOmega,
                            singleTB,
                            singleKa,
                            singleKp
                        )

                        resultDoubleLine = """
                            Двоколова система:
                            Частота відмов одночасної відмови двох кіл: %.6f
                            Частота відмов із секційним вимикачем: %.6f
                        """.trimIndent().format(
                            omegaDk,
                            omegaDs
                        )

                        resultConclusion = if (singleOmega > omegaDs) {
                            "Одноколова система менш надійна, ніж двоколова."
                        } else {
                            "Двоколова система менш надійна, ніж одноколова."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Розрахувати")
            }
        }

        // Виведення помилок
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        // Виведення результатів
        if (resultSingleLine.isNotEmpty() && resultDoubleLine.isNotEmpty()) {
            Text(resultSingleLine, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultDoubleLine, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultConclusion, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun DropdownMenuEquipment(
    selectedEquipment: Equipment?, // Обране обладнання
    onEquipmentSelected: (Equipment) -> Unit,
    filter: ((Equipment) -> Boolean)? = null // Фільтр яке обладнання показувати
) {
    var expanded by remember { mutableStateOf(false) } // Стан розгорнуте/згорнуте
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedEquipment?.name ?: "Оберіть обладнання")
        }
        DropdownMenu( // Випадаюче меню
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Якщо є фільтр, застосовуємо його до equipmentData
            val filteredData = filter?.let { equipmentData.filter(it) } ?: equipmentData
            for (equipment in filteredData) { // Перебір всіх елементів зі списку equipmentData
                DropdownMenuItem(
                    text = { Text(equipment.name) }, // Назва кожного елемента
                    onClick = {
                        onEquipmentSelected(equipment) // Передача вибраного елементу у функцію зворотного виклику
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun MainScreen() {
    // Змінна для збереження вибраної вкладки
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = { // Верхня панель вкладок
            TabRow(
                selectedTabIndex = selectedTab, //Індекс вибраної вкладки
                modifier = Modifier.systemBarsPadding()
            ) {
                Tab( // Вкладка "Завдання 1"
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Завдання 1") }
                )
                Tab(// Вкладка "Завдання 2"
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Завдання 2") }
                )
            }
        }
    ) { innerPadding ->
        Box( // Вміст що змінюється залежно від обраного завдання
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            when (selectedTab) {
                0 -> ReliabilityCalculator(modifier = Modifier.fillMaxSize()) // Екран 1 завдання
                1 -> TaskTwoScreen(modifier = Modifier.fillMaxSize()) // Екран 2 завдання
            }
        }
    }
}

@Composable
fun TaskTwoScreen(modifier: Modifier = Modifier) {
    // Змінні для зберігання введених користувачем значень
    var omega by remember { mutableStateOf("") } // Частота відмов (ω)
    var tB by remember { mutableStateOf("") } // Середній час відновлення (tB)
    var pM by remember { mutableStateOf("") } // Потужність навантаження (PM)
    var kp by remember { mutableStateOf("") } // Коефіцієнт планового простою (kP)
    var tM by remember { mutableStateOf("") } // Час роботи системи (T_M)
    var zPerA by remember { mutableStateOf("") } // Питомі збитки при аварійних відключеннях
    var zPerP by remember { mutableStateOf("") } // Питомі збитки при планових відключеннях
    var result by remember { mutableStateOf("") } // Результат розрахунків
    var errorMessage by remember { mutableStateOf("") } // Повідомлення про помилку

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Поля для вводу параметрів
        OutlinedTextField(
            value = omega,
            onValueChange = { omega = it },
            label = { Text("Частота відмов (ω, 1/рік)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = tB,
            onValueChange = { tB = it },
            label = { Text("Середній час відновлення (tB, роки)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = pM,
            onValueChange = { pM = it },
            label = { Text("Потужність навантаження (PM, кВт)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = kp,
            onValueChange = { kp = it },
            label = { Text("Коефіцієнт планового простою (kP, 1/рік)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = tM,
            onValueChange = { tM = it },
            label = { Text("Час роботи системи (T_M, роки)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = zPerA,
            onValueChange = { zPerA = it },
            label = { Text("Питомі збитки при аварійних відключеннях (грн/кВт-год)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = zPerP,
            onValueChange = { zPerP = it },
            label = { Text("Питомі збитки при планових відключеннях (грн/кВт-год)") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Кнопка "Розрахувати"
        Button(
            onClick = {
                try {
                    // Перетворення значень на числа
                    val omegaVal = omega.toDouble()
                    val tBVal = tB.toDouble()
                    val pMVal = pM.toDouble()
                    val kpVal = kp.toDouble()
                    val tMVal = tM.toDouble()
                    val zPerAVal = zPerA.toDouble()
                    val zPerPVal = zPerP.toDouble()

                    // Розрахунок математичного очікування недопостачання
                    val mwNedA = omegaVal * tBVal * pMVal * tMVal
                    val mwNedP = kpVal * pMVal * tMVal

                    // Загальні втрати
                    val losses = zPerAVal * mwNedA + zPerPVal * mwNedP

                    // Формування результату
                    result = """
                        Результати розрахунків:
                        - Математичне очікування аварійного недопостачання: ${"%.2f".format(mwNedA)} кВт-год
                        - Математичне очікування планового недопостачання: ${"%.2f".format(mwNedP)} кВт-год
                        - Загальні втрати: ${"%.2f".format(losses)} грн
                    """.trimIndent()

                    errorMessage = ""
                } catch (e: Exception) {
                    errorMessage = "Помилка введення даних. Перевірте правильність заповнення полів."
                    result = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Розрахувати")
        }

        // Виведення результатів або помилок
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        if (result.isNotEmpty()) {
            Text(result, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
