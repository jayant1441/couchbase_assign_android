package com.example.couchbaseassignment.model

import java.net.URI

data class ConfigModel(
    val capellaEndpointURL: URI,
    val username: String,
    val password: String
) 