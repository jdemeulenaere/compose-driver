@file:OptIn(ExperimentalTestApi::class)

package com.github.jdemeulenaere.compose.driver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withContext

/**
 * Start the Compose Driver server on port [port] with the composable whose full qualifier name is
 * [contentComposableFullyQualifiedName].
 *
 * Example: For the `MyFoo` composable inside `Foo.kt` and with the `com.example.foo` package, call
 * `startComposeDriverServer("com.example.foo.FooKt.MyFoo")`.
 */
fun startComposeDriverServer(
    contentComposableFullyQualifiedName: String = systemPropertyComposable(),
    port: Int = 8080,
    factory: ApplicationEngineFactory<*, *> = ApplicationEngineFactory,
    additionalModuleConfiguration: suspend Application.() -> Unit = {},
) {
    startComposeDriverServer(
        port = port,
        factory = factory,
        additionalModuleConfiguration = additionalModuleConfiguration,
        content = fullyQualifiedComposable(contentComposableFullyQualifiedName),
    )
}

private fun systemPropertyComposable(): String {
    return System.getProperty("compose.driver.composable")
        ?: error("System property compose.driver.composable not set")
}

/** Start the Compose Driver server on port [port] with the given [content]. */
fun startComposeDriverServer(
    port: Int = 8080,
    factory: ApplicationEngineFactory<*, *> = ApplicationEngineFactory,
    additionalModuleConfiguration: suspend Application.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val navigationEventDispatcher = NavigationEventDispatcher()
    val navigationEventDispatcherOwner =
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = navigationEventDispatcher
        }

    val runTestContext = StandardTestDispatcher()
    runUiTest(runTestContext = runTestContext, testTimeout = Duration.INFINITE) {
        // Android tests don't allow calling setContent multiple times, so we use an increasing key
        // to force recreate the content state when /reset is called.
        var contentKey by mutableStateOf(0)
        var currentContent by mutableStateOf(content)
        setContent {
            key(contentKey) {
                CompositionLocalProvider(
                    LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
                    content = currentContent,
                )
            }
        }

        embeddedServer(factory, port = port) {
                configureDriverModule(
                    test = this@runUiTest,
                    runTestContext = runTestContext,
                    navigationEventDispatcher = navigationEventDispatcher,
                    onReset = { composableName ->
                        if (composableName != null) {
                            currentContent = fullyQualifiedComposable(composableName)
                        }
                        contentKey++
                        waitForIdle()
                    },
                )

                additionalModuleConfiguration()
            }
            .start()

        awaitCancellation()
    }
}

private fun Application.configureDriverModule(
    test: ComposeUiTest,
    runTestContext: CoroutineContext,
    navigationEventDispatcher: NavigationEventDispatcher,
    onReset: (composableName: String?) -> Unit,
) {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respondText(
                text = "Bad Request: ${cause.message}",
                status = HttpStatusCode.BadRequest,
            )
        }
        exception<Throwable> { call, cause ->
            println("thread=${Thread.currentThread().name}")
            cause.printStackTrace()
            call.respondText(
                text = "Server Error: ${cause.message}",
                status = HttpStatusCode.InternalServerError,
            )
        }
    }

    suspend fun RoutingContext.onNode(
        autoRespondOkOrGif: Boolean = true,
        f: suspend ComposeUiTest.(SemanticsNodeInteraction) -> Unit,
    ) {
        withContext(runTestContext) {
            test.waitForIdle()
            onNode(test, autoRespondOkOrGif, f)
            test.waitForIdle()
        }
    }

    // Use GET so that we can manually test the endpoint directly in the browser.
    routing {
        get("/status") { ok() }
        get("/reset") {
            val composableName = call.optionalParam("composable")
            withContext(runTestContext) { onReset(composableName) }
            ok()
        }
        get("/screenshot") {
            onNode(autoRespondOkOrGif = false) { node ->
                val image = node.captureToImage()
                call.respondStream(ContentType.Image.PNG) { writePng(image, this) }
            }
        }
        get("/printTree") {
            onNode(autoRespondOkOrGif = false) { call.respondText(it.printToString()) }
        }
        get("/waitForIdle") { onNode {} }
        get("/waitForNode") {
            val timeout = call.optionalParam("timeout")?.toLong() ?: 5_000L
            onNode { waitUntilAtLeastOneExists(call.node(), timeout) }
        }
        get("/click") { onNode { it.performClick() } }
        get("/longClick") { onNode { it.performTouchInput { longClick() } } }
        get("/doubleClick") { onNode { it.performTouchInput { doubleClick() } } }
        get("/textInput") { onNode { it.performTextInput(call.requiredParam("text")) } }
        get("/textReplacement") { onNode { it.performTextReplacement(call.requiredParam("text")) } }
        get("/textClearance") { onNode { it.performTextClearance() } }
        get("/navigateBack") {
            onNode {
                val input = DirectNavigationEventInput()
                navigationEventDispatcher.addInput(input)
                input.backCompleted()
            }
        }
        get("/swipe") {
            onNode { node ->
                node.performTouchInput {
                    when (call.direction()) {
                        "UP" -> swipeUp()
                        "DOWN" -> swipeDown()
                        "LEFT" -> swipeLeft()
                        "RIGHT" -> swipeRight()
                        else ->
                            throw IllegalArgumentException("Unknown direction: ${call.direction()}")
                    }
                }
            }
        }
        get("/pointerInput/down") {
            onNode { it.performTouchInput { down(call.pointerId(), center) } }
        }
        get("/pointerInput/moveBy") {
            onNode { it.performTouchInput { moveBy(call.pointerId(), call.offset()) } }
        }
        get("/pointerInput/moveTo") {
            onNode { it.performTouchInput { moveTo(call.pointerId(), call.offset()) } }
        }
        get("/pointerInput/up") { onNode { it.performTouchInput { up(call.pointerId()) } } }
    }
}

private suspend fun RoutingContext.ok() {
    call.respondText("ok")
}

private fun ApplicationCall.node(): SemanticsMatcher {
    return optionalParam("tag")?.let { hasTestTag(it) } ?: isRoot()
}

private fun ApplicationCall.requiredParam(name: String): String {
    return requireNotNull(request.queryParameters[name]) { "Missing '$name' parameter" }
}

private fun ApplicationCall.optionalParam(name: String): String? {
    return request.queryParameters[name]
}

private fun ApplicationCall.direction(): String {
    return requiredParam("direction").uppercase()
}

private fun ApplicationCall.offset(): Offset {
    return Offset(requiredParam("x").toFloat(), requiredParam("y").toFloat())
}

private fun ApplicationCall.pointerId(): Int {
    return optionalParam("pointerId")?.toInt() ?: 0
}

private suspend fun RoutingContext.onNode(
    test: ComposeUiTest,
    autoRespondOkOrGif: Boolean,
    f: suspend ComposeUiTest.(SemanticsNodeInteraction) -> Unit,
) {
    val gifDurationMs = call.optionalParam("gifDurationMs")?.toInt()

    if (gifDurationMs == null || !autoRespondOkOrGif) {
        test.f(test.onNode(call.node()))
        if (autoRespondOkOrGif) ok()
        return
    }

    require(gifDurationMs in 0..10_000) { "gifDurationMs should be <= 10_000 and >= 0" }

    val timeBetweenFramesMs = 16L
    val frames = generateFrames(test, gifDurationMs, timeBetweenFramesMs, f)
    respondGif(frames, timeBetweenFramesMs)
}

private suspend fun RoutingContext.generateFrames(
    test: ComposeUiTest,
    gifDurationMs: Int,
    timeBetweenFrames: Long,
    f: suspend ComposeUiTest.(SemanticsNodeInteraction) -> Unit,
): List<ImageBitmap> {
    val frames = mutableListOf<ImageBitmap>()

    fun screenshotFrame() {
        test.waitForIdle()
        frames += test.onRoot().captureToImage()
    }

    try {
        test.mainClock.autoAdvance = false
        test.f(test.onNode(call.node()))

        var t = 0L
        while (t <= gifDurationMs) {
            screenshotFrame()
            test.mainClock.advanceTimeBy(timeBetweenFrames)
            t += timeBetweenFrames
        }

        return frames
    } finally {
        test.mainClock.autoAdvance = true
    }
}
