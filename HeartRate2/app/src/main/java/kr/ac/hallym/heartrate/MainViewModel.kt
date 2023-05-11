package kr.ac.hallym.heartrate

import android.util.Log
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 앱에 대한 대부분의 상호 작용 로직과 UI 상태
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val healthServicesManager: HealthServicesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Startup)   // UI 상태 변수
    var uiState: StateFlow<UiState> = _uiState

    private val _heartRateAvailable = MutableStateFlow(DataTypeAvailability.UNKNOWN)    // 심박수 측정 가능 여부
    val heartRateAvailable: StateFlow<DataTypeAvailability> = _heartRateAvailable

    private val _heartRateBpm = MutableStateFlow(0.0)       // 심박수 받는 변수
    val heartRateBpm: StateFlow<Double> = _heartRateBpm

    init {
        // 장치에 심박수 기능이 있는지 확인하고 다음 상태로 진행
        viewModelScope.launch {
            _uiState.value = if (healthServicesManager.hasHeartRateCapability()) {
                UiState.HeartRateAvailable      // 심박수 기능이 있으면 심박 측정 가능 ui로 변경
            } else {
                UiState.HeartRateNotAvailable       // 심박수 기능이 없으면 심박 측정 불가 ui로 변경
           }
        }
    }


    @ExperimentalCoroutinesApi
    suspend fun measureHeartRate() {    // 심박수 받아오는 함수
        healthServicesManager.heartRateMeasureFlow().collect {
            when (it) {
                // 심박수 측정이 가능한 상태인지 확인
                is MeasureMessage.MeasureAvailability -> {
                    Log.d(TAG, "Availability changed: ${it.availability}")
                    _heartRateAvailable.value = it.availability
                }
                // 심박수 데이터 받기
                is MeasureMessage.MeasureData -> {
                    val bpm = it.data.last().value  // 심박수 받음
                    Log.d(TAG, "Data update: $bpm")     // 받은 심박수 로그 찍음
                    _heartRateBpm.value = bpm           // 받은 심박수 저장
                }
            }
        }
    }
}

sealed class UiState {
    object Startup : UiState()      // 시작 UI
    object HeartRateAvailable : UiState()   // 심박 측정 가능 UI
    object HeartRateNotAvailable : UiState()    // 심박 측정 불가 UI
}