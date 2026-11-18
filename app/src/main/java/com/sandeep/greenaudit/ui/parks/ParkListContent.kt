package com.sandeep.greenaudit.ui.parks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.ParkStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkListContent(
    parks: List<ParkEntity>,
    searchQuery: String,
    statusFilter: ParkStatus?,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (ParkStatus?) -> Unit,
    onParkClick: (ParkEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search parks") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { onStatusFilterChange(null) },
                label = { Text("All") },
            )
            FilterChip(
                selected = statusFilter == ParkStatus.OPEN,
                onClick = { onStatusFilterChange(ParkStatus.OPEN) },
                label = { Text("Open") },
            )
            FilterChip(
                selected = statusFilter == ParkStatus.NEEDS_REPAIR,
                onClick = { onStatusFilterChange(ParkStatus.NEEDS_REPAIR) },
                label = { Text("Repair") },
            )
            FilterChip(
                selected = statusFilter == ParkStatus.CLOSED,
                onClick = { onStatusFilterChange(ParkStatus.CLOSED) },
                label = { Text("Closed") },
            )
        }

        if (parks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No parks match your search",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)) {
                items(parks, key = { it.id }) { park ->
                    ParkListItem(park = park, onClick = { onParkClick(park) })
                }
            }
        }
    }
}
