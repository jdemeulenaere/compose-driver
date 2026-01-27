package com.github.jdemeulenaere.compose.driver

import io.ktor.http.ContentType
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.routing.RoutingCall
import java.io.OutputStream

internal expect val ApplicationEngineFactory: ApplicationEngineFactory<*, *>

internal expect suspend fun RoutingCall.respondStream(
    contentType: ContentType,
    producer: suspend OutputStream.() -> Unit,
)
