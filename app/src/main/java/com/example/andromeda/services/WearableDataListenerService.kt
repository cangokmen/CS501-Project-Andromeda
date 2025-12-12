package com.example.andromeda.services

import androidx.compose.foundation.gestures.forEach
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.key.type
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
                val weight = dataMap.getInt("KEY_WEIGHT")
                val q1 = dataMap.getInt("KEY_Q1")
                val q2 = dataMap.getInt("KEY_Q2")
                val q3 = dataMap.getInt("KEY_Q3")

                saveWellnessData(weight, q1, q2, q3)
            }
        }
    }

    private fun saveWellnessData(weight: Int, q1: Int, q2: Int, q3: Int) {
        serviceScope.launch {
            // Fetch the currently logged-in user's email
            val currentUserEmail = userPrefsRepository.userEmail.first()

            val newEntry = WellnessData(
                userEmail = currentUserEmail, // Associate data with the logged-in user
                weight = weight.toDouble(),
                dietRating = q1,
                activityLevel = q2,
                sleepHours = q3,
                // These are null because they don't come from the watch
                waterIntake = null,
                proteinIntake = null
            )
            wellnessRepository.addWellnessData(newEntry)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
