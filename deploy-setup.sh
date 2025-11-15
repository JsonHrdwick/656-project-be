#!/bin/bash

# Deployment setup script for EC2
# This script sets up the Spring Boot application on EC2 instance

set -e

APP_DIR="$HOME/app"
SERVICE_FILE="/etc/systemd/system/springboot-app.service"

echo "Setting up Spring Boot application on EC2..."

# Create app directory
mkdir -p "$APP_DIR"
mkdir -p "$APP_DIR/uploads"

# Check if ..env file exists
if [ ! -f "$APP_DIR/.env" ]; then
    echo "WARNING: .env file not found at $APP_DIR/.env"
    echo "Please create it with the required environment variables"
    echo "See .env.example for reference"
    exit 1
fi

# Install systemd service if service file exists in repo
if [ -f "$HOME/springboot-app.service" ]; then
    echo "Installing systemd service..."
    sudo cp "$HOME/springboot-app.service" "$SERVICE_FILE"
    sudo chmod 644 "$SERVICE_FILE"
    sudo systemctl daemon-reload
    sudo systemctl enable springboot-app.service
    echo "Systemd service installed and enabled"
fi

# Ensure proper permissions
chmod 600 "$APP_DIR/.env"
chmod +x "$APP_DIR/app.jar" 2>/dev/null || true

# Check if PostgreSQL is running
if ! pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
    echo "WARNING: PostgreSQL does not appear to be running or accessible"
    echo "Please ensure PostgreSQL is installed and running"
fi

echo "Setup complete!"
echo ""
echo "To start the application:"
echo "  sudo systemctl start springboot-app"
echo ""
echo "To check status:"
echo "  sudo systemctl status springboot-app"
echo ""
echo "To view logs:"
echo "  sudo journalctl -u springboot-app -f"

