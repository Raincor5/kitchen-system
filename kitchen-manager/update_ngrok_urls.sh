#!/bin/bash

# Enable error handling
set -e

# Function to cleanup on exit
cleanup() {
    echo "Cleaning up..."
    rm -f ngrok.log
}

# Set up trap for cleanup
trap cleanup EXIT

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -i :$port > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to get process info for a port
get_process_info() {
    local port=$1
    local process_info=$(lsof -i :$port -t 2>/dev/null)
    if [ ! -z "$process_info" ]; then
        local pid=$process_info
        local process_name=$(ps -p $pid -o comm= 2>/dev/null)
        echo "$process_name"
    fi
}

# Function to detect running services
detect_services() {
    echo "Detecting running services..."
    
    # Common ports to check
    local ports=(
        "3000"  # React/Next.js frontend
        "5000"  # Flask/FastAPI backend
        "8000"  # Kitchen Manager backend
        "8080"  # Common backend port
        "8081"  # Prep tracking
        "8082"  # Recipe upscaler
        "19000" # Expo
        "19001" # Expo
        "19002" # Expo
    )
    
    declare -A running_services
    
    for port in "${ports[@]}"; do
        if check_port $port; then
            local process_name=$(get_process_info $port)
            if [ ! -z "$process_name" ]; then
                running_services[$port]=$process_name
                echo "Found service on port $port: $process_name"
            fi
        fi
    done
    
    # Map services to their types
    declare -A service_types
    for port in "${!running_services[@]}"; do
        local process_name=${running_services[$port]}
        case $process_name in
            *"node"*|*"expo"*|*"react"*)
                service_types[$port]="frontend"
                ;;
            *"python"*|*"flask"*|*"fastapi"*)
                service_types[$port]="backend"
                ;;
            *)
                service_types[$port]="unknown"
                ;;
        esac
    done
    
    # Return the arrays
    echo "${!running_services[@]}"
    echo "${running_services[@]}"
    echo "${service_types[@]}"
}

# We're now running this script before ngrok starts, so we don't need to check if ngrok is running
# Instead, we'll prepare the environment for ngrok to start

# Create or update the necessary .env files with placeholder values
# These will be updated with actual ngrok URLs once ngrok is running
echo "Preparing environment for ngrok..."

# Update Kitchen Manager backend .env with initial values
if [ -f "/home/mckrotsky/kitchen-manager/backend/.env.backup" ]; then
    echo "Setting up initial backend .env file..."
    cp /home/mckrotsky/kitchen-manager/backend/.env.backup /home/mckrotsky/kitchen-manager/backend/.env
    echo "Initial backend .env file created."
fi

# Continue with the rest of the script when ngrok starts

# Function to get tunnel URLs from ngrok API
get_tunnel_urls() {
    # Get the tunnel information from ngrok API
    local api_response=$(curl -s http://localhost:4040/api/tunnels)
    
    # Debug: Print the API response
    echo "API Response:"
    echo "$api_response" | jq '.'
    
    # Extract URLs for each service using jq with exact tunnel names
    KITCHEN_MANAGER_URL=$(echo "$api_response" | jq -r '.tunnels[] | select(.name == "kitchen-manager") | .public_url')
    PREP_TRACKING_URL=$(echo "$api_response" | jq -r '.tunnels[] | select(.name == "prep-tracking") | .public_url')
    RECIPE_UPSCALER_URL=$(echo "$api_response" | jq -r '.tunnels[] | select(.name == "recipe-upscaler") | .public_url')
    
    # Debug: Print extracted URLs
    echo "Extracted URLs:"
    echo "Kitchen Manager: $KITCHEN_MANAGER_URL"
    echo "Prep Tracking: $PREP_TRACKING_URL"
    echo "Recipe Upscaler: $RECIPE_UPSCALER_URL"
}

# Detect running services
read -r ports services service_types <<< $(detect_services)

# Map frontend apps to their backend services
declare -A service_mappings
for port in $ports; do
    if [ "${service_types[$port]}" = "frontend" ]; then
        # Find corresponding backend port
        for backend_port in $ports; do
            if [ "${service_types[$backend_port]}" = "backend" ]; then
                service_mappings[$port]=$backend_port
                break
            fi
        done
    fi
done

echo "Getting tunnel URLs from ngrok API..."
for i in {1..30}; do
    get_tunnel_urls
    
    # If we have all URLs, update the config files
    if [ ! -z "$KITCHEN_MANAGER_URL" ] && [ ! -z "$PREP_TRACKING_URL" ] && [ ! -z "$RECIPE_UPSCALER_URL" ]; then
        echo "Found all URLs, updating configuration..."

        # Update Kitchen Manager backend .env
        echo "PREP_TRACKER_URL=$PREP_TRACKING_URL" > /home/mckrotsky/kitchen-manager/backend/.env
        echo "RECIPE_UPSCALER_URL=$RECIPE_UPSCALER_URL" >> /home/mckrotsky/kitchen-manager/backend/.env
        echo "ENABLE_LABEL_PRINTING=true" >> /home/mckrotsky/kitchen-manager/backend/.env
        echo "LABEL_DETECTOR_URL=$KITCHEN_MANAGER_URL" >> /home/mckrotsky/kitchen-manager/backend/.env

        # Update Kitchen Manager frontend .env
        echo "REACT_APP_API_URL=$KITCHEN_MANAGER_URL" > /home/mckrotsky/kitchen-manager/frontend/KitchenManagerApp/.env
        echo "REACT_APP_RECIPE_UPSCALER_URL=$RECIPE_UPSCALER_URL" >> /home/mckrotsky/kitchen-manager/frontend/KitchenManagerApp/.env

        # Update Recipe Upscaler .env
        echo "KITCHEN_MANAGER_URL=$KITCHEN_MANAGER_URL" > /home/mckrotsky/projects/upscaler/recipe-upscale-backend/.env
        echo "PREP_TRACKER_URL=$PREP_TRACKING_URL" >> /home/mckrotsky/projects/upscaler/recipe-upscale-backend/.env

        # Update FTT Android app .env
        FTT_ANDROID_DIR="/home/mckrotsky/projects/ftt/frontend/fft-android"
        FTT_ENV_FILE="$FTT_ANDROID_DIR/.env"
        FTT_RETROFIT_FILE="$FTT_ANDROID_DIR/app/src/main/java/com/example/labels/data/RetrofitClient.kt"
        
        # Create or update the .env file
        cat > "$FTT_ENV_FILE" << EOF
# API URLs - Updated by ngrok script
API_URL_DEBUG=$KITCHEN_MANAGER_URL
API_URL_RELEASE=$KITCHEN_MANAGER_URL

# Add other environment variables here
EOF

        # Update the Android app's RetrofitClient.kt file
        if [ -f "$FTT_RETROFIT_FILE" ]; then
            echo "Updating Android app RetrofitClient.kt with ngrok URL: $KITCHEN_MANAGER_URL"
            sed -i "s|private const val BASE_URL = \".*\"|private const val BASE_URL = \"$KITCHEN_MANAGER_URL\"|g" "$FTT_RETROFIT_FILE"
        else
            echo "Warning: Android RetrofitClient.kt file not found at $FTT_RETROFIT_FILE"
        fi

        echo "Updated ngrok URLs:"
        echo "Kitchen Manager: $KITCHEN_MANAGER_URL"
        echo "Prep Tracking: $PREP_TRACKING_URL"
        echo "Recipe Upscaler: $RECIPE_UPSCALER_URL"
        echo "FTT Android App: $KITCHEN_MANAGER_URL"

        # Display the contents of the updated .env files
        echo -e "\nUpdated .env files:"
        echo "Kitchen Manager backend .env:"
        cat /home/mckrotsky/kitchen-manager/backend/.env
        echo -e "\nKitchen Manager frontend .env:"
        cat /home/mckrotsky/kitchen-manager/frontend/KitchenManagerApp/.env
        echo -e "\nRecipe Upscaler .env:"
        cat /home/mckrotsky/projects/upscaler/recipe-upscale-backend/.env
        echo -e "\nFTT Android App .env:"
        cat "$FTT_ENV_FILE"

        # Log the update
        echo "$(date): Updated ngrok URLs" >> /home/mckrotsky/kitchen-manager/ngrok_update.log
        echo "Kitchen Manager: $KITCHEN_MANAGER_URL" >> /home/mckrotsky/kitchen-manager/ngrok_update.log
        echo "Prep Tracking: $PREP_TRACKING_URL" >> /home/mckrotsky/kitchen-manager/ngrok_update.log
        echo "Recipe Upscaler: $RECIPE_UPSCALER_URL" >> /home/mckrotsky/kitchen-manager/ngrok_update.log
        echo "FTT Android App: $KITCHEN_MANAGER_URL" >> /home/mckrotsky/kitchen-manager/ngrok_update.log
        
        # Generate QR code for the FTT Android App URL
        echo "Generating QR code for FTT Android App URL..."
        python3 /home/mckrotsky/kitchen-manager/generate_qr_code.py
        
        # We'll let the launch script handle the concatenation now
        exit 0
    fi

    # If ngrok is no longer running, exit
    if ! pgrep -f ngrok > /dev/null; then
        echo "Error: ngrok stopped running"
        exit 1
    fi

    echo "Waiting for ngrok URLs... ($i/30)"
    sleep 2
done

echo "Error: Could not get all required URLs from ngrok"
exit 1 