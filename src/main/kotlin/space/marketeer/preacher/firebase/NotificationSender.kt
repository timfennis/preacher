package space.marketeer.preacher.firebase

import com.arangodb.ArangoCollection
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import space.marketeer.preacher.arangodb.getDocument
import space.marketeer.preacher.model.DeviceInfo
import space.marketeer.preacher.model.User
import java.time.Instant

class NotificationSender(
        val fcm: FirebaseMessaging,
        val users: ArangoCollection
) {


    private fun simpleMessage(deviceInfo: DeviceInfo, messageTitle: String, messageText: String): Message {
        return Message.builder()
                .setToken(deviceInfo.token)
                .setNotification(Notification(messageTitle, messageText))
                .build()
    }

    private fun sendNotification(message: Message): Boolean {
        return try {
            fcm.send(message)
            true
        } catch (e: FirebaseMessagingException) {
            false
        }
    }

    fun broadcastNotificationToUser(user: User, messageTitle: String, messageText: String): Set<DeviceInfo> {
        val failedDevices = HashSet<DeviceInfo>()

        user.devices.forEach {
            if (!sendNotification(simpleMessage(it, messageTitle, messageText))) {
                failedDevices.add(it)
            }
        }

        return failedDevices
    }

    fun broadcastMessageToUserId(userId: String, msgBuilder: Message.Builder) {

        val failedDevices = HashSet<DeviceInfo>()
        val user: User = users.getDocument(userId)

        user.devices.forEach {
            msgBuilder.setToken(it.token)
            msgBuilder.putData("timestamp", Instant.now().toEpochMilli().toString())

            if (!sendNotification(msgBuilder.build())) {
                failedDevices.add(it)
            }
        }


        if (failedDevices.isNotEmpty()) {
            user.devices = user.devices.filterNot { failedDevices.contains(it) }.toHashSet()
            println("Removing failed devices from ${user.key}")
            users.updateDocument(user.key, user)
        }
    }


}