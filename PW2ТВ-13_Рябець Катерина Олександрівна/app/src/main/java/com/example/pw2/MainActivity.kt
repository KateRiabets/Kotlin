package com.example.pw2
import kotlin.math.pow
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pw2.ui.theme.Pw2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Pw2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FuelEmissionCalculator(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun FuelEmissionCalculator(modifier: Modifier = Modifier) {
    var fuelType by remember { mutableStateOf("Виберіть паливо") } // Змінна для типу палива
    var mass by remember { mutableStateOf(TextFieldValue("")) } //Змінна для маси
    var result by remember { mutableStateOf("") } // Змінна для результату
    // Список варіантів палива
    val fuelOptions = listOf("Донецьке газове вугілля марки ГР", "Високосірчистий мазут марки 40",
        "Природний газ із газопроводу Уренгой-Ужгород")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // Вирівнювання  по горизонтально центру
        verticalArrangement = Arrangement.Top  // Розміщення зверху вниз
    ) {
        // Текстове поле для введення маси
        OutlinedTextField(
            value = mass,
            onValueChange = { mass = it },
            label = { Text("Введіть масу палива") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp), // Нижній відступ
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) //Тільки числа
        )

        // Випадаюче меню для вибору палива
        var expanded by remember { mutableStateOf(false) } // Стан меню відкрито/закрито
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton( //Кнопка для відкриття
                onClick = { expanded = true },  // Стан - відкрито
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(fuelType) // Текст на кнопці  -  вибраний тип палива або "Виберіть паливо"
            }
            DropdownMenu( // Саме випадаюче меню
                expanded = expanded,
                onDismissRequest = { expanded = false } //Закриття при натисканні поза меню
            ) {
                fuelOptions.forEach { option -> // Для кожного варіанту палива пункт меню
                    DropdownMenuItem(
                        text = { Text(option) }, // Назва кожного пункту
                        onClick = {
                            fuelType = option // Оновлення поточного виду палива
                            expanded = false //Закриття меню після вибору
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка "Розрахувати"
        Button(
            onClick = {
                result = calculateEmissions(fuelType, mass.text)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Розрахувати")
        }

        // Виведення результату
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = result)
    }
}

// Розрахунки
fun calculateEmissions(fuelType: String, massText: String): String {
    val mass = massText.toDoubleOrNull() ?: return "Неправильна маса палива"

    val Q_i: Double // Нижча теплота згоряння, МДж/кг
    val A_r: Double // Вміст золи, %
    val G_vyn: Double // Масовий вміст горючих речовин у викидах, %
    val a_vyn: Double // Частка золи
    val eta_zu: Double // Ефективність золовловлювання

    // Визначення параметрыв в залежносты выд типа палива
    when (fuelType) {
        "Донецьке газове вугілля марки ГР" -> {
            Q_i = 20.47
            A_r = 25.20
            G_vyn = 1.5
            a_vyn = 0.8
            eta_zu = 0.985
        }

        "Високосірчистий мазут марки 40" -> {
            Q_i = 40.40
            A_r = 0.15
            G_vyn = 0.0
            a_vyn = 1.0
            eta_zu = 0.985
        }

        "Природний газ із газопроводу Уренгой-Ужгород" -> {
            // Природний газ не утворює твердих частинок
            return """
                Показник емісії твердих частинок становитиме: 0 г/ГДж
                Валовий викид становитиме: 0 т
            """.trimIndent()
        }

        else -> return "Оберіть тип палива"
    }

    // Розрахунок показника емісії твердих частинок
    val k_tv = (10.0.pow(6) / Q_i) * a_vyn * (A_r / (100 - G_vyn)) * (1 - eta_zu)

    // Розрахунок валового викиду
    val E_j = 10.0.pow(-6) * k_tv * mass * Q_i

    // Повертаємо результат як рядок
    return """
        Показник емісії твердих частинок становитиме: ${"%.2f".format(k_tv)} г/ГДж
        Валовий викид становитиме: ${"%.2f".format(E_j)} т
    """.trimIndent()
}



@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Pw2Theme {
        FuelEmissionCalculator()
    }
}
