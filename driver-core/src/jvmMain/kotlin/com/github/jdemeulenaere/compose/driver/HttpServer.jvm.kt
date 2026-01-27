package com.github.jdemeulenaere.compose.driver

import io.ktor.http.ContentType
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.RoutingCall
import java.io.OutputStream

internal actual val ApplicationEngineFactory: ApplicationEngineFactory<*, *> = Netty

internal actual suspend fun RoutingCall.respondStream(
    contentType: ContentType,
    producer: suspend OutputStream.() -> Unit,
) = respondOutputStream(contentType, producer = producer)
