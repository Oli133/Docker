package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.chaos.ChaosLabDialog
import com.example.ui.logs.FluentdLogsScreen
import com.example.ui.manifests.ManifestsViewerDialog
import com.example.ui.observability.ObservabilityScreen
import com.example.ui.overview.ClusterOverviewScreen
import com.example.ui.overview.ServiceDetailBottomSheet
import com.example.ui.pipelines.DeployDialog
import com.example.ui.pipelines.PipelinesScreen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KubePulseApp(viewModel: DevOpsViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val services by viewModel.services.collectAsStateWithLifecycle()
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val pods by viewModel.pods.collectAsStateWithLifecycle()
    val hpas by viewModel.hpaConfigs.collectAsStateWithLifecycle()
    val pipelineRuns by viewModel.pipelineRuns.collectAsStateWithLifecycle()
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()

    val filteredLogs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val logFilterService by viewModel.logFilterService.collectAsStateWithLifecycle()
    val logFilterLevel by viewModel.logFilterLevel.collectAsStateWithLifecycle()
    val logSearchQuery by viewModel.logSearchQuery.collectAsStateWithLifecycle()
    val isAutoScrollLogs by viewModel.isAutoScrollLogs.collectAsStateWithLifecycle()

    val promQlQuery by viewModel.promQlQuery.collectAsStateWithLifecycle()

    val selectedService by viewModel.selectedService.collectAsStateWithLifecycle()
    val showChaosDialog by viewModel.showChaosDialog.collectAsStateWithLifecycle()
    val showManifestDialog by viewModel.showManifestDialog.collectAsStateWithLifecycle()
    val showDeployDialog by viewModel.showDeployDialog.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KubePulse",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = CyanAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "DEVOPS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setShowChaosDialog(true) },
                        modifier = Modifier.testTag("topbar_chaos_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Chaos Lab", tint = WarningYellow)
                    }
                    IconButton(
                        onClick = { viewModel.setShowManifestDialog(true) },
                        modifier = Modifier.testTag("topbar_manifests_button")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "Manifests", tint = CyanAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Hub, contentDescription = "Overview") },
                    label = { Text("K8s Mesh", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_overview")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.RocketLaunch, contentDescription = "Pipelines") },
                    label = { Text("CI/CD", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_pipelines")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Observability") },
                    label = { Text("Prometheus", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_observability")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Logs") },
                    label = { Text("Fluentd", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_logs")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ClusterOverviewScreen(
                    services = services,
                    nodes = nodes,
                    pods = pods,
                    hpas = hpas,
                    metrics = metrics,
                    onSelectService = { viewModel.selectService(it) },
                    onScaleService = { id, rep -> viewModel.scaleService(id, rep) },
                    onOpenChaos = { viewModel.setShowChaosDialog(true) },
                    onOpenManifests = { viewModel.setShowManifestDialog(true) },
                    onOpenDeploy = { viewModel.setShowDeployDialog(true) }
                )
                1 -> PipelinesScreen(
                    runs = pipelineRuns,
                    onTriggerDeploy = { viewModel.setShowDeployDialog(true) }
                )
                2 -> ObservabilityScreen(
                    metrics = metrics,
                    alerts = alerts,
                    promQlQuery = promQlQuery,
                    onPromQlChange = { viewModel.setPromQlQuery(it) },
                    onAckAlert = { viewModel.acknowledgeAlert(it) }
                )
                3 -> FluentdLogsScreen(
                    logs = filteredLogs,
                    selectedService = logFilterService,
                    selectedLevel = logFilterLevel,
                    searchQuery = logSearchQuery,
                    isAutoScroll = isAutoScrollLogs,
                    onSelectService = { viewModel.setLogFilterService(it) },
                    onSelectLevel = { viewModel.setLogFilterLevel(it) },
                    onSearchChange = { viewModel.setLogSearchQuery(it) },
                    onToggleAutoScroll = { viewModel.toggleAutoScrollLogs() }
                )
            }
        }
    }

    // Modal Bottom Sheet for Service Details
    selectedService?.let { service ->
        ServiceDetailBottomSheet(
            service = service,
            pods = pods,
            onDismiss = { viewModel.selectService(null) },
            onScale = { newReplicas -> viewModel.scaleService(service.id, newReplicas) }
        )
    }

    // Chaos Lab Dialog
    if (showChaosDialog) {
        ChaosLabDialog(
            onDismiss = { viewModel.setShowChaosDialog(false) },
            onTrafficSpike = { viewModel.triggerTrafficSpike(it) },
            onInjectLatency = { viewModel.injectLatency(it) },
            onInjectCrash = { viewModel.injectCrash(it) },
            onReset = { viewModel.resetCluster() }
        )
    }

    // DevOps Manifests Viewer Dialog
    if (showManifestDialog) {
        ManifestsViewerDialog(
            onDismiss = { viewModel.setShowManifestDialog(false) }
        )
    }

    // Deploy Modal Dialog
    if (showDeployDialog) {
        DeployDialog(
            onDismiss = { viewModel.setShowDeployDialog(false) },
            onConfirmDeploy = { branch, strategy ->
                viewModel.triggerDeployment(branch, strategy)
            }
        )
    }
}
