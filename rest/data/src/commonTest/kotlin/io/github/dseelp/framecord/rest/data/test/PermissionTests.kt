package io.github.dseelp.framecord.rest.data.test

import io.github.dseelp.framecord.rest.data.objects.FullPermission
import io.github.dseelp.framecord.rest.data.objects.Permission
import io.github.dseelp.framecord.rest.data.objects.bitwise
import kotlin.test.*

class PermissionTests {
    companion object {
        val availablePermissions = arrayOf(
            FullPermission(1, "Test Permission 1"),
            FullPermission(2, "Test Permission 2"),
            FullPermission(3, "Test Permission 3"),
        )

        init {
            Permission.getPermissions = { availablePermissions }
        }
    }

    @Test
    fun testPermissionEncoding() {
        val initial = arrayOf(Permission(1), Permission(2)) as Array<Permission>
        val raw = initial.bitwise
        assertEquals(6L, raw)
        assertFalse(Permission.isApplied(raw, Permission(4)))
        assertFalse(Permission.isApplied(raw, Permission(3)))
        assertTrue(Permission.isApplied(raw, Permission(1)))
        assertTrue(Permission.isApplied(raw, Permission(2)))
        val applied = Permission.getAllApplied(raw)
        assertContentEquals(Permission.getPermissions().sliceArray(0..1), applied as Array<FullPermission>)
    }
}