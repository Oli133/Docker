package com.example.data.model

object DevOpsManifests {

    val DOCKERFILE = """
# Multi-stage production Dockerfile with minimal footprint & non-root user
FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o /app/server ./cmd/api

FROM alpine:3.19
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN apk --no-cache add ca-certificates tzdata
WORKDIR /app
COPY --from=builder /app/server /app/server
USER appuser:appgroup
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/healthz || exit 1
ENTRYPOINT ["/app/server"]
""".trimIndent()

    val GITHUB_ACTIONS_WORKFLOW = """
name: CI/CD Microservice Pipeline

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${'$'}{{ github.repository }}/order-service

jobs:
  lint-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Go
        uses: actions/setup-go@v5
        with:
          go-version: '1.22'
      - name: Run Unit Tests & Lint
        run: |
          go vet ./...
          go test -race -covermode=atomic -coverprofile=coverage.out ./...

  docker-build-push:
    needs: lint-and-test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - name: Set up QEMU & Docker Buildx
        uses: docker/setup-buildx-action@v3
      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${'$'}{{ env.REGISTRY }}
          username: ${'$'}{{ github.actor }}
          password: ${'$'}{{ secrets.GITHUB_TOKEN }}
      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${'$'}{{ env.REGISTRY }}/${'$'}{{ env.IMAGE_NAME }}
          tags: |
            type=sha,format=short
            type=raw,value=latest
      - name: Build and Push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${'$'}{{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy-kubernetes:
    needs: docker-build-push
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Authenticate to K8s Cluster
        uses: azure/k8s-set-context@v3
        with:
          method: kubeconfig
          kubeconfig: ${'$'}{{ secrets.KUBECONFIG }}
      - name: Rolling Deployment with Helm / Kustomize
        run: |
          kubectl set image deployment/order-service \
            order-service=${'$'}{{ env.REGISTRY }}/${'$'}{{ env.IMAGE_NAME }}:${'$'}{{ github.sha }} \
            -n production
          kubectl rollout status deployment/order-service -n production --timeout=180s
""".trimIndent()

    val K8S_DEPLOYMENT_HPA = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: production
  labels:
    app: order-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 0
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/metrics"
    spec:
      containers:
      - name: order-service
        image: ghcr.io/company/order-service:v2.4.1
        ports:
        - containerPort: 8080
          name: http
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 512Mi
        livenessProbe:
          httpGet:
            path: /healthz
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 2
  maxReplicas: 15
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
""".trimIndent()

    val PROMETHEUS_CONFIG = """
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "/etc/prometheus/alerts/*.rules"

scrape_configs:
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: ${'$'}1:${'$'}2
        target_label: __address__
      - action: labelmap
        regex: __meta_kubernetes_pod_label_(.+)

alerting:
  alertmanagers:
  - static_configs:
    - targets:
      - 'alertmanager.monitoring.svc.cluster.local:9093'
""".trimIndent()

    val FLUENTD_CONFIG = """
<source>
  @type tail
  @id in_tail_container_logs
  path /var/log/containers/*.log
  pos_file /var/log/fluentd-containers.log.pos
  tag kubernetes.*
  read_from_head true
  <parse>
    @type json
    time_key time
    time_format %Y-%m-%dT%H:%M:%S.%NZ
  </parse>
</source>

<filter kubernetes.**>
  @type kubernetes_metadata
  @id filter_kube_metadata
</filter>

<filter kubernetes.**>
  @type record_transformer
  enable_ruby true
  <record>
    cluster "production-east-01"
    service_name ${'$'}{record.dig("kubernetes", "labels", "app") || "unknown"}
    env "production"
  </record>
</filter>

<match kubernetes.**>
  @type elasticsearch
  @id out_es
  host elasticsearch.logging.svc.cluster.local
  port 9200
  logstash_format true
  logstash_prefix k8s-logs
  <buffer>
    @type file
    path /var/log/fluentd-buffers/kubernetes.system.buffer
    flush_mode interval
    retry_type exponential_backoff
    flush_interval 5s
    chunk_limit_size 2M
    queue_limit_length 32
  </buffer>
</match>
""".trimIndent()
}
