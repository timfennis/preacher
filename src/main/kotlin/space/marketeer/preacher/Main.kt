package space.marketeer.preacher

import com.arangodb.ArangoDB
import com.arangodb.velocypack.module.jdk8.VPackJdk8Module
import com.google.firebase.messaging.FirebaseMessaging
import io.sentry.Sentry
import space.marketeer.preacher.firebase.NotificationSender
import space.marketeer.preacher.firebase.initFirebase
import space.marketeer.preacher.http.runServer
import space.marketeer.preacher.rabbitmq.PreacherConsumer
import space.marketeer.preacher.rabbitmq.connectToRabbitMQ
import java.net.URI

const val DATABASE_NAME = "notification"
const val COLLECTION_NAME = "users"

fun main(args: Array<String>) {

    val arangoDb by lazy {
        ArangoDB.Builder()
                .host(System.getenv("ARANGO_HOST"), System.getenv("ARANGO_PORT").toInt())
                .user(System.getenv("ARANGO_USER"))
                .password(System.getenv("ARANGO_PASSWORD"))
                .registerModule(VPackJdk8Module())
                .build()
    }


    val notificationSender by lazy {
        NotificationSender(FirebaseMessaging.getInstance(), arangoDb.db(DATABASE_NAME).collection(COLLECTION_NAME))
    }

    Sentry.init()
    try {
        initFirebase(System.getenv("GOOGLE_SERVICES_JSON_PATH"))

        val channel = connectToRabbitMQ(URI(System.getenv("RABBITMQ_URL"))).createChannel().apply {

            exchangeDeclare("event", "topic", true)
            exchangeDeclare("dlx", "direct")

            queueDeclare("preacher", true, true, false, mapOf(
                    "x-dead-letter-exchange" to "dlx",
                    "x-dead-letter-routing-key" to "preacher"
            ))

            queueBind("preacher", "event", "#")
        }

        channel.basicConsume("preacher", false, PreacherConsumer(channel, notificationSender))

        runServer(arangoDb, notificationSender)

    } catch (t: Throwable) {
        Sentry.capture(t)
    } finally {
        arangoDb.shutdown()
    }
}