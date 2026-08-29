package com.armsone.denimdex.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ports handoff 9.2 (QuickValuePhotoRolesTests.swift, 4 cases: minCount=1, maxCount=20 photo_N identifiers). */
class QuickValuePhotoRolesTest {

    @Test
    fun `bounds are 1 to 20`() {
        assertEquals(1, QuickValuePhotoRoles.minCount)
        assertEquals(20, QuickValuePhotoRoles.maxCount)
    }

    @Test
    fun `roleForIndex produces deterministic photo_N identifiers`() {
        assertEquals("photo_1", QuickValuePhotoRoles.roleForIndex(0))
        assertEquals("photo_20", QuickValuePhotoRoles.roleForIndex(19))
    }

    @Test
    fun `allRoles is bounded and ordered`() {
        assertEquals(listOf("photo_1", "photo_2", "photo_3"), QuickValuePhotoRoles.allRoles(3))
        assertEquals(20, QuickValuePhotoRoles.allRoles(30).size)
        assertEquals(emptyList<String>(), QuickValuePhotoRoles.allRoles(0))
    }

    @Test
    fun `isValidRole accepts only photo_1 through photo_20`() {
        assertTrue(QuickValuePhotoRoles.isValidRole("photo_1"))
        assertTrue(QuickValuePhotoRoles.isValidRole("photo_20"))
        assertFalse(QuickValuePhotoRoles.isValidRole("photo_21"))
        assertFalse(QuickValuePhotoRoles.isValidRole("photo_0"))
        assertFalse(QuickValuePhotoRoles.isValidRole("not_a_role"))
    }
}
