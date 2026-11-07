package com.example.ui.chaos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun ChaosLabDialog(
    onDismiss: () -> Unit,
    onTrafficSpike: (Float) -> Unit,
    onInjectLatency: (String) -> Unit,
    onInjectCrash: (String) -> Unit,
    onReset: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("chaos_lab_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Chaos & Resilience Lab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Simulate infrastructure failures, load surges, and watch Kubernetes HPA auto-scale pods, Prometheus trigger alerts, and Fluentd capture logs in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action 1: Traffic Surge (HPA scale-out)
                ChaosActionCard(
                    title = "Surge Traffic (+300% Load)",
                    desc = "Tests HPA autoscaling from 4 to 8 pods as CPU exceeds 70%",
                    icon = Icons.Default.TrendingUp,
                    color = CyanAccent,
                    buttonText = "Spike Traffic",
                    onClick = {
                        onTrafficSpike(3.0f)
                        onDismiss()
                    },
                    testTag = "chaos_traffic_spike_button"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2: Inject Latency & 5xx
                ChaosActionCard(
                    title = "Inject Latency (order-service)",
                    desc = "Spikes response time to 420ms; fires Prometheus HighLatency alert",
                    icon = Icons.Default.HourglassTop,
                    color = PrometheusOrange,
                    buttonText = "Inject Lag",
                    onClick = {
                        onInjectLatency("order-service")
                        onDismiss()
                    },
                    testTag = "chaos_inject_latency_button"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action 3: Kill Pod (CrashLoopBackOff)
                ChaosActionCard(
                    title = "Kill Pod (OOM Killed)",
                    desc = "Forces OutOfMemory exit; demonstrates K8s self-healing restart",
                    icon = Icons.Default.Warning,
                    color = DangerRed,
                    buttonText = "Kill Pod",
                    onClick = {
                        onInjectCrash("order-service")
                        onDismiss()
                    },
                    testTag = "chaos_kill_pod_button"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reset Action
                OutlinedButton(
                    onClick = {
                        onReset()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chaos_reset_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recover / Reset Cluster to Normal")
                }
            }
        }
    }
}

@Composable
fun ChaosActionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    buttonText: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(30.dp)
                    .testTag(testTag)
            ) {
                Text(buttonText, fontSize = 11.sp)
            }
        }
    }
}
