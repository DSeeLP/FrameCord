package io.github.dseelp.framecord.api.placeholders

data class PlaceholderContext(val arguments: Map<String, Any?>) {
    inline operator fun <reified T> get(key: String): T = @Suppress("UNCHECKED_CAST") arguments[key]!! as T
    inline fun <reified T> getOrNull(key: String): T? = @Suppress("UNCHECKED_CAST") arguments[key] as? T
    object Arguments {
        object Guild {
            const val Id = "guildId"
            const val Name = "guildName"
            const val Instance = "guild"
        }
        object TargetGuild {
            const val Id = "targetGuildId"
            const val Name = "targetGuildName"
            const val Instance = "targetGuild"
        }
        object User {
            const val Id = "userId"
            const val Name = "userName"
            const val Instance = "user"
        }
        object TargetUser {
            const val Id = "targetUserId"
            const val Name = "targetUserName"
            const val Instance = "targetUser"
        }
        object Shard {
            const val Id = "shardId"
        }
        object Discord {
            const val JoinedGuilds = "guildCount"
            const val TotalMembers = "totalMembers"
        }
        object Cord {
            const val Version = "version"
        }
    }
    object Parameters {
        object Shard {
            const val Id = "shardIdParameter"
        }
        object Kord {
            const val Instance = "kordInstance"
            const val Gateway = "kordGateway"
        }
    }
}