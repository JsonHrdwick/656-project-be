#!/bin/bash
# Quick fix script to repair systemd service on EC2

set -e

echo "Fixing systemd service..."

# Get current user and paths
CURRENT_USER=$(whoami)
CURRENT_HOME=$(eval echo ~$CURRENT_USER)
APP_DIR="$CURRENT_HOME/app"

# Stop and disable service first
sudo systemctl stop springboot-app.service 2>/dev/null || true
sudo systemctl disable springboot-app.service 2>/dev/null || true

# Find Java
JAVA_PATH=$(which java || echo "/usr/bin/java")
if [ ! -f "$JAVA_PATH" ]; then
  JAVA_PATH="/usr/lib/jvm/java-17-amazon-corretto/bin/java"
  [ ! -f "$JAVA_PATH" ] && JAVA_PATH="/usr/lib/jvm/java-17-openjdk/bin/java"
  [ ! -f "$JAVA_PATH" ] && JAVA_PATH="java"
fi

echo "Current user: $CURRENT_USER"
echo "Home directory: $CURRENT_HOME"
echo "App directory: $APP_DIR"
echo "Java path: $JAVA_PATH"

# Check if files exist
if [ ! -f "$APP_DIR/.env" ]; then
  echo "ERROR: .env file missing at $APP_DIR/.env"
  exit 1
fi

if [ ! -f "$APP_DIR/app.jar" ]; then
  echo "ERROR: app.jar missing at $APP_DIR/app.jar"
  exit 1
fi

# Create fixed service file
cat > /tmp/springboot-app.service << EOF
[Unit]
Description=Spring Boot Application
After=network.target

[Service]
Type=simple
WorkingDirectory=$APP_DIR
EnvironmentFile=$APP_DIR/.env
ExecStart=$JAVA_PATH -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -XX:+UseCompressedOops -XX:+UseCompressedClassPointers -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+DisableExplicitGC -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Duser.timezone=UTC -jar $APP_DIR/app.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=springboot-app

[Install]
WantedBy=multi-user.target
EOF

# Install service
sudo cp /tmp/springboot-app.service /etc/systemd/system/springboot-app.service
sudo chmod 644 /etc/systemd/system/springboot-app.service
sudo systemctl daemon-reload
sudo systemctl enable springboot-app.service

echo "Service file installed. Starting service..."
sudo systemctl start springboot-app.service

sleep 2
sudo systemctl status springboot-app.service --no-pager -l || true

