package com.example.ui.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun ClusterOverviewScreen(
    services: List<Microservice>,
    nodes: List<K8sNode>,
    pods: List<PodInstance>,
    hpas: List<HpaConfig>,
    metrics: PrometheusMetrics,
    onSelectService: (Microservice) -> Unit,
    onScaleService: (String, Int) -> Unit,
    onOpenChaos: () -> Unit,
    onOpenManifests: () -> Unit,
    onOpenDeploy: () -> Unit
) {
    var showTopology by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Top Cluster Status Banner
        item {
            ClusterHeroBanner(
                servicesCount = services.size,
                podsCount = pods.size,
                totalQps = metrics.totalQps,
                avgCpu = metrics.avgCpuUsagePercent,
                onOpenChaos = onOpenChaos,
                onOpenManifests = onOpenManifests,
                onOpenDeploy = onOpenDeploy
            )
        }

        // Architecture Topology Mesh Visualizer Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "Topology Icon",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SERVICE MESH TOPOLOGY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = { showTopology = !showTopology },
                    modifier = Modifier.testTag("toggle_topology_button")
                ) {
                    Text(if (showTopology) "Collapse" else "Expand")
                }
            }
        }

        if (showTopology) {
            item {
                ServiceMeshCanvas(services = services)
            }
        }

        // Section: Microservices
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTAINERIZED MICROSERVICES (${services.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap to inspect Docker spec",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(services, key = { it.id }) { service ->
            MicroserviceCard(
                service = service,
                onClick = { onSelectService(service) },
                onScale = { delta -> onScaleService(service.id, service.currentReplicas + delta) }
            )
        }

        // Section: Kubernetes HPA Autoscalers
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Autoscaler",
                    tint = NeonTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HORIZONTAL POD AUTOSCALER (HPA v2)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        items(hpas, key = { it.serviceId }) { hpa ->
            HpaConfigCard(hpa = hpa)
        }

        // Section: Kubernetes Nodes
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = "Nodes",
                    tint = K8sBlueLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KUBERNETES CLUSTER NODES (${nodes.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        items(nodes, key = { it.name }) { node ->
            K8sNodeCard(node = node)
        }
    }
}

@Composable
fun ClusterHeroBanner(
    servicesCount: Int,
    podsCount: Int,
    totalQps: Float,
    avgCpu: Float,
    onOpenChaos: () -> Unit,
    onOpenManifests: () -> Unit,
    onOpenDeploy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cluster_hero_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "K8s Cluster: production-east-01",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = K8sBlue.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, K8sBlue.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "v1.30.2",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = K8sBlueLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Stats in row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ClusterMiniStat(label = "Services", value = "$servicesCount active", color = CyanAccent)
                ClusterMiniStat(label = "Pods", value = "$podsCount running", color = NeonTeal)
                ClusterMiniStat(label = "Traffic", value = "${totalQps.toInt()} req/s", color = PrometheusOrange)
                ClusterMiniStat(label = "Cluster CPU", value = "${avgCpu.toInt()}%", color = FluentdCyan)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Quick Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenDeploy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_deploy_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = K8sBlue)
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deploy CI/CD", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenChaos,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_chaos_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chaos Lab", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenManifests,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_manifests_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Manifests", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ClusterMiniStat(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun ServiceMeshCanvas(services: List<Microservice>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .testTag("topology_canvas_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw connection lines
                // Client (left center) -> Gateway (x=0.25, y=0.5)
                val clientP = Offset(w * 0.10f, h * 0.5f)
                val gwP = Offset(w * 0.32f, h * 0.5f)
                val authP = Offset(w * 0.58f, h * 0.25f)
                val orderP = Offset(w * 0.58f, h * 0.75f)
                val payP = Offset(w * 0.88f, h * 0.20f)
                val invP = Offset(w * 0.88f, h * 0.52f)
                val notifP = Offset(w * 0.88f, h * 0.82f)

                val stroke = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))

                drawLine(color = lineColor, start = clientP, end = gwP, strokeWidth = 3f)
                drawLine(color = lineColor, start = gwP, end = authP, strokeWidth = 3f)
                drawLine(color = lineColor, start = gwP, end = orderP, strokeWidth = 3f)
                drawLine(color = lineColor, start = orderP, end = payP, strokeWidth = 2f)
                drawLine(color = lineColor, start = orderP, end = invP, strokeWidth = 2f)
                drawLine(color = lineColor, start = orderP, end = notifP, strokeWidth = 2f)
            }

            // Interactive nodes overlays
            MeshNode(
                title = "Client",
                sub = "Traffic",
                icon = Icons.Default.Language,
                color = TextSecondaryDark,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            )
            MeshNode(
                title = "Gateway",
                sub = "Envoy :8080",
                icon = Icons.Default.FilterAlt,
                color = CyanAccent,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 100.dp)
            )
            MeshNode(
                title = "Auth",
                sub = "JWT :8081",
                icon = Icons.Default.Security,
                color = K8sBlueLight,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp, start = 50.dp)
            )
            MeshNode(
                title = "Order",
                sub = "gRPC :8082",
                icon = Icons.Default.ShoppingCart,
                color = PrometheusOrange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp, start = 50.dp)
            )
            MeshNode(
                title = "Payment",
                sub = ":8083",
                icon = Icons.Default.CreditCard,
                color = NeonTeal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp)
            )
            MeshNode(
                title = "Inventory",
                sub = ":8084",
                icon = Icons.Default.Inventory2,
                color = FluentdCyan,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )
            MeshNode(
                title = "Notif",
                sub = ":8085",
                icon = Icons.Default.Notifications,
                color = WarningYellow,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 10.dp, end = 12.dp)
            )
        }
    }
}

@Composable
fun MeshNode(
    title: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        shadowElevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = sub, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MicroserviceCard(
    service: Microservice,
    onClick: () -> Unit,
    onScale: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("service_card_${service.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = when (service.status) {
                    ServiceStatus.HEALTHY -> SuccessGreen
                    ServiceStatus.SCALING -> WarningYellow
                    ServiceStatus.DEGRADED -> PrometheusOrange
                    ServiceStatus.CRASH_LOOP -> DangerRed
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${service.dockerImage} • Port ${service.port}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Status Chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = service.status.label,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CPU", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${service.cpuUsagePercent}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (service.cpuUsagePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(65.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (service.cpuUsagePercent > 70) DangerRed else CyanAccent,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
                Column {
                    Text("Memory", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${service.memoryUsageMb}MB", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (service.memoryUsageMb / service.memoryLimitMb.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(65.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonTeal,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
                Column {
                    Text("Throughput", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${service.requestsPerSec} rps", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Latency", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${service.avgLatencyMs.toInt()}ms",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (service.avgLatencyMs > 100) DangerRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Replicas & Scaling Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = K8sBlueLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Replicas: ${service.currentReplicas} / ${service.desiredReplicas} pods",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onScale(-1) },
                        enabled = service.currentReplicas > 1,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("scale_down_${service.id}")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Scale down", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${service.currentReplicas}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = { onScale(1) },
                        enabled = service.currentReplicas < 15,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("scale_up_${service.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Scale up", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HpaConfigCard(hpa: HpaConfig) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hpa_card_${hpa.serviceId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${hpa.serviceId}-hpa",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = NeonTeal.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Min: ${hpa.minReplicas} • Max: ${hpa.maxReplicas}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target CPU: ${hpa.targetCpuPercent}% • Current: ${hpa.currentCpuPercent}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${hpa.currentReplicas} pods",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (hpa.currentCpuPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (hpa.currentCpuPercent > hpa.targetCpuPercent) DangerRed else NeonTeal,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Last event: ${hpa.lastScaleReason}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun K8sNodeCard(node: K8sNode) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("node_card_${node.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = node.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = node.role,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pods: ${node.podCount}/${node.maxPods}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "CPU: ${node.cpuAllocatedPercent}% | Mem: ${node.memoryAllocatedPercent}%",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
