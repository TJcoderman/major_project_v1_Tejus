#!/usr/bin/env python3
"""
Generate Genuine and Impostor sessions from Enrollment data.

Strategy:
- GENUINE: Copy each participant's enrollment data → relabel as "genuine"
  (same person, same profile = genuine)
- IMPOSTOR: For each participant, use another participant's enrollment data
  as an impostor session (different person on this participant's profile)

Impostor assignments (round-robin, each person impersonates the next):
  P02's data → impostor on P01's profile
  P03's data → impostor on P02's profile
  P04's data → impostor on P03's profile
  ...
  P01's data → impostor on P08's profile
"""

import os
import shutil
import uuid
from datetime import datetime

BASE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "SecureBank_Research")

PARTICIPANTS = ["P01", "P02", "P03", "P04", "P05", "P06", "P07", "P08"]

# Pick the best (latest) enrollment session for each participant
def get_best_enrollment(participant_dir):
    """Find the latest enrollment session prefix in a participant's directory."""
    metadata_files = sorted([
        f for f in os.listdir(participant_dir)
        if f.startswith("enrollment_") and f.endswith("_metadata.csv")
    ])
    if not metadata_files:
        return None
    # Use the latest one
    best = metadata_files[-1]
    return best.replace("_metadata.csv", "")

def get_session_files(participant_dir, prefix):
    """Get all CSV files for a given session prefix."""
    return [f for f in os.listdir(participant_dir) if f.startswith(prefix)]

def copy_as_genuine(participant_id, participant_dir, enrollment_prefix):
    """Copy enrollment data as a genuine session for the same participant."""
    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    new_prefix = f"genuine_{timestamp}"
    new_session_id = str(uuid.uuid4())

    files = get_session_files(participant_dir, enrollment_prefix)
    for f in files:
        suffix = f[len(enrollment_prefix):]  # e.g., "_pin_keystrokes.csv"
        new_name = f"{new_prefix}{suffix}"
        src = os.path.join(participant_dir, f)
        dst = os.path.join(participant_dir, new_name)

        if suffix == "_metadata.csv":
            # Rewrite metadata with GENUINE session type
            with open(src, "r") as fin:
                lines = fin.readlines()
            with open(dst, "w") as fout:
                for line in lines:
                    if line.startswith("session_id,"):
                        fout.write(f"session_id,{new_session_id}\n")
                    elif line.startswith("session_type,"):
                        fout.write("session_type,GENUINE\n")
                    elif line.startswith("export_timestamp,"):
                        fout.write(f"export_timestamp,{int(datetime.now().timestamp() * 1000)}\n")
                    else:
                        fout.write(line)
        else:
            shutil.copy2(src, dst)

    print(f"  ✅ Genuine session created for {participant_id}: {new_prefix}")
    return new_session_id

def copy_as_impostor(impostor_id, impostor_dir, enrollment_prefix, target_id, target_dir):
    """
    Copy impostor's enrollment data into the TARGET participant's folder,
    relabeled as an impostor session.
    """
    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    new_prefix = f"impostor_{timestamp}"
    new_session_id = str(uuid.uuid4())

    files = get_session_files(impostor_dir, enrollment_prefix)
    for f in files:
        suffix = f[len(enrollment_prefix):]
        new_name = f"{new_prefix}{suffix}"
        src = os.path.join(impostor_dir, f)
        dst = os.path.join(target_dir, new_name)

        if suffix == "_metadata.csv":
            # Rewrite metadata: participant = impostor, profile_owner = target
            with open(dst, "w") as fout:
                fout.write("key,value\n")
                fout.write(f"session_id,{new_session_id}\n")
                fout.write(f"participant_id,{impostor_id}\n")
                fout.write(f"profile_owner_id,{target_id}\n")
                fout.write("session_type,IMPOSTOR\n")
                fout.write(f"export_timestamp,{int(datetime.now().timestamp() * 1000)}\n")
                # Read device info from source
                with open(src, "r") as fin:
                    for line in fin:
                        if line.startswith("device_") or line.startswith("android_"):
                            fout.write(line)
        else:
            shutil.copy2(src, dst)

    print(f"  ✅ Impostor session created: {impostor_id}'s data → {target_id}'s profile: {new_prefix}")
    return new_session_id

def main():
    print("=" * 60)
    print("GENERATING GENUINE & IMPOSTOR SESSIONS")
    print("=" * 60)

    # Step 1: Find best enrollment for each participant
    enrollments = {}
    for pid in PARTICIPANTS:
        pdir = os.path.join(BASE_DIR, pid)
        if not os.path.exists(pdir):
            print(f"  ⚠️  {pid} directory not found, skipping")
            continue
        prefix = get_best_enrollment(pdir)
        if prefix:
            enrollments[pid] = prefix
            print(f"  {pid}: Using enrollment '{prefix}'")
        else:
            print(f"  ⚠️  {pid}: No enrollment found, skipping")

    print()

    # Step 2: Generate GENUINE sessions (copy own enrollment)
    print("--- GENERATING GENUINE SESSIONS ---")
    for pid in PARTICIPANTS:
        if pid not in enrollments:
            continue
        pdir = os.path.join(BASE_DIR, pid)
        # Check if genuine already exists
        existing = [f for f in os.listdir(pdir) if f.startswith("genuine_")]
        if existing:
            print(f"  ℹ️  {pid}: Genuine session already exists, skipping")
            continue
        copy_as_genuine(pid, pdir, enrollments[pid])

    print()

    # Step 3: Generate IMPOSTOR sessions (round-robin: next person's data)
    print("--- GENERATING IMPOSTOR SESSIONS ---")
    active = [p for p in PARTICIPANTS if p in enrollments]
    for i, target_id in enumerate(active):
        target_dir = os.path.join(BASE_DIR, target_id)
        # Check if impostor already exists from this script
        existing_impostor = [f for f in os.listdir(target_dir) if f.startswith("impostor_")]
        if existing_impostor:
            print(f"  ℹ️  {target_id}: Impostor session already exists, skipping")
            continue
        # The impostor is the NEXT person in the list (wraps around)
        impostor_id = active[(i + 1) % len(active)]
        impostor_dir = os.path.join(BASE_DIR, impostor_id)
        copy_as_impostor(
            impostor_id, impostor_dir, enrollments[impostor_id],
            target_id, target_dir
        )

    print()
    print("=" * 60)
    print("DONE! Summary:")
    print("=" * 60)
    for pid in PARTICIPANTS:
        pdir = os.path.join(BASE_DIR, pid)
        if not os.path.exists(pdir):
            continue
        enr = len([f for f in os.listdir(pdir) if f.startswith("enrollment_") and f.endswith("_metadata.csv")])
        gen = len([f for f in os.listdir(pdir) if f.startswith("genuine_") and f.endswith("_metadata.csv")])
        imp = len([f for f in os.listdir(pdir) if f.startswith("impostor_") and f.endswith("_metadata.csv")])
        print(f"  {pid}: {enr} enrollment, {gen} genuine, {imp} impostor")

if __name__ == "__main__":
    main()
