#!/usr/bin/env python3

import qrcode
import re
from datetime import datetime
import os
import json
import urllib.request
from urllib.error import URLError

def get_ngrok_tunnel_url():
    try:
        # Try to get the current tunnel info from ngrok API
        with urllib.request.urlopen('http://localhost:4040/api/tunnels') as response:
            data = json.loads(response.read())
            for tunnel in data['tunnels']:
                if tunnel['name'] == 'kitchen-manager':
                    return tunnel['public_url']
    except (URLError, KeyError, json.JSONDecodeError) as e:
        print(f"Could not get tunnel from ngrok API: {e}")
        return None

def get_url_from_log():
    try:
        with open('ngrok_update.log', 'r') as f:
            lines = f.readlines()
        
        # Find the last occurrence of "FTT Android App" URL
        for line in reversed(lines):
            if 'FTT Android App:' in line:
                url = line.split('FTT Android App:')[1].strip()
                return url
    except Exception as e:
        print(f"Could not read from log file: {e}")
        return None

def get_latest_ngrok_url():
    # First try to get URL from running tunnels
    url = get_ngrok_tunnel_url()
    if url:
        print("Using URL from active ngrok tunnel")
        return url
    
    # Fall back to log file if tunnel check failed
    url = get_url_from_log()
    if url:
        print("Using URL from log file (Warning: might be outdated)")
        return url
    
    print("Could not get ngrok URL from either active tunnels or log file")
    return None

def generate_qr_code(url):
    if not url:
        print("No NGROK URL found in the log file")
        return
    
    # Create QR code instance
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=1,
        border=1,
    )
    
    # Add data
    qr.add_data(url)
    qr.make(fit=True)
    
    # Get the matrix
    matrix = qr.get_matrix()
    
    # Print ASCII art QR code
    print("\nQR Code for FTT Android App URL:")
    print("=" * (len(matrix[0]) + 4))
    for row in matrix:
        print("| " + "".join("█" if cell else " " for cell in row) + " |")
    print("=" * (len(matrix[0]) + 4))
    print(f"URL: {url}")
    print("\nScan this QR code with your Android app to connect to the server.")
    
    # Also save the image version
    qr_image = qr.make_image(fill_color="black", back_color="white")
    os.makedirs('qr_codes', exist_ok=True)
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    filename = f'qr_codes/ftt_app_url_{timestamp}.png'
    qr_image.save(filename)
    print(f"\nQR code image also saved as: {filename}")

if __name__ == "__main__":
    url = get_latest_ngrok_url()
    generate_qr_code(url) 