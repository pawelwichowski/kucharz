package com.example.kucharz2.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kucharz2.ui.components.HeaderCard

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onOpenPantry: () -> Unit,
    onOpenPermanentExclusions: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard("Ustawienia", "Preferencje aplikacji.") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tryb ciemny", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (isDarkTheme) "Aplikacja używa ciemnego motywu." else "Aplikacja używa jasnego motywu.")
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = onDarkThemeChange)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Spiżarnia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Zarządzaj składnikami, które zwykle masz w domu.")
                    Button(onClick = onOpenPantry) { Text("Otwórz spiżarnię") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Niechciane", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Zarządzaj składnikami, których aplikacja ma zawsze unikać w wynikach.")
                    Button(onClick = onOpenPermanentExclusions) { Text("Otwórz niechciane") }
                }
            }
        }
    }
}
