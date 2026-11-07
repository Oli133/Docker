package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class DevOpsRepository(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    // Microservices
    private val _services = MutableStateFlow<List<Microservice>>(createInitialServices())
    val services: StateFlow<List<Microservice>> = _services.asStateFlow()

    // Nodes
    private val _nodes = MutableStateFlow<List<K8sNode>>(createInitialNodes())
    val nodes: StateFlow<List<K8sNode>> = _nodes.asStateFlow()

    // Pods
    private val _pods = MutableStateFlow<List<PodInstance>>(createInitialPods())
    val pods: StateFlow<List<PodInstance>> = _pods.asStateFlow()

    // HPA Configs
    private val _hpaConfigs = MutableStateFlow<List<HpaConfig>>(createInitialHpas())
    val hpaConfigs: StateFlow<List<HpaConfig>> = _hpaConfigs.asStateFlow()

    // CI/CD Pipeline Runs
    private val _pipelineRuns = MutableStateFlow<List<PipelineRun>>(createInitialPipelines())
    val pipelineRuns: StateFlow<List<PipelineRun>> = _pipelineRuns.asStateFlow()

    // Prometheus Metrics
    private val _metrics = MutableStateFlow(createInitialMetrics())
    val metrics: StateFlow<PrometheusMetrics> = _metrics.asStateFlow()

    // Grafana Alerts
    private val _alerts = MutableStateFlow<List<GrafanaAlert>>(createInitialAlerts())
    val alerts: StateFlow<List<GrafanaAlert>> = _alerts.asStateFlow()

    // Fluentd Logs
    private val _logs = MutableStateFlow<List<FluentdLog>>(createInitialLogs())
    val logs: StateFlow<List<FluentdLog>> = _logs.asStateFlow()

    // Chaos state
    private var isTrafficSpikeActive = false
    private var spikeMultiplier = 1.0f
    private var injectedLatencyService: String? = null
    private var crashedServiceId: String? = null

    init {
        startTelemetryLoop()
        startLogStreamLoop()
    }

    private fun startTelemetryLoop() {
        coroutineScope.launch {
            var counter = 0
            while (isActive) {
                delay(2000)
                counter++
                updateTelemetryTick(counter)
            }
        }
    }

    private fun startLogStreamLoop() {
        coroutineScope.launch {
            while (isActive) {
                delay(Random.nextLong(1500, 3500))
                generateRandomLog()
            }
        }
    }

    private fun updateTelemetryTick(tick: Int) {
        val currentMetrics = _metrics.value
        val nowSec = System.currentTimeMillis() / 1000

        // Base traffic
        val baseQps = 1420f * (if (isTrafficSpikeActive) spikeMultiplier else 1.0f)
        val jitterQps = baseQps + Random.nextInt(-45, 60)

        // Latency
        val baseLat = if (injectedLatencyService != null) 385f else 18.5f
        val currentLat = baseLat + Random.nextFloat() * 4.2f
        val currentP99 = currentLat * 2.1f

        // Error rate
        val baseErr = if (crashedServiceId != null) 8.4f else if (injectedLatencyService != null) 3.1f else 0.08f
        val currentErr = (baseErr + (Random.nextFloat() * 0.15f)).coerceAtLeast(0.01f)

        // CPU
        val baseCpu = if (isTrafficSpikeActive) min(92f, 48f * spikeMultiplier) else 44.5f
        val currentCpu = (baseCpu + Random.nextFloat() * 3.5f).coerceIn(10f, 99f)
        val currentMem = 62.4f + Random.nextFloat() * 1.8f

        // Append to history (keep last 24 points)
        fun appendHistory(list: List<MetricPoint>, newVal: Float): List<MetricPoint> {
            val updated = list.toMutableList()
            updated.add(MetricPoint(nowSec, newVal))
            if (updated.size > 24) updated.removeAt(0)
            return updated
        }

        val updatedMetrics = currentMetrics.copy(
            totalQps = jitterQps,
            p95LatencyMs = currentLat,
            p99LatencyMs = currentP99,
            errorRatePercent = currentErr,
            avgCpuUsagePercent = currentCpu,
            avgMemoryUsagePercent = currentMem,
            qpsHistory = appendHistory(currentMetrics.qpsHistory, jitterQps),
            latencyHistory = appendHistory(currentMetrics.latencyHistory, currentLat),
            errorHistory = appendHistory(currentMetrics.errorHistory, currentErr),
            cpuHistory = appendHistory(currentMetrics.cpuHistory, currentCpu)
        )
        _metrics.value = updatedMetrics

        // Evaluate HPA autoscaling
        if (isTrafficSpikeActive && spikeMultiplier >= 2.0f) {
            evaluateAutoscaling(currentCpu)
        }
    }

    private fun evaluateAutoscaling(avgCpu: Float) {
        val updatedHpas = _hpaConfigs.value.map { hpa ->
            if (hpa.isAutoscalingEnabled && avgCpu > hpa.targetCpuPercent) {
                val newDesired = min(hpa.maxReplicas, hpa.currentReplicas + 1)
                if (newDesired != hpa.currentReplicas) {
                    // trigger scale up
                    scaleServiceReplicas(hpa.serviceId, newDesired, "HPA scale-out triggered: CPU ${avgCpu.toInt()}% > ${hpa.targetCpuPercent}%")
                    hpa.copy(
                        currentReplicas = newDesired,
                        currentCpuPercent = avgCpu.toInt(),
                        lastScaleReason = "HPA scale-up to $newDesired pods (CPU avg ${avgCpu.toInt()}%)"
                    )
                } else {
                    hpa.copy(currentCpuPercent = avgCpu.toInt())
                }
            } else {
                hpa.copy(currentCpuPercent = avgCpu.toInt())
            }
        }
        _hpaConfigs.value = updatedHpas
    }

    fun scaleServiceReplicas(serviceId: String, replicas: Int, reason: String = "Manual operator scale") {
        val clampedReplicas = replicas.coerceIn(1, 20)
        _services.value = _services.value.map { s ->
            if (s.id == serviceId) {
                s.copy(
                    desiredReplicas = clampedReplicas,
                    currentReplicas = clampedReplicas,
                    status = if (clampedReplicas > s.currentReplicas) ServiceStatus.SCALING else ServiceStatus.HEALTHY
                )
            } else s
        }

        _hpaConfigs.value = _hpaConfigs.value.map { hpa ->
            if (hpa.serviceId == serviceId) {
                hpa.copy(
                    currentReplicas = clampedReplicas,
                    lastScaleReason = reason
                )
            } else hpa
        }

        rebuildPods()
        addLog(
            level = LogLevel.INFO,
            service = "k8s-controller-manager",
            pod = "kube-controller-manager-master",
            message = "Deployment $serviceId scaled to $clampedReplicas replicas. Reason: $reason"
        )
    }

    private fun rebuildPods() {
        val nodeNames = listOf("k8s-node-worker-01", "k8s-node-worker-02", "k8s-node-spot-03")
        val newPods = mutableListOf<PodInstance>()

        _services.value.forEach { service ->
            for (i in 1..service.currentReplicas) {
                val node = nodeNames[(i + service.port) % nodeNames.size]
                val isCrashed = (service.id == crashedServiceId && i == 1)
                newPods.add(
                    PodInstance(
                        podName = "${service.id}-${Integer.toHexString(service.id.hashCode()).take(5)}-$i",
                        nodeName = node,
                        ip = "10.244.${(service.port % 10)}.$i",
                        status = if (isCrashed) "CrashLoopBackOff" else "Running",
                        restarts = if (isCrashed) 4 else 0,
                        age = "4d 12h",
                        cpuPercent = if (isCrashed) 0 else Random.nextInt(15, 65),
                        memMb = if (isCrashed) 32 else Random.nextInt(120, 380)
                    )
                )
            }
        }
        _pods.value = newPods
    }

    // Chaos & Simulation Actions
    fun triggerTrafficSpike(multiplier: Float) {
        isTrafficSpikeActive = true
        spikeMultiplier = multiplier
        addLog(
            level = LogLevel.WARN,
            service = "api-gateway",
            pod = "api-gateway-edge-01",
            message = "High inbound traffic detected: request rate multiplied by ${multiplier}x (~${(1400 * multiplier).toInt()} req/s)"
        )
    }

    fun injectServiceLatency(serviceId: String) {
        injectedLatencyService = serviceId
        _services.value = _services.value.map { s ->
            if (s.id == serviceId) s.copy(status = ServiceStatus.DEGRADED, avgLatencyMs = 420.0) else s
        }
        // Trigger alert
        val newAlert = GrafanaAlert(
            id = "alert-lat-${System.currentTimeMillis()}",
            title = "HighRequestLatencyBreached",
            state = AlertState.FIRING,
            severity = "P1 - Critical",
            service = serviceId,
            summary = "p95 latency of $serviceId exceeded 300ms SLA threshold (current: 420ms)",
            triggeredAt = timeFormat.format(Date()),
            currentVal = "420ms",
            threshold = "> 300ms for 1m"
        )
        _alerts.value = listOf(newAlert) + _alerts.value

        addLog(
            level = LogLevel.ERROR,
            service = serviceId,
            pod = "$serviceId-pod-1",
            message = "gRPC deadline exceeded / downstream database connection pool saturation. Latency spiked to 420ms."
        )
    }

    fun injectCrashLoop(serviceId: String) {
        crashedServiceId = serviceId
        _services.value = _services.value.map { s ->
            if (s.id == serviceId) s.copy(status = ServiceStatus.CRASH_LOOP, restartCount = s.restartCount + 1) else s
        }
        rebuildPods()

        val alert = GrafanaAlert(
            id = "alert-crash-${System.currentTimeMillis()}",
            title = "KubePodCrashLooping",
            state = AlertState.FIRING,
            severity = "P0 - Emergency",
            service = serviceId,
            summary = "Pod in deployment $serviceId entered CrashLoopBackOff after OOMKilled",
            triggeredAt = timeFormat.format(Date()),
            currentVal = "ExitCode 137 (OOM)",
            threshold = "restarts > 3 in 5m"
        )
        _alerts.value = listOf(alert) + _alerts.value

        addLog(
            level = LogLevel.FATAL,
            service = serviceId,
            pod = "$serviceId-pod-1",
            message = "FATAL: OutOfMemoryError - container exceeded memory limit 512Mi. Terminated by Linux OOM killer."
        )
    }

    fun resetClusterToNormal() {
        isTrafficSpikeActive = false
        spikeMultiplier = 1.0f
        injectedLatencyService = null
        crashedServiceId = null

        _services.value = createInitialServices()
        _hpaConfigs.value = createInitialHpas()
        _alerts.value = _alerts.value.map { it.copy(state = AlertState.RESOLVED) }
        rebuildPods()

        addLog(
            level = LogLevel.INFO,
            service = "cluster-admin",
            pod = "sre-ops-agent",
            message = "Cluster state reset. Traffic stabilized, alerts resolved, pod replicas normalized."
        )
    }

    // Trigger CI/CD Pipeline
    fun triggerDeployment(branch: String = "main", strategy: String = "RollingUpdate") {
        coroutineScope.launch {
            val runNumber = (_pipelineRuns.value.maxOfOrNull { it.runNumber } ?: 42) + 1
            val runId = "run-$runNumber"
            val commitSha = "b" + UUID.randomUUID().toString().replace("-", "").take(7)

            val initialStages = listOf(
                PipelineStage("Lint & Unit Tests", PipelineStatus.RUNNING, 8, listOf(
                    PipelineStep("Setup Go / Node", PipelineStatus.SUCCESS, 2, listOf("Installed Go 1.22", "Restored cache")),
                    PipelineStep("Run Tests & Vet", PipelineStatus.RUNNING, 6, listOf("Executing go test -race ./..."))
                )),
                PipelineStage("Docker Build & Push", PipelineStatus.QUEUED, 0, listOf(
                    PipelineStep("Setup Buildx", PipelineStatus.QUEUED, 0, emptyList()),
                    PipelineStep("Push to GHCR", PipelineStatus.QUEUED, 0, emptyList())
                )),
                PipelineStage("Trivy Vulnerability Scan", PipelineStatus.QUEUED, 0, listOf(
                    PipelineStep("Scan CVEs", PipelineStatus.QUEUED, 0, emptyList())
                )),
                PipelineStage("K8s $strategy Rollout", PipelineStatus.QUEUED, 0, listOf(
                    PipelineStep("Apply manifests", PipelineStatus.QUEUED, 0, emptyList()),
                    PipelineStep("Verify rollout health", PipelineStatus.QUEUED, 0, emptyList())
                ))
            )

            val newRun = PipelineRun(
                id = runId,
                workflowName = "Deploy Microservices to K8s",
                runNumber = runNumber,
                branch = branch,
                commitSha = commitSha,
                commitMessage = "deploy: release $commitSha with $strategy",
                author = "devops-engineer",
                startedAt = "Just now",
                durationSeconds = 12,
                status = PipelineStatus.RUNNING,
                stages = initialStages
            )

            _pipelineRuns.value = listOf(newRun) + _pipelineRuns.value

            addLog(
                level = LogLevel.INFO,
                service = "github-actions",
                pod = "runner-ubuntu-x64",
                message = "GitHub Actions workflow run #$runNumber triggered by devops-engineer on branch $branch [$commitSha]"
            )

            // Stage 1 finish
            delay(2500)
            updateStage(runId, 0, PipelineStatus.SUCCESS, 14)
            updateStage(runId, 1, PipelineStatus.RUNNING, 10)
            addLog(LogLevel.INFO, "docker-daemon", "buildx-builder", "Built multi-arch image ghcr.io/org/order-service:$commitSha (size: 28.4MB)")

            // Stage 2 finish
            delay(2800)
            updateStage(runId, 1, PipelineStatus.SUCCESS, 24)
            updateStage(runId, 2, PipelineStatus.RUNNING, 6)
            addLog(LogLevel.INFO, "trivy-scanner", "security-agent", "Security scan completed: 0 Critical, 0 High vulnerabilities.")

            // Stage 3 finish
            delay(2200)
            updateStage(runId, 2, PipelineStatus.SUCCESS, 8)
            updateStage(runId, 3, PipelineStatus.RUNNING, 12)
            addLog(LogLevel.INFO, "argo-rollouts", "k8s-apiserver", "Deploying image ghcr.io/org/order-service:$commitSha via $strategy")

            // Stage 4 finish
            delay(2500)
            updateStage(runId, 3, PipelineStatus.SUCCESS, 15)
            _pipelineRuns.value = _pipelineRuns.value.map { r ->
                if (r.id == runId) r.copy(status = PipelineStatus.SUCCESS, durationSeconds = 61) else r
            }
            addLog(LogLevel.INFO, "k8s-controller", "kubelet", "Deployment order-service rolled out successfully with 0 downtime.")
        }
    }

    private fun updateStage(runId: String, stageIndex: Int, status: PipelineStatus, durationSec: Int) {
        _pipelineRuns.value = _pipelineRuns.value.map { r ->
            if (r.id == runId) {
                val updatedStages = r.stages.toMutableList()
                if (stageIndex in updatedStages.indices) {
                    val st = updatedStages[stageIndex]
                    updatedStages[stageIndex] = st.copy(
                        status = status,
                        durationSec = durationSec,
                        steps = st.steps.map { it.copy(status = status) }
                    )
                }
                r.copy(stages = updatedStages)
            } else r
        }
    }

    fun acknowledgeAlert(alertId: String) {
        _alerts.value = _alerts.value.map { a ->
            if (a.id == alertId) a.copy(state = AlertState.RESOLVED) else a
        }
    }

    private fun addLog(level: LogLevel, service: String, pod: String, message: String) {
        val trace = UUID.randomUUID().toString().replace("-", "").take(16)
        val span = UUID.randomUUID().toString().replace("-", "").take(8)
        val log = FluentdLog(
            id = UUID.randomUUID().toString(),
            timestamp = timeFormat.format(Date()),
            level = level,
            service = service,
            podName = pod,
            message = message,
            traceId = trace,
            spanId = span,
            httpStatus = if (level == LogLevel.ERROR) 500 else if (level == LogLevel.FATAL) 503 else 200,
            durationMs = if (level == LogLevel.ERROR) 420.5 else 12.4,
            rawJson = """{"time":"${timeFormat.format(Date())}","level":"${level.name}","service":"$service","pod":"$pod","trace_id":"$trace","span_id":"$span","msg":"$message"}"""
        )
        _logs.value = listOf(log) + _logs.value.take(200)
    }

    private fun generateRandomLog() {
        val sampleServices = listOf("api-gateway", "auth-service", "order-service", "payment-service", "inventory-service", "notification-service")
        val service = sampleServices.random()
        val pod = "$service-${Integer.toHexString(service.hashCode()).take(4)}-${Random.nextInt(1, 4)}"

        val messages = listOf(
            "Processed GET /api/v1/$service/health - 200 OK (8ms)" to LogLevel.INFO,
            "Incoming request routed via Envoy mesh to internal ClusterIP" to LogLevel.INFO,
            "Cache hit for key stock_item_${Random.nextInt(100, 999)} in Redis cluster" to LogLevel.DEBUG,
            "Completed database read query with connection pool idle=18 active=4" to LogLevel.INFO,
            "Slow database query execution: elapsed 142ms (>100ms threshold)" to LogLevel.WARN,
            "Fluentd forwarded 64KB log chunk to Elasticsearch buffer queue" to LogLevel.INFO
        )

        val (msg, level) = messages.random()
        addLog(level, service, pod, msg)
    }

    // Initial Data Generators
    private fun createInitialServices(): List<Microservice> = listOf(
        Microservice(
            id = "api-gateway",
            name = "API Gateway",
            description = "Envoy edge proxy, rate-limiting & TLS termination",
            dockerImage = "envoyproxy/envoy:v1.28-distroless",
            version = "v1.28.0",
            port = 8080,
            desiredReplicas = 3,
            currentReplicas = 3,
            cpuUsagePercent = 42,
            memoryUsageMb = 184,
            status = ServiceStatus.HEALTHY,
            dependencies = listOf("auth-service", "order-service"),
            requestsPerSec = 1450,
            avgLatencyMs = 6.2
        ),
        Microservice(
            id = "auth-service",
            name = "Auth Service",
            description = "OAuth2, JWT issuer & IAM token verification",
            dockerImage = "ghcr.io/org/auth-service:v1.14.2",
            version = "v1.14.2",
            port = 8081,
            desiredReplicas = 2,
            currentReplicas = 2,
            cpuUsagePercent = 28,
            memoryUsageMb = 142,
            status = ServiceStatus.HEALTHY,
            dependencies = emptyList(),
            requestsPerSec = 380,
            avgLatencyMs = 12.4
        ),
        Microservice(
            id = "order-service",
            name = "Order Service",
            description = "Order checkout orchestration & saga pattern state",
            dockerImage = "ghcr.io/org/order-service:v2.4.1",
            version = "v2.4.1",
            port = 8082,
            desiredReplicas = 4,
            currentReplicas = 4,
            cpuUsagePercent = 54,
            memoryUsageMb = 296,
            status = ServiceStatus.HEALTHY,
            dependencies = listOf("payment-service", "inventory-service", "notification-service"),
            requestsPerSec = 780,
            avgLatencyMs = 24.8
        ),
        Microservice(
            id = "payment-service",
            name = "Payment Service",
            description = "Stripe/PayPal payment gateway with PCI DSS compliance",
            dockerImage = "ghcr.io/org/payment-service:v3.1.0",
            version = "v3.1.0",
            port = 8083,
            desiredReplicas = 3,
            currentReplicas = 3,
            cpuUsagePercent = 38,
            memoryUsageMb = 210,
            status = ServiceStatus.HEALTHY,
            dependencies = emptyList(),
            requestsPerSec = 290,
            avgLatencyMs = 45.2
        ),
        Microservice(
            id = "inventory-service",
            name = "Inventory Service",
            description = "Real-time stock reservation & distributed lock",
            dockerImage = "ghcr.io/org/inventory-service:v1.9.0",
            version = "v1.9.0",
            port = 8084,
            desiredReplicas = 3,
            currentReplicas = 3,
            cpuUsagePercent = 32,
            memoryUsageMb = 168,
            status = ServiceStatus.HEALTHY,
            dependencies = emptyList(),
            requestsPerSec = 610,
            avgLatencyMs = 14.1
        ),
        Microservice(
            id = "notification-service",
            name = "Notification Service",
            description = "Kafka event consumer for push, SMS & transactional email",
            dockerImage = "ghcr.io/org/notification-service:v2.0.4",
            version = "v2.0.4",
            port = 8085,
            desiredReplicas = 2,
            currentReplicas = 2,
            cpuUsagePercent = 21,
            memoryUsageMb = 128,
            status = ServiceStatus.HEALTHY,
            dependencies = emptyList(),
            requestsPerSec = 190,
            avgLatencyMs = 8.5
        )
    )

    private fun createInitialNodes(): List<K8sNode> = listOf(
        K8sNode("k8s-node-worker-01", "Worker / General", "Ready", 52, 64, 18, 110),
        K8sNode("k8s-node-worker-02", "Worker / MemoryOptimized", "Ready", 46, 58, 16, 110),
        K8sNode("k8s-node-spot-03", "Worker / Spot-Autoscale", "Ready", 61, 71, 14, 110)
    )

    private fun createInitialPods(): List<PodInstance> {
        val pods = mutableListOf<PodInstance>()
        val services = createInitialServices()
        val nodeNames = listOf("k8s-node-worker-01", "k8s-node-worker-02", "k8s-node-spot-03")
        services.forEach { service ->
            for (i in 1..service.currentReplicas) {
                val node = nodeNames[(i + service.port) % nodeNames.size]
                pods.add(
                    PodInstance(
                        podName = "${service.id}-${Integer.toHexString(service.id.hashCode()).take(5)}-$i",
                        nodeName = node,
                        ip = "10.244.${(service.port % 10)}.$i",
                        status = "Running",
                        restarts = 0,
                        age = "4d 12h",
                        cpuPercent = Random.nextInt(18, 55),
                        memMb = Random.nextInt(140, 290)
                    )
                )
            }
        }
        return pods
    }

    private fun createInitialHpas(): List<HpaConfig> = listOf(
        HpaConfig("order-service", 2, 15, 4, 70, 54, "Scaled to 4 pods on traffic peak"),
        HpaConfig("api-gateway", 3, 20, 3, 75, 42, "Steady state"),
        HpaConfig("payment-service", 2, 10, 3, 65, 38, "Scaled up for safety margin"),
        HpaConfig("inventory-service", 2, 12, 3, 70, 32, "Steady state"),
        HpaConfig("auth-service", 2, 8, 2, 80, 28, "Steady state"),
        HpaConfig("notification-service", 2, 6, 2, 75, 21, "Steady state")
    )

    private fun createInitialPipelines(): List<PipelineRun> = listOf(
        PipelineRun(
            id = "run-42",
            workflowName = "Deploy Microservices to K8s",
            runNumber = 42,
            branch = "main",
            commitSha = "c89f1a2",
            commitMessage = "feat(scaling): configure HPA v2 with custom Prometheus metrics",
            author = "alex-sre",
            startedAt = "18 mins ago",
            durationSeconds = 84,
            status = PipelineStatus.SUCCESS,
            stages = listOf(
                PipelineStage("Lint & Unit Tests", PipelineStatus.SUCCESS, 18, listOf(
                    PipelineStep("Go Vet & Lint", PipelineStatus.SUCCESS, 6, listOf("ok")),
                    PipelineStep("Go Test Race", PipelineStatus.SUCCESS, 12, listOf("PASS: coverage 88.4%"))
                )),
                PipelineStage("Docker Build & Push", PipelineStatus.SUCCESS, 36, listOf(
                    PipelineStep("Setup Buildx", PipelineStatus.SUCCESS, 4, listOf("Buildx ready")),
                    PipelineStep("Build and Push Multi-arch", PipelineStatus.SUCCESS, 32, listOf("Pushed to ghcr.io"))
                )),
                PipelineStage("Trivy Vulnerability Scan", PipelineStatus.SUCCESS, 12, listOf(
                    PipelineStep("CVE Analysis", PipelineStatus.SUCCESS, 12, listOf("0 Critical, 0 High"))
                )),
                PipelineStage("K8s Rolling Rollout", PipelineStatus.SUCCESS, 18, listOf(
                    PipelineStep("kubectl apply -f k8s/", PipelineStatus.SUCCESS, 6, listOf("Deployment configured")),
                    PipelineStep("rollout status check", PipelineStatus.SUCCESS, 12, listOf("deployment 'order-service' successfully rolled out"))
                ))
            )
        ),
        PipelineRun(
            id = "run-41",
            workflowName = "CI Test Suite & Security Gate",
            runNumber = 41,
            branch = "feature/fluentd-parser",
            commitSha = "3a7d2b4",
            commitMessage = "refactor(logs): add structured JSON parser in fluent.conf",
            author = "sarah-devops",
            startedAt = "2 hours ago",
            durationSeconds = 48,
            status = PipelineStatus.SUCCESS,
            stages = listOf(
                PipelineStage("Lint & Unit Tests", PipelineStatus.SUCCESS, 14, emptyList()),
                PipelineStage("Docker Build & Push", PipelineStatus.SUCCESS, 24, emptyList()),
                PipelineStage("Trivy Vulnerability Scan", PipelineStatus.SUCCESS, 10, emptyList())
            )
        )
    )

    private fun createInitialMetrics(): PrometheusMetrics {
        val now = System.currentTimeMillis() / 1000
        val qpsPts = mutableListOf<MetricPoint>()
        val latPts = mutableListOf<MetricPoint>()
        val errPts = mutableListOf<MetricPoint>()
        val cpuPts = mutableListOf<MetricPoint>()

        for (i in 20 downTo 0) {
            val t = now - (i * 15)
            qpsPts.add(MetricPoint(t, 1380f + Random.nextInt(-40, 50)))
            latPts.add(MetricPoint(t, 18.2f + Random.nextFloat() * 2.5f))
            errPts.add(MetricPoint(t, 0.05f + Random.nextFloat() * 0.04f))
            cpuPts.add(MetricPoint(t, 44f + Random.nextFloat() * 3.0f))
        }

        return PrometheusMetrics(
            totalQps = 1420f,
            p95LatencyMs = 18.5f,
            p99LatencyMs = 38.2f,
            errorRatePercent = 0.07f,
            avgCpuUsagePercent = 45.2f,
            avgMemoryUsagePercent = 63.1f,
            qpsHistory = qpsPts,
            latencyHistory = latPts,
            errorHistory = errPts,
            cpuHistory = cpuPts
        )
    }

    private fun createInitialAlerts(): List<GrafanaAlert> = listOf(
        GrafanaAlert(
            id = "alert-01",
            title = "TargetPodCpuUtilizationWarn",
            state = AlertState.RESOLVED,
            severity = "P2 - Warning",
            service = "order-service",
            summary = "Average CPU was > 65% for 3 minutes before HPA scaled replicas",
            triggeredAt = "45m ago",
            currentVal = "54%",
            threshold = "> 65%"
        ),
        GrafanaAlert(
            id = "alert-02",
            title = "FluentdBufferQueueLength",
            state = AlertState.RESOLVED,
            severity = "P3 - Info",
            service = "fluentd-aggregator",
            summary = "Fluentd buffer chunk queue length returned to baseline (<4 chunks)",
            triggeredAt = "1h ago",
            currentVal = "1 chunk",
            threshold = "> 16 chunks"
        )
    )

    private fun createInitialLogs(): List<FluentdLog> = listOf(
        FluentdLog(
            id = "log-1",
            timestamp = "10:14:02",
            level = LogLevel.INFO,
            service = "api-gateway",
            podName = "api-gateway-edge-01",
            message = "GET /api/v1/orders/10842 - 200 OK - 14ms (Envoy cluster: order-service)",
            traceId = "4a98f12c8e31004a",
            spanId = "1a88b021",
            httpStatus = 200,
            durationMs = 14.2,
            rawJson = """{"time":"10:14:02Z","level":"INFO","app":"api-gateway","path":"/api/v1/orders/10842","status":200,"dur_ms":14.2}"""
        ),
        FluentdLog(
            id = "log-2",
            timestamp = "10:14:01",
            level = LogLevel.INFO,
            service = "order-service",
            podName = "order-service-4a9f-1",
            message = "Saga step completed: reserved inventory itemId=4821 qty=2",
            traceId = "4a98f12c8e31004a",
            spanId = "2b77c042",
            httpStatus = 200,
            durationMs = 8.1,
            rawJson = """{"time":"10:14:01Z","level":"INFO","app":"order-service","action":"reserve_stock","item_id":4821,"qty":2}"""
        ),
        FluentdLog(
            id = "log-3",
            timestamp = "10:13:58",
            level = LogLevel.INFO,
            service = "payment-service",
            podName = "payment-service-82bb-2",
            message = "Payment intent pi_948194 succeeded for amount $128.50 USD",
            traceId = "4a98f12c8e31004a",
            spanId = "9f32d881",
            httpStatus = 200,
            durationMs = 38.4,
            rawJson = """{"time":"10:13:58Z","level":"INFO","app":"payment-service","intent":"pi_948194","amount":128.50,"currency":"USD"}"""
        ),
        FluentdLog(
            id = "log-4",
            timestamp = "10:13:50",
            level = LogLevel.DEBUG,
            service = "fluentd-daemonset",
            podName = "fluentd-node-01-tail",
            message = "Tailed container log /var/log/containers/order-service-*.log (bytes read: 4096)",
            traceId = "0000000000000000",
            spanId = "00000000",
            httpStatus = null,
            durationMs = null,
            rawJson = """{"time":"10:13:50Z","level":"DEBUG","source":"in_tail","bytes":4096}"""
        ),
        FluentdLog(
            id = "log-5",
            timestamp = "10:13:42",
            level = LogLevel.WARN,
            service = "inventory-service",
            podName = "inventory-service-33aa-1",
            message = "Low inventory warning for SKU SKU-TECH-882: 4 remaining in warehouse-east",
            traceId = "5c110992a838120b",
            spanId = "4e12c882",
            httpStatus = 200,
            durationMs = 12.0,
            rawJson = """{"time":"10:13:42Z","level":"WARN","app":"inventory-service","sku":"SKU-TECH-882","remaining":4}"""
        )
    )
}
