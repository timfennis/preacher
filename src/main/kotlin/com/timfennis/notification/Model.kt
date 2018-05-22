package com.timfennis.notification

import com.arangodb.entity.DocumentField

data class User(
        var uid: String,
        var devices: Set<DeviceInfo> = HashSet()
) {
    @Suppress("unused")
    constructor() : this("")

    @DocumentField(DocumentField.Type.KEY)
    val key = uid
}

data class DeviceInfo(
        var token: String,
        var osVersion: String,
        var sdkInt: Int,
        var device: String,
        var model: String,
        var product: String) {

    @Suppress("unused")
    constructor() : this("", "", -1, "", "", "")
}