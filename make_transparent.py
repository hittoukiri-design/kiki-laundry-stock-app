import sys
from PIL import Image

def make_transparent(img_path):
    img = Image.open(img_path)
    img = img.convert("RGBA")
    datas = img.getdata()
    
    newData = []
    for item in datas:
        # Check if pixel is close to white (R>240, G>240, B>240)
        if item[0] > 240 and item[1] > 240 and item[2] > 240:
            newData.append((255, 255, 255, 0)) # transparent
        else:
            newData.append(item)
            
    img.putdata(newData)
    img.save(img_path, "PNG")

if __name__ == '__main__':
    if len(sys.argv) > 1:
        make_transparent(sys.argv[1])
