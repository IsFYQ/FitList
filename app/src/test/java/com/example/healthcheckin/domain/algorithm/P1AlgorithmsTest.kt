package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.InventoryExpiryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class P1AlgorithmsTest {
    @Test fun expiryEvaluator_marks_expired_today_and_near_expiry() {
        assertEquals(InventoryExpiryStatus.EXPIRED, InventoryExpiryEvaluator.evaluate("2026-01-01", "2026-08-01", "2026-08-04").status)
        assertEquals("今天到期", InventoryExpiryEvaluator.evaluate("2026-08-01", "2026-08-04", "2026-08-04").label)
        assertEquals(InventoryExpiryStatus.NEAR_EXPIRY, InventoryExpiryEvaluator.evaluate("2026-08-01", "2026-08-06", "2026-08-04").status)
        val invalid = InventoryExpiryEvaluator.evaluate("not-a-date", "下周过期", "2026-08-04")
        assertEquals(InventoryExpiryStatus.NORMAL, invalid.status)
        assertEquals(null, invalid.label)
    }

    @Test fun milestoneEvaluator_hits_loss_targets_in_distance_order() {
        val results = MilestoneEvaluator.evaluate(70.0, "2026-08-04", 0L, false, listOf(
            MilestoneEvaluator.MilestoneCandidate("a", "70kg", 70.0, null, 1_750_000_000_000L),
            MilestoneEvaluator.MilestoneCandidate("b", "75kg", 75.0, null, 1_750_000_000_000L),
        ), 80.0)
        assertEquals(listOf("b", "a"), results.map { it.milestoneId })
        assertTrue(results.all { it.achievedWeightKg == 70.0 })
    }

    @Test fun inventoryUnitConverter_unifies_meal_and_inventory_units() {
        assertTrue(InventoryUnitConverter.dimensionsCompatible("G", "KG"))
        assertTrue(InventoryUnitConverter.dimensionsCompatible("G", "ML"))
        assertTrue(InventoryUnitConverter.dimensionsCompatible("ML", "G"))
        assertTrue(InventoryUnitConverter.dimensionsCompatible("G", "PIECE"))
        assertTrue(InventoryUnitConverter.canConvert("KG", null))
        assertFalse(InventoryUnitConverter.canConvert("PIECE", null))
        assertEquals(0.1, InventoryUnitConverter.fromBasis(100.0, "KG", null)!!, 1e-9)
        assertEquals(100.0, InventoryUnitConverter.fromBasis(100.0, "ML", null)!!, 1e-9)
        assertEquals(2.0, InventoryUnitConverter.fromBasis(100.0, "PIECE", 50.0)!!, 1e-9)
        assertEquals(500.0, InventoryUnitConverter.toBasis(0.5, "KG", null), 1e-9)
        assertEquals(null, InventoryUnitConverter.fromBasis(100.0, "PIECE", null))
    }
}
