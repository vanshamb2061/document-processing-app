from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from PIL import Image
import io
import numpy as np

app = FastAPI()

def analyze_image_characteristics(image):
    """
    Analyze image characteristics to determine if it's handwritten or printed.
    This is a simplified approach that looks at edge density and texture.
    """
    # Convert to grayscale
    gray = image.convert('L')
    gray_array = np.array(gray)
    
    # Calculate edge density (simplified)
    # Handwritten text typically has more irregular edges
    edges_h = np.abs(np.diff(gray_array, axis=1))  # horizontal edges
    edges_v = np.abs(np.diff(gray_array, axis=0))  # vertical edges
    
    # Pad the smaller array to match dimensions
    if edges_h.shape[0] != edges_v.shape[0]:
        min_rows = min(edges_h.shape[0], edges_v.shape[0])
        edges_h = edges_h[:min_rows, :]
        edges_v = edges_v[:min_rows, :]
    
    if edges_h.shape[1] != edges_v.shape[1]:
        min_cols = min(edges_h.shape[1], edges_v.shape[1])
        edges_h = edges_h[:, :min_cols]
        edges_v = edges_v[:, :min_cols]
    
    edges = edges_h + edges_v
    edge_density = np.mean(edges)
    
    # Calculate variance in pixel values (texture analysis)
    variance = np.var(gray_array)
    
    # Simple heuristic: handwritten text usually has higher edge density and variance
    if edge_density > 20 and variance > 1000:
        return "handwritten"
    else:
        return "printed"

@app.post("/detect")
async def detect_handwriting(file: UploadFile = File(...)):
    try:
        # Read and process the image
        image_data = await file.read()
        image = Image.open(io.BytesIO(image_data)).convert("RGB")
        
        # Analyze the image
        result = analyze_image_characteristics(image)
        
        return JSONResponse({"result": result, "confidence": 0.8})
    except Exception as e:
        return JSONResponse(
            {"error": f"Failed to process image: {str(e)}"}, 
            status_code=500
        )

@app.get("/health")
async def health_check():
    return JSONResponse({"status": "healthy", "service": "handwriting_detector"}) 