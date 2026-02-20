@file:OptIn(ExperimentalTestApi::class)

package io.github.jdemeulenaere.compose.driver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot as isRootMatcher
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeysDown
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
import java.nio.file.Files
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
    val videoRecorder = VideoRecorder()
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
            videoRecorder.captureFrame()
            onNode(test, autoRespondOkOrGif, videoRecorder, f)
            test.waitForIdle()
            videoRecorder.captureFrame()
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
            val matcher = call.node()
            if (matcher != null) {
                onNode(autoRespondOkOrGif = false) { call.respondText(it.printToString()) }
            } else {
                // Print all root nodes (handles popups which create additional roots).
                withContext(runTestContext) {
                    test.waitForIdle()
                    val allRoots = test.onAllNodes(isRootMatcher())
                    call.respondText(allRoots.printToString(maxDepth = Int.MAX_VALUE))
                    test.waitForIdle()
                }
            }
        }
        get("/waitForIdle") { onNode {} }
        get("/waitForNode") {
            val timeout = call.optionalParam("timeout")?.toLong() ?: 5_000L
            val matcher = call.node() ?: isRootMatcher()
            onNode { waitUntilAtLeastOneExists(matcher, timeout) }
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
        get("/scrollTo") { onNode { it.performScrollTo() } }
        get("/keyEvent") {
            onNode { node ->
                val key = keyByName(call.requiredParam("key"))
                val action = (call.optionalParam("action") ?: "press").lowercase()
                val modifiers = call.optionalParam("modifiers")
                    ?.split(",")
                    ?.map { keyByName(it.trim()) }
                    ?: emptyList()
                node.performKeyInput {
                    when (action) {
                        "press" -> {
                            if (modifiers.isEmpty()) {
                                pressKey(key)
                            } else {
                                withKeysDown(modifiers) { pressKey(key) }
                            }
                        }
                        "down" -> keyDown(key)
                        "up" -> keyUp(key)
                        else -> throw IllegalArgumentException("Unknown action '$action'. Use 'press', 'down', or 'up'.")
                    }
                }
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

        get("/startRecording") {
            if (videoRecorder.isRecording) {
                call.respondText(
                    "Recording already in progress",
                    status = HttpStatusCode.Conflict,
                )
                return@get
            }
            val format = VideoFormat.fromString(call.optionalParam("format") ?: "mp4")
            val fps = call.optionalParam("fps")?.toInt() ?: 30
            val matcher = call.node()
            withContext(runTestContext) {
                val target = matcher?.let { test.onNode(it) }
                videoRecorder.start(test, format, fps, target)
            }
            ok()
        }
        get("/stopRecording") {
            if (!videoRecorder.isRecording) {
                call.respondText(
                    "No recording in progress",
                    status = HttpStatusCode.BadRequest,
                )
                return@get
            }
            val result = withContext(runTestContext) {
                videoRecorder.captureFrame()
                videoRecorder.stop()
            }
            try {
                call.respondStream(result.contentType) { Files.copy(result.file, this) }
            } finally {
                result.cleanup()
            }
        }
    }
}

private suspend fun RoutingContext.ok() {
    call.respondText("ok")
}

/**
 * Builds a [SemanticsMatcher] from the node-selection query parameters (`nodeTag`, `nodeText`,
 * etc.). Returns `null` when no selector is provided, meaning "target the root(s)".
 */
private fun ApplicationCall.node(): SemanticsMatcher? {
    val nodeTag = optionalParam("nodeTag")
    val nodeText = optionalParam("nodeText")
    val nodeTextSubstring = optionalParam("nodeTextSubstring")?.toBoolean() ?: false
    val nodeTextIgnoreCase = optionalParam("nodeTextIgnoreCase")?.toBoolean() ?: false

    val tagMatcher = nodeTag?.let { hasTestTag(it) }
    val textMatcher =
        nodeText?.let {
            hasText(it, substring = nodeTextSubstring, ignoreCase = nodeTextIgnoreCase)
        }

    return when {
        tagMatcher != null && textMatcher != null -> tagMatcher and textMatcher
        tagMatcher != null -> tagMatcher
        textMatcher != null -> textMatcher
        else -> null
    }
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

internal fun keyByName(name: String): Key {
    val companionClass = Key.Companion::class.java
    val prefix = "get${name.replaceFirstChar { it.uppercase() }}"
    // Key is an inline value class, so JVM getter names are mangled with a hash suffix
    // (e.g. "getA-xxxx" instead of "getA"). Match by prefix, then require the next char
    // to be '-' (the mangling separator) to avoid false matches like "getA" -> "getApostrophe".
    val getter = companionClass.declaredMethods.firstOrNull {
        it.name.startsWith(prefix) &&
            it.parameterCount == 0 &&
            (it.name.length == prefix.length || it.name[prefix.length] == '-')
    } ?: throw IllegalArgumentException(
        "Unknown key: '$name'. Use a property name from androidx.compose.ui.input.key.Key " +
            "(e.g. 'A', 'Enter', 'DirectionUp')."
    )
    val keyCode = getter.invoke(Key.Companion) as Long
    return Key(keyCode)
}

private suspend fun RoutingContext.onNode(
    test: ComposeUiTest,
    autoRespondOkOrGif: Boolean,
    videoRecorder: VideoRecorder,
    f: suspend ComposeUiTest.(SemanticsNodeInteraction) -> Unit,
) {
    val gifDurationMs = call.optionalParam("gifDurationMs")?.toInt()
    val videoDurationMs = call.optionalParam("videoDurationMs")?.toInt()
    val videoFormat = call.optionalParam("videoFormat")?.let { VideoFormat.fromString(it) }
        ?: VideoFormat.MP4
    val matcher = call.node()

    // During session recording, inline GIF/video params are ignored — just run the action.
    val isSessionRecording = videoRecorder.isRecording
    val hasInlineMedia = (gifDurationMs != null || videoDurationMs != null) && !isSessionRecording

    if (!hasInlineMedia || !autoRespondOkOrGif) {
        test.f(test.resolveNode(matcher))
        if (autoRespondOkOrGif) ok()
        return
    }

    if (gifDurationMs != null) {
        require(gifDurationMs in 0..5_000) { "gifDurationMs should be <= 5_000 and >= 0" }
        val timeBetweenFramesMs = 16L
        val frames = generateFrames(test, gifDurationMs, timeBetweenFramesMs, matcher, f)
        respondGif(frames, timeBetweenFramesMs)
    } else if (videoDurationMs != null) {
        require(videoDurationMs >= 0) { "videoDurationMs should be >= 0" }
        val timeBetweenFramesMs = 16L
        val frames = generateFrames(test, videoDurationMs, timeBetweenFramesMs, matcher, f)
        respondVideo(frames, timeBetweenFramesMs, videoFormat)
    }
}

/**
 * Resolves a single [SemanticsNodeInteraction]. When [matcher] is non-null, uses
 * [ComposeUiTest.onNode] (which searches across all roots). When [matcher] is null (no selector
 * provided), targets the first root — safe even when popups add extra roots.
 */
private fun ComposeUiTest.resolveNode(matcher: SemanticsMatcher?): SemanticsNodeInteraction {
    return if (matcher != null) onNode(matcher) else onAllNodes(isRootMatcher())[0]
}

private suspend fun RoutingContext.generateFrames(
    test: ComposeUiTest,
    durationMs: Int,
    timeBetweenFrames: Long,
    matcher: SemanticsMatcher?,
    f: suspend ComposeUiTest.(SemanticsNodeInteraction) -> Unit,
): List<ImageBitmap> {
    val frames = mutableListOf<ImageBitmap>()

    fun screenshotFrame() {
        test.waitForIdle()
        frames += test.resolveNode(null).captureToImage()
    }

    try {
        test.mainClock.autoAdvance = false
        test.f(test.resolveNode(matcher))

        var t = 0L
        while (t <= durationMs) {
            screenshotFrame()
            test.mainClock.advanceTimeBy(timeBetweenFrames)
            t += timeBetweenFrames
        }

        return frames
    } finally {
        test.mainClock.autoAdvance = true
    }
}
