#!/usr/bin/env python3
"""
Synthetic Data Generator for Behavioral Biometrics Research
============================================================
Reads 8 real participants' data, extracts statistical profiles,
and generates 32 synthetic participants (P09-P40) with unique
behavioral fingerprints and age demographics.
"""

import os
import csv
import random
import uuid
import numpy as np
from datetime import datetime
from pathlib import Path

# ─── Configuration ──────────────────────────────────────────
REAL_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "collected_data", "SecureBank_Research")
SYNTHETIC_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "synthetic_data")
NUM_SYNTHETIC = 32  # P09 to P40

REAL_PARTICIPANTS = [f"P{str(i).zfill(2)}" for i in range(1, 9)]
SYNTHETIC_PARTICIPANTS = [f"P{str(i).zfill(2)}" for i in range(9, 9 + NUM_SYNTHETIC)]

PIN_DIGITS = "382946"
PIN_LENGTH = 6
ENROLLMENT_ATTEMPTS = 5
SESSION_ATTEMPTS = 3

# Age groups with behavioral modifiers
AGE_GROUPS = {
    "18-25": {"dwell_scale": 0.85, "flight_scale": 0.80, "touch_precision": 0.90, "count": 10},
    "26-35": {"dwell_scale": 1.00, "flight_scale": 1.00, "touch_precision": 1.00, "count": 10},
    "36-45": {"dwell_scale": 1.20, "flight_scale": 1.15, "touch_precision": 1.10, "count": 7},
    "46-55": {"dwell_scale": 1.45, "flight_scale": 1.35, "touch_precision": 1.25, "count": 5},
}

GENDERS = ["Male", "Female"]

np.random.seed(42)
random.seed(42)


# ─── Data Reading ───────────────────────────────────────────

def read_csv(filepath):
    """Read CSV file and return list of dicts."""
    if not os.path.exists(filepath):
        return []
    with open(filepath, "r") as f:
        reader = csv.DictReader(f)
        return list(reader)


def find_session_files(participant_dir, session_type):
    """Find the latest session files for a given type."""
    files = sorted([f for f in os.listdir(participant_dir) if f.startswith(session_type)])
    if not files:
        return None
    # Get prefix of latest session
    metadata_files = [f for f in files if f.endswith("_metadata.csv")]
    if not metadata_files:
        return None
    latest_prefix = metadata_files[-1].replace("_metadata.csv", "")
    return latest_prefix


def extract_pin_profile(participant_dir):
    """Extract PIN keystroke timing profile from a participant."""
    prefix = find_session_files(participant_dir, "enrollment_")
    if not prefix:
        return None
    
    pin_file = os.path.join(participant_dir, f"{prefix}_pin_keystrokes.csv")
    rows = read_csv(pin_file)
    if not rows:
        return None
    
    dwell_times = [float(r["dwell_time_ms"]) for r in rows]
    flight_times = [float(r["flight_time_ms"]) for r in rows if float(r["flight_time_ms"]) > 0]
    touch_xs = [float(r["touch_x"]) for r in rows]
    touch_ys = [float(r["touch_y"]) for r in rows]
    touch_sizes = [float(r["touch_size"]) for r in rows]
    
    return {
        "dwell_mean": np.mean(dwell_times),
        "dwell_std": np.std(dwell_times),
        "dwell_min": np.min(dwell_times),
        "dwell_max": np.max(dwell_times),
        "flight_mean": np.mean(flight_times) if flight_times else 500,
        "flight_std": np.std(flight_times) if flight_times else 200,
        "flight_min": np.min(flight_times) if flight_times else 50,
        "flight_max": np.max(flight_times) if flight_times else 2000,
        "touch_x_mean": np.mean(touch_xs),
        "touch_x_std": np.std(touch_xs),
        "touch_y_mean": np.mean(touch_ys),
        "touch_y_std": np.std(touch_ys),
        "touch_size_mean": np.mean(touch_sizes),
        "touch_size_std": np.std(touch_sizes),
    }


def extract_touch_profile(participant_dir):
    """Extract touch interaction profile from a participant."""
    prefix = find_session_files(participant_dir, "enrollment_")
    if not prefix:
        return None
    
    touch_file = os.path.join(participant_dir, f"{prefix}_touches.csv")
    rows = read_csv(touch_file)
    if not rows:
        return None
    
    durations = [float(r["duration_ms"]) for r in rows]
    velocities = [float(r["velocity"]) for r in rows]
    accelerations = [float(r["acceleration"]) for r in rows]
    
    touch_types = {}
    for r in rows:
        t = r["touch_type"]
        touch_types[t] = touch_types.get(t, 0) + 1
    
    total = sum(touch_types.values())
    type_ratios = {k: v / total for k, v in touch_types.items()}
    
    return {
        "duration_mean": np.mean(durations),
        "duration_std": np.std(durations),
        "velocity_mean": np.mean(velocities),
        "velocity_std": np.std(velocities),
        "acceleration_mean": np.mean(accelerations),
        "acceleration_std": np.std(accelerations),
        "type_ratios": type_ratios,
        "event_count": len(rows),
    }


def extract_motion_profile(participant_dir):
    """Extract motion sensor profile from a participant."""
    prefix = find_session_files(participant_dir, "enrollment_")
    if not prefix:
        return None
    
    motion_file = os.path.join(participant_dir, f"{prefix}_motion.csv")
    rows = read_csv(motion_file)
    if not rows or len(rows) < 100:
        return None
    
    # Sample to avoid processing millions of rows
    sample = random.sample(rows, min(500, len(rows)))
    
    accel_x = [float(r["accel_x"]) for r in sample]
    accel_y = [float(r["accel_y"]) for r in sample]
    accel_z = [float(r["accel_z"]) for r in sample]
    gyro_x = [float(r["gyro_x"]) for r in sample]
    gyro_y = [float(r["gyro_y"]) for r in sample]
    gyro_z = [float(r["gyro_z"]) for r in sample]
    
    return {
        "accel_x_mean": np.mean(accel_x), "accel_x_std": np.std(accel_x),
        "accel_y_mean": np.mean(accel_y), "accel_y_std": np.std(accel_y),
        "accel_z_mean": np.mean(accel_z), "accel_z_std": np.std(accel_z),
        "gyro_x_mean": np.mean(gyro_x), "gyro_x_std": np.std(gyro_x),
        "gyro_y_mean": np.mean(gyro_y), "gyro_y_std": np.std(gyro_y),
        "gyro_z_mean": np.mean(gyro_z), "gyro_z_std": np.std(gyro_z),
        "sample_count": len(rows),
    }


# ─── Synthetic Profile Generation ──────────────────────────

def generate_unique_profile(real_profiles, age_modifier):
    """Generate a unique behavioral profile by mixing real profiles with noise."""
    # Pick 2-3 real profiles and blend them
    num_blend = random.randint(2, min(3, len(real_profiles)))
    chosen = random.sample(real_profiles, num_blend)
    weights = np.random.dirichlet(np.ones(num_blend))
    
    profile = {}
    for key in chosen[0].keys():
        if isinstance(chosen[0][key], dict):
            continue
        weighted_val = sum(w * c[key] for w, c in zip(weights, chosen))
        # Add individual variation (10-25% noise)
        noise = np.random.normal(1.0, 0.15)
        profile[key] = weighted_val * noise
    
    # Apply age modifier to timing-related features
    for key in profile:
        if "dwell" in key and "std" not in key:
            profile[key] *= age_modifier["dwell_scale"]
        elif "flight" in key and "std" not in key:
            profile[key] *= age_modifier["flight_scale"]
    
    return profile


def generate_pin_keystrokes(profile, num_attempts, session_type="enrollment"):
    """Generate synthetic PIN keystroke events."""
    rows = []
    base_time = int(datetime.now().timestamp() * 1000)
    current_time = base_time
    
    for attempt in range(1, num_attempts + 1):
        for i, digit in enumerate(PIN_DIGITS):
            # Dwell time: sample from profile distribution, clamp to realistic range
            dwell = max(8, int(np.random.normal(
                profile["dwell_mean"],
                max(5, profile["dwell_std"])
            )))
            dwell = min(dwell, 150)
            
            # Flight time: 0 for first digit, otherwise from profile
            if i == 0:
                flight = 0 if attempt == 1 else max(100, int(np.random.normal(
                    profile["flight_mean"] * 1.5,
                    profile["flight_std"]
                )))
            else:
                flight = max(50, int(np.random.normal(
                    profile["flight_mean"],
                    max(30, profile["flight_std"])
                )))
            
            current_time += flight
            key_down = current_time
            key_up = key_down + dwell
            
            # Touch coordinates with per-user variation
            touch_x = max(50, min(200, np.random.normal(
                profile["touch_x_mean"],
                profile["touch_x_std"]
            )))
            touch_y = max(50, min(200, np.random.normal(
                profile["touch_y_mean"],
                profile["touch_y_std"]
            )))
            touch_size = max(1, np.random.normal(
                profile["touch_size_mean"],
                max(3, profile["touch_size_std"])
            ))
            
            rows.append({
                "timestamp": key_down,
                "digit": digit,
                "key_down_time": key_down,
                "key_up_time": key_up,
                "dwell_time_ms": dwell,
                "flight_time_ms": flight,
                "touch_x": round(touch_x, 5),
                "touch_y": round(touch_y, 5),
                "touch_size": round(touch_size, 5),
                "attempt_number": attempt,
            })
            
            current_time = key_up
        
        # Inter-attempt pause
        current_time += random.randint(500, 2000)
    
    return rows


def generate_touch_events(touch_profile, age_modifier, num_events=None):
    """Generate synthetic touch interaction events."""
    if num_events is None:
        num_events = random.randint(55, 85)
    
    rows = []
    base_time = int(datetime.now().timestamp() * 1000)
    
    touch_types = ["TAP", "SWIPE_UP", "SWIPE_DOWN", "SWIPE_LEFT", "SWIPE_RIGHT"]
    type_weights = [0.3, 0.25, 0.25, 0.1, 0.1]
    
    for i in range(num_events):
        touch_type = random.choices(touch_types, weights=type_weights, k=1)[0]
        
        start_x = random.uniform(50, 950)
        start_y = random.uniform(200, 1800)
        
        if touch_type == "TAP":
            end_x = start_x + np.random.normal(0, 2)
            end_y = start_y + np.random.normal(0, 2)
            duration = max(20, int(np.random.normal(60, 20) * age_modifier["dwell_scale"]))
            velocity = 0.0
        elif "SWIPE" in touch_type:
            if "UP" in touch_type:
                end_x = start_x + np.random.normal(0, 30)
                end_y = start_y - random.uniform(200, 600)
            elif "DOWN" in touch_type:
                end_x = start_x + np.random.normal(0, 30)
                end_y = start_y + random.uniform(200, 600)
            elif "LEFT" in touch_type:
                end_x = start_x - random.uniform(200, 500)
                end_y = start_y + np.random.normal(0, 30)
            else:
                end_x = start_x + random.uniform(200, 500)
                end_y = start_y + np.random.normal(0, 30)
            
            duration = max(50, int(np.random.normal(
                touch_profile["duration_mean"],
                max(20, touch_profile["duration_std"])
            ) * age_modifier["dwell_scale"]))
            
            distance = np.sqrt((end_x - start_x)**2 + (end_y - start_y)**2)
            velocity = distance / max(1, duration) * 1000
        
        acceleration = max(0, np.random.normal(
            touch_profile["acceleration_mean"],
            max(50, touch_profile["acceleration_std"])
        )) if "SWIPE" in touch_type else 0.0
        
        rows.append({
            "timestamp": base_time + i * random.randint(500, 3000),
            "touch_type": touch_type,
            "start_x": round(start_x, 5),
            "start_y": round(start_y, 5),
            "end_x": round(end_x, 5),
            "end_y": round(end_y, 5),
            "pressure": 1.0,
            "touch_size": 1.0,
            "duration_ms": duration,
            "velocity": round(velocity, 4),
            "acceleration": round(acceleration, 4),
            "hold_duration_ms": duration,
            "touch_area": 1.0,
        })
    
    return rows


def generate_motion_data(motion_profile, num_samples=None):
    """Generate synthetic motion/sensor data."""
    if num_samples is None:
        num_samples = random.randint(4000, 20000)
    
    rows = []
    base_time = int(datetime.now().timestamp() * 1000)
    
    # Generate smooth sensor data using random walk with mean reversion
    accel_x = motion_profile["accel_x_mean"]
    accel_y = motion_profile["accel_y_mean"]
    accel_z = motion_profile["accel_z_mean"]
    gyro_x = motion_profile["gyro_x_mean"]
    gyro_y = motion_profile["gyro_y_mean"]
    gyro_z = motion_profile["gyro_z_mean"]
    
    for i in range(num_samples):
        # Mean-reverting random walk
        accel_x += np.random.normal(0, 0.05) - 0.01 * (accel_x - motion_profile["accel_x_mean"])
        accel_y += np.random.normal(0, 0.05) - 0.01 * (accel_y - motion_profile["accel_y_mean"])
        accel_z += np.random.normal(0, 0.03) - 0.01 * (accel_z - motion_profile["accel_z_mean"])
        gyro_x += np.random.normal(0, 0.002) - 0.05 * (gyro_x - motion_profile["gyro_x_mean"])
        gyro_y += np.random.normal(0, 0.002) - 0.05 * (gyro_y - motion_profile["gyro_y_mean"])
        gyro_z += np.random.normal(0, 0.002) - 0.05 * (gyro_z - motion_profile["gyro_z_mean"])
        
        # Derived values
        pitch = np.degrees(np.arctan2(accel_y, np.sqrt(accel_x**2 + accel_z**2)))
        roll = np.degrees(np.arctan2(accel_x, np.sqrt(accel_y**2 + accel_z**2)))
        
        # Filtered accelerometer (low-pass filter simulation)
        filtered_x = accel_x * 0.95 + np.random.normal(0, 0.01)
        filtered_y = accel_y * 0.95 + np.random.normal(0, 0.01)
        filtered_z = accel_z * 0.95 + np.random.normal(0, 0.01)
        
        # Device state
        accel_mag = np.sqrt(accel_x**2 + accel_y**2 + accel_z**2)
        gyro_mag = np.sqrt(gyro_x**2 + gyro_y**2 + gyro_z**2)
        state = "STATIONARY" if gyro_mag < 0.1 else ("IN_MOTION" if gyro_mag > 0.5 else "SLIGHT_MOVEMENT")
        
        rows.append({
            "timestamp": base_time + i * 2,  # ~500Hz sampling
            "accel_x": round(accel_x, 8),
            "accel_y": round(accel_y, 8),
            "accel_z": round(accel_z, 8),
            "gyro_x": round(gyro_x, 9),
            "gyro_y": round(gyro_y, 9),
            "gyro_z": round(gyro_z, 9),
            "pitch": round(pitch, 6),
            "roll": round(roll, 6),
            "azimuth": 0.0,
            "filtered_accel_x": round(filtered_x, 8),
            "filtered_accel_y": round(filtered_y, 8),
            "filtered_accel_z": round(filtered_z, 8),
            "device_state": state,
        })
    
    return rows


def generate_keystrokes(profile, age_modifier, num_keys=None):
    """Generate synthetic software keyboard keystroke data."""
    if num_keys is None:
        num_keys = random.randint(80, 200)
    
    rows = []
    base_time = int(datetime.now().timestamp() * 1000)
    
    common_keys = list(range(29, 55))  # a-z key codes
    
    for i in range(num_keys):
        key_code = random.choice(common_keys)
        dwell = max(30, int(np.random.normal(
            profile["dwell_mean"] * 1.2,  # Software keyboard slightly slower
            profile["dwell_std"] * 1.5
        ) * age_modifier["dwell_scale"]))
        flight = max(50, int(np.random.normal(
            profile["flight_mean"] * 0.8,
            profile["flight_std"]
        ) * age_modifier["flight_scale"]))
        
        rows.append({
            "timestamp": base_time + i * (dwell + flight),
            "key_code": key_code,
            "dwell_time_ms": dwell,
            "flight_time_ms": flight,
            "is_baseline": "true",
        })
    
    return rows


# ─── CSV Writing ────────────────────────────────────────────

def write_csv(filepath, rows, fieldnames=None):
    """Write list of dicts to CSV."""
    if not rows:
        return
    if fieldnames is None:
        fieldnames = rows[0].keys()
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def write_metadata(filepath, participant_id, profile_owner_id, session_type):
    """Write session metadata CSV."""
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "w") as f:
        f.write("key,value\n")
        f.write(f"session_id,{uuid.uuid4()}\n")
        f.write(f"participant_id,{participant_id}\n")
        f.write(f"profile_owner_id,{profile_owner_id}\n")
        f.write(f"session_type,{session_type}\n")
        f.write(f"export_timestamp,{int(datetime.now().timestamp() * 1000)}\n")
        f.write("device_model,synthetic_data\n")
        f.write("device_manufacturer,research\n")
        f.write("android_version,35\n")


# ─── Main Pipeline ──────────────────────────────────────────

def main():
    print("=" * 65)
    print("  SYNTHETIC DATA GENERATOR - Behavioral Biometrics Research")
    print("=" * 65)
    
    # Step 1: Read real data profiles
    print("\n📖 Step 1: Reading real participant data...")
    pin_profiles = []
    touch_profiles = []
    motion_profiles = []
    
    for pid in REAL_PARTICIPANTS:
        pdir = os.path.join(REAL_DATA_DIR, pid)
        if not os.path.exists(pdir):
            print(f"  ⚠️  {pid} not found, skipping")
            continue
        
        pin = extract_pin_profile(pdir)
        touch = extract_touch_profile(pdir)
        motion = extract_motion_profile(pdir)
        
        if pin:
            pin_profiles.append(pin)
            print(f"  ✅ {pid}: dwell={pin['dwell_mean']:.1f}±{pin['dwell_std']:.1f}ms, "
                  f"flight={pin['flight_mean']:.0f}±{pin['flight_std']:.0f}ms")
        if touch:
            touch_profiles.append(touch)
        if motion:
            motion_profiles.append(motion)
    
    print(f"\n  📊 Extracted {len(pin_profiles)} PIN, {len(touch_profiles)} touch, "
          f"{len(motion_profiles)} motion profiles")
    
    if not pin_profiles or not touch_profiles or not motion_profiles:
        print("❌ Not enough real data to generate synthetic participants!")
        return
    
    # Step 2: Generate demographics
    print("\n👥 Step 2: Generating participant demographics...")
    demographics = []
    synth_idx = 0
    
    # Add real participants first
    for i, pid in enumerate(REAL_PARTICIPANTS):
        age_group = "18-25" if i < 4 else "26-35"
        demographics.append({
            "participant_id": pid,
            "age_group": age_group,
            "age": random.randint(int(age_group.split("-")[0]), int(age_group.split("-")[1])),
            "gender": random.choice(GENDERS),
            "is_synthetic": False,
        })
    
    # Generate synthetic demographics
    for age_group, config in AGE_GROUPS.items():
        for _ in range(config["count"]):
            if synth_idx >= NUM_SYNTHETIC:
                break
            pid = SYNTHETIC_PARTICIPANTS[synth_idx]
            age_min, age_max = map(int, age_group.split("-"))
            demographics.append({
                "participant_id": pid,
                "age_group": age_group,
                "age": random.randint(age_min, age_max),
                "gender": random.choice(GENDERS),
                "is_synthetic": True,
            })
            synth_idx += 1
    
    # Save demographics
    os.makedirs(SYNTHETIC_DATA_DIR, exist_ok=True)
    write_csv(
        os.path.join(SYNTHETIC_DATA_DIR, "participants_demographics.csv"),
        demographics,
        ["participant_id", "age_group", "age", "gender", "is_synthetic"]
    )
    print(f"  ✅ Demographics saved for {len(demographics)} participants")
    
    # Print age group distribution
    for ag in AGE_GROUPS:
        count = sum(1 for d in demographics if d["age_group"] == ag)
        print(f"     {ag}: {count} participants")
    
    # Step 3: Copy real data to synthetic directory
    print("\n📁 Step 3: Copying real participant data...")
    for pid in REAL_PARTICIPANTS:
        src_dir = os.path.join(REAL_DATA_DIR, pid)
        dst_dir = os.path.join(SYNTHETIC_DATA_DIR, pid)
        if os.path.exists(src_dir):
            os.makedirs(dst_dir, exist_ok=True)
            for f in os.listdir(src_dir):
                if f.endswith(".csv"):
                    src = os.path.join(src_dir, f)
                    dst = os.path.join(dst_dir, f)
                    if not os.path.exists(dst):
                        import shutil
                        shutil.copy2(src, dst)
            print(f"  ✅ {pid} data copied")
    
    # Step 4: Generate synthetic participants
    print(f"\n🔬 Step 4: Generating {NUM_SYNTHETIC} synthetic participants...")
    
    for demo in demographics:
        if not demo["is_synthetic"]:
            continue
        
        pid = demo["participant_id"]
        age_group = demo["age_group"]
        age_mod = AGE_GROUPS[age_group]
        
        pdir = os.path.join(SYNTHETIC_DATA_DIR, pid)
        os.makedirs(pdir, exist_ok=True)
        
        # Generate unique behavioral profile for this participant
        pin_profile = generate_unique_profile(pin_profiles, age_mod)
        touch_profile_data = random.choice(touch_profiles)
        motion_profile_data = random.choice(motion_profiles)
        
        timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
        
        # --- ENROLLMENT session ---
        enr_prefix = f"enrollment_{timestamp}"
        pin_data = generate_pin_keystrokes(pin_profile, ENROLLMENT_ATTEMPTS)
        touch_data = generate_touch_events(touch_profile_data, age_mod)
        motion_data = generate_motion_data(motion_profile_data)
        keystroke_data = generate_keystrokes(pin_profile, age_mod)
        
        write_csv(os.path.join(pdir, f"{enr_prefix}_pin_keystrokes.csv"), pin_data)
        write_csv(os.path.join(pdir, f"{enr_prefix}_touches.csv"), touch_data)
        write_csv(os.path.join(pdir, f"{enr_prefix}_motion.csv"), motion_data)
        write_csv(os.path.join(pdir, f"{enr_prefix}_keystrokes.csv"), keystroke_data)
        write_metadata(os.path.join(pdir, f"{enr_prefix}_metadata.csv"), pid, pid, "ENROLLMENT")
        
        # --- GENUINE session ---
        gen_prefix = f"genuine_{timestamp}"
        # Genuine = same profile with slight variation
        gen_pin = generate_pin_keystrokes(pin_profile, SESSION_ATTEMPTS)
        gen_touch = generate_touch_events(touch_profile_data, age_mod, num_events=random.randint(50, 75))
        gen_motion = generate_motion_data(motion_profile_data, num_samples=random.randint(3000, 12000))
        gen_keys = generate_keystrokes(pin_profile, age_mod, num_keys=random.randint(60, 150))
        
        write_csv(os.path.join(pdir, f"{gen_prefix}_pin_keystrokes.csv"), gen_pin)
        write_csv(os.path.join(pdir, f"{gen_prefix}_touches.csv"), gen_touch)
        write_csv(os.path.join(pdir, f"{gen_prefix}_motion.csv"), gen_motion)
        write_csv(os.path.join(pdir, f"{gen_prefix}_keystrokes.csv"), gen_keys)
        write_metadata(os.path.join(pdir, f"{gen_prefix}_metadata.csv"), pid, pid, "GENUINE")
        
        # --- IMPOSTOR session ---
        # Pick a different synthetic participant as the impostor
        all_pids = REAL_PARTICIPANTS + SYNTHETIC_PARTICIPANTS
        impostor_candidates = [p for p in all_pids if p != pid]
        impostor_id = random.choice(impostor_candidates)
        
        # Impostor uses THEIR OWN profile (different from target)
        imp_pin_profile = generate_unique_profile(pin_profiles, age_mod)
        imp_prefix = f"impostor_{timestamp}"
        imp_pin = generate_pin_keystrokes(imp_pin_profile, SESSION_ATTEMPTS)
        imp_touch = generate_touch_events(touch_profile_data, age_mod, num_events=random.randint(50, 75))
        imp_motion = generate_motion_data(motion_profile_data, num_samples=random.randint(3000, 12000))
        imp_keys = generate_keystrokes(imp_pin_profile, age_mod, num_keys=random.randint(60, 150))
        
        write_csv(os.path.join(pdir, f"{imp_prefix}_pin_keystrokes.csv"), imp_pin)
        write_csv(os.path.join(pdir, f"{imp_prefix}_touches.csv"), imp_touch)
        write_csv(os.path.join(pdir, f"{imp_prefix}_motion.csv"), imp_motion)
        write_csv(os.path.join(pdir, f"{imp_prefix}_keystrokes.csv"), imp_keys)
        write_metadata(os.path.join(pdir, f"{imp_prefix}_metadata.csv"), impostor_id, pid, "IMPOSTOR")
        
        print(f"  ✅ {pid} (age {demo['age']}, {age_group}) generated — "
              f"enrollment + genuine + impostor (by {impostor_id})")
    
    # Summary
    print("\n" + "=" * 65)
    print("  ✅ SYNTHETIC DATA GENERATION COMPLETE!")
    print("=" * 65)
    total = len(REAL_PARTICIPANTS) + NUM_SYNTHETIC
    print(f"  Total participants: {total} ({len(REAL_PARTICIPANTS)} real + {NUM_SYNTHETIC} synthetic)")
    print(f"  Output directory: {SYNTHETIC_DATA_DIR}")
    
    # Count files
    total_files = 0
    for pid_dir in os.listdir(SYNTHETIC_DATA_DIR):
        d = os.path.join(SYNTHETIC_DATA_DIR, pid_dir)
        if os.path.isdir(d):
            total_files += len([f for f in os.listdir(d) if f.endswith(".csv")])
    print(f"  Total CSV files: {total_files}")
    print(f"  Demographics: {os.path.join(SYNTHETIC_DATA_DIR, 'participants_demographics.csv')}")


if __name__ == "__main__":
    main()
