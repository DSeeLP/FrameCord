package io.github.dseelp.framecord.rest.data

import io.github.dseelp.framecord.rest.data.objects.User
import io.ktor.http.*

object CdnUtils {
    val cdnBaseUrl = Url("https://cdn.discordapp.com/")
    fun avatarUrl(user: User): Url {
        fun default() = defaultAvatarUrl(user.discriminator)
        return if (user.avatarHash != null) runCatching { avatarUrl(user.id, user.avatarHash) }.getOrNull() ?: default()
        else default()
    }

    fun avatarUrl(userId: Long, hash: String, extension: String = ".png"): Url {
        return URLBuilder(cdnBaseUrl).apply {
            pathComponents("avatars", userId.toString(), "$hash$extension")
        }.build()
    }

    fun defaultAvatarUrl(discriminator: Int): Url {
        if (discriminator > 9999) throw IllegalArgumentException("The maximum value of a user discriminator is 9999")
        return URLBuilder(cdnBaseUrl).apply {
            pathComponents("embed", "avatars", "${discriminator % 5}.png")
        }.build()
    }
}