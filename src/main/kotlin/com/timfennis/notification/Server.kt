package com.timfennis.notification

import com.arangodb.ArangoDB
import com.arangodb.ArangoDBException
import com.arangodb.velocypack.module.jdk8.VPackJdk8Module
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.toLogString
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.ContentTransformationException
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.sentry.Sentry
import java.text.DateFormat

fun runServer() {
    initFirebase(System.getenv("GOOGLE_SERVICES_JSON_PATH"))

    val arangoDb by lazy {
        ArangoDB.Builder()
                .host(System.getenv("ARANGO_HOST"), System.getenv("ARANGO_PORT").toInt())
                .user(System.getenv("ARANGO_USER"))
                .password(System.getenv("ARANGO_PASSWORD"))
                .registerModule(VPackJdk8Module())
                .build()
    }

    val notification by lazy {
        arangoDb.db(DATABASE_NAME)
    }

    val users by lazy {
        notification.collection(COLLECTION_NAME)
    }

    embeddedServer(Netty, 8080) {
        install(ContentNegotiation) {
            gson {
                setDateFormat(DateFormat.LONG)
                setPrettyPrinting()
            }
        }

        routing {
            get("/preach/{uid?}") {
                Sentry.getContext().addExtra("request", call.request.toLogString())
                println(call.request.toLogString())

                val uid = call.parameters["uid"]

                val messageTitle = call.request.queryParameters["title"] ?: "Test"
                val messageText = call.request.queryParameters["message"] ?: "This is a test message"


                val recipients: Iterable<User> = if (uid != null) {
                    listOf(users.getDocument(uid))
                } else {
                    notification.query("FOR user IN users RETURN user", null, null, User::class.java)
                }

                recipients.forEach {
                    val failedDevices = NotificationSender().sendNotificationToUser(it, messageTitle, messageText)

                    if (failedDevices.isNotEmpty()) {
                        it.devices = it.devices.filterNot { failedDevices.contains(it) }.toHashSet()
                        println("Removing failed devices from ${it.key}")
                        users.updateDocument(it.key, it)
                    }
                }

                call.respond(HttpStatusCode.OK)
            }

            post("/register-device/") {
                Sentry.getContext().addExtra("request", call.request.toLogString())
                println(call.request.toLogString())

                try {
                    val token = call.request.header("Authorization")?.removePrefix("Bearer ")
                    val deviceInfo = call.receive<DeviceInfo>()
                    val firebaseUser = FirebaseAuth.getInstance().verifyIdToken(token)
                    val user = User(firebaseUser.uid, setOf(deviceInfo))

                    if (users.documentExists(user.key)) {
                        val existingUser = users.getDocument<User>(user.key)
                        user.devices = user.devices.plus(existingUser.devices)
                        //@todo merge devices
                        users.updateDocument(user.key, user)
                    } else {
                        users.insertDocument(user)
                    }
                    call.respond(HttpStatusCode.Accepted)
                } catch (e: ArangoDBException) {
                    Sentry.capture(e)
                    call.respond(HttpStatusCode.InternalServerError)
                } catch (e: ContentTransformationException) {
                    Sentry.capture(e)
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "")
                }
            }

            get {
                call.respond(HttpStatusCode.NotFound, "Nope there's only trash here")
            }
        }
    }.start(wait = true)

    println("Sutting down ArrangoDB")
    arangoDb.shutdown()
}