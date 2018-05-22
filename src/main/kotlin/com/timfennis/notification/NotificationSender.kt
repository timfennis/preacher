package com.timfennis.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder

class NotificationSender {
    private val fcm: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }

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
//            Sentry.getContext().breadcrumbs.add(BreadcrumbBuilder().setMessage(e.message).build())
            false
        }
    }

    fun sendNotificationToUser(user: User, messageTitle: String, messageText: String): Set<DeviceInfo> {
        val failedDevices = HashSet<DeviceInfo>()

        user.devices.forEach {
            if (!sendNotification(simpleMessage(it, messageTitle, messageText))) {
                failedDevices.add(it)
            }
        }

        return failedDevices
    }
}