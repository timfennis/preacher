package space.marketeer.preacher.firebase

import com.arangodb.ArangoCollection
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import space.marketeer.preacher.arangodb.getDocument
import space.marketeer.preacher.model.DeviceInfo
import space.marketeer.preacher.model.User

class NotificationSender(
        val fcm: FirebaseMessaging,
        val users: ArangoCollection
) {


    fun simpleMessage(deviceInfo: DeviceInfo, messageTitle: String, messageText: String): Message {
        return Message.builder()
                .setToken(deviceInfo.token)
                .setNotification(Notification(messageTitle, messageText))
                .build()
    }

    fun sendNotification(message: Message): Boolean {
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

    fun broadcastNotificationToUserId(userId: String, messageTitle: String, messageText: String) {
        this.broadcastNotificationToUser(users.getDocument(userId), messageTitle, messageText)
    }


}