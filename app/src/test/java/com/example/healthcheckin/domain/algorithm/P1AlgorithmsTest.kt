package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.InventoryExpiryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class P1AlgorithmsTest {
    @Test fun expiryEvaluator_marks_expired_today_and_near_expiry() {
        assertEquals(InventoryExpiryStatus.EXPIRED, InventoryExpiryEvaluator.evaluate("2026-01-01", "2026-08-01", "2026-08-04").status)
        assertEquals("今天到期", InventoryExpiryEvaluator.evaluate("2026-08-01", "2026-08-04", "2026-08-04").label)
        assertEquals(InventoryExpiryStatus.NEAR_EXPIRY, InventoryExpiryEvaluator.evaluate("2026-08-01", "2026-08-06", "2026-08-04").status)
    }

    @Test fun milestoneEvaluator_hits_loss_targets_in_distance_order() {
        val results = MilestoneEvaluator.evaluate(70.0, "2026-08-04", 0L, false, listOf(
            MilestoneEvaluator.MilestoneCandidate("a", "70kg", 70.0, null, 1_750_000_000_000L),
            MilestoneEvaluator.MilestoneCandidate("b", "75kg", 75.0, null, 1_750_000_000_000L),
        ), 80.0)
        assertEquals(listOf("b", "a"), results.map { it.milestoneId })
        assertTrue(results.all { it.achievedWeightKg == 70.0 })
    }
}
