package de.dseelp.kotlincord.api.utils

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member

fun Member.checkPermission(permission: Permission): Boolean =
    if (hasPermission(Permission.ADMINISTRATOR)) true else hasPermission(permission)