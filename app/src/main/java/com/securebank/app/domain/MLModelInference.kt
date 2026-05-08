package com.securebank.app.domain

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.max

/**
 * ============================================
 * ML MODEL INFERENCE ENGINE
 * ============================================
 * Loads a trained MLP neural network from JSON (exported by ml_model.py)
 * and runs on-device inference to classify behavioral sessions as
 * genuine (1) or impostor (0).
 *
 * Architecture: 124 -> 128 -> 64 -> 32 -> 1 (sigmoid)
 * Activation: ReLU (hidden), Sigmoid (output)
 * Input: Enrollment-relative deviation features (standardized by scaler)
 *
 * No TensorFlow or ONNX dependency required - pure Kotlin matrix math.
 */
@Singleton
class MLModelInference @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MLModelInference"
    }

    private var model: ModelData? = null
    private var isLoaded = false

    data class ModelData(
        @SerializedName("model_type") val modelType: String,
        val activation: String,
        @SerializedName("output_activation") val outputActivation: String,
        @SerializedName("n_features") val nFeatures: Int,
        @SerializedName("feature_names") val featureNames: List<String>,
        val layers: List<LayerData>,
        val scaler: ScalerData,
        val threshold: Float,
        @SerializedName("training_accuracy") val trainingAccuracy: Float,
        @SerializedName("training_samples") val trainingSamples: Int
    )

    data class LayerData(
        @SerializedName("layer_index") val layerIndex: Int,
        @SerializedName("input_size") val inputSize: Int,
        @SerializedName("output_size") val outputSize: Int,
        val weights: List<List<Double>>,
        val biases: List<Double>
    )

    data class ScalerData(
        val means: List<Double>,
        val scales: List<Double>
    )

    /**
     * Load the model from assets/ml/behavioral_auth_model.json.
     * Call this once at app startup or first inference.
     */
    fun loadModel(): Boolean {
        if (isLoaded) return true

        return try {
            val json = context.assets.open("ml/behavioral_auth_model.json")
                .bufferedReader().use { it.readText() }
            val parsedModel = Gson().fromJson(json, ModelData::class.java)
            validateModel(parsedModel)
            model = parsedModel
            isLoaded = model != null
            if (isLoaded) {
                Log.d(TAG, "Loaded behavioral_auth_model.json with ${model?.nFeatures} features")
            }
            isLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load behavioral_auth_model.json", e)
            model = null
            isLoaded = false
            false
        }
    }

    private fun validateModel(modelData: ModelData?) {
        requireNotNull(modelData) { "Model JSON parsed to null" }
        require(modelData.nFeatures > 0) { "Model n_features must be positive" }
        require(modelData.featureNames.size == modelData.nFeatures) {
            "Feature name count ${modelData.featureNames.size} does not match n_features ${modelData.nFeatures}"
        }
        require(modelData.scaler.means.size == modelData.nFeatures) {
            "Scaler means count ${modelData.scaler.means.size} does not match n_features ${modelData.nFeatures}"
        }
        require(modelData.scaler.scales.size == modelData.nFeatures) {
            "Scaler scales count ${modelData.scaler.scales.size} does not match n_features ${modelData.nFeatures}"
        }
        require(modelData.layers.isNotEmpty()) { "Model has no layers" }
    }

    /**
     * Run inference on a feature vector.
     *
     * @param features Raw deviation features (NOT scaled). Length must match model.nFeatures.
     * @return Probability that the session is genuine (0.0 = impostor, 1.0 = genuine).
     *         Returns -1.0 if model is not loaded or input is invalid.
     */
    fun predict(features: FloatArray): Float {
        val m = model ?: run {
            if (!loadModel()) return -1f
            model!!
        }

        if (features.size != m.nFeatures) return -1f

        // Step 1: Standardize using scaler (z-score normalization)
        val scaled = FloatArray(features.size)
        for (i in features.indices) {
            val scale = m.scaler.scales[i].toFloat()
            scaled[i] = if (scale > 1e-8f) {
                (features[i] - m.scaler.means[i].toFloat()) / scale
            } else {
                0f
            }
        }

        // Step 2: Forward pass through each layer
        var current = scaled
        for ((idx, layer) in m.layers.withIndex()) {
            val output = FloatArray(layer.outputSize)

            // Matrix multiply: output = weights^T * input + bias
            for (j in 0 until layer.outputSize) {
                var sum = layer.biases[j].toFloat()
                for (i in 0 until layer.inputSize) {
                    sum += current[i] * layer.weights[i][j].toFloat()
                }
                output[j] = sum
            }

            // Apply activation
            val isLastLayer = idx == m.layers.size - 1
            if (isLastLayer) {
                // Sigmoid for output layer
                for (j in output.indices) {
                    output[j] = sigmoid(output[j])
                }
            } else {
                // ReLU for hidden layers
                for (j in output.indices) {
                    output[j] = max(0f, output[j])
                }
            }

            current = output
        }

        return current[0]
    }

    /**
     * Classify a session as genuine or impostor.
     *
     * @return Pair(isGenuine, confidence), or null if model is not loaded / prediction failed.
     *         Returning null instead of fail-open prevents treating errors as "genuine".
     */
    fun classify(features: FloatArray): Pair<Boolean, Float>? {
        val score = predict(features)
        if (score < 0f) return null // Model not loaded or input invalid — unavailable, not "genuine"
        val isGenuine = score >= (model?.threshold ?: 0.5f)
        val confidence = if (isGenuine) score else (1f - score)
        return Pair(isGenuine, confidence)
    }

    /**
     * Get the number of features the model expects.
     */
    fun getExpectedFeatureCount(): Int = model?.nFeatures ?: 0

    /**
     * Get the ordered list of feature names the model expects.
     * Used by MLFeatureExtractor to produce correctly-ordered feature arrays.
     */
    fun getFeatureNames(): List<String> = model?.featureNames ?: emptyList()

    /**
     * Check if the model is loaded and ready.
     */
    fun isReady(): Boolean = isLoaded && model != null

    private fun sigmoid(x: Float): Float {
        return (1.0f / (1.0f + exp(-x.toDouble()))).toFloat()
    }
}
