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

// Hilt ->  종속 항목 삽입
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val viewModel : MainViewModel by viewModels()

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
                if(it.equals(DataTypeAvailability.AVAILABLE)) {
                    binding.statusLabel.text = "심박수 측정 중"
                    binding.unheartImg.isVisible = false
                    binding.statusLabel.isVisible = true
                    binding.heartRate.isVisible = true
                    binding.heartImg.isVisible = true
                } else {
                    binding.statusLabel.text = "심박수 측정 불가"
                    binding.heartRate.isVisible = false
                    binding.heartImg.isVisible = false
                    binding.statusLabel.isVisible = true
                    binding.unheartImg.isVisible = true
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.heartRateBpm.collect {
                binding.heartRate.text = String.format("%.1f", it)

                val myRef = firebaseDatabase.getReference("bpm")
                myRef.push().setValue(it)
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
    }

    fun initDatabase() {
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("bpm")

    }
}