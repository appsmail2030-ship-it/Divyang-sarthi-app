package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistanceRequest
import com.example.model.AssistanceStatus
import com.example.model.NetworkStatus
import com.example.model.PassengerType
import com.example.model.Station
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.PassengerCategoryButton
import com.example.ui.components.StationHeaderStrip
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MetroBlue
import com.example.ui.theme.PinkLineAccent
import com.example.ui.theme.PinkLinePrimary
import com.example.ui.theme.StatusAcknowledged
import com.example.ui.theme.StatusRequestSent
import com.example.ui.theme.StatusTwoStationAlert
import com.example.ui.viewmodel.DashboardMetrics

@Composable
fun HomeScreen(
    stationName: String,
    networkStatus: NetworkStatus,
    metrics: DashboardMetrics,
    activeRequests: List<AssistanceRequest>,
    isAdmin: Boolean,
    stations: List<Station> = emptyList(),
    onSwitchStation: ((String) -> Unit)? = null,
    onCategorySelected: (PassengerType) -> Unit,
    onNavigateToActiveRequests: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSelectRequest: (AssistanceRequest) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Find if there is any pending incoming request for this station
    val incomingPending = activeRequests.find {
        it.destinationStation.equals(stationName, ignoreCase = true) &&
                it.status == AssistanceStatus.REQUEST_SENT
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen")
    ) {
        // Station Header
        StationHeaderStrip(
            stationName = stationName,
            networkStatus = networkStatus,
            isAdmin = isAdmin,
            stations = stations,
            onSwitchStation = onSwitchStation,
            onLogout = onLogout
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Dashboard Summary Metrics Bar (Section 21)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSummaryCard(
                    title = "Active",
                    count = metrics.activeAssistanceCount,
                    accentColor = MetroBlue,
                    icon = Icons.Default.ListAlt,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToActiveRequests
                )
                MetricSummaryCard(
                    title = "Pending Ack",
                    count = metrics.pendingAcknowledgementCount,
                    accentColor = StatusRequestSent,
                    icon = Icons.Default.Warning,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToActiveRequests
                )
                MetricSummaryCard(
                    title = "Upcoming",
                    count = metrics.upcomingArrivalsCount,
                    accentColor = StatusTwoStationAlert,
                    icon = Icons.Default.NotificationsActive,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToActiveRequests
                )
            }

            // Urgent incoming alert banner if any
            if (incomingPending != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFFDC2626), RoundedCornerShape(12.dp))
                        .clickable { onSelectRequest(incomingPending) }
                        .testTag("urgent_incoming_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFDC2626), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ACTION REQUIRED: INCOMING REQUEST",
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Train Set ${incomingPending.trainId} from ${incomingPending.sourceStation} (${incomingPending.passengerType.displayName})",
                                color = Color(0xFF1E293B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "VIEW",
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Three large passenger assistance cards
            Text(
                text = "NEW ASSISTANCE REQUEST",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            PassengerCategoryButton(
                type = PassengerType.VISUALLY_IMPAIRED,
                subtitle = "Assistance for blind / low vision passengers",
                onClick = { onCategorySelected(PassengerType.VISUALLY_IMPAIRED) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PassengerCategoryButton(
                type = PassengerType.WHEELCHAIR,
                subtitle = "Wheelchair ramp & platform escort assistance",
                onClick = { onCategorySelected(PassengerType.WHEELCHAIR) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PassengerCategoryButton(
                type = PassengerType.OTHER,
                subtitle = "Elderly, injured, or special assistance",
                onClick = { onCategorySelected(PassengerType.OTHER) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Operations Navigation Strip
            Text(
                text = "STATION OPERATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active Requests Button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToActiveRequests() }
                        .testTag("nav_active_requests_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ListAlt,
                            contentDescription = null,
                            tint = PinkLinePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Active Requests",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${activeRequests.size} Live",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // History Button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToHistory() }
                        .testTag("nav_history_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MetroBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Request History",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Logs & Filters",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Admin Panel Access button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAdmin() }
                    .testTag("nav_admin_panel_button")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Admin Control & Settings",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Station sequences, passwords, travel times & audio tests",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Text(text = "⚙️", fontSize = 18.sp)
                }
            }

            // Recent Active Requests preview snippet
            if (activeRequests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE ACTIVE REQUESTS (${activeRequests.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "View All →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PinkLinePrimary,
                        modifier = Modifier.clickable { onNavigateToActiveRequests() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                activeRequests.take(3).forEach { req ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectRequest(req) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = req.requestId,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PinkLinePrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Set ${req.trainId}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${req.sourceStation} → ${req.destinationStation}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${req.passengerCount} ${req.passengerType.displayName} • ${req.direction.shortLabel}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            StatusBadge(status = req.status)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
