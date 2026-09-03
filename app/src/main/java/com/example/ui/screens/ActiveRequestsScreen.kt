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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistanceRequest
import com.example.model.AssistanceStatus
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MetroBlue
import com.example.ui.theme.PinkLineAccent
import com.example.ui.theme.PinkLinePrimary
import com.example.ui.theme.PinkLinePrimaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRequestsScreen(
    currentStationName: String,
    activeRequests: List<AssistanceRequest>,
    onSelectRequest: (AssistanceRequest) -> Unit,
    onAcknowledgeRequest: (String) -> Unit,
    onAdvanceTrainProgress: (String) -> Unit,
    onMarkCompleted: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRequests = remember(activeRequests, selectedTab, searchQuery, currentStationName) {
        val tabFiltered = when (selectedTab) {
            0 -> activeRequests // All active
            1 -> activeRequests.filter { it.destinationStation.equals(currentStationName, ignoreCase = true) } // Incoming
            2 -> activeRequests.filter { it.sourceStation.equals(currentStationName, ignoreCase = true) } // Outgoing
            else -> activeRequests
        }

        if (searchQuery.isBlank()) tabFiltered
        else tabFiltered.filter {
            it.requestId.contains(searchQuery, ignoreCase = true) ||
                    it.trainId.contains(searchQuery, ignoreCase = true) ||
                    it.sourceStation.contains(searchQuery, ignoreCase = true) ||
                    it.destinationStation.contains(searchQuery, ignoreCase = true)
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ACTIVE ASSISTANCE REQUESTS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${activeRequests.size} Live on Pink Line",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("active_requests_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PinkLinePrimaryDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        modifier = modifier.testTag("active_requests_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tabs: All Active, Incoming to Me, Outgoing from Me
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = PinkLinePrimaryDark,
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All (${activeRequests.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
                val incomingCount = activeRequests.count { it.destinationStation.equals(currentStationName, ignoreCase = true) }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Incoming ($incomingCount)", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
                val outgoingCount = activeRequests.count { it.sourceStation.equals(currentStationName, ignoreCase = true) }
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Outgoing ($outgoingCount)", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Train Set, Request ID, or Station...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filteredRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsTransit,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Active Requests Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "Assistance requests in transit or awaiting acknowledgment will appear here.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRequests, key = { it.requestId }) { req ->
                        val isDestinationDevice = req.destinationStation.equals(currentStationName, ignoreCase = true)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isDestinationDevice && req.status == AssistanceStatus.REQUEST_SENT) 2.dp else 1.dp,
                                    color = if (isDestinationDevice && req.status == AssistanceStatus.REQUEST_SENT) PinkLinePrimary else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectRequest(req) }
                                .testTag("active_request_card_${req.requestId}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Header: ID, Train Set, Status Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = PinkLinePrimary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = req.requestId,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = PinkLinePrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = Color(0xFF0F172A),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "TRAIN SET ${req.trainId}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFFFD54F),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    StatusBadge(status = req.status)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Source and Destination Stations Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "SOURCE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = req.sourceStation,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0xFFF1F5F9), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "→", fontWeight = FontWeight.Black, color = PinkLinePrimary)
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                        Text(text = "DESTINATION", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = req.destinationStation,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Passenger Type and Direction Details
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = req.passengerType.icon,
                                            contentDescription = null,
                                            tint = req.passengerType.cardColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${req.passengerCount} ${req.passengerType.displayName}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = req.passengerType.cardColor
                                        )
                                    }

                                    Text(
                                        text = req.direction.shortLabel,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Journey station progress bar
                                Spacer(modifier = Modifier.height(10.dp))
                                val progress = if (req.totalStationsDistance > 0) {
                                    1f - (req.stationsRemaining.toFloat() / req.totalStationsDistance.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (req.stationsRemaining <= 0) "Arriving Platform" else "${req.stationsRemaining} stations away",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (req.stationsRemaining <= 2) Color(0xFFDC2626) else PinkLinePrimary
                                        )
                                        Text(
                                            text = "Created ${timeFormatter.format(Date(req.createdAt))}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (req.stationsRemaining <= 2) Color(0xFFDC2626) else PinkLinePrimary,
                                        trackColor = Color(0xFFE2E8F0)
                                    )
                                }

                                // Operational Action Buttons
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Acknowledge button if this is destination device and pending
                                    if (req.status == AssistanceStatus.REQUEST_SENT) {
                                        Button(
                                            onClick = { onAcknowledgeRequest(req.requestId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("inline_acknowledge_button_${req.requestId}")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("ACKNOWLEDGE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (req.status != AssistanceStatus.COMPLETED) {
                                        // Mark Completed Button
                                        Button(
                                            onClick = { onMarkCompleted(req.requestId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .testTag("inline_complete_button_${req.requestId}")
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("MARK COMPLETED", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Simulation Step Button: "Advance Station" (Allows immediate testing of 2-station & arrival alarms)
                                    OutlinedButton(
                                        onClick = { onAdvanceTrainProgress(req.requestId) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("advance_train_step_button_${req.requestId}")
                                    ) {
                                        Icon(Icons.Default.FastForward, contentDescription = null, tint = PinkLinePrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Step Train", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PinkLinePrimary)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
