package com.example.data.model

data class PipelineRun(
    val id: String,
    val workflowName: String,
    val runNumber: Int,
    val branch: String,
    val commitSha: String,
    val commitMessage: String,
    val author: String,
    val startedAt: String,
    val durationSeconds: Int,
    val status: PipelineStatus,
    val stages: List<PipelineStage>,
    val triggerType: String = "push"
)

enum class PipelineStatus(val label: String) {
    QUEUED("Queued"),
    RUNNING("Running"),
    SUCCESS("Success"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

data class PipelineStage(
    val name: String,
    val status: PipelineStatus,
    val durationSec: Int,
    val steps: List<PipelineStep>
)

data class PipelineStep(
    val name: String,
    val status: PipelineStatus,
    val durationSec: Int,
    val logs: List<String>
)
