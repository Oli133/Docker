package com.example.data.model

data class MetricPoint(
    val timestampSeconds: Long,
    val value: Float
)

data class PrometheusMetrics(
    val totalQps: Float,
    val p95LatencyMs: Float,
    val p99LatencyMs: Float,
    val errorRatePercent: Float,
    val avgCpuUsagePercent: Float,
    val avgMemoryUsagePercent: Float,
    val qpsHistory: List<MetricPoint>,
    val latencyHistory: List<MetricPoint>,
    val errorHistory: List<MetricPoint>,
    val cpuHistory: List<MetricPoint>
)

data class PromQlQuery(
    val title: String,
    val query: String,
    val description: String,
    val category: String
)

data class GrafanaAlert(
    val id: String,
    val title: String,
    val state: AlertState,
    val severity: String,
    val service: String,
    val summary: String,
    val triggeredAt: String,
    val currentVal: String,
    val threshold: String
)

enum class AlertState(val label: String) {
    FIRING("Firing"),
    PENDING("Pending"),
    RESOLVED("Resolved")
}

data class FluentdLog(
    val id: String,
    val timestamp: String,
    val level: LogLevel,
    val service: String,
    val podName: String,
    val message: String,
    val traceId: String,
    val spanId: String,
    val httpStatus: Int? = null,
    val durationMs: Double? = null,
    val rawJson: String
)

enum class LogLevel(val label: String) {
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    DEBUG("DEBUG"),
    FATAL("FATAL")
}
