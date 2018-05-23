package space.marketeer.preacher.model

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