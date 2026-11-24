package com.levelup.gym.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.data.Units

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: GymViewModel, onBack: () -> Unit) {
    val units by vm.units.collectAsStateWithLifecycle()
    val heroName by vm.heroName.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(heroName) { if (name.isEmpty()) name = heroName }

    Column {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GameCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hero name", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(name, { name = it }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Button(onClick = { vm.renameHero(name) }, enabled = name.isNotBlank() && name.trim() != heroName) {
                        Text("Save name")
                    }
                }
            }
            GameCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Units", style = MaterialTheme.typography.titleMedium)
                    val options = listOf(Units.METRIC to "Kilograms (kg, km)", Units.IMPERIAL to "Pounds (lb, mi)")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, (u, label) ->
                            SegmentedButton(
                                selected = units == u,
                                onClick = { vm.setUnits(u) },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            ) { Text(label) }
                        }
                    }
                    Text(
                        "Everything is stored in kg, so switching never changes your history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GameColors.Muted,
                    )
                }
            }
        }
    }
}
