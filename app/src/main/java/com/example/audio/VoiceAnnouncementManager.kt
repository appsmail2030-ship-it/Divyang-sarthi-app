package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.model.PassengerType
import java.util.Locale

class VoiceAnnouncementManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceAnnouncement", "English language is not supported or missing data")
            } else {
                tts?.setSpeechRate(0.92f) // Slightly clear, professional announcement pace
                tts?.setPitch(1.0f)
                isInitialized = true
            }
        } else {
            Log.e("VoiceAnnouncement", "Failed to initialize TextToSpeech, status: $status")
        }
    }

    fun speakTwoStationAlert(
        trainId: String,
        passengerCount: Int,
        passengerType: PassengerType,
        destinationStation: String
    ) {
        val passengerPhrase = when (passengerType) {
            PassengerType.VISUALLY_IMPAIRED -> {
                if (passengerCount == 1) "1 visually impaired passenger" else "$passengerCount visually impaired passengers"
            }
            PassengerType.WHEELCHAIR -> {
                if (passengerCount == 1) "1 wheelchair passenger" else "$passengerCount wheelchair passengers"
            }
            PassengerType.OTHER -> {
                if (passengerCount == 1) "1 passenger requiring assistance" else "$passengerCount passengers requiring assistance"
            }
        }

        val arrangePhrase = when (passengerType) {
            PassengerType.VISUALLY_IMPAIRED -> "Please arrange assistance for the visually impaired passengers."
            PassengerType.WHEELCHAIR -> "Please arrange assistance."
            PassengerType.OTHER -> "Please arrange assistance."
        }

        val message = "Attention please. Train Set $trainId carrying $passengerPhrase is now two stations away from $destinationStation Station. $arrangePhrase"
        speak(message)
    }

    fun speakArrivalAlert(trainId: String, destinationStation: String) {
        val message = "Train Set $trainId is approaching $destinationStation Station. Please provide passenger assistance."
        speak(message)
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Log.w("VoiceAnnouncement", "TTS not initialized yet")
            return
        }
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "metro_announcement_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
