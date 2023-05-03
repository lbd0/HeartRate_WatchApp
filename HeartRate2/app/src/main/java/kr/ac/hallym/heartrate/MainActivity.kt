package kr.ac.hallym.heartrate

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.health.services.client.data.DataTypeAvailability
import kr.ac.hallym.heartrate.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Hilt ->  종속 항목 삽입
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val viewModel : MainViewModel by viewModels()
    private var uiState : UiState = UiState.Startup

    private lateinit var firebaseDatabase : FirebaseDatabase
    private lateinit var databaseReference : DatabaseReference

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)        // 화면에 표시

        initDatabase()

        // 퍼미션 결과 콜백 등록
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                when(result) {
                    true -> {
                        // 퍼미션 받음
                        Log.i(TAG, "Body sensors permission granted")
                        // 활동이 최소한 STARTED 상태일 때만 측정
                        lifecycleScope.launchWhenStarted {
                            viewModel.measureHeartRate()
                        }
                    }
                    // 퍼미션 못 받음
                    false -> Log.i(TAG, "Body sensors permission not granted")
                }
            }

        // 뷰 모델 상태를 UI에 바인딩
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect {
                updateViewVisiblity(it)
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.heartRateAvailable.collect {
                if(it == DataTypeAvailability.AVAILABLE) {
                    binding.statusLabel.text = "심박수 측정 중"
                    binding.heartImg.setImageResource(R.drawable.heart_rate)
                    binding.heartRate.isVisible = true

                } else {
                    binding.statusLabel.text = "심박수 측정 불가"
                    binding.heartImg.setImageResource(R.drawable.broken_heart)
                    binding.heartRate.isVisible = false
                }
                
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.heartRateBpm.collect {
                binding.heartRate.text = String.format("%.1f", it)

                var myRef = firebaseDatabase.getReference("bpm")    // Firebase DB의 bpm 참조
                val dateAndtime : LocalDateTime = LocalDateTime.now()   // 현재 날짜와 시간 받기
                myRef.setValue(it.toString() + " $dateAndtime")  // bpm에 심박수, 날짜와 시간 덮어쓰기로 저장

                myRef = firebaseDatabase.getReference("test_bpm")   // Firebase DB의 test_bpm 참조
                myRef.push().setValue(it.toString() + " $dateAndtime")  // test_bpm에 심박수, 날짜와 시간 쌓아서 저장
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 바디 센서에 대한 퍼미션
        permissionLauncher.launch(android.Manifest.permission.BODY_SENSORS)
    }

    private fun updateViewVisiblity(uiState : UiState) {
        (uiState is UiState.Startup).let {
            binding.startImg.isVisible = it
            binding.startTxt.isVisible = it
        }

        (uiState is UiState.HeartRateNotAvailable).let {
            binding.statusLabel.isVisible = it
            binding.unheartImg.isVisible = it
        }

        (uiState is UiState.HeartRateAvailable).let {
            binding.statusLabel.isVisible = it
            binding.heartImg.isVisible = it
        }
    }

    fun initDatabase() {
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("bpm")
    }
}