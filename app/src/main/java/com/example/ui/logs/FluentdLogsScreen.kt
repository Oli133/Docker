package com.example.ui.logs

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FluentdLog
import com.example.data.model.LogLevel
import com.example.ui.theme.*

@Composable
fun FluentdLogsScreen(
    logs: List<FluentdLog>,
    selectedService: String?,
    selectedLevel: LogLevel?,
    searchQuery: String,
    isAutoScroll: Boolean,
    onSelectService: (String?) -> Unit,
    onSelectLevel: (LogLevel?) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleAutoScroll: () -> Unit
) {
    var expandedLogId by remember { mutableStateOf<String?>(null) }

    val serviceFilters = listOf(
        null to "All Services",
        "api-gateway" to "api-gateway",
        "order-service" to "order-service",
        "payment-service" to "payment-service",
        "inventory-service" to "inventory-service",
        "auth-service" to "auth-service",
        "notification-service" to "notif-service"
    )

    val levelFilters = listOf(
        null to "ALL",
        LogLevel.INFO to "INFO",
        LogLevel.WARN to "WARN",
        LogLevel.ERROR to "ERROR",
        LogLevel.FATAL to "FATAL",
        LogLevel.DEBUG to "DEBUG"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Fluentd Terminal Status Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fluentd_header_card")
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
                                    .background(FluentdCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = FluentdCyan)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Fluentd Log Aggregator",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "in_tail container stdout ➔ out_es",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleAutoScroll,
                            modifier = Modifier.testTag("toggle_autoscroll_logs")
                        ) {
                            Icon(
                                imageVector = if (isAutoScroll) Icons.Default.VerticalAlignBottom else Icons.Default.Pause,
                                contentDescription = "Auto Scroll",
                                tint = if (isAutoScroll) SuccessGreen else TextSecondaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LogHeaderMetric(label = "Buffer Memory", value = "64KB / 2MB", color = CyanAccent)
                        LogHeaderMetric(label = "Flush Interval", value = "5.0s", color = NeonTeal)
                        LogHeaderMetric(label = "Log Drop Rate", value = "0.00%", color = SuccessGreen)
                        LogHeaderMetric(label = "Total Ingested", value = "${logs.size} events", color = PrometheusOrange)
                    }
                }
            }
        }

        // Search Input Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_search_field"),
                placeholder = { Text("Filter logs, trace IDs or messages...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FluentdCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // Service Filter Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(serviceFilters) { (serviceKey, label) ->
                    val isSelected = selectedService == serviceKey
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) FluentdCyan else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onSelectService(serviceKey) }
                            .testTag("filter_service_${label.replace(" ", "_")}")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Log Level Filter Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(levelFilters) { (levelKey, label) ->
                    val isSelected = selectedLevel == levelKey
                    val levelColor = when (levelKey) {
                        LogLevel.INFO -> SuccessGreen
                        LogLevel.WARN -> WarningYellow
                        LogLevel.ERROR -> DangerRed
                        LogLevel.FATAL -> PrometheusOrange
                        LogLevel.DEBUG -> TextSecondaryDark
                        null -> FluentdCyan
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) levelColor else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onSelectLevel(levelKey) }
                            .testTag("filter_level_$label")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Logs Stream
        items(logs, key = { it.id }) { log ->
            val isExpanded = expandedLogId == log.id
            FluentdLogCard(
                log = log,
                isExpanded = isExpanded,
                onToggle = { expandedLogId = if (isExpanded) null else log.id }
            )
        }
    }
}

@Composable
fun LogHeaderMetric(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun FluentdLogCard(
    log: FluentdLog,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val levelColor = when (log.level) {
        LogLevel.INFO -> SuccessGreen
        LogLevel.WARN -> WarningYellow
        LogLevel.ERROR -> DangerRed
        LogLevel.FATAL -> PrometheusOrange
        LogLevel.DEBUG -> TextSecondaryDark
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_item_${log.id}")
            .clickable { onToggle() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = levelColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = log.level.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = levelColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.timestamp,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = K8sBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = log.service,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = K8sBlueLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Log Message
            Text(
                text = log.message,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Expanded Metadata & Structured JSON
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Trace ID: ${log.traceId} • Span ID: ${log.spanId}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CyanAccent
                    )
                    Text(
                        text = "Pod Instance: ${log.podName}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Raw JSON Box
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = log.rawJson,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NeonTeal,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
