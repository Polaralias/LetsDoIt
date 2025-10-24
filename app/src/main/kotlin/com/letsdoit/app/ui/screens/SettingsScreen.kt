package com.letsdoit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.R
import com.letsdoit.app.ui.theme.CardFamily
import com.letsdoit.app.ui.theme.PaletteFamily
import com.letsdoit.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val clickUpToken by viewModel.clickUpToken.collectAsState()
    val openAiKey by viewModel.openAiKey.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val accentPacks by viewModel.accentPacks.collectAsState()
    val presets = viewModel.presets

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(id = R.string.nav_settings), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = clickUpToken,
            onValueChange = viewModel::onClickUpTokenChanged,
            label = { Text(text = stringResource(id = R.string.label_clickup_token)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(autoCorrect = false)
        )
        Button(onClick = { viewModel.saveClickUpToken() }) {
            Text(text = stringResource(id = R.string.action_save))
        }
        OutlinedTextField(
            value = openAiKey,
            onValueChange = viewModel::onOpenAiKeyChanged,
            label = { Text(text = stringResource(id = R.string.label_openai_key)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(autoCorrect = false)
        )
        Button(onClick = { viewModel.saveOpenAiKey() }) {
            Text(text = stringResource(id = R.string.action_save))
        }
        Text(text = stringResource(id = R.string.label_theme), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(id = R.string.label_theme_presets), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                FilterChip(
                    selected = preset.key == preferences.themePreset,
                    onClick = { viewModel.selectPreset(preset.key) },
                    label = { Text(text = preset.label) }
                )
            }
        }
        Text(text = stringResource(id = R.string.label_custom_theme), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(id = R.string.label_shape), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardFamily.entries.forEach { shape ->
                FilterChip(
                    selected = shape == theme.cardFamily,
                    onClick = { viewModel.setCardFamily(shape) },
                    label = { Text(text = shape.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }) }
                )
            }
        }
        Text(text = stringResource(id = R.string.label_palette), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PaletteFamily.entries.forEach { palette ->
                FilterChip(
                    selected = palette == theme.paletteFamily,
                    onClick = { viewModel.setPaletteFamily(palette) },
                    label = { Text(text = palette.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(id = R.string.label_dynamic_colour), style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = theme.dynamicColour,
                onCheckedChange = { viewModel.setDynamicColour(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
            )
        }
        Text(text = stringResource(id = R.string.label_accent_pack), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val neutralLabel = stringResource(id = R.string.label_neutral_pack)
            FilterChip(
                selected = theme.accentPackId == null,
                onClick = { viewModel.setAccentPack(null) },
                label = { Text(text = neutralLabel) }
            )
            accentPacks.forEach { pack ->
                FilterChip(
                    selected = pack.id == theme.accentPackId,
                    onClick = { viewModel.setAccentPack(pack.id) },
                    label = { Text(text = pack.label) }
                )
            }
        }
    }
}
