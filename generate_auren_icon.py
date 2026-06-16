import os
from PIL import Image, ImageDraw, ImageFilter

def create_premium_icon():
    # 1. Initialize a 512x512 canvas with a deep obsidian black background
    # #09090A (RGB: 9, 9, 10)
    img_size = 512
    bg_color = (9, 9, 10)
    image = Image.new("RGBA", (img_size, img_size), bg_color)
    draw = ImageDraw.Draw(image)

    # 2. Draw a subtle gold glowing background circle
    # Gold glow color with opacity
    glow_color = (212, 175, 55, 30) # semi-transparent gold
    glow_radius = 200
    glow_box = [
        (img_size // 2 - glow_radius, img_size // 2 - glow_radius),
        (img_size // 2 + glow_radius, img_size // 2 + glow_radius)
    ]
    draw.ellipse(glow_box, fill=glow_color)

    # Apply blur to the glow layer
    # We create a separate image for the glow to blur it
    glow_img = Image.new("RGBA", (img_size, img_size), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_img)
    glow_draw.ellipse(glow_box, fill=(212, 175, 55, 45))
    glow_blurred = glow_img.filter(ImageFilter.GaussianBlur(radius=30))
    image.alpha_composite(glow_blurred)

    # 3. Draw a thin, elegant gold concentric ring
    ring_radius = 170
    ring_box = [
        (img_size // 2 - ring_radius, img_size // 2 - ring_radius),
        (img_size // 2 + ring_radius, img_size // 2 + ring_radius)
    ]
    # We will draw a gold ring with a width of 3 pixels
    # Gold color: #D4AF37 (RGB: 212, 175, 55)
    gold_color = (212, 175, 55, 180)
    draw.ellipse(ring_box, outline=gold_color, width=3)

    # 4. Draw the 3D-faceted 4-pointed Star
    # Center: (256, 256)
    cx, cy = img_size // 2, img_size // 2

    # Star tip coordinates
    top = (cx, cy - 140)
    bottom = (cx, cy + 140)
    left = (cx - 140, cy)
    right = (cx + 140, cy)

    # Inner corners (for a sharp diamond star shape)
    inner_tl = (cx - 20, cy - 20)
    inner_tr = (cx + 20, cy - 20)
    inner_bl = (cx - 20, cy + 20)
    inner_br = (cx + 20, cy + 20)

    # Colors for faceted 3D metal look:
    # Left sides (light gold): #F5D77F (RGB: 245, 215, 127)
    light_gold = (245, 215, 127, 255)
    # Right sides (burnished gold): #B8860B (RGB: 184, 134, 11)
    dark_gold = (184, 134, 11, 255)

    # Left-Top half
    draw.polygon([top, inner_tl, left, (cx, cy)], fill=light_gold)
    # Left-Bottom half
    draw.polygon([left, inner_bl, bottom, (cx, cy)], fill=light_gold)
    # Right-Top half
    draw.polygon([top, inner_tr, right, (cx, cy)], fill=dark_gold)
    # Right-Bottom half
    draw.polygon([right, inner_br, bottom, (cx, cy)], fill=dark_gold)

    # 5. Draw an inner diamond core for added premium detail (refraction effect)
    core_top = (cx, cy - 20)
    core_bottom = (cx, cy + 20)
    core_left = (cx - 20, cy)
    core_right = (cx + 20, cy)

    # Inner core colors (platinum/white 3D contrast)
    platinum_light = (240, 240, 245, 255)
    platinum_dark = (160, 160, 170, 255)

    draw.polygon([core_top, core_left, core_bottom, (cx, cy)], fill=platinum_light)
    draw.polygon([core_top, core_right, core_bottom, (cx, cy)], fill=platinum_dark)

    # 6. Save the premium launcher icon
    output_path = "/Users/adhithyapandiri/Documents/Indian Bloom/app/src/main/res/drawable/ic_launcher_auren.png"
    # Ensure directory exists
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    # Save as PNG
    # Convert to RGB to ensure compatibility as a launcher icon
    final_img = image.convert("RGB")
    final_img.save(output_path, "PNG")
    print(f"Success: Saved premium launcher icon to {output_path}")

if __name__ == "__main__":
    create_premium_icon()
