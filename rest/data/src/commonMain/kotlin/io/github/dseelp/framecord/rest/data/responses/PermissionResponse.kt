package io.github.dseelp.framecord.rest.data.responses

import io.github.dseelp.framecord.rest.data.objects.FullPermission
import kotlinx.serialization.Serializable

@Serializable
data class PermissionResponse(val permissions: Array<FullPermission>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PermissionResponse) return false

        if (!permissions.contentEquals(other.permissions)) return false

        return true
    }

    override fun hashCode(): Int {
        return permissions.contentHashCode()
    }
}
