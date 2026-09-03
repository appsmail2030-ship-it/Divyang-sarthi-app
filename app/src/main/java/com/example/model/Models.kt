package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.OtherPassengerCard
import com.example.ui.theme.StatusAcknowledged
import com.example.ui.theme.StatusArriving
import com.example.ui.theme.StatusCancelled
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusRequestSent
import com.example.ui.theme.StatusTwoStationAlert
import com.example.ui.theme.VisualImpairedCard
import com.example.ui.theme.WheelchairCard

enum class PassengerType(
    val displayName: String,
    val speechLabel: String,
    val cardColor: Color
) {
    VISUALLY_IMPAIRED(
        displayName = "Visually Impaired",
        speechLabel = "visually impaired",
        cardColor = VisualImpairedCard
    ),
    WHEELCHAIR(
        displayName = "Wheelchair Passenger",
        speechLabel = "wheelchair",
        cardColor = WheelchairCard
    ),
    OTHER(
        displayName = "Other Passenger",
        speechLabel = "requiring assistance",
        cardColor = OtherPassengerCard
    );

    val icon: ImageVector
        get() = when (this) {
            VISUALLY_IMPAIRED -> Icons.Default.Visibility
            WHEELCHAIR -> Icons.Default.Accessible
            OTHER -> Icons.Default.PersonOutline
        }
}

enum class Direction(val displayName: String, val shortLabel: String, val routeSummary: String) {
    PLUS_CIRCULAR(
        displayName = "+ Circular Line",
        shortLabel = "+ Circular",
        routeSummary = "Majlis Park → Shiv Vihar"
    ),
    MINUS_CIRCULAR(
        displayName = "- Circular Line",
        shortLabel = "- Circular",
        routeSummary = "Shiv Vihar → Majlis Park"
    )
}

enum class AssistanceStatus(val displayName: String, val badgeColor: Color) {
    REQUEST_SENT("REQUEST SENT", StatusRequestSent),
    ACKNOWLEDGED("ACKNOWLEDGED", StatusAcknowledged),
    TWO_STATION_REMINDER("TWO-STATION REMINDER", StatusTwoStationAlert),
    ARRIVING("ARRIVING", StatusArriving),
    COMPLETED("COMPLETED", StatusCompleted),
    CANCELLED("CANCELLED", StatusCancelled)
}

enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    SYNC_FAILED
}

enum class NetworkStatus {
    ONLINE,
    OFFLINE,
    SYNCING
}

data class Station(
    val code: String,
    val name: String,
    val sequenceNumber: Int,
    val isActive: Boolean = true,
    val interchanges: List<String> = emptyList()
)

data class AssistanceRequest(
    val requestId: String,
    val sourceStation: String,
    val destinationStation: String,
    val passengerType: PassengerType,
    val passengerCount: Int,
    val trainId: String,
    val direction: Direction,
    val createdAt: Long = System.currentTimeMillis(),
    val status: AssistanceStatus = AssistanceStatus.REQUEST_SENT,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null,
    val twoStationAlertTriggered: Boolean = false,
    val arrivalAlertTriggered: Boolean = false,
    val completedBy: String? = null,
    val completedAt: Long? = null,
    val notes: String = "",
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val totalStationsDistance: Int = 0,
    val stationsRemaining: Int = 0,
    val estimatedArrivalMillis: Long = 0L,
    val currentStationProgress: String = ""
)

data class SystemConfig(
    val commonPassword: String = "12345",
    val adminPassword: String = "admin123",
    val averageTravelTimeMinutes: Double = 3.0,
    val sirenEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val ttsAnnouncementEnabled: Boolean = true,
    val twoStationDistanceThreshold: Int = 2
)

object PinkLineStationsData {
    // Official Delhi Metro Pink Line (Line 7) sequence of 38 stations
    val DEFAULT_STATIONS: List<Station> = listOf(
        Station("MJP", "Majlis Park", 1, true, listOf("Pink Line Terminal")),
        Station("AZP", "Azadpur", 2, true, listOf("Yellow Line")),
        Station("SHB", "Shalimar Bagh", 3, true),
        Station("NSP", "Netaji Subhash Place", 4, true, listOf("Red Line")),
        Station("SHK", "Shakurpur", 5, true),
        Station("PBW", "Punjabi Bagh West", 6, true, listOf("Green Line")),
        Station("ESI", "ESI - Basai Darapur", 7, true),
        Station("RJG", "Rajouri Garden", 8, true, listOf("Blue Line")),
        Station("MYP", "Maya Puri", 9, true),
        Station("NVI", "Naraina Vihar", 10, true),
        Station("DLT", "Delhi Cantt", 11, true),
        Station("DSC", "Durgabai Deshmukh South Campus", 12, true, listOf("Airport Express")),
        Station("MTI", "Sir M. Vishweshwaraiah Moti Bagh", 13, true),
        Station("BCP", "Bhikaji Cama Place", 14, true),
        Station("SJN", "Sarojini Nagar", 15, true),
        Station("INA", "Dilli Haat - INA", 16, true, listOf("Yellow Line")),
        Station("SXN", "South Extension", 17, true),
        Station("LJN", "Lajpat Nagar", 18, true, listOf("Violet Line")),
        Station("VNB", "Vinobapuri", 19, true),
        Station("ASM", "Ashram", 20, true),
        Station("SKK", "Sarai Kale Khan - Nizamuddin", 21, true, listOf("Hazrat Nizamuddin Rly", "RRTS")),
        Station("MV1", "Mayur Vihar Phase-1", 22, true, listOf("Blue Line")),
        Station("MVP", "Mayur Vihar Pocket-1", 23, true),
        Station("TLP", "Trilokpuri Sanjay Lake", 24, true),
        Station("EVN", "East Vinod Nagar - Mayur Vihar-II", 25, true),
        Station("MDW", "Mandawali - West Vinod Nagar", 26, true),
        Station("IPX", "IP Extension", 27, true),
        Station("AVH", "Anand Vihar ISBT", 28, true, listOf("Blue Line", "Indian Railways", "ISBT")),
        Station("KKD", "Karkarduma", 29, true, listOf("Blue Line")),
        Station("KKC", "Karkarduma Court", 30, true),
        Station("KRN", "Krishna Nagar", 31, true),
        Station("EAN", "East Azad Nagar", 32, true),
        Station("WLC", "Welcome", 33, true, listOf("Red Line")),
        Station("JFD", "Jafrabad", 34, true),
        Station("MPB", "Maujpur - Babarpur", 35, true, listOf("Pink Line Junction")),
        Station("GKP", "Gokulpuri", 36, true),
        Station("JRE", "Johri Enclave", 37, true),
        Station("SVH", "Shiv Vihar", 38, true, listOf("Pink Line Terminal"))
    )
}
