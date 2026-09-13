#!/usr/bin/env bash
set -e

if ! command -v clang-format >/dev/null 2>&1; then
  echo "clang-format not found"
  exit 1
fi

files=($(find ./src -type f \( -name "*.c" -o -name "*.h" -o -name "*.cpp" \)))

if [[ ${#files[@]} -eq 0 ]]; then
  exit 0
fi

for file in "${files[@]}"; do
  clang-format -i --style=file "$file"
  echo "Formatting : $file"
done

echo "Formatting successfully."
