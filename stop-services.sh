#!/bin/bash

echo "🛑 Stopping Document Processing Application Services"
echo "=================================================="

# Stop Spring Boot application
echo "🌐 Stopping Spring Boot Application..."
pkill -f "spring-boot:run"
pkill -f "DocumentProcessingApplication"

# Stop Python microservices
echo "📝 Stopping TrOCR Service..."
pkill -f "uvicorn.*trocr"

echo "🔍 Stopping Handwriting Detector Service..."
pkill -f "uvicorn.*handwriting"

# Wait a moment for processes to stop
sleep 2

# Check if any processes are still running
if pgrep -f "spring-boot:run" > /dev/null; then
    echo "⚠️  Spring Boot process still running, force killing..."
    pkill -9 -f "spring-boot:run"
fi

if pgrep -f "uvicorn.*trocr" > /dev/null; then
    echo "⚠️  TrOCR process still running, force killing..."
    pkill -9 -f "uvicorn.*trocr"
fi

if pgrep -f "uvicorn.*handwriting" > /dev/null; then
    echo "⚠️  Handwriting Detector process still running, force killing..."
    pkill -9 -f "uvicorn.*handwriting"
fi

echo "✅ All services stopped successfully!" 