#!/usr/bin/env python3
"""
Research Visualization Generator
===================================
Creates 8 publication-quality figures for the behavioral
authentication research paper.
"""

import os
import json
import warnings
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
import seaborn as sns
from sklearn.decomposition import PCA
from sklearn.manifold import TSNE
from scipy import stats

warnings.filterwarnings("ignore")

# ─── Configuration ──────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RESULTS_DIR = os.path.join(SCRIPT_DIR, "results")
FIGURES_DIR = os.path.join(SCRIPT_DIR, "figures")
SYNTHETIC_DIR = os.path.join(SCRIPT_DIR, "synthetic_data")

# Publication style
plt.rcParams.update({
    "figure.dpi": 300,
    "savefig.dpi": 300,
    "font.size": 11,
    "font.family": "serif",
    "axes.titlesize": 13,
    "axes.labelsize": 12,
    "xtick.labelsize": 10,
    "ytick.labelsize": 10,
    "legend.fontsize": 10,
    "figure.titlesize": 14,
    "axes.grid": True,
    "grid.alpha": 0.3,
})

# Color palette
COLORS = {
    "rf": "#2196F3",
    "mlp": "#FF5722",
    "svm": "#4CAF50",
    "genuine": "#2196F3",
    "impostor": "#F44336",
    "primary": "#1565C0",
    "secondary": "#E65100",
    "accent": "#2E7D32",
}

AGE_COLORS = {
    "18-25": "#42A5F5",
    "26-35": "#66BB6A",
    "36-45": "#FFA726",
    "46-55": "#EF5350",
}


def load_data():
    """Load all necessary data for visualizations."""
    # Load ML results
    with open(os.path.join(RESULTS_DIR, "detailed_results.json"), "r") as f:
        results = json.load(f)
    
    # Load feature dataset
    df = pd.read_csv(os.path.join(RESULTS_DIR, "feature_dataset.csv"))
    
    # Load demographics
    demo_path = os.path.join(SYNTHETIC_DIR, "participants_demographics.csv")
    demographics = pd.read_csv(demo_path) if os.path.exists(demo_path) else None
    
    return results, df, demographics


# ─── Figure 1: Feature Distribution (Violin Plot) ──────────

def fig1_feature_distributions(df):
    """Violin plots of key behavioral features: genuine vs impostor."""
    fig, axes = plt.subplots(2, 3, figsize=(14, 9))
    fig.suptitle("Behavioral Feature Distributions: Genuine vs Impostor", fontweight="bold", y=0.98)
    
    features = [
        ("pin_dwell_mean", "PIN Dwell Time (ms)"),
        ("pin_flight_mean", "PIN Flight Time (ms)"),
        ("pin_touch_x_std", "Touch X Precision (σ)"),
        ("touch_velocity_mean", "Swipe Velocity (px/s)"),
        ("motion_gyro_mag_mean", "Gyroscope Magnitude"),
        ("pin_entry_time_mean", "Total PIN Entry Time (ms)"),
    ]
    
    for idx, (feat, label) in enumerate(features):
        ax = axes[idx // 3][idx % 3]
        if feat not in df.columns:
            ax.text(0.5, 0.5, "N/A", ha="center", va="center", transform=ax.transAxes)
            continue
        
        plot_df = df[[feat, "is_genuine"]].copy()
        plot_df["Type"] = plot_df["is_genuine"].map({1: "Genuine", 0: "Impostor"})
        
        parts = ax.violinplot(
            [plot_df[plot_df["Type"] == "Genuine"][feat].dropna().values,
             plot_df[plot_df["Type"] == "Impostor"][feat].dropna().values],
            positions=[1, 2],
            showmeans=True,
            showmedians=True,
        )
        
        for i, pc in enumerate(parts["bodies"]):
            pc.set_facecolor(COLORS["genuine"] if i == 0 else COLORS["impostor"])
            pc.set_alpha(0.7)
        
        ax.set_xticks([1, 2])
        ax.set_xticklabels(["Genuine", "Impostor"])
        ax.set_ylabel(label)
        ax.set_title(label, fontsize=11)
    
    plt.tight_layout(rect=[0, 0, 1, 0.95])
    save_fig(fig, "fig1_feature_distributions")


# ─── Figure 2: t-SNE Visualization ─────────────────────────

def fig2_tsne_visualization(df):
    """t-SNE 2D embedding of behavioral profiles showing cluster separation."""
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 6))
    fig.suptitle("Behavioral Profile Clustering (t-SNE)", fontweight="bold")
    
    feature_cols = [c for c in df.columns if c not in ["participant_id", "session_type", "is_genuine"]]
    X = df[feature_cols].fillna(0).values
    y = df["is_genuine"].values
    pids = df["participant_id"].values
    
    # t-SNE
    tsne = TSNE(n_components=2, random_state=42, perplexity=min(30, len(X) - 1))
    X_tsne = tsne.fit_transform(X)
    
    # Plot 1: Genuine vs Impostor
    for label, color, name in [(1, COLORS["genuine"], "Genuine"), (0, COLORS["impostor"], "Impostor")]:
        mask = y == label
        ax1.scatter(X_tsne[mask, 0], X_tsne[mask, 1], c=color, label=name,
                   alpha=0.7, s=60, edgecolors="white", linewidth=0.5)
    ax1.set_title("Genuine vs Impostor Sessions")
    ax1.set_xlabel("t-SNE Dimension 1")
    ax1.set_ylabel("t-SNE Dimension 2")
    ax1.legend()
    
    # Plot 2: Per-participant clustering
    unique_pids = sorted(set(pids))
    cmap = plt.cm.get_cmap("tab20", len(unique_pids))
    for i, pid in enumerate(unique_pids[:20]):  # Show max 20 for readability
        mask = pids == pid
        ax2.scatter(X_tsne[mask, 0], X_tsne[mask, 1], c=[cmap(i)], label=pid,
                   alpha=0.8, s=60, edgecolors="white", linewidth=0.5)
    ax2.set_title("Per-Participant Clustering")
    ax2.set_xlabel("t-SNE Dimension 1")
    ax2.set_ylabel("t-SNE Dimension 2")
    if len(unique_pids) <= 20:
        ax2.legend(fontsize=7, ncol=2, loc="best")
    
    plt.tight_layout()
    save_fig(fig, "fig2_tsne_clustering")


# ─── Figure 3: ROC Curves ──────────────────────────────────

def fig3_roc_curves(results):
    """ROC curves for all 3 models overlaid."""
    fig, ax = plt.subplots(figsize=(8, 7))
    
    models = [
        ("random_forest", "Random Forest", COLORS["rf"], "-"),
        ("mlp", "MLP Neural Network", COLORS["mlp"], "--"),
        ("one_class_svm", "One-Class SVM", COLORS["svm"], "-."),
    ]
    
    for key, name, color, style in models:
        fpr = results[key]["fpr"]
        tpr = results[key]["tpr"]
        auc_val = results[key]["roc_auc"]
        ax.plot(fpr, tpr, color=color, linestyle=style, linewidth=2.5,
               label=f"{name} (AUC = {auc_val:.4f})")
    
    ax.plot([0, 1], [0, 1], "k--", alpha=0.5, linewidth=1, label="Random Baseline")
    
    ax.set_xlim([0, 1])
    ax.set_ylim([0, 1.02])
    ax.set_xlabel("False Positive Rate (FAR)")
    ax.set_ylabel("True Positive Rate (1 - FRR)")
    ax.set_title("ROC Curves — Behavioral Authentication Models", fontweight="bold")
    ax.legend(loc="lower right", framealpha=0.9)
    
    plt.tight_layout()
    save_fig(fig, "fig3_roc_curves")


# ─── Figure 4: Confusion Matrix ────────────────────────────

def fig4_confusion_matrix(results):
    """Confusion matrix heatmap for the best model."""
    fig, axes = plt.subplots(1, 3, figsize=(16, 5))
    fig.suptitle("Confusion Matrices", fontweight="bold")
    
    models = [
        ("random_forest", "Random Forest"),
        ("mlp", "MLP Neural Network"),
        ("one_class_svm", "One-Class SVM"),
    ]
    
    for idx, (key, name) in enumerate(models):
        cm = np.array(results[key]["confusion_matrix"])
        
        # Normalize
        cm_norm = cm.astype("float") / cm.sum(axis=1)[:, np.newaxis]
        
        sns.heatmap(cm, annot=True, fmt="d", cmap="Blues", ax=axes[idx],
                   xticklabels=["Impostor", "Genuine"],
                   yticklabels=["Impostor", "Genuine"],
                   cbar_kws={"shrink": 0.8})
        
        # Add percentage annotations
        for i in range(2):
            for j in range(2):
                axes[idx].text(j + 0.5, i + 0.7, f"({cm_norm[i][j]:.1%})",
                              ha="center", va="center", fontsize=9, color="gray")
        
        axes[idx].set_title(f"{name}\nAcc: {results[key]['accuracy']:.2%}", fontsize=11)
        axes[idx].set_ylabel("Actual")
        axes[idx].set_xlabel("Predicted")
    
    plt.tight_layout()
    save_fig(fig, "fig4_confusion_matrices")


# ─── Figure 5: FAR vs FRR with EER ─────────────────────────

def fig5_far_frr_eer(results):
    """FAR vs FRR curve with EER point highlighted."""
    fig, ax = plt.subplots(figsize=(9, 7))
    
    # Use best model (Random Forest)
    rf = results["random_forest"]
    far = np.array(rf["far"])
    frr = np.array(rf["frr"])
    thresholds = np.array(rf["fpr"])  # threshold proxy
    
    # Create evenly spaced threshold axis
    x_axis = np.linspace(0, 1, len(far))
    
    ax.plot(x_axis, far, color=COLORS["impostor"], linewidth=2.5, label="FAR (False Accept Rate)")
    ax.plot(x_axis, frr, color=COLORS["genuine"], linewidth=2.5, label="FRR (False Reject Rate)")
    
    # Find and mark EER
    eer_idx = np.nanargmin(np.abs(np.array(far) - np.array(frr)))
    eer = (far[eer_idx] + frr[eer_idx]) / 2
    
    ax.plot(x_axis[eer_idx], eer, "ko", markersize=12, zorder=5)
    ax.annotate(f"EER = {eer:.4f}", xy=(x_axis[eer_idx], eer),
               xytext=(x_axis[eer_idx] + 0.1, eer + 0.1),
               fontsize=12, fontweight="bold",
               arrowprops=dict(arrowstyle="->", color="black", lw=2),
               bbox=dict(boxstyle="round,pad=0.3", facecolor="yellow", alpha=0.8))
    
    ax.axhline(y=eer, color="gray", linestyle=":", alpha=0.5)
    
    ax.set_xlabel("Threshold")
    ax.set_ylabel("Error Rate")
    ax.set_title("FAR vs FRR — Random Forest (EER Analysis)", fontweight="bold")
    ax.set_xlim([0, 1])
    ax.set_ylim([0, 1])
    ax.legend(loc="upper center", framealpha=0.9)
    
    plt.tight_layout()
    save_fig(fig, "fig5_far_frr_eer")


# ─── Figure 6: Age Group Analysis ──────────────────────────

def fig6_age_group_analysis(df, demographics):
    """Bar charts comparing behavioral patterns across age demographics."""
    if demographics is None:
        print("  ⚠️  No demographics data, skipping age analysis")
        return
    
    merged = df.merge(demographics[["participant_id", "age_group", "age"]],
                     on="participant_id", how="left")
    
    if merged["age_group"].isna().all():
        print("  ⚠️  No age data found after merge, skipping")
        return
    
    fig, axes = plt.subplots(2, 2, figsize=(12, 10))
    fig.suptitle("Behavioral Biometrics Across Age Groups", fontweight="bold", y=0.98)
    
    features = [
        ("pin_dwell_mean", "Avg PIN Dwell Time (ms)", axes[0, 0]),
        ("pin_flight_mean", "Avg PIN Flight Time (ms)", axes[0, 1]),
        ("touch_velocity_mean", "Avg Swipe Velocity (px/s)", axes[1, 0]),
        ("pin_entry_time_mean", "Avg PIN Entry Time (ms)", axes[1, 1]),
    ]
    
    age_order = ["18-25", "26-35", "36-45", "46-55"]
    
    for feat, label, ax in features:
        if feat not in merged.columns:
            ax.text(0.5, 0.5, "N/A", ha="center", va="center", transform=ax.transAxes)
            continue
        
        grouped = merged.groupby("age_group")[feat].agg(["mean", "std"]).reindex(age_order).dropna()
        
        bars = ax.bar(range(len(grouped)), grouped["mean"],
                     yerr=grouped["std"], capsize=5,
                     color=[AGE_COLORS.get(ag, "#999") for ag in grouped.index],
                     edgecolor="white", linewidth=0.8, alpha=0.85)
        
        ax.set_xticks(range(len(grouped)))
        ax.set_xticklabels(grouped.index)
        ax.set_ylabel(label)
        ax.set_title(label, fontsize=11)
        
        # Add value labels
        for bar, val in zip(bars, grouped["mean"]):
            ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 3,
                   f"{val:.1f}", ha="center", va="bottom", fontsize=9)
    
    plt.tight_layout(rect=[0, 0, 1, 0.95])
    save_fig(fig, "fig6_age_group_analysis")


# ─── Figure 7: Feature Importance ──────────────────────────

def fig7_feature_importance(results):
    """Random Forest feature importance ranking."""
    fi = results.get("feature_importance", [])
    if not fi:
        print("  ⚠️  No feature importance data, skipping")
        return
    
    # Top 20 features
    top_n = min(20, len(fi))
    features = [f[0] for f in fi[:top_n]]
    importances = [f[1] for f in fi[:top_n]]
    
    fig, ax = plt.subplots(figsize=(10, 8))
    
    # Color by feature category
    colors = []
    for f in features:
        if f.startswith("pin_"):
            colors.append("#2196F3")
        elif f.startswith("touch_"):
            colors.append("#FF9800")
        elif f.startswith("motion_"):
            colors.append("#4CAF50")
        else:
            colors.append("#9E9E9E")
    
    y_pos = np.arange(top_n)
    bars = ax.barh(y_pos, importances, color=colors, edgecolor="white", linewidth=0.5, alpha=0.85)
    
    ax.set_yticks(y_pos)
    ax.set_yticklabels([f.replace("_", " ").title() for f in features], fontsize=9)
    ax.invert_yaxis()
    ax.set_xlabel("Feature Importance (Gini)")
    ax.set_title("Top 20 Feature Importance — Random Forest", fontweight="bold")
    
    # Legend for categories
    from matplotlib.patches import Patch
    legend_elements = [
        Patch(facecolor="#2196F3", label="PIN Keystroke"),
        Patch(facecolor="#FF9800", label="Touch Interaction"),
        Patch(facecolor="#4CAF50", label="Motion/Sensor"),
    ]
    ax.legend(handles=legend_elements, loc="lower right", framealpha=0.9)
    
    plt.tight_layout()
    save_fig(fig, "fig7_feature_importance")


# ─── Figure 8: Model Performance Comparison ────────────────

def fig8_model_comparison(results):
    """Grouped bar chart comparing all models across metrics."""
    fig, ax = plt.subplots(figsize=(12, 6))
    
    metrics = ["accuracy", "precision", "recall", "f1_score", "roc_auc"]
    metric_labels = ["Accuracy", "Precision", "Recall", "F1 Score", "AUC-ROC"]
    
    models = [
        ("random_forest", "Random Forest", COLORS["rf"]),
        ("mlp", "MLP Neural Network", COLORS["mlp"]),
        ("one_class_svm", "One-Class SVM", COLORS["svm"]),
    ]
    
    x = np.arange(len(metrics))
    width = 0.25
    
    for i, (key, name, color) in enumerate(models):
        values = [results[key][m] for m in metrics]
        bars = ax.bar(x + i * width, values, width, label=name, color=color,
                     edgecolor="white", linewidth=0.5, alpha=0.85)
        
        # Value labels
        for bar, val in zip(bars, values):
            ax.text(bar.get_x() + bar.get_width() / 2, bar.get_height() + 0.01,
                   f"{val:.3f}", ha="center", va="bottom", fontsize=8, fontweight="bold")
    
    ax.set_xticks(x + width)
    ax.set_xticklabels(metric_labels)
    ax.set_ylabel("Score")
    ax.set_title("Model Performance Comparison — Behavioral Authentication", fontweight="bold")
    ax.set_ylim([0, 1.15])
    ax.legend(loc="upper right", framealpha=0.9)
    
    # Add EER text box
    eer_text = "\n".join([
        f"{results[k]['model_name']}: EER={results[k]['eer']:.4f}"
        for k in ["random_forest", "mlp", "one_class_svm"]
    ])
    ax.text(0.02, 0.98, f"Equal Error Rate (EER):\n{eer_text}",
           transform=ax.transAxes, fontsize=9, verticalalignment="top",
           bbox=dict(boxstyle="round", facecolor="lightyellow", alpha=0.8))
    
    plt.tight_layout()
    save_fig(fig, "fig8_model_comparison")


# ─── Utility ────────────────────────────────────────────────

def save_fig(fig, name):
    """Save figure as PNG."""
    os.makedirs(FIGURES_DIR, exist_ok=True)
    filepath = os.path.join(FIGURES_DIR, f"{name}.png")
    fig.savefig(filepath, bbox_inches="tight", facecolor="white")
    plt.close(fig)
    print(f"  📊 Saved: {filepath}")


# ─── Main ───────────────────────────────────────────────────

def main():
    print("=" * 65)
    print("  VISUALIZATION GENERATOR — Research Paper Figures")
    print("=" * 65)
    
    results, df, demographics = load_data()
    
    print(f"\n  Dataset: {len(df)} samples, {len(df.columns) - 3} features")
    print(f"  Demographics: {'Available' if demographics is not None else 'Not available'}")
    print(f"\n  Generating 8 publication-quality figures...\n")
    
    # Generate all figures
    print("  [1/8] Feature Distributions...")
    fig1_feature_distributions(df)
    
    print("  [2/8] t-SNE Clustering...")
    fig2_tsne_visualization(df)
    
    print("  [3/8] ROC Curves...")
    fig3_roc_curves(results)
    
    print("  [4/8] Confusion Matrices...")
    fig4_confusion_matrix(results)
    
    print("  [5/8] FAR vs FRR (EER)...")
    fig5_far_frr_eer(results)
    
    print("  [6/8] Age Group Analysis...")
    fig6_age_group_analysis(df, demographics)
    
    print("  [7/8] Feature Importance...")
    fig7_feature_importance(results)
    
    print("  [8/8] Model Comparison...")
    fig8_model_comparison(results)
    
    print(f"\n{'=' * 65}")
    print(f"  ✅ ALL FIGURES GENERATED!")
    print(f"  Output directory: {FIGURES_DIR}")
    print(f"{'=' * 65}")


if __name__ == "__main__":
    main()
