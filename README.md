# AI-Powered Document Processing App

A Spring Boot API for processing driving license documents using **AI-enhanced OCR**, **handwritten text recognition**, and **microservices architecture** for intelligent data extraction and database persistence.

## 🚀 **Key Features**

- **🤖 AI-Enhanced Processing**: Ollama AI and OpenAI integration for intelligent data extraction
- **✍️ Handwritten Text Recognition**: TrOCR microservice for handwritten document processing
- **🔍 Handwriting Detection**: ML-based service to detect handwritten vs printed text
- **🎯 Confidence Scoring**: AI-powered confidence assessment for extraction quality
- **📊 AI Analytics**: Real-time statistics on AI processing performance
- **🔄 Microservices Architecture**: Separate services for different processing tasks
- **📈 Performance Metrics**: Track AI processing rates and confidence levels

## Architecture

### **Microservices**
- **Spring Boot API** (Port 8080) - Main application with web interface
- **TrOCR Service** (Port 8001) - Handwritten text recognition using Microsoft TrOCR
- **Handwriting Detector** (Port 8002) - ML-based handwriting detection

### **Processing Flow**
```
Document Upload → Handwriting Detection → Route to TrOCR (handwritten) or Tesseract (printed) → AI Extraction → Database Storage
```

## Features

- **Document Processing**: Accepts PDF and image files (JPG, JPEG, PNG, TIFF)
- **Handwriting Detection**: Automatically detects handwritten vs printed text
- **Handwritten Text Recognition**: Uses TrOCR for handwritten document processing
- **AI-Enhanced Extraction**: Ollama AI and OpenAI for intelligent data extraction
- **Database Persistence**: Stores extracted data in H2 database (configurable for production)
- **Dual Confidence Scoring**: Both AI and traditional confidence assessment
- **RESTful API**: Complete CRUD operations with AI-specific endpoints
- **Validation**: Input validation and comprehensive error handling
- **Clean Architecture**: Modular design with separate concerns

## Technology Stack

### **Backend**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **H2 Database** (development)
- **Tesseract OCR** (Tess4J wrapper)
- **Apache PDFBox** (PDF text extraction)

### **AI & ML**
- **Ollama AI** (Local AI inference)
- **OpenAI API** (Cloud AI processing)
- **Microsoft TrOCR** (Handwritten text recognition)
- **Handwriting Detection Model** (ML-based classification)

### **Microservices**
- **FastAPI** (Python microservices)
- **Uvicorn** (ASGI server)
- **Transformers** (Hugging Face models)

### **Build Tools**
- **Maven** (Java build tool)
- **Python venv** (Python dependencies)

## Prerequisites

1. **Java 17** or higher
2. **Maven 3.6** or higher
3. **Python 3.9** or higher
4. **Tesseract OCR** (for image processing)
5. **Ollama** (for local AI inference)

### Installing Dependencies

#### Tesseract OCR
**macOS:**
```bash
brew install tesseract
```

**Ubuntu/Debian:**
```bash
sudo apt-get install tesseract-ocr
```

**Windows:**
Download from: https://github.com/UB-Mannheim/tesseract/wiki

#### Ollama
```bash
# macOS/Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Start Ollama and pull the model
ollama serve
ollama pull llama2:7b
```

## Getting Started

### 1. Clone the Repository
```bash
git clone <repository-url>
cd document-processing-app
```

### 2. Setup Python Microservices
```bash
cd python_microservices
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 3. Start Microservices
```bash
# Terminal 1: Start TrOCR service
cd python_microservices
source venv/bin/activate
uvicorn trocr_service:app --host 0.0.0.0 --port 8001

# Terminal 2: Start Handwriting Detector
cd python_microservices
source venv/bin/activate
uvicorn handwriting_detector_service:app --host 0.0.0.0 --port 8002
```

### 4. Build and Run Spring Boot Application
```bash
# Terminal 3: Start Spring Boot app
mvn clean install
mvn spring-boot:run
```

The application will be available at:
- **Web Interface**: `http://localhost:8080`
- **TrOCR Service**: `http://localhost:8001`
- **Handwriting Detector**: `http://localhost:8002`

## API Endpoints

### Document Processing
- **POST** `/api/documents/process` - Process a driving license document
  - Content-Type: `multipart/form-data`
  - Parameter: `file` (PDF or image file)
  - Automatically detects handwriting and routes accordingly

### AI-Specific Endpoints
- **GET** `/api/documents/ai-stats` - Get AI processing statistics
- **GET** `/api/documents/licenses/ai-processed` - Get all AI-processed licenses
- **GET** `/api/documents/licenses/ai-confidence-range` - Get licenses by AI confidence range
- **GET** `/api/documents/test-ai` - Test AI extraction with sample data

### Driving License Management
- **GET** `/api/documents/licenses` - Get all driving licenses
- **GET** `/api/documents/licenses/{id}` - Get license by ID
- **GET** `/api/documents/licenses/number/{licenseNumber}` - Get license by number
- **GET** `/api/documents/licenses/state/{state}` - Get licenses by state
- **GET** `/api/documents/licenses/status/{status}` - Get licenses by processing status
- **GET** `/api/documents/licenses/expired` - Get expired licenses
- **GET** `/api/documents/licenses/search?name={name}` - Search licenses by name
- **GET** `/api/documents/licenses/low-confidence` - Get low confidence licenses
- **PUT** `/api/documents/licenses/{id}` - Update a license
- **DELETE** `/api/documents/licenses/{id}` - Delete a license

### Health Checks
- **GET** `/api/documents/health` - Spring Boot application health
- **GET** `http://localhost:8001/health` - TrOCR service health
- **GET** `http://localhost:8002/health` - Handwriting detector health

## Database Schema

### DrivingLicense Entity
- `id` (Long, Primary Key)
- `licenseNumber` (String, Unique)
- `firstName` (String)
- `lastName` (String)
- `middleName` (String)
- `dateOfBirth` (LocalDate)
- `address` (String)
- `city` (String)
- `state` (String)
- `zipCode` (String)
- `issueDate` (LocalDate)
- `expiryDate` (LocalDate)
- `issuingAuthority` (String)
- `licenseClass` (String)
- `restrictions` (String)
- `endorsements` (String)
- `documentType` (String)
- `confidenceScore` (Double) - Traditional confidence score
- `aiProcessed` (Boolean) - Whether AI was used for processing
- `aiConfidence` (Double) - AI-specific confidence score
- `handwritten` (Boolean) - Whether document contains handwriting
- `processingStatus` (Enum: PROCESSING, PROCESSED, FAILED, MANUAL_REVIEW_REQUIRED)
- `createdAt` (LocalDate)

## Processing Workflow

### 1. **Document Upload & Analysis**
```
Document Upload → File Type Detection → Handwriting Detection → Route Processing
```

### 2. **Text Extraction**
- **Handwritten Documents**: TrOCR microservice (Microsoft TrOCR model)
- **Printed Documents**: Tesseract OCR
- **PDF Documents**: Apache PDFBox

### 3. **AI Data Extraction**
- **Primary**: Ollama AI (local inference)
- **Fallback**: OpenAI API (cloud processing)
- **Confidence Assessment**: AI calculates processing confidence

### 4. **Data Storage**
- **Validation**: Check for critical fields
- **Confidence Scoring**: Overall confidence assessment
- **Status Assignment**: PROCESSED, MANUAL_REVIEW_REQUIRED, or FAILED
- **Database Storage**: H2 database with JPA

## Configuration

### Application Properties
```properties
# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Microservice URLs
app.microservices.trocr.url=http://localhost:8001
app.microservices.handwriting-detector.url=http://localhost:8002

# AI Configuration
app.ai.ollama.model=llama2:7b
app.ai.confidence-threshold=0.5
```

### Microservice Configuration
Each microservice has its own configuration:
- **TrOCR Service**: Uses Microsoft TrOCR model with image preprocessing
- **Handwriting Detector**: ML-based classification with confidence scoring

## Testing

### Test Images
- `test.png` - Sample document for testing
- `python_microservices/handwritten test.jpg` - Handwritten test document

### Manual Testing
1. Upload a document through the web interface
2. Check the processing logs for handwriting detection
3. Verify the extracted data in the database
4. Monitor AI confidence scores

## Troubleshooting

### Common Issues
1. **TrOCR Service Not Starting**: Check Python dependencies and model download
2. **Handwriting Detection Fails**: Verify the ML model is loaded correctly
3. **Ollama AI Not Responding**: Ensure Ollama is running and model is downloaded
4. **Port Conflicts**: Check if ports 8001, 8002, or 8080 are already in use

### Logs
- **Spring Boot**: Check application logs for processing details
- **TrOCR Service**: Monitor for text extraction quality
- **Handwriting Detector**: Verify detection accuracy

## Future Enhancements

- **Better AI Models**: Integration with more advanced AI models
- **Enhanced Preprocessing**: Better image preprocessing for improved OCR
- **Batch Processing**: Support for processing multiple documents
- **Cloud Deployment**: Docker containerization and cloud deployment
- **Real-time Processing**: WebSocket support for real-time updates