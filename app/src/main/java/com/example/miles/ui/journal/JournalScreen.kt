package com.example.miles.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.miles.data.model.ActivityEntity
import com.example.miles.ui.history.HistoryScreen
import com.example.miles.ui.stats.StatisticsScreen

@Composable
fun JournalScreen(
    activities: List<ActivityEntity>,
    onSelectActivity: (ActivityEntity) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Workouts", "Trends & Records")

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> HistoryScreen(
                    activities = activities,
                    onSelectActivity = onSelectActivity
                )
                1 -> StatisticsScreen(
                    activities = activities
                )
            }
        }
    }
}
