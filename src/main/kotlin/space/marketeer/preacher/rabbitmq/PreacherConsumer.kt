package space.marketeer.preacher.rabbitmq

import com.google.gson.Gson
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import space.marketeer.preacher.firebase.NotificationSender

class PreacherConsumer(channel: Channel, val notificationSender: NotificationSender) : DefaultConsumer(channel) {


    override fun handleDelivery(consumerTag: String?, envelope: Envelope?, properties: AMQP.BasicProperties?, body: ByteArray?) {
        if (envelope == null) return

        println("consumerTag: $consumerTag\r\n\tenvelope: $envelope\r\n\tproperties: ${properties.toString()}\r\n\tbody: ${body.toString()}")

        val text = body?.let { String(it) }
        val event = Gson().fromJson(text, Event::class.java)
        val userId = event._metadata["userId"]

        when (userId) {
            null -> {
                println("nacking")
                channel.basicNack(envelope.deliveryTag, false, false)
            }
            else -> {
                println("acking")
                notificationSender.broadcastNotificationToUserId(userId, "Test", "Event: ${event.name} occurred")
                channel.basicAck(envelope.deliveryTag, false)
            }
        }
    }

    class Event {
        var name: String? = null
        var _metadata: Map<String, String?> = HashMap()
    }

    class Metadata {
        var userId: String? = null
    }
}