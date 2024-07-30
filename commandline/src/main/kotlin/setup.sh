#!/bin/bash

# this shell can expand the tar ball, and copy the extracted file to right place for debug.
# Check if a filename is provided
if [ $# -eq 0 ]; then
    echo "Please provide a tar.gz filename as an argument."
    exit 1
fi

# Create a temporary directory
temp_dir=$(mktemp -d)

# Extract the tar.gz file to the temporary directory
tar -xzf "$1" -C "$temp_dir"

# Process all .kt files in the temporary directory
find "$temp_dir/en" -name "*.kt" | while read -r file; do
    # Get the filename without path
    filename=$(basename "$file")

    first_line=$(head -n 1 "$file")
    start_position=8
    substring=${first_line:$start_position}
    IFS='.' read -ra parts <<< "$substring"

    # Split the filename by underscores
    IFS='_' read -ra parts1 <<< "$filename"
    last_element="${parts1[@]: -1}"

    parts+=($last_element)


    # Check if the filename has at least 2 parts (for x/y.kt)
    if [ ${#parts[@]} -ge 2 ]; then
        # Extract all parts except the last one for the directory structure
        dir_parts=("${parts[@]::${#parts[@]}-1}")

        # Join directory parts with '/'
        dir_path=$(IFS=/; echo "${dir_parts[*]}")

        # Get the last part as the filename
        last_part="${parts[${#parts[@]}-1]}"

        # Create the directory structure
        mkdir -p "$dir_path"

        # Copy the file to the new location
        cp "$file" "$dir_path/$last_part"

        echo "Copied $filename to $dir_path/$last_part"
    else
        echo "Skipping $filename: doesn't match expected pattern"
    fi
done

# Clean up: remove the temporary directory
rm -rf "$temp_dir"

echo "Processing complete"