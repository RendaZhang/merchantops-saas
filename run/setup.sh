#!/bin/bash
set -e

# ------------------------------
# 前置准备：非交互安装 + 更新源
# ------------------------------
export DEBIAN_FRONTEND=noninteractive
sudo apt-get update -y

# ------------------------------
# 执行 Maven 构建
# ------------------------------
echo "🏗️ 执行 Maven 构建..."
mvn -pl merchantops-api -am -DskipTests install
