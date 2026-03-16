#!/bin/bash
# Research Pipeline Runner
# Run all steps: synthetic data → ML model → visualizations

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=============================================="
echo "  SecureBank Research Pipeline"
echo "=============================================="

echo ""
echo "Step 1/3: Installing dependencies..."
pip3 install -r requirements.txt -q 2>/dev/null

echo ""
echo "Step 2/3: Generating synthetic data..."
python3 synthetic_data_generator.py

echo ""
echo "Step 3/3: Training ML models..."
python3 ml_model.py

echo ""
echo "Step 4/4: Generating visualizations..."
python3 generate_visualizations.py

echo ""
echo "=============================================="
echo "  ✅ PIPELINE COMPLETE!"
echo "  Results: $SCRIPT_DIR/results/"
echo "  Figures: $SCRIPT_DIR/figures/"
echo "=============================================="
