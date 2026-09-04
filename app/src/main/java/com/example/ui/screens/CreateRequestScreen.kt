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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AssistanceRequest
import com.example.model.Direction
import com.example.model.PassengerType
import com.example.model.Station
import com.example.model.SystemConfig
import com.example.ui.theme.PinkLineAccent
import com.example.ui.theme.PinkLinePrimary
import com.example.ui.theme.PinkLinePrimaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    sourceStationName: String,
    initialPassengerType: PassengerType,
    allStations: List<Station>,
    systemConfig: SystemConfig,
    onBack: () -> Unit,
    onSubmitRequest: (
        destinationStationName: String,
        passengerType: PassengerType,
        passengerCount: Int,
        trainId: String,
        direction: Direction,
        notes: String,
        onSuccess: (AssistanceRequest) -> Unit
    ) -> Unit,
    onRequestCreated: (AssistanceRequest) -> Unit,
    onSwitchToDestinationAndAcknowledge: ((AssistanceRequest) -> Unit)? = null,
    onSimulateAlertNow: ((AssistanceRequest) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedPassengerType by remember { mutableStateOf(initialPassengerType) }
    var passengerCount by remember { mutableIntStateOf(1) }
    var trainId by remember { mutableStateOf("47") }
    var selectedDirection by remember { mutableStateOf(Direction.PLUS_CIRCULAR) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var deliveredRequest by remember { mutableStateOf<AssistanceRequest?>(null) }

    // Destination station list excluding source station
    val eligibleDestinations = remember(allStations, sourceStationName) {
        allStations.filter { !it.name.equals(sourceStationName, ignoreCase = true) }
    }

    var selectedDestination by remember(eligibleDestinations) {
        mutableStateOf(
            eligibleDestinations.find { it.name.contains("Trilokpuri", ignoreCase = true) }?.name
                ?: (eligibleDestinations.firstOrNull()?.name ?: "Trilokpuri Sanjay Lake")
        )
    }

    var showDestinationDialog by remember { mutableStateOf(false) }

    // Calculate station distance and travel time preview
    val journeyCalculation by remember(sourceStationName, selectedDestination, selectedDirection, allStations, systemConfig) {
        derivedStateOf {
            val src = allStations.find { it.name == sourceStationName }
            val dst = allStations.find { it.name == selectedDestination }
            if (src != null && dst != null) {
                val srcSeq = src.sequenceNumber
                val dstSeq = dst.sequenceNumber
                val distance = when (selectedDirection) {
                    Direction.PLUS_CIRCULAR -> {
                        if (dstSeq >= srcSeq) dstSeq - srcSeq else (37 - srcSeq) + dstSeq
                    }
                    Direction.MINUS_CIRCULAR -> {
                        if (srcSeq >= dstSeq) srcSeq - dstSeq else srcSeq + (37 - dstSeq)
                    }
                }.coerceAtLeast(1)

                val estTime = distance * systemConfig.averageTravelTimeMinutes
                Pair(distance, estTime)
            } else {
                Pair(2, 6.0)
            }
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CREATE ASSISTANCE REQUEST",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Pink Line Inter-Station Alert",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("create_request_back_button")
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
        modifier = modifier.testTag("create_request_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // A. PASSENGER TYPE (Pre-selected from button, but selectable)
            Text(
                text = "A. PASSENGER CATEGORY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PassengerType.values().forEach { type ->
                    val isSelected = (selectedPassengerType == type)
                    Surface(
                        color = if (isSelected) type.cardColor else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) type.cardColor else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedPassengerType = type }
                            .testTag("passenger_type_tab_${type.name}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = type.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (type) {
                                    PassengerType.VISUALLY_IMPAIRED -> "Visually Impaired"
                                    PassengerType.WHEELCHAIR -> "Wheelchair"
                                    PassengerType.OTHER -> "Other"
                                },
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // B. SOURCE STATION (READ ONLY)
            Text(
                text = "B. SOURCE STATION (READ ONLY)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    .testTag("source_station_readonly")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = PinkLinePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Originating Station (Fixed to device login)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = sourceStationName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // C. DESTINATION STATION (Dropdown / Searchable, cannot be same as source)
            Text(
                text = "C. DESTINATION STATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, PinkLinePrimary.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .clickable { showDestinationDialog = true }
                    .testTag("destination_station_picker")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PinkLinePrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsTransit,
                                contentDescription = null,
                                tint = PinkLinePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Arrival Station",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = selectedDestination,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select",
                        tint = PinkLinePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // D. NUMBER OF PASSENGERS & E. TRAIN ID / TRAIN SET (Side by side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // D. Number of Passengers
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "D. PASSENGERS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            IconButton(
                                onClick = { if (passengerCount > 1) passengerCount-- },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PinkLinePrimary)
                            }
                            Text(
                                text = passengerCount.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { if (passengerCount < 20) passengerCount++ },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = PinkLinePrimary)
                            }
                        }
                    }
                }

                // E. Train ID / Train Set
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "E. TRAIN SET",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = trainId,
                        onValueChange = { trainId = it },
                        placeholder = { Text("Set 47") },
                        label = { Text("Train ID / Set") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PinkLinePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("train_id_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // F. DIRECTION (+ Circular Line / - Circular Line)
            Text(
                text = "F. DIRECTION (SELECT ONE)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // + Circular Line
                val isPlus = (selectedDirection == Direction.PLUS_CIRCULAR)
                Surface(
                    color = if (isPlus) PinkLinePrimary else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = if (isPlus) 4.dp else 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (isPlus) 2.dp else 1.dp,
                            color = if (isPlus) PinkLinePrimary else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedDirection = Direction.PLUS_CIRCULAR }
                        .testTag("direction_plus_circular")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "+ Circular Line",
                                color = if (isPlus) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isPlus) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Majlis Park → Shiv Vihar",
                            color = if (isPlus) Color.White.copy(alpha = 0.8f) else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // - Circular Line
                val isMinus = (selectedDirection == Direction.MINUS_CIRCULAR)
                Surface(
                    color = if (isMinus) PinkLinePrimary else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = if (isMinus) 4.dp else 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = if (isMinus) 2.dp else 1.dp,
                            color = if (isMinus) PinkLinePrimary else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedDirection = Direction.MINUS_CIRCULAR }
                        .testTag("direction_minus_circular")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "- Circular Line",
                                color = if (isMinus) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isMinus) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Shiv Vihar → Majlis Park",
                            color = if (isMinus) Color.White.copy(alpha = 0.8f) else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Journey Time Calculation Preview Card (Section 7)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PinkLineAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = PinkLinePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ESTIMATED JOURNEY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PinkLinePrimary
                            )
                            Text(
                                text = "${journeyCalculation.first} Stations Distance",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "~${journeyCalculation.second.toInt()} Minutes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = PinkLinePrimary
                        )
                        Text(
                            text = "@ ${systemConfig.averageTravelTimeMinutes} min/station",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Operational Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Special Staff Notes (Optional: Coach #, platform notes)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PinkLinePrimary,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // G. CREATE REQUEST BUTTON
            Button(
                onClick = {
                    if (trainId.isBlank()) {
                        errorMessage = "Please specify the Train ID / Train Set"
                        return@Button
                    }
                    if (selectedDestination == sourceStationName) {
                        errorMessage = "Destination station cannot be the same as Source station"
                        return@Button
                    }
                    isSubmitting = true
                    errorMessage = null
                    onSubmitRequest(
                        selectedDestination,
                        selectedPassengerType,
                        passengerCount,
                        trainId,
                        selectedDirection,
                        notes
                    ) { createdRequest ->
                        isSubmitting = false
                        deliveredRequest = createdRequest
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkLinePrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_create_request_button")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSubmitting) "TRANSMITTING TO DESTINATION..." else "CREATE ASSISTANCE REQUEST",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Destination station selection dialog
    if (showDestinationDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredStations = remember(eligibleDestinations, searchQuery) {
            if (searchQuery.isBlank()) eligibleDestinations
            else eligibleDestinations.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
            }
        }

        Dialog(onDismissRequest = { showDestinationDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .border(1.dp, PinkLinePrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Destination Station",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showDestinationDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Destination Stations...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredStations) { station ->
                            Surface(
                                color = if (station.name == selectedDestination) PinkLinePrimary.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDestination = station.name
                                        showDestinationDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = station.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        if (station.interchanges.isNotEmpty()) {
                                            Text(
                                                text = "Interchange: " + station.interchanges.joinToString(", "),
                                                color = PinkLineAccent,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFF334155),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = station.code,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }

    // Request Dispatched & Delivered Confirmation Modal
    deliveredRequest?.let { req ->
        Dialog(onDismissRequest = { /* Staff decision required */ }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF16A34A), RoundedCornerShape(20.dp))
                    .testTag("delivery_confirmation_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF16A34A).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Delivered",
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "REQUEST DISPATCHED & DELIVERED",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    Surface(
                        color = Color(0xFF22C55E).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = "DELIVERY CONFIRMED TO DESTINATION TERMINAL",
                            color = Color(0xFF4ADE80),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Request receipt details card
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("REQUEST ID", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(req.requestId, color = PinkLinePrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("DESTINATION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(req.destinationStation, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TRAIN / DIRECTION", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("Train Set ${req.trainId} • ${req.direction.shortLabel}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("PASSENGER", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${req.passengerCount} ${req.passengerType.displayName}", color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("ESTIMATED ARRIVAL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("~${journeyCalculation.second.toInt()} mins (${journeyCalculation.first} stations away)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Option 1: Switch Station to Destination to View Alert & Acknowledge
                    if (onSwitchToDestinationAndAcknowledge != null) {
                        Button(
                            onClick = {
                                deliveredRequest = null
                                onSwitchToDestinationAndAcknowledge(req)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PinkLinePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("delivery_switch_station_button")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Switch to ${req.destinationStation} & Acknowledge",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Option 2: Test Destination Alert on this Device Now
                    if (onSimulateAlertNow != null) {
                        OutlinedButton(
                            onClick = {
                                deliveredRequest = null
                                onSimulateAlertNow(req)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("delivery_simulate_alert_button")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Alert Overlay on this Device", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Option 3: Continue to Active Requests
                    Button(
                        onClick = {
                            deliveredRequest = null
                            onRequestCreated(req)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF334155),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("delivery_done_button")
                    ) {
                        Text("View in Active Requests List", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
