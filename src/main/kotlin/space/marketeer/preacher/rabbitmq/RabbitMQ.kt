package space.marketeer.preacher.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import java.net.URI

fun connectToRabbitMQ(uri: URI) = ConnectionFactory()
        .apply {
            setUri(uri)
        }
        .newConnection()