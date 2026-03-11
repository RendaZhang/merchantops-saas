#!/bin/bash
set -e

# ------------------------------
# 1. 前置准备：非交互安装 + 更新源
# ------------------------------
export DEBIAN_FRONTEND=noninteractive
sudo apt-get update -y

# ------------------------------
# 2. 安装并配置 MySQL 8.0
# ------------------------------
echo "📦 安装 MySQL 8.0..."
sudo apt-get install -y mysql-server

# 启动 MySQL（适配非 systemd 环境）
echo "🚀 启动 MySQL..."
sudo service mysql start

# 配置 MySQL（对应 env.example 中的变量）
echo "🗄️ 配置 MySQL 数据库和用户..."
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=merchantops
MYSQL_USER=merchantops
MYSQL_PASSWORD=merchantops

# 使用 sudo 执行 mysql 命令（初始 root 无密码）
sudo mysql <<EOF
-- 设置 root 密码
ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';
FLUSH PRIVILEGES;

-- 创建数据库和用户
CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE};
CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'localhost' IDENTIFIED BY '${MYSQL_PASSWORD}';
GRANT ALL PRIVILEGES ON ${MYSQL_DATABASE}.* TO '${MYSQL_USER}'@'localhost';
FLUSH PRIVILEGES;
EOF

# ------------------------------
# 3. 安装并配置 RabbitMQ 3（带 Management 插件）
# ------------------------------
echo "📦 安装 RabbitMQ..."
sudo apt-get install -y rabbitmq-server

# 启动 RabbitMQ
echo "🚀 启动 RabbitMQ..."
sudo service rabbitmq-server start

# 配置 RabbitMQ（对应 env.example 中的变量）
echo "🐰 配置 RabbitMQ 用户和 Management 插件..."
RABBITMQ_DEFAULT_USER=merchantops
RABBITMQ_DEFAULT_PASS=merchantops

# 添加用户、设置权限、启用 Management 插件
sudo rabbitmqctl add_user ${RABBITMQ_DEFAULT_USER} ${RABBITMQ_DEFAULT_PASS} || true  # 忽略用户已存在错误
sudo rabbitmqctl set_user_tags ${RABBITMQ_DEFAULT_USER} administrator
sudo rabbitmqctl set_permissions -p / ${RABBITMQ_DEFAULT_USER} ".*" ".*" ".*"
sudo rabbitmq-plugins enable rabbitmq_management

# ------------------------------
# 4. 安装并启动 Redis 7
# ------------------------------
echo "📦 安装 Redis..."
sudo apt-get install -y redis-server

# 启动 Redis
echo "🚀 启动 Redis..."
sudo service redis-server start

# ------------------------------
# 5. 配置项目 .env 文件
# ------------------------------
echo "⚙️ 生成并配置 .env 文件..."
cp .env.example .env

# 修改 .env 中的连接地址（从 Docker 主机名改为 localhost）
# 注意：如果你的项目连接字符串有特殊格式，可根据实际情况调整 sed 规则
# 这里假设连接字符串使用的是主机名（mysql、rabbitmq、redis），替换为 localhost
sed -i 's/mysql/localhost/g' .env
sed -i 's/rabbitmq/localhost/g' .env
sed -i 's/redis/localhost/g' .env

# ------------------------------
# 6. 执行 Maven 构建
# ------------------------------
echo "🏗️ 执行 Maven 构建..."
mvn -pl merchantops-api -am -DskipTests install

# ------------------------------
# 7. 输出验证信息
# ------------------------------
echo ""
echo "✅ 无 Docker 环境配置完成！"
echo ""
echo "📌 服务连接信息："
echo "  - MySQL: localhost:3306 (用户: ${MYSQL_USER}, 密码: ${MYSQL_PASSWORD}, 数据库: ${MYSQL_DATABASE})"
echo "  - RabbitMQ: localhost:5672 (用户: ${RABBITMQ_DEFAULT_USER}, 密码: ${RABBITMQ_DEFAULT_PASS})"
echo "  - RabbitMQ Management: http://localhost:15672 (同上用户密码)"
echo "  - Redis: localhost:6379 (无密码)"
echo ""
echo "🚀 你可以直接运行项目了！"
