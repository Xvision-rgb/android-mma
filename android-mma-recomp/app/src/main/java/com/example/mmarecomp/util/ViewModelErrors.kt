package com.example.mmarecomp.util

import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Relance les annulations de coroutine — ne jamais les avaler dans un
 *  `catch (Exception)`. */
fun rethrowCancellation(e: Throwable): Nothing? {
    if (e is CancellationException) throw e
    return null
}

fun Throwable.isNetworkError(): Boolean = this is IOException
