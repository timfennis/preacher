package space.marketeer.preacher.rabbitmq

import com.google.firebase.messaging.Message
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
                channel.basicNack(envelope.deliveryTag, false, false)
            }
            else -> {
                val messageBuilder = Message.builder().apply {
                    putData("eventName", event.name)
                    event.gameId?.let { putData("gameId", it) }
                    event.roundId?.let { putData("roundId", it) }
                    event.companyId?.let { putData("companyId", it) }
                }

                notificationSender.broadcastMessageToUserId(userId, messageBuilder)
                channel.basicAck(envelope.deliveryTag, false)
            }
        }
    }

    class Event {
        var name: String? = null
        val gameId: String? = null
        val roundId: String? = null
        val companyId: String? = null

        var _metadata: Map<String, String?> = HashMap()
    }
}