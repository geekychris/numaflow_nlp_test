#!/bin/bash
set -e

# Script to download OpenNLP models
# Models are downloaded from the OpenNLP model repository

MODEL_DIR="/app/models"
BASE_URL="https://dlcdn.apache.org/opennlp/models"

echo "Starting OpenNLP model download..."

# Create models directory if it doesn't exist
mkdir -p "$MODEL_DIR"

# Function to download a model
download_model() {
    local model_name="$1"
    local model_file="$2"
    local model_url="$BASE_URL/$model_file"
    local target_path="$MODEL_DIR/$model_file"
    
    if [ -f "$target_path" ]; then
        echo "Model $model_name already exists at $target_path"
        return 0
    fi
    
    echo "Downloading $model_name from $model_url..."
    
    if command -v curl >/dev/null 2>&1; then
        curl -L -o "$target_path" "$model_url"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$target_path" "$model_url"
    else
        echo "Error: Neither curl nor wget is available for downloading models"
        return 1
    fi
    
    if [ $? -eq 0 ]; then
        echo "Successfully downloaded $model_name"
    else
        echo "Failed to download $model_name"
        rm -f "$target_path"  # Remove partial download
        return 1
    fi
}

# Download required models
echo "Downloading OpenNLP models..."

# Sentence detection model
download_model "Sentence Detection" "opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin" || {
    echo "Warning: Failed to download sentence model, will use fallback implementation"
}

# Tokenizer model  
download_model "Tokenizer" "opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin" || {
    echo "Warning: Failed to download tokenizer model, will use fallback implementation"
}

# Named Entity Recognition models
download_model "Person NER" "opennlp-en-ner-person-1.0-1.9.3.bin" || {
    echo "Warning: Failed to download person NER model, will use fallback implementation"
}

download_model "Location NER" "opennlp-en-ner-location-1.0-1.9.3.bin" || {
    echo "Warning: Failed to download location NER model, will use fallback implementation"
}

download_model "Organization NER" "opennlp-en-ner-organization-1.0-1.9.3.bin" || {
    echo "Warning: Failed to download organization NER model, will use fallback implementation"
}

# Create symbolic links with expected names
cd "$MODEL_DIR"
ln -sf opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin en-sent.bin 2>/dev/null || true
ln -sf opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin en-token.bin 2>/dev/null || true
ln -sf opennlp-en-ner-person-1.0-1.9.3.bin en-ner-person.bin 2>/dev/null || true
ln -sf opennlp-en-ner-location-1.0-1.9.3.bin en-ner-location.bin 2>/dev/null || true
ln -sf opennlp-en-ner-organization-1.0-1.9.3.bin en-ner-organization.bin 2>/dev/null || true

echo "Model download process completed!"
echo "Available models in $MODEL_DIR:"
ls -la "$MODEL_DIR"

echo ""
echo "Note: If any model downloads failed, the application will use fallback implementations."
echo "The fallback implementations provide basic functionality but with reduced accuracy."
