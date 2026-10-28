package io.github.staernid.sunbeam

import android.app.Activity
import io.github.staernid.sunbeam.nvstream.http.ComputerDetails
import io.github.staernid.sunbeam.nvstream.http.NvApp
import io.github.staernid.sunbeam.utils.ServerHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ShortcutTrampolineTest {
    private class TestShortcutTrampoline : ShortcutTrampoline() {
        public override fun validateHostInput(
            hostUUID: String?,
            hostName: String?,
        ): Boolean {
            return super.validateHostInput(hostUUID, hostName)
        }

        public override fun validateAppInput(
            appUUID: String?,
            appIDStr: String?,
            appName: String?,
        ): Boolean {
            return super.validateAppInput(appUUID, appIDStr, appName)
        }
    }

    @Test
    fun createPcShortcutIntent_targetsShortcutTrampolineWithCorrectExtras() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val computer = ComputerDetails().apply {
            name = "Gaming Rig"
            uuid = UUID.randomUUID().toString()
        }

        val intent = ServerHelper.createPcShortcutIntent(activity, computer)

        assertEquals(ShortcutTrampoline::class.java.name, intent.component?.className)
        assertEquals(computer.name, intent.getStringExtra(ShortcutTrampoline.EXTRA_NAME))
        assertEquals(computer.uuid, intent.getStringExtra(ShortcutTrampoline.EXTRA_UUID))
    }

    @Test
    fun createAppShortcutIntent_targetsShortcutTrampolineWithHostAndAppExtras() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val computer = ComputerDetails().apply {
            name = "Gaming Rig"
            uuid = UUID.randomUUID().toString()
        }
        val app = NvApp("Steam", UUID.randomUUID().toString(), 42, true)

        val intent = ServerHelper.createAppShortcutIntent(activity, computer, app)

        assertEquals(ShortcutTrampoline::class.java.name, intent.component?.className)
        assertEquals(computer.name, intent.getStringExtra(ShortcutTrampoline.EXTRA_NAME))
        assertEquals(computer.uuid, intent.getStringExtra(ShortcutTrampoline.EXTRA_UUID))
        assertEquals(app.appName, intent.getStringExtra(Game.EXTRA_APP_NAME))
        assertEquals(app.appUUID, intent.getStringExtra(Game.EXTRA_APP_UUID))
        assertEquals(app.appId.toString(), intent.getStringExtra(Game.EXTRA_APP_ID))
        assertTrue(intent.getBooleanExtra(Game.EXTRA_APP_HDR, false))
    }

    @Test
    fun validateHostInput_validatesUUIDAndFallbackName() {
        val activity = Robolectric.buildActivity(TestShortcutTrampoline::class.java).get()
        val validUuid = UUID.randomUUID().toString()

        assertTrue(activity.validateHostInput(validUuid, null))
        assertTrue(activity.validateHostInput(null, "Desktop-PC"))
        assertFalse(activity.validateHostInput("not-a-uuid", null))
        assertFalse(activity.validateHostInput(null, null))
    }

    @Test
    fun validateAppInput_requiresAtLeastOneAppIdentifier() {
        val activity = Robolectric.buildActivity(TestShortcutTrampoline::class.java).get()

        assertTrue(activity.validateAppInput(UUID.randomUUID().toString(), null, null))
        assertTrue(activity.validateAppInput(null, "123", null))
        assertTrue(activity.validateAppInput(null, null, "GameName"))
        assertFalse(activity.validateAppInput(null, null, null))
        assertFalse(activity.validateAppInput(null, "invalid_num", null))
    }
}
