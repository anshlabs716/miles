package com.example.miles.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.repository.MilesRepository
import com.example.miles.ui.history.HistoryScreen
import com.example.miles.ui.stats.StatisticsScreen

@Composable
fun JournalScreen(
    activities: List<ActivityEntity>,
    repository: MilesRepository? = null,
    onSelectActivity: (ActivityEntity) -> Unit,
    onToggleFavorite: ((ActivityEntity) -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showImportDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Workouts", "Trends & Records")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium), color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        )
                    }
                }
            }
            if (repository != null) {
                Spacer(Modifier.width(10.dp))
                FilledTonalButton(onClick = { showImportDialog = true }) {
                    Icon(Icons.Default.FileUpload, "Import", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> HistoryScreen(activities = activities, onSelectActivity = onSelectActivity, onToggleFavorite = onToggleFavorite)
                1 -> StatisticsScreen(activities = activities)
            }
        }
    }

    if (showImportDialog && repository != null) {
        ImportFitnessDataDialog(repository = repository, onDismiss = { showImportDialog = false }, onImportSuccess = { })
    }
}
