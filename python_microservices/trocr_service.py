from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from PIL import Image, ImageEnhance, ImageFilter
import io
import numpy as np
from transformers import TrOCRProcessor, VisionEncoderDecoderModel
import logging

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

processor = TrOCRProcessor.from_pretrained("microsoft/trocr-base-handwritten")
model = VisionEncoderDecoderModel.from_pretrained("microsoft/trocr-base-handwritten")

def preprocess_image(image):
    """Enhance image for better OCR results"""
    # Convert to RGB if needed
    if image.mode != 'RGB':
        image = image.convert('RGB')
    
    # Resize image if too small (TrOCR works better with larger images)
    min_size = 512
    if min(image.size) < min_size:
        ratio = min_size / min(image.size)
        new_size = (int(image.size[0] * ratio), int(image.size[1] * ratio))
        image = image.resize(new_size, Image.Resampling.LANCZOS)
        logger.info(f"Resized image from {image.size} to {new_size}")
    
    # Enhance contrast
    enhancer = ImageEnhance.Contrast(image)
    image = enhancer.enhance(1.5)
    
    # Enhance sharpness
    enhancer = ImageEnhance.Sharpness(image)
    image = enhancer.enhance(1.2)
    
    # Convert to grayscale for better text recognition
    image = image.convert('L')
    
    # Apply slight blur to reduce noise
    image = image.filter(ImageFilter.GaussianBlur(radius=0.5))
    
    # Convert back to RGB (TrOCR expects RGB)
    image = image.convert('RGB')
    
    return image

@app.post("/ocr")
async def ocr_image(file: UploadFile = File(...)):
    try:
        # Read and preprocess image
        image_data = await file.read()
        image = Image.open(io.BytesIO(image_data))
        
        logger.info(f"Original image size: {image.size}, mode: {image.mode}")
        
        # Preprocess the image
        processed_image = preprocess_image(image)
        
        logger.info(f"Processed image size: {processed_image.size}")
        
        # Process with TrOCR
        pixel_values = processor(images=processed_image, return_tensors="pt").pixel_values
        generated_ids = model.generate(pixel_values)
        text = processor.batch_decode(generated_ids, skip_special_tokens=True)[0]
        
        logger.info(f"Extracted text: '{text}' (length: {len(text)})")
        
        return JSONResponse({"text": text, "confidence": 0.8})
        
    except Exception as e:
        logger.error(f"Error processing image: {str(e)}")
        return JSONResponse(
            {"error": f"Failed to process image: {str(e)}"}, 
            status_code=500
        )

@app.get("/health")
async def health_check():
    return JSONResponse({"status": "healthy", "service": "trocr"}) 