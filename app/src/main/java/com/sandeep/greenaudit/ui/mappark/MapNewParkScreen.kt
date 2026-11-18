package com.sandeep.greenaudit.ui.mappark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sandeep.greenaudit.data.local.entity.ParkType
import com.sandeep.greenaudit.domain.gis.AreaUnits
import com.sandeep.greenaudit.ui.parks.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapNewParkScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MapNewParkViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map a new park") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            MapDrawingView(
                corners = viewModel.corners,
                onMapTapped = viewModel::onMapTapped,
                onCornerDragged = viewModel::onCornerDragged,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (viewModel.corners.isEmpty()) "Tap to add corners" else "${viewModel.corners.size} corners",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Area: %,.0f m² (%.2f acres)".format(
                        viewModel.areaSqMeters,
                        AreaUnits.squareMetersToAcres(viewModel.areaSqMeters),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Perimeter: %,.0f m".format(viewModel.perimeterMeters),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Drag a corner to adjust",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = viewModel.parkName,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Park name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                ParkTypeDropdown(
                    selected = viewModel.parkType,
                    onSelected = viewModel::onTypeChange,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::undoLastCorner,
                        enabled = viewModel.corners.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Undo")
                    }
                    Button(
                        onClick = { viewModel.save(onSaved) },
                        enabled = viewModel.canSave,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save park")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParkTypeDropdown(selected: ParkType, onSelected: (ParkType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ParkType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label()) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}
