package com.example.andromeda.services

import android.util.Log
import com.example.andromeda.data.WellnessData
import com.example.andromeda.data.WellnessDataRepository
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WearableDataListenerService : WearableListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var wellnessRepository: WellnessDataRepository

    override fun onCreate() {
        super.onCreate()
        wellnessRepository = WellnessDataRepository(application)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == "/request_average_weight") {
            Log.d("WearableService", "Received average weight request from watch. Triggering repository to send update.")
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/wellness_data") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                println("PHONE: Received DataMap: $dataMap")

                serviceScope.launch {
                    val timestamp = dataMap.getLong("KEY_TIMESTAMP")

                    fun getNullableInt(key: String): Int? {
                        val value = dataMap.getInt(key)
                        return if (value == 0) null else value
                    }

                    val newEntry = WellnessData(
                        weight = dataMap.getDouble("KEY_WEIGHT"),
                        timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp)),
                        dietRating = getNullableInt("KEY_Q1"),
                        activityLevel = getNullableInt("KEY_Q2"),
                        sleepHours = getNullableInt("KEY_Q3"),
                        waterIntake = getNullableInt("KEY_Q4"),
                        proteinIntake = getNullableInt("KEY_Q5")
                    )

                    println("PHONE: Reconstructed WellnessData: $newEntry")
                    wellnessRepository.addWellnessData(newEntry)
                    println("PHONE: Saved wellness data and triggered proactive weight update.")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
