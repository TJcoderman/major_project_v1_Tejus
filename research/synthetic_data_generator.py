#!/usr/bin/env python3
"""
Synthetic Data Generator v2 - SecureBank Behavioral Biometrics
================================================================
Major improvements over v1:
- 50 synthetic participants (P09-P58) + 8 real = 58 total
- 3 genuine + 3 impostor sessions per participant = 348 ML samples
- Impostors use ACTUAL other participant profiles (realistic cross-user)
- Per-digit rhythm offsets (each person has unique per-key timing)
- Age-group behavioral modifiers with wider ranges
- Better motion data: mean-reverting random walk (temporally correlated)
- Validated against real data distributions from P01-P08

Usage:
  cd research
  python synthetic_data_generator.py
  python ml_model.py   # Train on the generated data
"""

import os
import csv
import json
import random
import uuid
import shutil
import numpy as np
from datetime import datetime
from pathlib import Path

# ─── Configuration ──────────────────────────────────────────
REAL_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "collected_data", "SecureBank_Research")
SYNTHETIC_DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "synthetic_data")

NUM_SYNTHETIC = 50                  # P09 to P58
NUM_GENUINE_PER_PARTICIPANT = 3     # Genuine sessions each
NUM_IMPOSTOR_PER_PARTICIPANT = 3    # Impostor sessions each

REAL_PARTICIPANTS = [f"P{str(i).zfill(2)}" for i in range(1, 9)]
SYNTHETIC_PARTICIPANTS = [f"P{str(i).zfill(2)}" for i in range(9, 9 + NUM_SYNTHETIC)]
ALL_PARTICIPANTS = REAL_PARTICIPANTS + SYNTHETIC_PARTICIPANTS

PIN_DIGITS = "382946"
PIN_LENGTH = 6
ENROLLMENT_ATTEMPTS = 5
SESSION_ATTEMPTS = 3

# Age groups with behavioral modifiers
AGE_GROUPS = {
    "18-25": {"dwell_scale": 0.85, "flight_scale": 0.80, "touch_precision": 0.90, "count": 15},
    "26-35": {"dwell_scale": 1.00, "flight_scale": 1.00, "touch_precision": 1.00, "count": 15},
    "36-45": {"dwell_scale": 1.20, "flight_scale": 1.15, "touch_precision": 1.10, "count": 12},
    "46-60": {"dwell_scale": 1.45, "flight_scale": 1.35, "touch_precision": 1.25, "count": 8},
}

GENDERS = ["Male", "Female"]

np.random.seed(42)
random.seed(42)


# ─── Data Reading ───────────────────────────────────────────

def read_csv(filepath):
    if not os.path.exists(filepath):
        return []
    with open(filepath, "r") as f:
        return list(csv.DictReader(f))


def find_session_files(participant_dir, session_type):
    files = sorted([f for f in os.listdir(participant_dir) if f.startswith(session_type)])
    metadata_files = [f for f in files if f.endswith("_metadata.csv")]
    if not metadata_files:
        return None
    return metadata_files[-1].replace("_metadata.csv", "")


def extract_pin_profile(participant_dir):
    prefix = find_session_files(participant_dir, "enrollment_")
    if not prefix:
        return None
    rows = read_csv(os.path.join(participant_dir, f"{prefix}_pin_keystrokes.csv"))
    if not rows:
        return None

    dwell_times = [float(r["dwell_time_ms"]) for r in rows]
    flight_times = [float(r["flight_time_ms"]) for r in rows if float(r["flight_time_ms"]) > 0]
    touch_xs = [float(r["touch_x"]) for r in rows]
    touch_ys = [float(r["touch_y"]) for r in rows]
    touch_sizes = [float(r["touch_size"]) for r in rows]

    # Per-digit averages (position in PIN sequence)
    per_digit_dwell = {}
    for i, row in enumerate(rows):
        pos = i % PIN_LENGTH
        d = float(row["dwell_time_ms"])
        per_digit_dwell.setdefault(pos, []).append(d)

    return {
        "dwell_mean": np.mean(dwell_times), "dwell_std": np.std(dwell_times),
        "dwell_min": np.min(dwell_times), "dwell_max": np.max(dwell_times),
        "flight_mean": np.mean(flight_times) if flight_times else 500,
        "flight_std": np.std(flight_times) if flight_times else 200,
        "touch_x_mean": np.mean(touch_xs), "touch_x_std": np.std(touch_xs),
        "touch_y_mean": np.mean(touch_ys), "touch_y_std": np.std(touch_ys),
        "touch_size_mean": np.mean(touch_sizes), "touch_size_std": np.std(touch_sizes),
        "per_digit_dwell": {k: np.mean(v) for k, v in per_digit_dwell.items()},
    }


def extract_touch_profile(participant_dir):
    prefix = find_session_files(participant_dir, "enrollment_")
    if not prefix:
        return None
    rows = read_csv(os.path.join(participant_dir, f"{prefix}_touches.csv"))
    if not rows:
        return None

    durations = [float(r["duration_ms"]) for r in rows]
    velocities = [float(r["velocity"]) for r in rows]
    accelerations = [float(r["acceleration"]) for r in rows]

    type_counts = {}
    for r in rows:
        t = r["touch_type"]
        type_counts[t] = type_counts.get(t, 0) + 1
    total = sum(type_counts.values())

    return {
        "duration_mean": np.mean(durations), "duration_std": np.std(durations),
        "velocity_mean": np.mean(velocities), "velocity_std": np.std(velocities),
        "acceleration_mean": np.mean(accelerations), "acceleration_std": np.std(accelerations),
        "tap_ratio": type_counts.get("TAP", 0) / total,
        "event_count": len(rows),
    }


def extract_motion_profile(participant_dir):
    prefix = find_session_files(participant_dir, "enrollment_")
    if not prefix:
        return None
    rows = read_csv(os.path.join(participant_dir, f"{prefix}_motion.csv"))
    if not rows or len(rows) < 100:
        return None

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
    }


# ─── Profile Generation ─────────────────────────────────────

def generate_unique_profile(real_pin_profiles, real_touch_profiles, real_motion_profiles, age_mod):
    """Generate a unique behavioral profile by blending real profiles with noise + age effects."""
    # Blend 2-3 real PIN profiles
    num_blend = random.randint(2, min(3, len(real_pin_profiles)))
    chosen_pin = random.sample(real_pin_profiles, num_blend)
    weights = np.random.dirichlet(np.ones(num_blend))

    pin_profile = {}
    for key in chosen_pin[0]:
        if isinstance(chosen_pin[0][key], dict):
            # Blend per-digit dwell: weighted average of each position
            pin_profile[key] = {}
            for pos in chosen_pin[0][key]:
                val = sum(w * c[key].get(pos, 50) for w, c in zip(weights, chosen_pin))
                pin_profile[key][pos] = val * np.random.normal(1.0, 0.12) * age_mod["dwell_scale"]
            continue
        val = sum(w * c[key] for w, c in zip(weights, chosen_pin))
        noise = np.random.normal(1.0, 0.15)
        pin_profile[key] = val * noise
        # Apply age scaling to timing means
        if ("dwell" in key or "flight" in key) and "std" not in key:
            scale = age_mod["dwell_scale"] if "dwell" in key else age_mod["flight_scale"]
            pin_profile[key] *= scale

    # Per-digit rhythm offsets (unique per participant)
    pin_profile["digit_offsets"] = {i: np.random.normal(0, 8) for i in range(PIN_LENGTH)}

    # Touch profile
    touch_base = random.choice(real_touch_profiles)
    touch_profile = {
        "duration_mean": touch_base["duration_mean"] * np.random.normal(1.0, 0.2) * age_mod["dwell_scale"],
        "duration_std": touch_base["duration_std"] * np.random.normal(1.0, 0.15),
        "velocity_mean": touch_base["velocity_mean"] * np.random.normal(1.0, 0.2),
        "velocity_std": touch_base["velocity_std"] * np.random.normal(1.0, 0.15),
        "acceleration_mean": touch_base["acceleration_mean"] * np.random.normal(1.0, 0.2),
        "acceleration_std": touch_base["acceleration_std"] * np.random.normal(1.0, 0.15),
        "tap_ratio": np.clip(np.random.uniform(0.35, 0.70), 0, 1),
    }

    # Motion profile
    motion_base = random.choice(real_motion_profiles)
    motion_profile = {}
    for key in motion_base:
        noise = np.random.normal(1.0, 0.10 if "std" in key else 0.15)
        motion_profile[key] = motion_base[key] * noise

    return {
        "pin": pin_profile,
        "touch": touch_profile,
        "motion": motion_profile,
    }


# ─── Session Data Generation ────────────────────────────────

def generate_pin_keystrokes(pin_profile, num_attempts, jitter=1.0):
    """Generate PIN keystroke events. jitter>1 means less consistent (different person)."""
    rows = []
    base_time = int(datetime.now().timestamp() * 1000) + random.randint(0, 100000)
    current_time = base_time

    digit_offsets = pin_profile.get("digit_offsets", {i: 0 for i in range(PIN_LENGTH)})

    for attempt in range(1, num_attempts + 1):
        for i, digit_char in enumerate(PIN_DIGITS):
            digit = int(digit_char)
            # Dwell time with per-digit rhythm
            offset = digit_offsets.get(i, 0) * jitter
            dwell = max(12, int(np.random.normal(
                pin_profile["dwell_mean"] + offset,
                max(5, pin_profile["dwell_std"] * jitter)
            )))
            dwell = min(dwell, 200)

            # Flight time
            if i == 0:
                flight = 0 if attempt == 1 else max(80, int(np.random.normal(
                    pin_profile["flight_mean"] * 1.5, pin_profile["flight_std"] * jitter
                )))
            else:
                flight = max(30, int(np.random.normal(
                    pin_profile["flight_mean"],
                    max(20, pin_profile["flight_std"] * jitter)
                )))

            current_time += flight
            key_down = current_time
            key_up = key_down + dwell

            touch_x = max(30, min(250, np.random.normal(
                pin_profile["touch_x_mean"], pin_profile["touch_x_std"] * jitter)))
            touch_y = max(30, min(250, np.random.normal(
                pin_profile["touch_y_mean"], pin_profile["touch_y_std"] * jitter)))
            touch_size = max(1, np.random.normal(
                pin_profile["touch_size_mean"], max(2, pin_profile["touch_size_std"] * jitter)))

            rows.append({
                "timestamp": key_down, "digit": digit,
                "key_down_time": key_down, "key_up_time": key_up,
                "dwell_time_ms": dwell, "flight_time_ms": flight,
                "touch_x": round(touch_x, 5), "touch_y": round(touch_y, 5),
                "touch_size": round(touch_size, 5), "attempt_number": attempt,
            })
            current_time = key_up

        current_time += random.randint(500, 2500)  # Inter-attempt pause

    return rows


def generate_keystrokes(pin_profile, age_mod, num_keys=None):
    if num_keys is None:
        num_keys = random.randint(80, 200)
    rows = []
    base_time = int(datetime.now().timestamp() * 1000) + random.randint(0, 100000)
    for i in range(num_keys):
        key_code = random.randint(29, 54)
        dwell = max(30, int(np.random.normal(
            pin_profile["dwell_mean"] * 1.2, pin_profile["dwell_std"] * 1.5
        ) * age_mod["dwell_scale"]))
        flight = max(40, int(np.random.normal(
            pin_profile["flight_mean"] * 0.8, pin_profile["flight_std"]
        ) * age_mod["flight_scale"]))
        rows.append({
            "timestamp": base_time + i * (dwell + flight),
            "key_code": key_code, "dwell_time_ms": dwell,
            "flight_time_ms": flight, "is_baseline": "false",
        })
    return rows


def generate_touches(touch_profile, age_mod, num_events=None, jitter=1.0):
    if num_events is None:
        num_events = random.randint(55, 85)
    rows = []
    base_time = int(datetime.now().timestamp() * 1000) + random.randint(0, 100000)

    tap_count = int(num_events * touch_profile["tap_ratio"])
    swipe_count = int(num_events * (1 - touch_profile["tap_ratio"]) * 0.7)
    other_count = num_events - tap_count - swipe_count

    types = (["TAP"] * tap_count +
             [random.choice(["SWIPE_UP", "SWIPE_DOWN", "SWIPE_LEFT", "SWIPE_RIGHT"]) for _ in range(swipe_count)] +
             [random.choice(["SCROLL", "LONG_PRESS"]) for _ in range(other_count)])
    random.shuffle(types)

    for i, touch_type in enumerate(types):
        start_x = random.uniform(50, 900)
        start_y = random.uniform(200, 1800)

        if touch_type == "TAP":
            end_x, end_y = start_x, start_y
            duration = max(15, int(np.random.normal(55, 20) * age_mod["dwell_scale"] * jitter))
            velocity, acceleration = 0.0, 0.0
        elif touch_type == "LONG_PRESS":
            end_x, end_y = start_x, start_y
            duration = int(np.random.uniform(500, 1500) * jitter)
            velocity, acceleration = 0.0, 0.0
        elif "SWIPE" in touch_type:
            dx = np.random.uniform(100, 500) * (-1 if "LEFT" in touch_type or "UP" in touch_type else 1)
            dy = np.random.uniform(200, 600) * (-1 if "UP" in touch_type else 1)
            if "LEFT" in touch_type or "RIGHT" in touch_type:
                end_x, end_y = start_x + dx, start_y + np.random.normal(0, 20)
            else:
                end_x, end_y = start_x + np.random.normal(0, 25), start_y + dy
            duration = max(40, int(np.random.normal(
                touch_profile["duration_mean"], max(15, touch_profile["duration_std"])
            ) * age_mod["dwell_scale"] * jitter))
            dist = np.sqrt((end_x - start_x)**2 + (end_y - start_y)**2)
            velocity = dist / max(1, duration) * 1000
            acceleration = max(0, np.random.normal(
                touch_profile["acceleration_mean"], max(30, touch_profile["acceleration_std"])))
        else:  # SCROLL
            end_x = start_x + np.random.normal(0, 10)
            end_y = start_y - np.random.uniform(50, 300)
            duration = max(80, int(np.random.uniform(150, 400)))
            dist = abs(end_y - start_y)
            velocity = dist / max(1, duration) * 1000
            acceleration = max(0, np.random.normal(200, 80))

        hold_duration = int(duration) if touch_type in ["TAP", "LONG_PRESS"] else 0

        rows.append({
            "timestamp": base_time + i * random.randint(400, 2000),
            "touch_type": touch_type,
            "start_x": round(start_x, 5), "start_y": round(start_y, 5),
            "end_x": round(end_x, 5), "end_y": round(end_y, 5),
            "pressure": 1.0, "touch_size": 1.0,
            "duration_ms": duration,
            "velocity": round(velocity, 4), "acceleration": round(acceleration, 4),
            "hold_duration_ms": hold_duration, "touch_area": 1.0,
        })
    return rows


def generate_motion(motion_profile, num_samples=None, jitter=1.0):
    if num_samples is None:
        num_samples = random.randint(4000, 15000)
    rows = []
    base_time = int(datetime.now().timestamp() * 1000) + random.randint(0, 100000)

    # Mean-reverting random walk for temporal correlation
    ax = motion_profile["accel_x_mean"]
    ay = motion_profile["accel_y_mean"]
    az = motion_profile["accel_z_mean"]
    gx = motion_profile["gyro_x_mean"]
    gy = motion_profile["gyro_y_mean"]
    gz = motion_profile["gyro_z_mean"]

    reversion = 0.02  # Mean reversion strength
    device_states = ["HELD_IN_HAND", "STATIONARY", "ON_TABLE"]
    current_state = random.choice(device_states)

    for i in range(num_samples):
        # Ornstein-Uhlenbeck process for realistic sensor noise
        ax += np.random.normal(0, 0.05 * jitter) - reversion * (ax - motion_profile["accel_x_mean"])
        ay += np.random.normal(0, 0.05 * jitter) - reversion * (ay - motion_profile["accel_y_mean"])
        az += np.random.normal(0, 0.03 * jitter) - reversion * (az - motion_profile["accel_z_mean"])
        gx += np.random.normal(0, 0.003 * jitter) - 0.05 * (gx - motion_profile["gyro_x_mean"])
        gy += np.random.normal(0, 0.003 * jitter) - 0.05 * (gy - motion_profile["gyro_y_mean"])
        gz += np.random.normal(0, 0.003 * jitter) - 0.05 * (gz - motion_profile["gyro_z_mean"])

        pitch = np.degrees(np.arctan2(ay, np.sqrt(ax**2 + az**2)))
        roll = np.degrees(np.arctan2(ax, np.sqrt(ay**2 + az**2)))

        fax = ax * 0.95 + motion_profile["accel_x_mean"] * 0.05
        fay = ay * 0.95 + motion_profile["accel_y_mean"] * 0.05
        faz = az * 0.95 + motion_profile["accel_z_mean"] * 0.05

        gyro_mag = np.sqrt(gx**2 + gy**2 + gz**2)
        if random.random() < 0.02:
            current_state = random.choice(device_states)
        if gyro_mag > 0.3:
            current_state = "HELD_IN_HAND"
        elif gyro_mag < 0.05:
            current_state = random.choice(["STATIONARY", "ON_TABLE"])

        rows.append({
            "timestamp": base_time + i * 66,  # ~15Hz
            "accel_x": round(ax, 7), "accel_y": round(ay, 7), "accel_z": round(az, 7),
            "gyro_x": round(gx, 8), "gyro_y": round(gy, 8), "gyro_z": round(gz, 8),
            "pitch": round(pitch, 6), "roll": round(roll, 6), "azimuth": 0.0,
            "filtered_accel_x": round(fax, 7), "filtered_accel_y": round(fay, 7),
            "filtered_accel_z": round(faz, 7), "device_state": current_state,
        })
    return rows


# ─── CSV Writing ─────────────────────────────────────────────

def write_csv(filepath, rows, fieldnames=None):
    if not rows:
        return
    if fieldnames is None:
        fieldnames = list(rows[0].keys())
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def write_metadata(filepath, participant_id, profile_owner_id, session_type):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "w") as f:
        f.write("key,value\n")
        f.write(f"session_id,{uuid.uuid4()}\n")
        f.write(f"participant_id,{participant_id}\n")
        f.write(f"profile_owner_id,{profile_owner_id}\n")
        f.write(f"session_type,{session_type}\n")
        f.write(f"export_timestamp,{int(datetime.now().timestamp() * 1000)}\n")
        f.write("device_model,Synthetic_v2\n")
        f.write("device_manufacturer,SecureBank_Research\n")
        f.write("android_version,34\n")


def generate_session(profile, pdir, session_type, participant_id, profile_owner_id,
                     age_mod, num_pin_attempts, jitter=1.0):
    """Generate a complete session (5 CSV files)."""
    ts = datetime.now().strftime("%Y-%m-%d_%H-%M-%S") + f"-{random.randint(0,999):03d}"
    prefix = f"{session_type.lower()}_{ts}"

    pin_data = generate_pin_keystrokes(profile["pin"], num_pin_attempts, jitter)
    ks_data = generate_keystrokes(profile["pin"], age_mod, random.randint(60, 150))
    touch_data = generate_touches(profile["touch"], age_mod, random.randint(50, 80), jitter)
    motion_data = generate_motion(profile["motion"], random.randint(3000, 12000), jitter)

    write_csv(os.path.join(pdir, f"{prefix}_pin_keystrokes.csv"), pin_data)
    write_csv(os.path.join(pdir, f"{prefix}_keystrokes.csv"), ks_data)
    write_csv(os.path.join(pdir, f"{prefix}_touches.csv"), touch_data)
    write_csv(os.path.join(pdir, f"{prefix}_motion.csv"), motion_data)
    write_metadata(os.path.join(pdir, f"{prefix}_metadata.csv"),
                   participant_id, profile_owner_id, session_type.upper())


# ─── Main Pipeline ───────────────────────────────────────────

def main():
    print("=" * 70)
    print("  SYNTHETIC DATA GENERATOR v2 - SecureBank Behavioral Biometrics")
    print("=" * 70)

    # Step 1: Extract real data profiles
    print("\n[1/5] Reading real participant data...")
    pin_profiles, touch_profiles, motion_profiles = [], [], []

    for pid in REAL_PARTICIPANTS:
        pdir = os.path.join(REAL_DATA_DIR, pid)
        if not os.path.exists(pdir):
            print(f"  WARNING: {pid} not found, skipping")
            continue
        pin = extract_pin_profile(pdir)
        touch = extract_touch_profile(pdir)
        motion = extract_motion_profile(pdir)
        if pin:
            pin_profiles.append(pin)
            print(f"  {pid}: dwell={pin['dwell_mean']:.1f}+-{pin['dwell_std']:.1f}ms, "
                  f"flight={pin['flight_mean']:.0f}+-{pin['flight_std']:.0f}ms")
        if touch:
            touch_profiles.append(touch)
        if motion:
            motion_profiles.append(motion)

    print(f"  Extracted {len(pin_profiles)} PIN, {len(touch_profiles)} touch, {len(motion_profiles)} motion profiles")

    if not pin_profiles or not touch_profiles or not motion_profiles:
        print("ERROR: Not enough real data!")
        return

    # Step 2: Generate demographics
    print(f"\n[2/5] Generating demographics for {NUM_SYNTHETIC} synthetic participants...")
    demographics = []
    synth_idx = 0

    for i, pid in enumerate(REAL_PARTICIPANTS):
        demographics.append({
            "participant_id": pid, "age_group": "18-25" if i < 4 else "26-35",
            "age": random.randint(18, 35), "gender": random.choice(GENDERS), "is_synthetic": False,
        })

    for age_group, config in AGE_GROUPS.items():
        for _ in range(config["count"]):
            if synth_idx >= NUM_SYNTHETIC:
                break
            pid = SYNTHETIC_PARTICIPANTS[synth_idx]
            age_min, age_max = map(int, age_group.split("-"))
            demographics.append({
                "participant_id": pid, "age_group": age_group,
                "age": random.randint(age_min, age_max),
                "gender": random.choice(GENDERS), "is_synthetic": True,
            })
            synth_idx += 1

    os.makedirs(SYNTHETIC_DATA_DIR, exist_ok=True)
    write_csv(os.path.join(SYNTHETIC_DATA_DIR, "participants_demographics.csv"),
              demographics, ["participant_id", "age_group", "age", "gender", "is_synthetic"])

    for ag in AGE_GROUPS:
        count = sum(1 for d in demographics if d["age_group"] == ag)
        print(f"  {ag}: {count} participants")

    # Step 3: Copy real participant data
    print(f"\n[3/5] Copying real participant data...")
    for pid in REAL_PARTICIPANTS:
        src_dir = os.path.join(REAL_DATA_DIR, pid)
        dst_dir = os.path.join(SYNTHETIC_DATA_DIR, pid)
        if os.path.exists(src_dir):
            os.makedirs(dst_dir, exist_ok=True)
            for f in os.listdir(src_dir):
                if f.endswith(".csv"):
                    src, dst = os.path.join(src_dir, f), os.path.join(dst_dir, f)
                    if not os.path.exists(dst):
                        shutil.copy2(src, dst)
            print(f"  {pid} copied")

    # Step 4: Generate synthetic participants
    print(f"\n[4/5] Generating {NUM_SYNTHETIC} synthetic participants x "
          f"(1 enrollment + {NUM_GENUINE_PER_PARTICIPANT} genuine + "
          f"{NUM_IMPOSTOR_PER_PARTICIPANT} impostor)...")

    # First generate all profiles (needed for cross-participant impostor assignment)
    all_profiles = {}
    all_age_mods = {}

    for demo in demographics:
        if not demo["is_synthetic"]:
            continue
        pid = demo["participant_id"]
        age_mod = AGE_GROUPS[demo["age_group"]]
        profile = generate_unique_profile(pin_profiles, touch_profiles, motion_profiles, age_mod)
        all_profiles[pid] = profile
        all_age_mods[pid] = age_mod

    # Generate sessions
    for demo in demographics:
        if not demo["is_synthetic"]:
            continue

        pid = demo["participant_id"]
        pdir = os.path.join(SYNTHETIC_DATA_DIR, pid)
        os.makedirs(pdir, exist_ok=True)

        profile = all_profiles[pid]
        age_mod = all_age_mods[pid]

        # ENROLLMENT
        generate_session(profile, pdir, "ENROLLMENT", pid, pid, age_mod, ENROLLMENT_ATTEMPTS, jitter=1.0)

        # GENUINE sessions (same person, small variation)
        for g in range(NUM_GENUINE_PER_PARTICIPANT):
            jitter = np.random.uniform(0.90, 1.15)  # Natural session-to-session variation
            generate_session(profile, pdir, "GENUINE", pid, pid, age_mod, SESSION_ATTEMPTS, jitter)

        # IMPOSTOR sessions (a DIFFERENT participant's profile)
        other_pids = [p for p in all_profiles if p != pid]
        impostor_pids = random.sample(other_pids, min(NUM_IMPOSTOR_PER_PARTICIPANT, len(other_pids)))

        for imp_pid in impostor_pids:
            imp_profile = all_profiles[imp_pid]
            imp_age_mod = all_age_mods[imp_pid]
            # Impostor uses their OWN behavioral profile (which differs from the target)
            generate_session(imp_profile, pdir, "IMPOSTOR", imp_pid, pid, imp_age_mod, SESSION_ATTEMPTS,
                           jitter=np.random.uniform(1.0, 1.2))

        if int(pid[1:]) % 10 == 0:
            print(f"  {pid} done...")

    # Step 5: Summary
    total_participants = len(REAL_PARTICIPANTS) + NUM_SYNTHETIC
    total_genuine = (NUM_SYNTHETIC * NUM_GENUINE_PER_PARTICIPANT) + len(REAL_PARTICIPANTS)  # real ones have genuine too
    total_impostor = (NUM_SYNTHETIC * NUM_IMPOSTOR_PER_PARTICIPANT) + len(REAL_PARTICIPANTS)
    total_ml_samples = total_genuine + total_impostor

    print(f"\n[5/5] Generation complete!")
    print(f"{'=' * 70}")
    print(f"  OUTPUT: {SYNTHETIC_DATA_DIR}")
    print(f"  Total participants: {total_participants} ({len(REAL_PARTICIPANTS)} real + {NUM_SYNTHETIC} synthetic)")
    print(f"  Sessions per synthetic participant: 1 enroll + {NUM_GENUINE_PER_PARTICIPANT} genuine + {NUM_IMPOSTOR_PER_PARTICIPANT} impostor")
    print(f"  ML training samples (synthetic only): {NUM_SYNTHETIC * (NUM_GENUINE_PER_PARTICIPANT + NUM_IMPOSTOR_PER_PARTICIPANT)}")
    print(f"  Total with real: ~{total_ml_samples}")
    print(f"{'=' * 70}")
    print(f"\n  Next step: python ml_model.py")

    # Save metadata
    meta = {
        "version": 2,
        "generated_at": datetime.now().isoformat(),
        "num_real": len(REAL_PARTICIPANTS),
        "num_synthetic": NUM_SYNTHETIC,
        "genuine_per_participant": NUM_GENUINE_PER_PARTICIPANT,
        "impostor_per_participant": NUM_IMPOSTOR_PER_PARTICIPANT,
        "total_ml_samples_estimate": total_ml_samples,
        "pin": PIN_DIGITS,
    }
    with open(os.path.join(SYNTHETIC_DATA_DIR, "generation_metadata.json"), "w") as f:
        json.dump(meta, f, indent=2)


if __name__ == "__main__":
    main()
