package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.DevOpsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DevOpsViewModel(
    private val repository: DevOpsRepository = DevOpsRepository()
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0) // 0: Overview, 1: Pipelines, 2: Observability, 3: Logs
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val services: StateFlow<List<Microservice>> = repository.services
    val nodes: StateFlow<List<K8sNode>> = repository.nodes
    val pods: StateFlow<List<PodInstance>> = repository.pods
    val hpaConfigs: StateFlow<List<HpaConfig>> = repository.hpaConfigs
    val pipelineRuns: StateFlow<List<PipelineRun>> = repository.pipelineRuns
    val metrics: StateFlow<PrometheusMetrics> = repository.metrics
    val alerts: StateFlow<List<GrafanaAlert>> = repository.alerts
    val logs: StateFlow<List<FluentdLog>> = repository.logs

    private val _selectedService = MutableStateFlow<Microservice?>(null)
    val selectedService: StateFlow<Microservice?> = _selectedService.asStateFlow()

    private val _selectedPipelineRun = MutableStateFlow<PipelineRun?>(null)
    val selectedPipelineRun: StateFlow<PipelineRun?> = _selectedPipelineRun.asStateFlow()

    private val _logFilterService = MutableStateFlow<String?>(null)
    val logFilterService: StateFlow<String?> = _logFilterService.asStateFlow()

    private val _logFilterLevel = MutableStateFlow<LogLevel?>(null)
    val logFilterLevel: StateFlow<LogLevel?> = _logFilterLevel.asStateFlow()

    private val _logSearchQuery = MutableStateFlow("")
    val logSearchQuery: StateFlow<String> = _logSearchQuery.asStateFlow()

    private val _isAutoScrollLogs = MutableStateFlow(true)
    val isAutoScrollLogs: StateFlow<Boolean> = _isAutoScrollLogs.asStateFlow()

    private val _promQlQuery = MutableStateFlow("sum(rate(http_requests_total[5m])) by (service)")
    val promQlQuery: StateFlow<String> = _promQlQuery.asStateFlow()

    private val _showChaosDialog = MutableStateFlow(false)
    val showChaosDialog: StateFlow<Boolean> = _showChaosDialog.asStateFlow()

    private val _showManifestDialog = MutableStateFlow(false)
    val showManifestDialog: StateFlow<Boolean> = _showManifestDialog.asStateFlow()

    private val _showDeployDialog = MutableStateFlow(false)
    val showDeployDialog: StateFlow<Boolean> = _showDeployDialog.asStateFlow()

    // Filtered logs stream
    val filteredLogs: StateFlow<List<FluentdLog>> = combine(
        logs,
        logFilterService,
        logFilterLevel,
        logSearchQuery
    ) { allLogs, serviceFilter, levelFilter, query ->
        allLogs.filter { log ->
            val matchesService = serviceFilter == null || log.service == serviceFilter
            val matchesLevel = levelFilter == null || log.level == levelFilter
            val matchesQuery = query.isEmpty() ||
                    log.message.contains(query, ignoreCase = true) ||
                    log.service.contains(query, ignoreCase = true) ||
                    log.traceId.contains(query, ignoreCase = true)
            matchesService && matchesLevel && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun selectService(service: Microservice?) {
        _selectedService.value = service
    }

    fun selectPipelineRun(run: PipelineRun?) {
        _selectedPipelineRun.value = run
    }

    fun scaleService(serviceId: String, replicas: Int) {
        repository.scaleServiceReplicas(serviceId, replicas)
        // update selected service view if open
        _selectedService.value?.let { current ->
            if (current.id == serviceId) {
                _selectedService.value = current.copy(desiredReplicas = replicas, currentReplicas = replicas)
            }
        }
    }

    fun triggerDeployment(branch: String, strategy: String) {
        _showDeployDialog.value = false
        repository.triggerDeployment(branch, strategy)
    }

    fun triggerTrafficSpike(multiplier: Float) {
        repository.triggerTrafficSpike(multiplier)
    }

    fun injectLatency(serviceId: String) {
        repository.injectServiceLatency(serviceId)
    }

    fun injectCrash(serviceId: String) {
        repository.injectCrashLoop(serviceId)
    }

    fun resetCluster() {
        repository.resetClusterToNormal()
    }

    fun acknowledgeAlert(alertId: String) {
        repository.acknowledgeAlert(alertId)
    }

    fun setLogFilterService(service: String?) {
        _logFilterService.value = service
    }

    fun setLogFilterLevel(level: LogLevel?) {
        _logFilterLevel.value = level
    }

    fun setLogSearchQuery(query: String) {
        _logSearchQuery.value = query
    }

    fun toggleAutoScrollLogs() {
        _isAutoScrollLogs.value = !_isAutoScrollLogs.value
    }

    fun setPromQlQuery(query: String) {
        _promQlQuery.value = query
    }

    fun setShowChaosDialog(show: Boolean) {
        _showChaosDialog.value = show
    }

    fun setShowManifestDialog(show: Boolean) {
        _showManifestDialog.value = show
    }

    fun setShowDeployDialog(show: Boolean) {
        _showDeployDialog.value = show
    }
}
