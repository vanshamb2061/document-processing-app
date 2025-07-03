from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from PIL import Image
import io
import numpy as np
import easyocr
from paddleocr import PaddleOCR

app = FastAPI()

reader = easyocr.Reader(['en'], gpu=False)
ocr = PaddleOCR(use_angle_cls=True, lang='en')

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
        np_image = np.array(image)

        # Use PaddleOCR to detect text and script type
        result = ocr.ocr(np_image)
        # Log the PaddleOCR output for debugging
        print("PaddleOCR result:", result)
        confidences = []
        try:
            for line in result:
                for word_info in line:
                    print("word_info:", word_info)
                    # Defensive: check structure before accessing
                    if len(word_info) > 1 and isinstance(word_info[1], (list, tuple)) and len(word_info[1]) > 1:
                        conf = word_info[1][1]
                        confidences.append(conf)
        except Exception as e:
            print("Error while parsing PaddleOCR result:", e)
            return JSONResponse({"error": f"Failed to parse PaddleOCR result: {str(e)}"}, status_code=500)
        avg_conf = sum(confidences) / len(confidences) if confidences else 0.0
        if avg_conf < 0.5:
            return JSONResponse({"result": "handwritten", "confidence": avg_conf})
        else:
            return JSONResponse({"result": "printed", "confidence": avg_conf})
    except Exception as e:
        return JSONResponse(
            {"error": f"Failed to process image: {str(e)}"}, 
            status_code=500
        )

@app.get("/health")
async def health_check():
    return JSONResponse({"status": "healthy", "service": "handwriting_detector"}) 