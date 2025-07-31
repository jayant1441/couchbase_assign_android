package com.example.couchbaseassignment.model

import java.util.UUID

data class ErrorModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val closable: Boolean
)

object ConfigurationErrors {
    val configFileMissing = ErrorModel(
        title = "Config file not found",
        message = "Could not load config.properties.",
        closable = true
    )
    val configError = ErrorModel(
        title = "Configuration Error",
        message = "Invalid Capella URL or credentials.",
        closable = true
    )
} 