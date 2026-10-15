package com.example.memoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class HealthConnectFragment : Fragment() {

    private val viewModel: HealthConnectViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(viewModel.permissions)) {
            viewModel.checkAndFetchData()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val stepCount by viewModel.stepCount.collectAsStateWithLifecycle()
                val avgHeartRate by viewModel.avgHeartRate.collectAsStateWithLifecycle()
                val totalSleepHours by viewModel.totalSleepHours.collectAsStateWithLifecycle()
                val latestWeight by viewModel.latestWeight.collectAsStateWithLifecycle()
                val totalCalories by viewModel.totalCalories.collectAsStateWithLifecycle()

                val notesCount by viewModel.notesCount.collectAsStateWithLifecycle()
                val logItemsCount by viewModel.logItemsCount.collectAsStateWithLifecycle()
                val conceptsCount by viewModel.conceptsCount.collectAsStateWithLifecycle()
                val symbolsCount by viewModel.symbolsCount.collectAsStateWithLifecycle()
                val measurementsCount by viewModel.measurementsCount.collectAsStateWithLifecycle()

                val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.checkAndFetchData()
                }

                MaterialTheme {
                    Scaffold(
                        topBar = {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(title = { Text("Dashboard & Health Connect") })
                        }
                    ) { padding ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                Text(
                                    text = "状態: $statusMessage",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Section 1: Health Connect External Data
                            item {
                                SectionHeader(title = "【Health Connect 外部データ】")
                            }
                            item {
                                HealthMetricCard(title = "歩数 (過去7日間)", value = "$stepCount 歩")
                            }
                            item {
                                HealthMetricCard(title = "平均心拍数", value = "%.1f BPM".format(avgHeartRate))
                            }
                            item {
                                HealthMetricCard(title = "睡眠時間 (過去7日間)", value = "%.1f 時間".format(totalSleepHours))
                            }
                            item {
                                HealthMetricCard(title = "最新の体重", value = "%.1f kg".format(latestWeight))
                            }
                            item {
                                HealthMetricCard(title = "消費カロリー (過去7日間)", value = "%.1f kcal".format(totalCalories))
                            }

                            // Section 2: MemoApp Internal Data
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionHeader(title = "【MemoApp アプリ内管理データ】")
                            }
                            item {
                                HealthMetricCard(title = "ノート登録数", value = "$notesCount 件")
                            }
                            item {
                                HealthMetricCard(title = "ログアイテム数", value = "$logItemsCount 件")
                            }
                            item {
                                HealthMetricCard(title = "コンセプト数", value = "$conceptsCount 件")
                            }
                            item {
                                HealthMetricCard(title = "シンボル数", value = "$symbolsCount 件")
                            }
                            item {
                                HealthMetricCard(title = "測定プロジェクト数", value = "$measurementsCount 件")
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    requestPermissionLauncher.launch(viewModel.permissions)
                                }) {
                                    Text("Health Connect 権限をリクエスト")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = {
                                    viewModel.checkAndFetchData()
                                }) {
                                    Text("データを再取得・同期")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterStart) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun HealthMetricCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
