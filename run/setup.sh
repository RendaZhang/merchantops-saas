#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

APT_GET="apt-get"
if ! command -v apt-get >/dev/null 2>&1; then
  echo "❌ apt-get 未找到，当前脚本仅支持 Debian/Ubuntu 环境。" >&2
  exit 1
fi

if command -v sudo >/dev/null 2>&1; then
  APT_GET="sudo apt-get"
fi

echo "📦 更新软件源..."
$APT_GET update -y

echo "🐳 安装 Docker CLI/daemon（docker.io）..."
$APT_GET install -y docker.io

echo "☕ 安装 Maven（若缺失）..."
$APT_GET install -y maven

if [ -f "mvnw" ]; then
  chmod +x mvnw
fi

MVN_CMD="mvn"
if [ -x "./mvnw" ]; then
  MVN_CMD="./mvnw"
fi

echo "🔍 校验 Docker CLI..."
docker --version

if ! docker info >/dev/null 2>&1; then
  echo "⚙️ Docker daemon 未启动，尝试自动启动..."
  if command -v systemctl >/dev/null 2>&1; then
    (sudo systemctl start docker || systemctl start docker || true)
  fi

  if ! docker info >/dev/null 2>&1; then
    if command -v dockerd >/dev/null 2>&1; then
      nohup dockerd >/tmp/dockerd.log 2>&1 &
      sleep 5
    fi
  fi
fi

if docker info >/dev/null 2>&1; then
  echo "✅ Docker daemon 可用。"
else
  echo "⚠️ Docker daemon 仍不可用（容器权限/宿主机限制），仅验证到 Docker CLI 安装成功。"
fi

echo "🔍 校验 Maven..."
$MVN_CMD -version

echo "🏗️ 执行 Testing Role 默认回归命令..."
$MVN_CMD -pl merchantops-api -am test
