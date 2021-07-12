package de.dseelp.kotlincord.api.utils

import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import kotlinx.coroutines.flow.first

fun Role.canInteract(other: Role): Boolean {
    return rawPosition > other.rawPosition
}

suspend fun Member.canInteract(other: Member): Boolean {
    return roles.first().canInteract(other.roles.first())
}