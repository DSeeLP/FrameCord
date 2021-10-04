package io.github.dseelp.framecord.api.presence

import dev.kord.common.entity.ActivityType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Presence(val status: PresenceStatus, val activity: Activity, val stayTime: Long, val enabled: Boolean)

@Serializable
enum class PresenceStatus(val status: dev.kord.common.entity.PresenceStatus) {
    ONLINE(dev.kord.common.entity.PresenceStatus.Online),
    IDLE(dev.kord.common.entity.PresenceStatus.Idle),
    @SerialName("DND") DO_NOT_DISTURB(dev.kord.common.entity.PresenceStatus.DoNotDisturb),
    INVISIBLE(dev.kord.common.entity.PresenceStatus.Invisible)
}

@Serializable
sealed class Activity() {
    abstract val activityType: ActivityType
    abstract val name: String

    @Serializable
    @SerialName("playing")
    class Playing(override val name: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Game
    }

    @Serializable
    @SerialName("streaming")
    class Streaming(override val name: String, val url: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Game
    }

    @Serializable
    @SerialName("listening")
    class Listening(override val name: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Listening
    }

    @Serializable
    @SerialName("watching")
    class Watching(override val name: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Watching
    }

    @Serializable
    @SerialName("competing")
    class Competing(override val name: String) : Activity() {
        @Transient
        override val activityType: ActivityType = ActivityType.Competing
    }
}