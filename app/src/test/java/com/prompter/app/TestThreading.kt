package com.prompter.app

import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/**
 * Room refuses DAO calls on the main thread. Production creates PromptEngine on
 * Dispatchers.IO, so tests do the same through this helper instead of relaxing
 * the database's thread checks.
 */
internal fun <T> onBackgroundThread(block: () -> T): T {
    val executor = Executors.newSingleThreadExecutor()
    return try {
        executor.submit<T> { block() }.get()
    } catch (failure: ExecutionException) {
        throw failure.cause ?: failure
    } finally {
        executor.shutdown()
    }
}
