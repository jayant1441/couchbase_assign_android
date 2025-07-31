package com.example.couchbaseassignment.manager

import android.content.Context
import com.example.couchbaseassignment.model.ConfigModel
import com.example.couchbaseassignment.model.ConfigurationErrors
import java.net.URI
import java.util.Properties

object ConfigurationManager {
    @Volatile
    private var configuration: ConfigModel? = null
    
    fun initialize(context: Context) {
        loadConfiguration(context)
    }
    
    fun getConfiguration(): ConfigModel? {
        return configuration
    }
    
    private fun loadConfiguration(context: Context) {
        try {
            val properties = Properties()
            
            context.assets.open("config.properties").use { inputStream ->
                properties.load(inputStream)
            }
            
            val parsedConfig = parseConfiguration(properties)
            configuration = parsedConfig
            
        } catch (e: java.io.FileNotFoundException) {
            ErrorManager.showError(ConfigurationErrors.configFileMissing)
        } catch (e: Exception) {
            ErrorManager.showError(ConfigurationErrors.configFileMissing)
        }
    }
    
    private fun parseConfiguration(properties: Properties): ConfigModel? {
        return try {
            val urlString = properties.getProperty("CAPELLA_ENDPOINT_URL")?.trim()
                ?: throw IllegalArgumentException("Missing CAPELLA_ENDPOINT_URL")
            
            val uri = URI.create(urlString)
            
            val username = properties.getProperty("USERNAME")?.trim()
                ?: throw IllegalArgumentException("Missing USERNAME")
            
            val rawPassword = properties.getProperty("PASSWORD")
                ?: throw IllegalArgumentException("Missing PASSWORD")
            val password = rawPassword.trim()
            
            if (username.isEmpty() || password.isEmpty()) {
                throw IllegalArgumentException("Empty credentials")
            }
            
            ConfigModel(capellaEndpointURL = uri, username = username, password = password)
        } catch (e: Exception) {
            ErrorManager.showError(ConfigurationErrors.configError)
            null
        }
    }
} 