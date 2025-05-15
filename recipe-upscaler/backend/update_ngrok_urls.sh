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
        "8080"  # Common backend port
        "8083"  # Prep tracking
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

# Check if ngrok is running
if ! pgrep -f ngrok > /dev/null; then
    echo "Error: ngrok is not running. Please start ngrok in a separate terminal with:"
    echo "ngrok start --config=ngrok.yml --all"
    exit 1
fi

echo "Waiting for ngrok to start..."
sleep 5

# Function to get tunnel URLs from ngrok API
get_tunnel_urls() {
    # Get the tunnel information from ngrok API
    local api_response=$(curl -s http://localhost:4040/api/tunnels)
    
    # Debug: Print the API response
    echo "API Response:"
    echo "$api_response" | jq '.'
    
    # Extract URLs for each service using jq with exact tunnel names
    RECIPE_UPSCALER_URL=$(echo "$api_response" | jq -r '.tunnels[] | select(.name == "recipe-upscaler") | .public_url')
    KITCHEN_MANAGER_URL=$(echo "$api_response" | jq -r '.tunnels[] | select(.name == "kitchen-manager") | .public_url')
    PREP_TRACKING_URL=$(echo "$api_response" | jq -r '.tunnels[] | select(.name == "prep-tracking") | .public_url')
    RECIPE_UPSCALER_BACKEND_URL=$(echo "$api_response" | jq -r '.tunnels[] | select(.name == "recipe-upscaler-backend") | .public_url')
    
    # Debug: Print extracted URLs
    echo "Extracted URLs:"
    echo "Recipe Upscaler: $RECIPE_UPSCALER_URL"
    echo "Kitchen Manager: $KITCHEN_MANAGER_URL"
    echo "Prep Tracking: $PREP_TRACKING_URL"
    echo "Recipe Upscaler Backend: $RECIPE_UPSCALER_BACKEND_URL"
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
    if [ ! -z "$RECIPE_UPSCALER_URL" ] && [ ! -z "$KITCHEN_MANAGER_URL" ] && [ ! -z "$PREP_TRACKING_URL" ] && [ ! -z "$RECIPE_UPSCALER_BACKEND_URL" ]; then
        echo "Found all URLs, updating configuration..."

        # Update Recipe Upscaler Calculator app .env
        echo "API_URL=$RECIPE_UPSCALER_BACKEND_URL" > /home/mckrotsky/projects/upscaler/recipe-upscale-calculator-cross/.env
        echo "SENTRY_AUTH_TOKEN=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" >> /home/mckrotsky/projects/upscaler/recipe-upscale-calculator-cross/.env

        # Update Prep Tracker app .env and app.json
        echo "API_URL=$KITCHEN_MANAGER_URL/api" > /home/mckrotsky/projects/prep-tracker/prep-tracking-app/.env
        # Update app.json using jq
        jq --arg url "$KITCHEN_MANAGER_URL/api" '.expo.extra.API_URL = $url' /home/mckrotsky/projects/prep-tracker/prep-tracking-app/app.json > /tmp/app.json.tmp && mv /tmp/app.json.tmp /home/mckrotsky/projects/prep-tracker/prep-tracking-app/app.json

        # Update Kitchen Manager backend .env
        echo "PREP_TRACKER_URL=$PREP_TRACKING_URL" > /home/mckrotsky/kitchen-manager/backend/.env
        echo "RECIPE_UPSCALER_URL=$RECIPE_UPSCALER_BACKEND_URL" >> /home/mckrotsky/kitchen-manager/backend/.env

        # Update Kitchen Manager frontend .env
        echo "REACT_APP_API_URL=$KITCHEN_MANAGER_URL" > /home/mckrotsky/kitchen-manager/frontend/KitchenManagerApp/.env

        echo "Updated ngrok URLs:"
        echo "Recipe Upscaler: $RECIPE_UPSCALER_URL"
        echo "Kitchen Manager: $KITCHEN_MANAGER_URL"
        echo "Prep Tracking: $PREP_TRACKING_URL"
        echo "Recipe Upscaler Backend: $RECIPE_UPSCALER_BACKEND_URL"

        # Display the contents of the updated .env files
        echo -e "\nUpdated .env files:"
        echo "Recipe Upscaler Calculator .env:"
        cat /home/mckrotsky/projects/upscaler/recipe-upscale-calculator-cross/.env
        echo -e "\nPrep Tracker .env:"
        cat /home/mckrotsky/projects/prep-tracker/prep-tracking-app/.env
        echo -e "\nKitchen Manager backend .env:"
        cat /home/mckrotsky/kitchen-manager/backend/.env
        echo -e "\nKitchen Manager frontend .env:"
        cat /home/mckrotsky/kitchen-manager/frontend/KitchenManagerApp/.env
        echo -e "\nPrep Tracker app.json:"
        cat /home/mckrotsky/projects/prep-tracker/prep-tracking-app/app.json

        exit 0
    fi

    # If ngrok is no longer running, exit
    if ! pgrep -f ngrok > /dev/null; then
        echo "Error: ngrok process stopped"
        exit 1
    fi

    echo "Waiting for tunnels to be ready... (attempt $i/30)"
    sleep 2
done

echo "Error: Failed to get all tunnel URLs after 30 attempts"
exit 1 