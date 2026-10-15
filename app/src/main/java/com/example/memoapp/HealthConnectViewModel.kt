package com.example.memoapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val healthConnectClient: HealthConnectClient? by lazy {
        val providerPackageName = "com.google.android.apps.healthdata"
        if (HealthConnectClient.getSdkStatus(context, providerPackageName) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    // Health Connect Metrics
    private val _stepCount = MutableStateFlow<Long>(0L)
    val stepCount: StateFlow<Long> = _stepCount.asStateFlow()

    private val _avgHeartRate = MutableStateFlow<Double>(0.0)
    val avgHeartRate: StateFlow<Double> = _avgHeartRate.asStateFlow()

    private val _totalSleepHours = MutableStateFlow<Double>(0.0)
    val totalSleepHours: StateFlow<Double> = _totalSleepHours.asStateFlow()

    private val _latestWeight = MutableStateFlow<Double>(0.0)
    val latestWeight: StateFlow<Double> = _latestWeight.asStateFlow()

    private val _totalCalories = MutableStateFlow<Double>(0.0)
    val totalCalories: StateFlow<Double> = _totalCalories.asStateFlow()

    // MemoApp Internal Data Counts
    private val _notesCount = MutableStateFlow<Int>(0)
    val notesCount: StateFlow<Int> = _notesCount.asStateFlow()

    private val _logItemsCount = MutableStateFlow<Int>(0)
    val logItemsCount: StateFlow<Int> = _logItemsCount.asStateFlow()

    private val _conceptsCount = MutableStateFlow<Int>(0)
    val conceptsCount: StateFlow<Int> = _conceptsCount.asStateFlow()

    private val _symbolsCount = MutableStateFlow<Int>(0)
    val symbolsCount: StateFlow<Int> = _symbolsCount.asStateFlow()

    private val _measurementsCount = MutableStateFlow<Int>(0)
    val measurementsCount: StateFlow<Int> = _measurementsCount.asStateFlow()

    private val _statusMessage = MutableStateFlow<String>("未接続 / 権限未確認")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    init {
        fetchInternalAppCounts()
    }

    fun checkAndFetchData() {
        fetchInternalAppCounts()
        val client = healthConnectClient
        if (client == null) {
            _statusMessage.value = "Health Connect が利用できません（未インストール等）"
            return
        }

        viewModelScope.launch {
            try {
                val granted = client.permissionController.getGrantedPermissions()
                if (granted.containsAll(permissions)) {
                    _statusMessage.value = "権限取得済み：全データを取得中..."
                    fetchAllHealthData(client)
                } else {
                    _statusMessage.value = "一部またはすべての権限が未許可です。『権限をリクエスト』を押してください。"
                }
            } catch (e: Exception) {
                _statusMessage.value = "エラー: ${e.message}"
            }
        }
    }

    private fun fetchInternalAppCounts() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("notes").whereEqualTo("user_id", userId).get()
            .addOnSuccessListener { _notesCount.value = it.size() }

        viewModelScope.launch {
            val repo = LogItemRepository(getApplication())
            _logItemsCount.value = repo.getAll().size
        }

        db.collection("concepts").whereEqualTo("user_id", userId).get()
            .addOnSuccessListener { _conceptsCount.value = it.size() }

        db.collection("symbols").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { _symbolsCount.value = it.size() }

        db.collection("measurements").whereEqualTo("user_id", userId).get()
            .addOnSuccessListener { _measurementsCount.value = it.size() }
    }

    private suspend fun fetchAllHealthData(client: HealthConnectClient) {
        try {
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            val timeFilter = TimeRangeFilter.between(startTime, endTime)

            val stepsResp = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRangeFilter = timeFilter))
            _stepCount.value = stepsResp.records.sumOf { it.count }

            val hrResp = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = timeFilter))
            val allBeats = hrResp.records.flatMap { it.samples }
            _avgHeartRate.value = if (allBeats.isNotEmpty()) allBeats.map { it.beatsPerMinute }.average() else 0.0

            val sleepResp = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = timeFilter))
            val totalSleepMinutes = sleepResp.records.sumOf { 
                java.time.Duration.between(it.startTime, it.endTime).toMinutes() 
            }
            _totalSleepHours.value = totalSleepMinutes / 60.0

            val weightResp = client.readRecords(ReadRecordsRequest(WeightRecord::class, timeRangeFilter = timeFilter))
            _latestWeight.value = weightResp.records.maxByOrNull { it.time }?.weight?.inKilograms ?: 0.0

            val calResp = client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRangeFilter = timeFilter))
            _totalCalories.value = calResp.records.sumOf { it.energy.inKilocalories }

            _statusMessage.value = "ヘルスコネクト & アプリ内データを正常に同期しました"
        } catch (e: Exception) {
            _statusMessage.value = "データ取得エラー: ${e.message}"
        }
    }
}
