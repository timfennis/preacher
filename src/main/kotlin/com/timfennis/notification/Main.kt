package com.timfennis.notification

import io.sentry.Sentry


const val DATABASE_NAME = "notification"
const val COLLECTION_NAME = "users"

fun main(args: Array<String>) {

    Sentry.init()
    try {
        runServer()
    } catch (t: Throwable) {
        Sentry.capture(t)
    }
}