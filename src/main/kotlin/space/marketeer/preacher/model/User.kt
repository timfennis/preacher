package space.marketeer.preacher.model

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