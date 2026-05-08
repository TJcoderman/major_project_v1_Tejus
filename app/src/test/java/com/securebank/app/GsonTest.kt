package com.securebank.app

import com.google.gson.Gson
import com.securebank.app.domain.MLModelInference
import org.junit.Test
import java.io.File

class GsonTest {
    @Test
    fun testParse() {
        try {
            val json = File("src/main/assets/ml/behavioral_auth_model.json").readText()
            val model = Gson().fromJson(json, MLModelInference.ModelData::class.java)
            println("Parse successful! modelType: ${model.modelType}")
            println("Layers count: ${model.layers.size}")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
