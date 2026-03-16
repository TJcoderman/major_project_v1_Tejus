# `research/` - ML Pipeline & Research Module

## Directory Structure

```
research/
├── ml_model.py                # ML pipeline v2 (enrollment-relative features)
├── generate_visualizations.py # Figure generation for IEEE paper
├── generate_report.py         # Report generation
├── requirements.txt           # Python dependencies
├── run_pipeline.sh            # Shell script to run full pipeline
├── results/
│   ├── detailed_results.json  # Full metrics + ROC/FAR/FRR curves
│   ├── feature_dataset.csv    # Extracted feature matrix
│   ├── feature_importance.csv # Random Forest feature rankings
│   └── model_comparison.csv   # Model accuracy/F1/AUC/EER summary
├── synthetic_data/            # Augmented data (mirrors collected_data structure)
│   └── P01-P08/              # Per-participant session files
├── paper/                     # IEEE format paper + figures
│   ├── securebank_ieee_paper.tex
│   └── figures/               # Generated visualizations
└── SecureBank_Project_Report.docx
```

## ML Pipeline (ml_model.py)

### Approach: Enrollment-Relative Deviation Features
Instead of feeding raw features directly, the pipeline computes how much each session **deviates** from the user's enrolled baseline. This is the correct approach for behavioral authentication because:
- A genuine user's session should have LOW deviation from their enrollment
- An impostor's session should have HIGH deviation

### Feature Extraction
For each session, raw features are extracted from 3 CSV types:
- `pin_keystrokes.csv` -> PIN timing features (dwell, flight, touch position, size)
- `touches.csv` -> Touch interaction features (duration, velocity, acceleration, type ratios)
- `motion.csv` -> Motion sensor features (accel/gyro magnitude, per-axis stats)

Then deviation features are computed:
- `dev_{feature}_abs`: Absolute difference from enrollment
- `dev_{feature}_rel`: Relative (percentage) difference from enrollment
- `raw_{feature}`: Raw session value
- Plus 4 aggregate deviation features (overall mean/max of abs/rel deviations)

### Models Trained
| Model | Type | Key Parameters |
|-------|------|---------------|
| Random Forest | Supervised binary | 300 trees, max_depth=12, balanced classes |
| MLP Neural Network | Supervised binary | 3 layers (128-64-32), ReLU, Adam, early stopping |
| One-Class SVM | Anomaly detection | RBF kernel, nu=0.1, trained on genuine only |

### Evaluation
- 5-fold stratified cross-validation
- Metrics: Accuracy, Precision, Recall, F1, AUC-ROC, EER
- Current best: **Random Forest at 92.5% accuracy, 0.98 AUC, 7.5% EER**

### Current Data Limitation
- Only **8 participants** (P01-P08) with ~40 total samples (8 genuine + 8 impostor + enrollment variations)
- This is far too small for a production ML model
- Synthetic data augmentation is the immediate fix (addressed by the data generator)

## Collected Data Structure

```
collected_data/SecureBank_Research/
├── experiment_summary_*.csv    # Session metadata summaries
├── generate_sessions.py        # Script to create genuine/impostor from enrollment data
└── P01-P08/                    # Per-participant folders
    ├── enrollment_*_pin_keystrokes.csv
    ├── enrollment_*_keystrokes.csv
    ├── enrollment_*_touches.csv
    ├── enrollment_*_motion.csv
    ├── enrollment_*_metadata.csv
    ├── genuine_*_{same files}
    └── impostor_*_{same files}
```

### CSV Formats

**pin_keystrokes.csv**: `timestamp, digit, key_down_time, key_up_time, dwell_time_ms, flight_time_ms, touch_x, touch_y, touch_size, attempt_number`

**touches.csv**: `timestamp, touch_type, start_x, start_y, end_x, end_y, pressure, touch_size, duration_ms, velocity, acceleration, hold_duration_ms, touch_area`

**motion.csv**: `timestamp, accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z, pitch, roll, azimuth, filtered_accel_x, filtered_accel_y, filtered_accel_z, device_state`

**metadata.csv**: `key, value` pairs (session_id, participant_id, profile_owner_id, session_type, device info)
