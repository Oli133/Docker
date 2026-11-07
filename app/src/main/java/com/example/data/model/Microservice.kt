package com.example.data.model

data class Microservice(
    val id: String,
    val name: String,
    val description: String,
    val dockerImage: String,
    val version: String,
    val port: Int,
    val protocol: String = "HTTP/gRPC",
    val desiredReplicas: Int,
    val currentReplicas: Int,
    val cpuUsagePercent: Int,
    val memoryUsageMb: Int,
    val memoryLimitMb: Int = 512,
    val status: ServiceStatus,
    val dependencies: List<String> = emptyList(),
    val healthCheckPath: String = "/healthz",
    val isHealthy: Boolean = true,
    val requestsPerSec: Int = 120,
    val avgLatencyMs: Double = 18.5,
    val restartCount: Int = 0,
    val uptimeHours: Double = 168.4
)

enum class ServiceStatus(val label: String) {
    HEALTHY("Healthy"),
    SCALING("Scaling"),
    DEGRADED("Degraded"),
    CRASH_LOOP("CrashLoopBackOff")
}

data class PodInstance(
    val podName: String,
    val nodeName: String,
    val ip: String,
    val status: String,
    val restarts: Int,
    val age: String,
    val cpuPercent: Int,
    val memMb: Int
)

data class K8sNode(
    val name: String,
    val role: String,
    val status: String,
    val cpuAllocatedPercent: Int,
    val memoryAllocatedPercent: Int,
    val podCount: Int,
    val maxPods: Int = 110,
    val kubeletVersion: String = "v1.30.2"
)

data class HpaConfig(
    val serviceId: String,
    val minReplicas: Int,
    val maxReplicas: Int,
    val currentReplicas: Int,
    val targetCpuPercent: Int,
    val currentCpuPercent: Int,
    val lastScaleReason: String,
    val isAutoscalingEnabled: Boolean = true
)
