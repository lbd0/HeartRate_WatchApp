package kr.ac.hallym.heartrate

import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
    HealthSercivesClient API의 시작점, 코루틴 친화적인 API로 wrapping
 */

class HealthServicesManager @Inject constructor(
    healthServicesClient: HealthServicesClient
) {
    private val measureClient = healthServicesClient.measureClient

    // 기기가 심박수를 제공할 수 있는지 확인
    suspend fun hasHeartRateCapability(): Boolean {
        val capabiliies = measureClient.getCapabilitiesAsync().await()
        return (DataType.HEART_RATE_BPM in capabiliies.supportedDataTypesMeasure)
    }

    /**
     * 콜드 플로우 반환, 활성화되면 흐름이 심박수 데이터에 대한 콜백으로 등록하고 메시지 보내기 시작
     * consuming 코루틴이 취소되면 측정 콜백 등록 취소
     *
     * [callbackFlow]는 콜백 기반 API와 Kotlin 흐름을 연결하는데 사용
     */
    // 심박수 콜백 함수
    @ExperimentalCoroutinesApi
    fun heartRateMeasureFlow() = callbackFlow {
        val callback = object : MeasureCallback {
            // 심박수 측정이 가능한가
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                // DataTypeAvailability만 다시 전송 (LocationAvailability X)
                if (availability is DataTypeAvailability) {
                    trySendBlocking(MeasureMessage.MeasureAvailability(availability))
                }
            }

            // 심박수 측정
            override fun onDataReceived(data: DataPointContainer) {
                val heartRateBpm = data.getData(DataType.HEART_RATE_BPM)
                trySendBlocking(MeasureMessage.MeasureData(heartRateBpm))

            }
        }

        Log.d(TAG, "Registering for data")
        // 데이터 수신 등록 콜백
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

        // 등록 취소 콜백
        awaitClose {
            Log.d(TAG, "Unregistering for data")
            runBlocking {
                measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
            }
        }
    }
}

sealed class MeasureMessage {
    class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
    class MeasureData(val data: List<SampleDataPoint<Double>>): MeasureMessage()
}