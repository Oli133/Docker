package com.example

import com.example.data.repository.DevOpsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun devopsRepository_initializesMicroservices() {
        val repo = DevOpsRepository()
        val services = repo.services.value
        assertEquals(6, services.size)
        assertTrue(services.any { it.id == "order-service" })
        assertTrue(services.any { it.id == "api-gateway" })

        val hpas = repo.hpaConfigs.value
        assertEquals(6, hpas.size)
        val orderHpa = hpas.find { it.serviceId == "order-service" }
        assertNotNull(orderHpa)
        assertEquals(2, orderHpa?.minReplicas)
        assertEquals(15, orderHpa?.maxReplicas)
    }

    @Test
    fun devopsRepository_scalingUpdatesReplicasAndPods() {
        val repo = DevOpsRepository()
        repo.scaleServiceReplicas("order-service", 6, "Unit test scale")
        val orderService = repo.services.value.first { it.id == "order-service" }
        assertEquals(6, orderService.currentReplicas)

        val pods = repo.pods.value.filter { it.podName.startsWith("order-service") }
        assertEquals(6, pods.size)
    }

    @Test
    fun devopsRepository_chaosSimulationAndReset() {
        val repo = DevOpsRepository()
        repo.triggerTrafficSpike(3.0f)
        repo.injectServiceLatency("order-service")
        repo.injectCrashLoop("payment-service")

        repo.resetClusterToNormal()
        val services = repo.services.value
        val payment = services.first { it.id == "payment-service" }
        assertEquals(3, payment.currentReplicas)
    }
}
