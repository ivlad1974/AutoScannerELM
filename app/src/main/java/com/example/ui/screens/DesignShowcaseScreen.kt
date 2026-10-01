package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.AutomotiveBlue
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.StatusGreen

data class DesignConcept(
    val id: Int,
    val title: String,
    val subtitle: String,
    val imageRes: Int,
    val bulletPoints: List<String>
)

@Composable
fun DesignShowcaseScreen(
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var fullScreenImageRes by remember { mutableStateOf<Int?>(null) }

    val concepts = listOf(
        DesignConcept(
            id = 1,
            title = "Концепт 1: Бортовой Компьютер",
            subtitle = "Плиточный интерфейс (Dashboard Tiles)",
            imageRes = R.drawable.concept_1_tiles,
            bulletPoints = listOf(
                "4 крупные информативные плитки (Скорость, RPM, Температура ОЖ, АКБ)",
                "Цветовая индикация безопасных зон в реальном времени",
                "2 широкие кнопки быстрого доступа: Диагностика ЭБУ и Голосовой ИИ",
                "Оптимизирован для читаемости на расстоянии вытянутой руки"
            )
        ),
        DesignConcept(
            id = 2,
            title = "Концепт 2: Крупные Режимы",
            subtitle = "Карточки-режимы во весь экран (Mode Cards)",
            imageRes = R.drawable.concept_2_modes,
            bulletPoints = listOf(
                "4 массивных экрана-раздела: Приборка, Диагностика, ИИ-Механик, Журнал",
                "Нулевая перегрузка: на каждом экране только нужные кнопки",
                "Крупные шрифты 20-38sp для легкого нажатия в движении",
                "Изолированные функции без мелких настроек на виду"
            )
        ),
        DesignConcept(
            id = 3,
            title = "Концепт 3: Driver HUD",
            subtitle = "Высококонтрастный щиток с дуговыми шкалами",
            imageRes = R.drawable.concept_3_hud,
            bulletPoints = listOf(
                "Гигантский спидометр и температура двигателя со световыми шкалами",
                "Большая центральная кнопка голосового ассистента в одно касание",
                "Высококонтрастный темный OLED режим против бликов солнца и ночью",
                "Идеально для размещения смартфона в держателе на лобовом стекле"
            )
        )
    )

    val current = concepts[selectedTab]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ВАРИАНТЫ ДИЗАЙНА",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AutomotiveBlue
                        )
                        Text(
                            text = "Графические концепты интерфейса",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = AutomotiveBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AutomotiveBlue
                        )
                    }
                ) {
                    concepts.forEachIndexed { index, item ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = "№${item.id}",
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            icon = {
                                when (index) {
                                    0 -> Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                                    1 -> Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                                    else -> Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        // Body
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = current.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = current.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Big Graphic Card with Image Preview
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, AutomotiveBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { fullScreenImageRes = current.imageRes },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(Color.Black)
                        ) {
                            Image(
                                painter = painterResource(id = current.imageRes),
                                contentDescription = current.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.75f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Нажмите для увеличения", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Bullet points description
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AutomotiveBlue.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Особенности данного варианта:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AutomotiveBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        current.bulletPoints.forEach { point ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = point,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Switch Concepts Bottom Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    concepts.forEachIndexed { idx, c ->
                        Button(
                            onClick = { selectedTab = idx },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == idx) AutomotiveBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedTab == idx) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Вариант ${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Full screen zoom modal dialog
    if (fullScreenImageRes != null) {
        Dialog(
            onDismissRequest = { fullScreenImageRes = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenImageRes = null },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = fullScreenImageRes!!),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )

                IconButton(
                    onClick = { fullScreenImageRes = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Закрыть", tint = Color.White)
                }
            }
        }
    }
}
