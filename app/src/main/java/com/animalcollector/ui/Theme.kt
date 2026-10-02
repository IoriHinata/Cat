package com.animalcollector.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Colors = darkColorScheme(primary=Color(0xFFFFC857), secondary=Color(0xFF8BE0C2), tertiary=Color(0xFFBBA7FF), background=Color(0xFF0D1924), surface=Color(0xFF142838))
@Composable fun AnimalTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Colors, typography = Typography(), content = content)
