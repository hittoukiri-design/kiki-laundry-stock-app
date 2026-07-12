from PIL import Image

def crop_leaf():
    # Load original cropped card image
    img = Image.open('/Users/christambayong/.gemini/antigravity/brain/6d7dde8a-80ad-4e87-b535-47b6918a43c1/media__1781685825236.png')
    
    # Crop slightly wider than the leaf bounding box to be safe
    cropped = img.crop((94, 157, 174, 260))
    cropped = cropped.convert("RGBA")
    width, height = cropped.size
    
    newData = []
    for y in range(height):
        for x in range(width):
            r, g, b, a = cropped.getpixel((x, y))
            # Background threshold: very light colors
            is_bg = (r > 220 and g > 220 and b > 220)
            # Greyscale text/shadow threshold: close RGB channels
            is_grey = (abs(r - g) < 8 and abs(g - b) < 8)
            
            # If it is not background, not greyscale, and green/blue dominate (leaf color)
            if not is_bg and not is_grey and (g - r > 15 and b - r > 15):
                # Keep the original soft green/teal color, fully opaque in PNG
                newData.append((r, g, b, 255))
            else:
                # Make everything else transparent
                newData.append((255, 255, 255, 0))
                
    cropped.putdata(newData)
    cropped.save('app/src/main/res/drawable/leaf_watermark.png', "PNG")
    print("Adjusted leaf cropped and extracted successfully!")

if __name__ == '__main__':
    crop_leaf()
