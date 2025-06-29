#!/bin/bash

echo "🚀 Starting Document Processing Application Services"
echo "=================================================="

# Function to check if a port is in use
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        echo "❌ Port $1 is already in use"
        return 1
    else
        echo "✅ Port $1 is available"
        return 0
    fi
}

# Check if ports are available
echo "Checking port availability..."
check_port 8001 || exit 1
check_port 8002 || exit 1
check_port 8080 || exit 1

# Start TrOCR Service
echo ""
echo "📝 Starting TrOCR Service (Port 8001)..."
cd python_microservices
source venv/bin/activate
uvicorn trocr_service:app --host 0.0.0.0 --port 8001 &
TROCR_PID=$!
cd ..

# Start Handwriting Detector Service
echo "🔍 Starting Handwriting Detector Service (Port 8002)..."
cd python_microservices
source venv/bin/activate
uvicorn handwriting_detector_service:app --host 0.0.0.0 --port 8002 &
HANDWRITING_PID=$!
cd ..

# Wait for microservices to start
echo "⏳ Waiting for microservices to start..."
sleep 5

# Check if microservices are running
echo "🔍 Checking microservice health..."
if curl -s http://localhost:8001/health > /dev/null; then
    echo "✅ TrOCR Service is running"
else
    echo "❌ TrOCR Service failed to start"
    exit 1
fi

if curl -s http://localhost:8002/health > /dev/null; then
    echo "✅ Handwriting Detector Service is running"
else
    echo "❌ Handwriting Detector Service failed to start"
    exit 1
fi

# Start Spring Boot Application
echo ""
echo "🌐 Starting Spring Boot Application (Port 8080)..."
mvn spring-boot:run &
SPRING_PID=$!

# Wait for Spring Boot to start
echo "⏳ Waiting for Spring Boot application to start..."
sleep 15

# Check if Spring Boot is running
if curl -s http://localhost:8080/ | grep -q "Document Processing"; then
    echo "✅ Spring Boot Application is running"
else
    echo "❌ Spring Boot Application failed to start"
    exit 1
fi

echo ""
echo "🎉 All services are running successfully!"
echo "=================================================="
echo "📱 Web Interface: http://localhost:8080"
echo "🔧 TrOCR Service: http://localhost:8001"
echo "🔍 Handwriting Detector: http://localhost:8002"
echo "📊 Health Checks:"
echo "   - Spring Boot: http://localhost:8080/api/documents/health"
echo "   - TrOCR: http://localhost:8001/health"
echo "   - Handwriting Detector: http://localhost:8002/health"
echo ""
echo "Press Ctrl+C to stop all services"

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Stopping all services..."
    kill $TROCR_PID 2>/dev/null
    kill $HANDWRITING_PID 2>/dev/null
    kill $SPRING_PID 2>/dev/null
    echo "✅ All services stopped"
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM

# Keep script running
wait 