#!/usr/bin/env python3
"""
ML Model Pipeline v2 — Behavioral Authentication
=====================================================
Uses ENROLLMENT-RELATIVE features: computes deviation from each 
user's enrolled baseline, which is the proper approach for 
behavioral authentication. This gives the model a clear signal 
for genuine (low deviation) vs impostor (high deviation).
"""

import os
import csv
import json
import warnings
import numpy as np
import pandas as pd
from pathlib import Path
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.svm import OneClassSVM
from sklearn.neural_network import MLPClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import (accuracy_score, precision_score, recall_score,
                             f1_score, roc_curve, auc, confusion_matrix)
from scipy import stats

warnings.filterwarnings("ignore")

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
SYNTHETIC_DATA_DIR = os.path.join(SCRIPT_DIR, "synthetic_data")
REAL_DATA_DIR = os.path.join(SCRIPT_DIR, "..", "collected_data", "SecureBank_Research")
RESULTS_DIR = os.path.join(SCRIPT_DIR, "results")


def read_csv_data(filepath):
    if not os.path.exists(filepath):
        return []
    with open(filepath, "r") as f:
        return list(csv.DictReader(f))


def find_session_prefix(participant_dir, session_type):
    """Find the latest session prefix for a given type."""
    files = sorted([f for f in os.listdir(participant_dir) if f.startswith(session_type)])
    metadata = [f for f in files if f.endswith("_metadata.csv")]
    if not metadata:
        return None
    return metadata[-1].replace("_metadata.csv", "")


def find_all_session_prefixes(participant_dir, session_type):
    """Find ALL session prefixes for a given type (not just the last one)."""
    files = sorted([f for f in os.listdir(participant_dir) if f.startswith(session_type)])
    metadata = [f for f in files if f.endswith("_metadata.csv")]
    return [m.replace("_metadata.csv", "") for m in metadata]


def extract_raw_features(participant_dir, prefix):
    """Extract raw behavioral features from a session."""
    if not prefix:
        return None
    
    pin_rows = read_csv_data(os.path.join(participant_dir, f"{prefix}_pin_keystrokes.csv"))
    touch_rows = read_csv_data(os.path.join(participant_dir, f"{prefix}_touches.csv"))
    motion_rows = read_csv_data(os.path.join(participant_dir, f"{prefix}_motion.csv"))
    
    features = {}
    
    # PIN features
    if pin_rows and len(pin_rows) >= 6:
        dwell = [float(r["dwell_time_ms"]) for r in pin_rows]
        flight = [float(r["flight_time_ms"]) for r in pin_rows if float(r["flight_time_ms"]) > 0]
        tx = [float(r["touch_x"]) for r in pin_rows]
        ty = [float(r["touch_y"]) for r in pin_rows]
        ts = [float(r["touch_size"]) for r in pin_rows]
        
        features.update({
            "pin_dwell_mean": np.mean(dwell), "pin_dwell_std": np.std(dwell),
            "pin_dwell_median": np.median(dwell),
            "pin_dwell_q25": np.percentile(dwell, 25),
            "pin_dwell_q75": np.percentile(dwell, 75),
            "pin_flight_mean": np.mean(flight) if flight else 0,
            "pin_flight_std": np.std(flight) if flight else 0,
            "pin_flight_median": np.median(flight) if flight else 0,
            "pin_touch_x_mean": np.mean(tx),
            "pin_touch_x_std": np.std(tx),
            "pin_touch_y_mean": np.mean(ty),
            "pin_touch_y_std": np.std(ty),
            "pin_touch_size_mean": np.mean(ts),
            "pin_touch_size_std": np.std(ts),
            "pin_dwell_skew": float(stats.skew(dwell)) if len(dwell) > 2 else 0,
            "pin_dwell_kurtosis": float(stats.kurtosis(dwell)) if len(dwell) > 2 else 0,
            "pin_dwell_iqr": np.percentile(dwell, 75) - np.percentile(dwell, 25),
            "pin_flight_iqr": (np.percentile(flight, 75) - np.percentile(flight, 25)) if len(flight) > 3 else 0,
        })
        
        # Per-digit position rhythm
        for pos in range(6):
            pos_dwells = [float(pin_rows[i]["dwell_time_ms"]) for i in range(len(pin_rows)) if i % 6 == pos]
            features[f"pin_rhythm_d{pos}"] = np.mean(pos_dwells) if pos_dwells else 0
    
    # Touch features
    if touch_rows and len(touch_rows) >= 3:
        durations = [float(r["duration_ms"]) for r in touch_rows]
        velocities = [float(r["velocity"]) for r in touch_rows]
        accs = [float(r["acceleration"]) for r in touch_rows]
        pressures = [float(r.get("pressure", 0.0)) for r in touch_rows]
        touch_sizes = [float(r.get("touch_size", 0.0)) for r in touch_rows]
        touch_areas = [float(r.get("touch_area", r.get("touch_size", 0.0))) for r in touch_rows]
        holds = [float(r.get("hold_duration_ms", 0.0)) for r in touch_rows]
        taps = sum(1 for r in touch_rows if r["touch_type"] == "TAP")
        swipes = sum(1 for r in touch_rows if r["touch_type"] in ["SWIPE_UP", "SWIPE_DOWN", "SWIPE_LEFT", "SWIPE_RIGHT", "SCROLL"])
        long_presses = sum(1 for r in touch_rows if r["touch_type"] == "LONG_PRESS")
        
        features.update({
            "touch_pressure_mean": np.mean(pressures),
            "touch_pressure_std": np.std(pressures),
            "touch_area_mean": np.mean(touch_areas),
            "touch_area_std": np.std(touch_areas),
            "touch_size_mean": np.mean(touch_sizes),
            "touch_size_std": np.std(touch_sizes),
            "touch_hold_mean": np.mean(holds),
            "touch_hold_std": np.std(holds),
            "touch_duration_mean": np.mean(durations),
            "touch_duration_std": np.std(durations),
            "touch_velocity_mean": np.mean(velocities),
            "touch_velocity_std": np.std(velocities),
            "touch_velocity_max": np.max(velocities),
            "touch_accel_mean": np.mean(accs),
            "touch_accel_std": np.std(accs),
            "touch_tap_ratio": taps / len(touch_rows),
            "touch_swipe_ratio": swipes / len(touch_rows),
            "touch_long_press_ratio": long_presses / len(touch_rows),
            "touch_count": len(touch_rows),
        })
    
    # Motion features
    if motion_rows and len(motion_rows) >= 50:
        sample = motion_rows if len(motion_rows) <= 500 else [motion_rows[i] for i in np.random.choice(len(motion_rows), 500, replace=False)]
        try:
            ax_vals = [float(r["accel_x"]) for r in sample]
            ay_vals = [float(r["accel_y"]) for r in sample]
            az_vals = [float(r["accel_z"]) for r in sample]
            gx_vals = [float(r["gyro_x"]) for r in sample]
            gy_vals = [float(r["gyro_y"]) for r in sample]
            gz_vals = [float(r["gyro_z"]) for r in sample]
            
            accel_mag = [np.sqrt(x**2 + y**2 + z**2) for x, y, z in zip(ax_vals, ay_vals, az_vals)]
            gyro_mag = [np.sqrt(x**2 + y**2 + z**2) for x, y, z in zip(gx_vals, gy_vals, gz_vals)]
            
            features.update({
                "motion_accel_mag_mean": np.mean(accel_mag),
                "motion_accel_mag_std": np.std(accel_mag),
                "motion_gyro_mag_mean": np.mean(gyro_mag),
                "motion_gyro_mag_std": np.std(gyro_mag),
                "motion_accel_x_mean": np.mean(ax_vals),
                "motion_accel_y_mean": np.mean(ay_vals),
                "motion_accel_z_mean": np.mean(az_vals),
            })
        except (ValueError, KeyError):
            pass
    
    return features if features else None


def compute_deviation_features(enrollment_feats, session_feats):
    """Compute deviation of session features from enrollment baseline."""
    deviation = {}
    
    common_keys = set(enrollment_feats.keys()) & set(session_feats.keys())
    
    for key in common_keys:
        ev = enrollment_feats[key]
        sv = session_feats[key]
        
        # Absolute deviation
        deviation[f"dev_{key}_abs"] = abs(sv - ev)
        
        # Relative deviation (percentage change)
        if abs(ev) > 1e-6:
            deviation[f"dev_{key}_rel"] = abs(sv - ev) / abs(ev)
        else:
            deviation[f"dev_{key}_rel"] = abs(sv)
        
        # Raw session value (also useful)
        deviation[f"raw_{key}"] = sv
    
    # Overall deviation magnitude
    abs_devs = [v for k, v in deviation.items() if "_abs" in k]
    rel_devs = [v for k, v in deviation.items() if "_rel" in k]
    
    deviation["overall_abs_deviation"] = np.mean(abs_devs) if abs_devs else 0
    deviation["overall_rel_deviation"] = np.mean(rel_devs) if rel_devs else 0
    deviation["max_abs_deviation"] = np.max(abs_devs) if abs_devs else 0
    deviation["max_rel_deviation"] = np.max(rel_devs) if rel_devs else 0
    
    return deviation


def build_dataset_v2(data_dir):
    """Build feature dataset using enrollment-relative deviation features.
    Now picks up ALL genuine/impostor sessions per participant (not just the last)."""
    print("\n📊 Building enrollment-relative feature dataset...")

    participants = sorted([d for d in os.listdir(data_dir)
                          if os.path.isdir(os.path.join(data_dir, d)) and d.startswith("P")])

    samples = []

    for pid in participants:
        pdir = os.path.join(data_dir, pid)

        # Get enrollment features (baseline) - use the last enrollment
        enr_prefix = find_session_prefix(pdir, "enrollment_")
        if not enr_prefix:
            continue

        enr_feats = extract_raw_features(pdir, enr_prefix)
        if not enr_feats:
            continue

        # Process ALL genuine sessions
        gen_prefixes = find_all_session_prefixes(pdir, "genuine_")
        for gen_prefix in gen_prefixes:
            gen_feats = extract_raw_features(pdir, gen_prefix)
            if gen_feats:
                dev = compute_deviation_features(enr_feats, gen_feats)
                dev["participant_id"] = pid
                dev["session_type"] = "genuine"
                dev["is_genuine"] = 1
                samples.append(dev)

        # Process ALL impostor sessions
        imp_prefixes = find_all_session_prefixes(pdir, "impostor_")
        for imp_prefix in imp_prefixes:
            imp_feats = extract_raw_features(pdir, imp_prefix)
            if imp_feats:
                dev = compute_deviation_features(enr_feats, imp_feats)
                dev["participant_id"] = pid
                dev["session_type"] = "impostor"
                dev["is_genuine"] = 0
                samples.append(dev)

    print(f"  ✅ {len(samples)} samples from {len(participants)} participants")
    df = pd.DataFrame(samples)
    genuine = df["is_genuine"].sum()
    impostor = len(df) - genuine
    print(f"  📈 Genuine: {genuine}, Impostor: {impostor}")

    return df


def train_models_v2(df):
    """Train and evaluate models with improved features."""
    print("\n🤖 Training ML Models (v2 — Enrollment-Relative)...")
    
    feature_cols = [c for c in df.columns if c not in ["participant_id", "session_type", "is_genuine"]]
    X = df[feature_cols].fillna(0).values
    y = df["is_genuine"].values
    
    # Remove any constant features
    non_const = np.std(X, axis=0) > 1e-8
    X = X[:, non_const]
    active_features = [f for f, nc in zip(feature_cols, non_const) if nc]
    
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    results = {}
    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    
    # ── Random Forest ──
    print("\n  🌲 Random Forest...")
    rf = RandomForestClassifier(n_estimators=300, max_depth=12, min_samples_split=3,
                                min_samples_leaf=2, random_state=42, class_weight="balanced")
    rf_pred = cross_val_predict(rf, X_scaled, y, cv=cv)
    rf_proba = cross_val_predict(rf, X_scaled, y, cv=cv, method="predict_proba")
    rf.fit(X_scaled, y)
    
    results["random_forest"] = compute_metrics(y, rf_pred, rf_proba[:, 1], "Random Forest")
    results["random_forest"]["feature_importance"] = list(zip(active_features, rf.feature_importances_.tolist()))
    
    # ── MLP Neural Network ──
    print("  🧠 MLP Neural Network...")
    mlp = MLPClassifier(hidden_layer_sizes=(128, 64, 32), activation="relu", solver="adam",
                       max_iter=1000, random_state=42, early_stopping=True,
                       validation_fraction=0.15, alpha=0.0005)
    mlp_pred = cross_val_predict(mlp, X_scaled, y, cv=cv)
    mlp_proba = cross_val_predict(mlp, X_scaled, y, cv=cv, method="predict_proba")
    mlp.fit(X_scaled, y)
    
    results["mlp"] = compute_metrics(y, mlp_pred, mlp_proba[:, 1], "MLP Neural Network")
    
    # ── One-Class SVM ──
    print("  🔍 One-Class SVM...")
    genuine_mask = y == 1
    X_genuine = X_scaled[genuine_mask]
    ocsvm = OneClassSVM(kernel="rbf", gamma="scale", nu=0.1)
    ocsvm.fit(X_genuine)
    
    ocsvm_raw = ocsvm.decision_function(X_scaled)
    ocsvm_pred = np.where(ocsvm.predict(X_scaled) == 1, 1, 0)
    ocsvm_scores = (ocsvm_raw - ocsvm_raw.min()) / (ocsvm_raw.max() - ocsvm_raw.min() + 1e-8)
    
    results["one_class_svm"] = compute_metrics(y, ocsvm_pred, ocsvm_scores, "One-Class SVM")
    
    results["feature_names"] = active_features
    results["X_scaled"] = X_scaled
    results["y"] = y
    results["participant_ids"] = df["participant_id"].values
    
    return results


def compute_metrics(y_true, y_pred, y_scores, model_name):
    acc = accuracy_score(y_true, y_pred)
    prec = precision_score(y_true, y_pred, zero_division=0)
    rec = recall_score(y_true, y_pred, zero_division=0)
    f1 = f1_score(y_true, y_pred, zero_division=0)
    
    fpr, tpr, thresholds = roc_curve(y_true, y_scores)
    roc_auc = auc(fpr, tpr)
    
    far = fpr
    frr = 1 - tpr
    eer_idx = np.nanargmin(np.abs(far - frr))
    eer = (far[eer_idx] + frr[eer_idx]) / 2
    
    cm = confusion_matrix(y_true, y_pred)
    
    print(f"     ✅ {model_name}: Acc={acc:.4f}, F1={f1:.4f}, AUC={roc_auc:.4f}, EER={eer:.4f}")
    
    return {
        "model_name": model_name,
        "accuracy": float(acc), "precision": float(prec),
        "recall": float(rec), "f1_score": float(f1),
        "roc_auc": float(roc_auc), "eer": float(eer),
        "fpr": fpr.tolist(), "tpr": tpr.tolist(),
        "far": far.tolist(), "frr": frr.tolist(),
        "thresholds": thresholds.tolist(),
        "confusion_matrix": cm.tolist(),
        "y_true": y_true.tolist(), "y_pred": y_pred.tolist(),
        "y_scores": y_scores.tolist(),
    }


def save_results(results):
    os.makedirs(RESULTS_DIR, exist_ok=True)

    summary = []
    for key in ["random_forest", "mlp", "one_class_svm"]:
        m = results[key]
        summary.append({
            "Model": m["model_name"],
            "Accuracy": f"{m['accuracy']:.4f}",
            "Precision": f"{m['precision']:.4f}",
            "Recall": f"{m['recall']:.4f}",
            "F1 Score": f"{m['f1_score']:.4f}",
            "AUC-ROC": f"{m['roc_auc']:.4f}",
            "EER": f"{m['eer']:.4f}",
        })

    summary_df = pd.DataFrame(summary)
    summary_df.to_csv(os.path.join(RESULTS_DIR, "model_comparison.csv"), index=False)

    json_results = {}
    for key in ["random_forest", "mlp", "one_class_svm"]:
        m = results[key]
        json_results[key] = {k: v for k, v in m.items()
                            if k not in ["y_true", "y_pred", "y_scores"]}

    if "feature_importance" in results["random_forest"]:
        fi = results["random_forest"]["feature_importance"]
        fi_sorted = sorted(fi, key=lambda x: x[1], reverse=True)
        json_results["feature_importance"] = fi_sorted

        fi_df = pd.DataFrame(fi_sorted, columns=["Feature", "Importance"])
        fi_df.to_csv(os.path.join(RESULTS_DIR, "feature_importance.csv"), index=False)

    with open(os.path.join(RESULTS_DIR, "detailed_results.json"), "w") as f:
        json.dump(json_results, f, indent=2)

    print(f"\n  📄 Results saved to {RESULTS_DIR}")
    print("\n" + "=" * 70)
    print("  MODEL COMPARISON RESULTS (v2 — Enrollment-Relative)")
    print("=" * 70)
    print(summary_df.to_string(index=False))
    print("=" * 70)


def export_model_for_android(X, y, feature_names, scaler):
    """Export the trained MLP model as JSON weights for on-device Kotlin inference.

    This approach avoids TensorFlow/ONNX dependency issues and works on any Python version.
    The exported JSON contains the MLP layer weights + biases + scaler params.
    A lightweight Kotlin inference engine (MLModelInference.kt) loads and runs it.
    """
    print("\n  Exporting model for Android on-device inference...")

    # Train a final MLP on full data
    from sklearn.neural_network import MLPClassifier

    X_scaled = scaler.transform(X)

    mlp = MLPClassifier(
        hidden_layer_sizes=(128, 64, 32),
        activation="relu",
        solver="adam",
        max_iter=1000,
        random_state=42,
        early_stopping=True,
        validation_fraction=0.15,
        alpha=0.0005
    )
    mlp.fit(X_scaled, y)

    train_acc = mlp.score(X_scaled, y)
    print(f"    Final MLP training accuracy: {train_acc:.4f}")

    # Export weights as JSON
    model_data = {
        "model_type": "mlp_binary_classifier",
        "activation": "relu",
        "output_activation": "sigmoid",
        "n_features": int(X.shape[1]),
        "feature_names": feature_names,
        "layers": [],
        "scaler": {
            "means": scaler.mean_.tolist(),
            "scales": scaler.scale_.tolist(),
        },
        "threshold": 0.5,
        "training_accuracy": float(train_acc),
        "training_samples": int(X.shape[0]),
    }

    for i, (weights, biases) in enumerate(zip(mlp.coefs_, mlp.intercepts_)):
        model_data["layers"].append({
            "layer_index": i,
            "input_size": weights.shape[0],
            "output_size": weights.shape[1],
            "weights": weights.tolist(),
            "biases": biases.tolist(),
        })

    model_dir = os.path.join(RESULTS_DIR, "android_model")
    os.makedirs(model_dir, exist_ok=True)

    model_path = os.path.join(model_dir, "behavioral_auth_model.json")
    with open(model_path, "w") as f:
        json.dump(model_data, f)

    model_size_kb = os.path.getsize(model_path) / 1024
    print(f"    Model saved: {model_path} ({model_size_kb:.1f} KB)")
    print(f"    Layers: {' -> '.join(str(l['output_size']) for l in model_data['layers'])}")
    print(f"    Copy to: app/src/main/assets/ml/behavioral_auth_model.json")

    return model_path


def main():
    print("=" * 70)
    print("  ML MODEL PIPELINE v2 — Enrollment-Relative Authentication")
    print("=" * 70)

    data_dir = SYNTHETIC_DATA_DIR if os.path.exists(SYNTHETIC_DATA_DIR) else REAL_DATA_DIR
    print(f"\n  Dataset: {data_dir}")

    df = build_dataset_v2(data_dir)

    if len(df) < 10:
        print("❌ Not enough data! Run synthetic_data_generator.py first.")
        return

    os.makedirs(RESULTS_DIR, exist_ok=True)
    df.to_csv(os.path.join(RESULTS_DIR, "feature_dataset.csv"), index=False)
    print(f"  💾 Saved: {len(df)} samples × {len(df.columns) - 3} features")

    results = train_models_v2(df)
    save_results(results)

    # Export TFLite model for Android
    X = results["X_scaled"]
    y = results["y"]
    feature_names = results["feature_names"]

    # Reconstruct scaler from the scaled data
    feature_cols = [c for c in df.columns if c not in ["participant_id", "session_type", "is_genuine"]]
    X_raw = df[feature_cols].fillna(0).values
    non_const = np.std(X_raw, axis=0) > 1e-8
    X_raw = X_raw[:, non_const]

    scaler = StandardScaler()
    scaler.fit(X_raw)

    export_model_for_android(X_raw, y, feature_names, scaler)

    print("\n✅ Pipeline complete! Run generate_visualizations.py to update figures.")


if __name__ == "__main__":
    main()
