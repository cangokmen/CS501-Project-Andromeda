package com.example.andromeda.services

import com.example.andromeda.data.UserPreferencesRepository
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WearableDataListenerService : WearableListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var wellnessRepository: WellnessDataRepository
    private lateinit var userPrefsRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        wellnessRepository = WellnessDataRepository(application)
        userPrefsRepository = UserPreferencesRepository(application)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/wellness_data") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                println("PHONE: Received DataMap: $dataMap")

                serviceScope.launch {
                    val currentUserEmail = userPrefsRepository.userEmail.first()
                    val timestamp = dataMap.getLong("KEY_TIMESTAMP")

                    // Helper function to convert 0 to null
                    fun getNullableInt(key: String): Int? {
                        val value = dataMap.getInt(key)
                        return if (value == 0) null else value
                    }

                    // --- THIS BLOCK IS NOW SIMPLIFIED ---
                    // Reconstruct the WellnessData object.
                    // The watch now guarantees all keys (Q1-Q5) are present.
                    val newEntry = WellnessData(
                        userEmail = currentUserEmail,
                        weight = dataMap.getDouble("KEY_WEIGHT"),
                        timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp)),

                        // Use the helper to convert any '0' ratings to 'null'
                        dietRating = getNullableInt("KEY_Q1"),
                        activityLevel = getNullableInt("KEY_Q2"),
                        sleepHours = getNullableInt("KEY_Q3"),
                        waterIntake = getNullableInt("KEY_Q4"),
                        proteinIntake = getNullableInt("KEY_Q5")
                    )

                    println("PHONE: Reconstructed WellnessData: $newEntry")
                    wellnessRepository.addWellnessData(newEntry)
                    println("PHONE: Saved wellness data from watch to local database.")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
