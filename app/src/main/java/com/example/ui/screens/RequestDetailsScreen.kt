package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistanceRequest
import com.example.model.AssistanceStatus
import com.example.ui.components.StatusBadge
import com.example.ui.theme.PinkLineAccent
import com.example.ui.theme.PinkLinePrimary
import com.example.ui.theme.PinkLinePrimaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    request: AssistanceRequest,
    currentStation: String,
    onBack: () -> Unit,
    onAcknowledge: (String) -> Unit,
    onAdvanceTrain: (String) -> Unit,
    onMarkCompleted: (String, String) -> Unit,
    onPlayAnnouncement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }
    var completionNotes by remember { mutableStateOf("") }
    var showCompletionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "REQUEST DETAILS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = request.requestId,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("details_back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onPlayAnnouncement) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "TTS", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PinkLinePrimaryDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        modifier = modifier.testTag("request_details_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PinkLinePrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = request.requestId,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = PinkLinePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        StatusBadge(status = request.status)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SOURCE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                text = request.sourceStation,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PinkLinePrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("→", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PinkLinePrimary)
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("DESTINATION", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                text = request.destinationStation,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TRAIN ID / SET", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("Train Set ${request.trainId}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("PASSENGERS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${request.passengerCount} ${request.passengerType.displayName}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("DIRECTION", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(request.direction.shortLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 12: Visual Step-by-Step Lifecycle Timeline
            Text(
                text = "OPERATIONAL TIMELINE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TimelineItem(
                        stepNumber = "1",
                        title = "Request Created",
                        description = "Originating at ${request.sourceStation}",
                        timestamp = dateFormatter.format(Date(request.createdAt)),
                        isDone = true,
                        isCurrent = false
                    )

                    TimelineItem(
                        stepNumber = "2",
                        title = "Request Sent",
                        description = "Broadcast to ${request.destinationStation} Terminal",
                        timestamp = dateFormatter.format(Date(request.createdAt)),
                        isDone = true,
                        isCurrent = request.status == AssistanceStatus.REQUEST_SENT
                    )

                    TimelineItem(
                        stepNumber = "3",
                        title = "Destination Acknowledged",
                        description = if (request.acknowledgedBy != null) "Confirmed by ${request.acknowledgedBy}" else "Awaiting station staff confirmation",
                        timestamp = request.acknowledgedAt?.let { dateFormatter.format(Date(it)) } ?: "Pending",
                        isDone = request.acknowledgedAt != null,
                        isCurrent = request.status == AssistanceStatus.ACKNOWLEDGED
                    )

                    TimelineItem(
                        stepNumber = "4",
                        title = "Train Journey En-Route",
                        description = "${request.stationsRemaining} stations remaining out of ${request.totalStationsDistance}",
                        timestamp = if (request.stationsRemaining <= 2) "Passed" else "In Transit",
                        isDone = request.stationsRemaining <= 2,
                        isCurrent = request.status == AssistanceStatus.ACKNOWLEDGED && request.stationsRemaining > 2
                    )

                    TimelineItem(
                        stepNumber = "5",
                        title = "Two Stations Before Reminder",
                        description = "Automated Siren + Voice Broadcast triggered",
                        timestamp = if (request.twoStationAlertTriggered) "Triggered" else "Pending (${request.stationsRemaining} stns away)",
                        isDone = request.twoStationAlertTriggered,
                        isCurrent = request.status == AssistanceStatus.TWO_STATION_REMINDER
                    )

                    TimelineItem(
                        stepNumber = "6",
                        title = "Final Reminder (Platform Approaching)",
                        description = "Train Set ${request.trainId} arriving at platform",
                        timestamp = if (request.arrivalAlertTriggered) "Train at Platform" else "Awaiting arrival",
                        isDone = request.arrivalAlertTriggered,
                        isCurrent = request.status == AssistanceStatus.ARRIVING
                    )

                    TimelineItem(
                        stepNumber = "7 & 8",
                        title = "Passenger Assistance & Completed",
                        description = if (request.completedBy != null) "Assistance provided by ${request.completedBy}" else "Staff escorting passenger",
                        timestamp = request.completedAt?.let { dateFormatter.format(Date(it)) } ?: "In Progress",
                        isDone = request.status == AssistanceStatus.COMPLETED,
                        isCurrent = request.status == AssistanceStatus.COMPLETED,
                        isLast = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            if (request.status == AssistanceStatus.REQUEST_SENT) {
                Button(
                    onClick = { onAcknowledge(request.requestId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("details_acknowledge_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ACKNOWLEDGE REQUEST", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (request.status != AssistanceStatus.COMPLETED) {
                // Complete Request Form / Action
                Button(
                    onClick = { onMarkCompleted(request.requestId, completionNotes) },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkLinePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("details_mark_completed_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MARK AS COMPLETED", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test Train Step advance button
                OutlinedButton(
                    onClick = { onAdvanceTrain(request.requestId) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("details_advance_train_button")
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = PinkLinePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Advance Train Progress (Step Station)", fontWeight = FontWeight.Bold, color = PinkLinePrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TimelineItem(
    stepNumber: String,
    title: String,
    description: String,
    timestamp: String,
    isDone: Boolean,
    isCurrent: Boolean,
    isLast: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = when {
                            isDone -> Color(0xFF16A34A)
                            isCurrent -> PinkLinePrimary
                            else -> Color(0xFFCBD5E1)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = stepNumber,
                        color = if (isCurrent) Color.White else Color.DarkGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(if (isDone) Color(0xFF16A34A) else Color(0xFFE2E8F0))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isCurrent) PinkLinePrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timestamp,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
