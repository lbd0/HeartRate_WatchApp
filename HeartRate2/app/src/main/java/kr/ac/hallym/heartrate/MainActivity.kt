package kr.ac.hallym.heartrate

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import kr.ac.hallym.heartrate.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val viewModel : MainViewModel by viewModels()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                when(result) {
                    true -> {
                        Log.i(TAG, "Body sensors permission granted")
                        // 활동이 최소한 STARTED 상태일 때만 측정
                        lifecycleScope.launchWhenStarted {
                            viewModel.measureHeartRate()
                        }
                    }
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
                binding.status.text = it.toString()
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.heartRateBpm.collect {
                binding.heartRate.text = String.format("%.1f", it)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        permissionLauncher.launch(android.Manifest.permission.BODY_SENSORS)
    }

    private fun updateViewVisiblity(uiState : UiState) {
        (uiState is UiState.Startup).let {

        }

        // 심박수 기능을 사용할 수 없을 때 화면에 표시
        (uiState is UiState.HeartRateNotAvailable).let {

        }

        // 심박수 기능을 사용할 수 있을 때 화면에 표시
        (uiState is UiState.HeartRateAvailable).let {
            binding.status.isVisible = it
            binding.statusLabel.isVisible = it
            binding.heartRate.isVisible = it
            binding.heartImg.isVisible = it

        }
    }
}