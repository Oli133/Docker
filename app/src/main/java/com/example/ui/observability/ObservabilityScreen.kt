package com.example.ui.observability

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun ObservabilityScreen(
    metrics: PrometheusMetrics,
    alerts: List<GrafanaAlert>,
    promQlQuery: String,
    onPromQlChange: (String) -> Unit,
    onAckAlert: (String) -> Unit
) {
    var selectedMetricTab by remember { mutableStateOf("QPS") } // QPS, Latency, Errors, CPU

    val promQlPresets = listOf(
        PromQlQuery(
            title = "Request Rate",
            query = "sum(rate(http_requests_total[5m])) by (service)",
            description = "Per-second HTTP throughput across microservices",
            category = "Traffic"
        ),
        PromQlQuery(
            title = "p95 Latency",
            query = "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))",
            description = "95th percentile response duration SLA metric",
            category = "Latency"
        ),
        PromQlQuery(
            title = "5xx Error Ratio",
            query = "sum(rate(http_requests_total{status=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m])) * 100",
            description = "Server-side error budget consumption",
            category = "Reliability"
        ),
        PromQlQuery(
            title = "Container CPU",
            query = "sum(rate(container_cpu_usage_seconds_total{namespace=\"production\"}[5m])) by (pod)",
            description = "Raw CPU core consumption per Kubernetes pod",
            category = "Resources"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Prometheus & Grafana Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("observability_header_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrometheusOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Insights, contentDescription = null, tint = PrometheusOrange)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Prometheus & Grafana",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Time-series Telemetry & Alertmanager",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SuccessGreen))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "LIVE 15s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Core KPIs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        KpiBox(label = "Total QPS", value = "${metrics.totalQps.toInt()}", sub = "req/sec", color = CyanAccent)
                        KpiBox(label = "p95 Latency", value = "${metrics.p95LatencyMs.toInt()}", sub = "ms", color = if (metrics.p95LatencyMs > 100) DangerRed else NeonTeal)
                        KpiBox(label = "Error Rate", value = String.format("%.2f%%", metrics.errorRatePercent), sub = "5xx / 4xx", color = if (metrics.errorRatePercent > 2f) DangerRed else PrometheusOrange)
                        KpiBox(label = "Cluster CPU", value = "${metrics.avgCpuUsagePercent.toInt()}%", sub = "Allocated", color = FluentdCyan)
                    }
                }
            }
        }

        // Section: Real-time Prometheus Graph
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prometheus_chart_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROMETHEUS TIME-SERIES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.1.sp
                        )
                        // Metric Tab Selector Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("QPS", "Latency", "Errors", "CPU").forEach { tab ->
                                val isSelected = selectedMetricTab == tab
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clickable { selectedMetricTab = tab }
                                        .testTag("metric_tab_$tab")
                                ) {
                                    Text(
                                        text = tab,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Chart Canvas
                    val points = when (selectedMetricTab) {
                        "QPS" -> metrics.qpsHistory
                        "Latency" -> metrics.latencyHistory
                        "Errors" -> metrics.errorHistory
                        else -> metrics.cpuHistory
                    }
                    val lineColor = when (selectedMetricTab) {
                        "QPS" -> CyanAccent
                        "Latency" -> NeonTeal
                        "Errors" -> DangerRed
                        else -> PrometheusOrange
                    }
                    val unit = when (selectedMetricTab) {
                        "QPS" -> "req/s"
                        "Latency" -> "ms"
                        "Errors" -> "%"
                        else -> "%"
                    }

                    PrometheusChartCanvas(
                        points = points,
                        lineColor = lineColor,
                        unit = unit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        }

        // Section: PromQL Interactive Playground
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("promql_console_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PromQL Query Console",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Query Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(promQlPresets) { preset ->
                            OutlinedCard(
                                onClick = { onPromQlChange(preset.query) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("preset_${preset.title.replace(" ", "_")}")
                            ) {
                                Text(
                                    text = preset.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // PromQL Input Field
                    OutlinedTextField(
                        value = promQlQuery,
                        onValueChange = onPromQlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("promql_input_field"),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        singleLine = false,
                        maxLines = 3,
                        label = { Text("Prometheus Expression (PromQL)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Result Vector Simulator Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Evaluation: vector [instant]",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "{job=\"kubernetes-pods\", namespace=\"production\"} => ${metrics.totalQps.toInt()} [status=OK]",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }
        }

        // Section: Grafana Alertmanager
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PrometheusOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GRAFANA ALERTMANAGER (${alerts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        items(alerts, key = { it.id }) { alert ->
            GrafanaAlertCard(alert = alert, onAck = { onAckAlert(alert.id) })
        }
    }
}

@Composable
fun KpiBox(label: String, value: String, sub: String, color: Color) {
    Column {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PrometheusChartCanvas(
    points: List<MetricPoint>,
    lineColor: Color,
    unit: String,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val minVal = points.minOf { it.value }
    val maxVal = points.maxOf { it.value }
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padBottom = 20f
            val chartH = h - padBottom

            // Draw horizontal grid lines
            for (i in 0..3) {
                val y = chartH * (i / 3f)
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // Build path
            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, pt ->
                val x = (index.toFloat() / (points.size - 1).coerceAtLeast(1)) * w
                val normalizedY = 1f - ((pt.value - minVal) / range)
                val y = (normalizedY * (chartH - 20f)) + 10f

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, chartH)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == points.size - 1) {
                    fillPath.lineTo(x, chartH)
                    fillPath.close()
                }
            }

            // Shaded gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.0f)),
                    startY = 0f,
                    endY = chartH
                )
            )

            // Stroke line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.5f)
            )
        }

        // Min & Max label overlay
        Text(
            text = "Max: ${maxVal.toInt()} $unit",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp, top = 2.dp)
        )
        Text(
            text = "Min: ${minVal.toInt()} $unit",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 4.dp)
        )
    }
}

@Composable
fun GrafanaAlertCard(alert: GrafanaAlert, onAck: () -> Unit) {
    val isFiring = alert.state == AlertState.FIRING
    val alertColor = if (isFiring) DangerRed else SuccessGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_card_${alert.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(alertColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.title,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = alertColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = alert.state.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = alertColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.summary,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Threshold: ${alert.threshold} • Current: ${alert.currentVal}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isFiring) {
                    OutlinedButton(
                        onClick = onAck,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("ack_alert_${alert.id}")
                    ) {
                        Text("Acknowledge", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
