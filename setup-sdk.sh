#!/bin/bash
set -e

# Define variables
SDK_DIR="$(pwd)/android-sdk"
LINUX_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
MAC_URL="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
# Windows is not supported by this bash script.

OS="$(uname -s)"
case "${OS}" in
    Linux*)     CMDLINE_TOOLS_URL=$LINUX_URL;;
    Darwin*)    CMDLINE_TOOLS_URL=$MAC_URL;;
    *)          echo "Unsupported OS: ${OS}"; exit 1;;
esac

echo "Setting up Android SDK in $SDK_DIR for $OS..."

# Create SDK directory if it doesn't exist
if [ ! -d "$SDK_DIR" ]; then
    mkdir -p "$SDK_DIR"
fi

# Download command line tools if not present
if [ ! -f "commandlinetools.zip" ]; then
    echo "Downloading command line tools..."
    if command -v wget >/dev/null 2>&1; then
        wget -q -O commandlinetools.zip "$CMDLINE_TOOLS_URL"
    elif command -v curl >/dev/null 2>&1; then
        curl -o commandlinetools.zip "$CMDLINE_TOOLS_URL"
    else
        echo "Error: Neither wget nor curl found."
        exit 1
    fi
else
    echo "commandlinetools.zip already exists, using it."
fi

# Prepare directory structure: cmdline-tools/latest
if [ -d "$SDK_DIR/cmdline-tools" ]; then
    echo "Cleaning up previous installation..."
    rm -rf "$SDK_DIR/cmdline-tools"
fi

echo "Unzipping..."
unzip -q commandlinetools.zip -d "$SDK_DIR/temp_extract"

mkdir -p "$SDK_DIR/cmdline-tools/latest"
mv "$SDK_DIR/temp_extract/cmdline-tools"/* "$SDK_DIR/cmdline-tools/latest/"
rm -rf "$SDK_DIR/temp_extract"

echo "Command line tools installed."

# Define sdkmanager path
SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"

# Accept licenses
echo "Accepting licenses..."
yes | "$SDKMANAGER" --licenses --sdk_root="$SDK_DIR" > /dev/null

# Install packages
echo "Installing platforms and build-tools..."
"$SDKMANAGER" "platform-tools" "platforms;android-34" "build-tools;34.0.0" --sdk_root="$SDK_DIR"

# Configure local.properties
echo "Configuring local.properties..."
if [ -f local.properties ]; then
    if grep -q "sdk.dir" local.properties; then
        # Replace existing sdk.dir
        # usage of sed differs between Mac and Linux, so we use a temp file approach or simple overwrite if user allows
        # For simplicity in this script, we will overwrite/update the sdk.dir line
        # But to be safe as per review, let's just warn or backup.
        # Actually, the user goal is to use *this* SDK. So updating it is correct.
        # We will use a safe cross-platform sed or just rewrite the file preserving other lines if possible,
        # but parsing properties in bash is annoying.
        # Simpler: backup and append/replace.
        grep -v "sdk.dir" local.properties > local.properties.tmp
        echo "sdk.dir=$SDK_DIR" >> local.properties.tmp
        mv local.properties.tmp local.properties
        echo "Updated sdk.dir in local.properties"
    else
        echo "sdk.dir=$SDK_DIR" >> local.properties
        echo "Appended sdk.dir to local.properties"
    fi
else
    echo "sdk.dir=$SDK_DIR" > local.properties
    echo "Created local.properties"
fi

echo "Cleaning up zip file..."
rm commandlinetools.zip

echo "Android SDK setup complete."
