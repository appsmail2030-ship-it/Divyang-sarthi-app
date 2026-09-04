package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.example.model.AssistanceStatus
import com.example.model.NetworkStatus
import com.example.model.PassengerType
import com.example.model.Station
import com.example.ui.theme.PinkLineAccent
import com.example.ui.theme.PinkLinePrimary
import com.example.ui.theme.PinkLinePrimaryDark
import com.example.ui.theme.StatusAcknowledged
import com.example.ui.theme.StatusArriving
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusRequestSent
import com.example.ui.theme.StatusTwoStationAlert

@Composable
fun StatusBadge(
    status: AssistanceStatus,
    modifier: Modifier = Modifier
) {
    val (icon, bg, fg) = when (status) {
        AssistanceStatus.REQUEST_SENT -> Triple(Icons.Default.Warning, StatusRequestSent.copy(alpha = 0.15f), StatusRequestSent)
        AssistanceStatus.ACKNOWLEDGED -> Triple(Icons.Default.CheckCircle, StatusAcknowledged.copy(alpha = 0.15f), StatusAcknowledged)
        AssistanceStatus.TWO_STATION_REMINDER -> Triple(Icons.Default.NotificationsActive, StatusTwoStationAlert.copy(alpha = 0.15f), StatusTwoStationAlert)
        AssistanceStatus.ARRIVING -> Triple(Icons.Default.DirectionsTransit, StatusArriving.copy(alpha = 0.15f), StatusArriving)
        AssistanceStatus.COMPLETED -> Triple(Icons.Default.CheckCircle, StatusCompleted.copy(alpha = 0.15f), StatusCompleted)
        AssistanceStatus.CANCELLED -> Triple(Icons.Default.Warning, Color.Gray.copy(alpha = 0.15f), Color.DarkGray)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .testTag("status_badge_${status.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.displayName,
                color = fg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun NetworkIndicatorPill(
    status: NetworkStatus,
    modifier: Modifier = Modifier
) {
    val (label, dotColor, icon) = when (status) {
        NetworkStatus.ONLINE -> Triple("ONLINE", Color(0xFF16A34A), Icons.Default.CloudQueue)
        NetworkStatus.OFFLINE -> Triple("OFFLINE", Color(0xFFDC2626), Icons.Default.CloudOff)
        NetworkStatus.SYNCING -> Triple("SYNCING", Color(0xFFD97706), Icons.Default.CloudSync)
    }

    Surface(
        color = dotColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, dotColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (status == NetworkStatus.SYNCING) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = dotColor,
                    modifier = Modifier.size(10.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = dotColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun StationHeaderStrip(
    stationName: String,
    networkStatus: NetworkStatus,
    isAdmin: Boolean = false,
    stations: List<Station> = emptyList(),
    onSwitchStation: ((String) -> Unit)? = null,
    onLogout: () -> Unit
) {
    var showSwitchDialog by remember { mutableStateOf(false) }

    Surface(
        color = PinkLinePrimaryDark,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onSwitchStation != null) Modifier.clickable { showSwitchDialog = true }
                            else Modifier
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsTransit,
                            contentDescription = "Metro",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DMRC DIVYA • PINK LINE",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stationName.uppercase(),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (onSwitchStation != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "Switch Station",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "SWITCH",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (isAdmin) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFFFD54F),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = Color(0xFF4A148C),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    NetworkIndicatorPill(status = networkStatus)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showSwitchDialog && onSwitchStation != null) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(stations, query) {
            if (query.isBlank()) stations
            else stations.filter {
                it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true)
            }
        }

        Dialog(onDismissRequest = { showSwitchDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .border(1.dp, PinkLinePrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .testTag("quick_station_switch_dialog")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quick Switch Active Station",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current: $stationName",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { showSwitchDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search 38 Pink Line Stations...", color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PinkLinePrimary,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.code }) { stn ->
                            val isSelected = stn.name.equals(stationName, ignoreCase = true)
                            Surface(
                                color = if (isSelected) PinkLinePrimary.copy(alpha = 0.35f) else Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) PinkLinePrimary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onSwitchStation(stn.name)
                                        showSwitchDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = stn.name,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Code: ${stn.code} • Stn #${stn.sequenceNumber}",
                                            color = Color.LightGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "ACTIVE",
                                            color = Color(0xFF4ADE80),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    count: Int,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format("%02d", count),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }
    }
}

@Composable
fun PassengerCategoryButton(
    type: PassengerType,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = type.cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("category_button_${type.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = type.displayName,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "→",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
