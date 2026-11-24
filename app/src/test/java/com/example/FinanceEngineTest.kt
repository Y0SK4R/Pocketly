package com.example

import com.example.domain.Account
import com.example.domain.AccountKind
import com.example.domain.Budget
import com.example.domain.CompoundingMode
import com.example.domain.DateUtils
import com.example.domain.DayDate
import com.example.domain.FinanceEngine
import com.example.domain.Holding
import com.example.domain.HoldingContribution
import com.example.domain.HoldingMode
import com.example.domain.HoldingType
import com.example.domain.MoneyMath
import com.example.domain.MonthKey
import com.example.domain.PinSecurity
import com.example.domain.RepeatFrequency
import com.example.domain.ScheduledItem
import com.example.domain.Transaction
import com.example.domain.TransactionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FinanceEngineTest {

    @Test
    fun balances_includeTransfersPerAccount_andExcludeTransfersFromCashFlow() {
        val checking = Account("acc_chk", "Checking", AccountKind.CHECKING, "landmark", "sky", 100_000L, "USD", null, "2026-01-01T00:00:00", 0)
        val savings = Account("acc_sav", "Savings", AccountKind.SAVINGS, "piggy_bank", "purple", 50_000L, "USD", null, "2026-01-01T00:00:00", 1)
        val credit = Account("acc_cc", "Credit", AccountKind.CREDIT, "credit_card", "pink", 0L, "USD", null, "2026-01-01T00:00:00", 2)

        val txs = listOf(
            Transaction("t1", TransactionKind.INCOME, 200_000L, checking.id, null, "cat_salary", null, null, "2026-10-01T12:00:00", "2026-10-01", null, "", ""),
            Transaction("t2", TransactionKind.EXPENSE, 25_000L, credit.id, null, "cat_food", null, null, "2026-10-05T12:00:00", "2026-10-05", null, "", ""),
            Transaction("t3", TransactionKind.TRANSFER, 40_000L, checking.id, savings.id, null, null, null, "2026-10-10T12:00:00", "2026-10-10", null, "", "")
        )

        assertEquals(260_000L, FinanceEngine.computeAccountBalance(checking, txs))
        assertEquals(90_000L, FinanceEngine.computeAccountBalance(savings, txs))
        assertEquals(-25_000L, FinanceEngine.computeAccountBalance(credit, txs))

        val cashFlow = FinanceEngine.computeMonthCashFlow(txs, MonthKey(2026, 10))
        assertEquals(200_000L, cashFlow.incomeCents)
        assertEquals(25_000L, cashFlow.expenseCents)
        assertEquals(175_000L, cashFlow.netCents)
    }

    @Test
    fun periodBoundaries_monthStartInclusive_nextMonthStartExclusive() {
        val oct = MonthKey(2026, 10)
        assertTrue(DateUtils.isInMonth("2026-10-01", oct))
        assertTrue(DateUtils.isInMonth("2026-10-31", oct))
        assertFalse(DateUtils.isInMonth("2026-09-30", oct))
        assertFalse(DateUtils.isInMonth("2026-11-01", oct))
    }

    @Test
    fun zeroPreviousPeriod_returnsNullInsteadOfNaNOrInfinity() {
        assertNull(MoneyMath.percentDelta(current = 50_000L, previous = 0L))
        assertEquals("$0.00", MoneyMath.formatCents(-0L, "USD"))
    }

    @Test
    fun budgetPace_andOverBudgetGaugeCopyNeverCelebrate() {
        val budget = Budget("b1", "all", 100_000L, "monthly", false, "2026-10-01T00:00:00")
        val overTx = listOf(
            Transaction("t1", TransactionKind.EXPENSE, 143_200L, "acc1", null, "cat_food", null, null, "2026-10-15T12:00:00", "2026-10-15", null, "", "")
        )
        val status = FinanceEngine.computeBudgetStatus(budget, overTx, MonthKey(2026, 10), DayDate(2026, 10, 15))
        assertTrue(status.isOverBudget)
        assertEquals(1.0f, status.fillRatio, 0.0001f)
        assertEquals(-43_200L, status.savedCents)
        assertTrue(status.gaugeSubtitle("USD").contains("over budget"))
        assertFalse(status.gaugeHeadline("USD").contains("Well done"))

        // Ideal pace on day 15 of 31 days = 100,000 * 15 / 31 = 48,387 cents
        assertTrue(abs(status.idealPaceCents - 48_387L) <= 2L)
    }

    @Test
    fun yieldHolding_depositWithdrawalAccrual_andMaturityStop() {
        val holding = Holding(
            id = "h1",
            name = "CD",
            type = HoldingType.CD,
            mode = HoldingMode.YIELD,
            apy = 0.05,
            startDayKey = "2025-01-01",
            maturityDayKey = "2026-01-01",
            contributions = listOf(
                HoldingContribution("c1", "h1", "2025-01-01", 1_000_000L), // $10,000
                HoldingContribution("c2", "h1", "2025-07-02", -200_000L) // -$2,000 halfway through year
            ),
            icon = "certificate",
            color = "purple"
        )

        val atMaturity = FinanceEngine.computeHoldingValuation(holding, DayDate(2026, 1, 1))
        val afterMaturity = FinanceEngine.computeHoldingValuation(holding, DayDate(2026, 6, 1))
        assertTrue(atMaturity.isMatured)
        assertTrue(afterMaturity.isMatured)
        assertEquals(atMaturity.currentValueCents, afterMaturity.currentValueCents)
        assertEquals(0L, afterMaturity.estimatedYearlyYieldCents)
        assertEquals(800_000L, atMaturity.totalPutInCents)
        assertTrue(atMaturity.growthCents > 40_000L)
    }

    @Test
    fun growthCalculator_referenceCase_matchesSpecWithinFewDollars() {
        // P = $5,000, PMT = $100, a = 4.5%, Y = 10, monthly compounding
        // Expected: ~$22,813 total, $17,000 put in, ~$5,813 earned
        val result = FinanceEngine.calculateGrowth(
            startingCents = 500_000L,
            monthlyContributionCents = 10_000L,
            annualYieldRate = 0.045,
            years = 10,
            compounding = CompoundingMode.MONTHLY
        )
        assertEquals(1_700_000L, result.totalPutInCents)
        val totalDollars = result.finalValueCents / 100.0
        val earnedDollars = result.totalEarnedCents / 100.0
        assertTrue("Expected ~$22,813, got $totalDollars", abs(totalDollars - 22_813.0) <= 5.0)
        assertTrue("Expected ~$5,813, got $earnedDollars", abs(earnedDollars - 5_813.0) <= 5.0)
        assertEquals(121, result.points.size)
    }

    @Test
    fun growthCalculator_zeroRate_andYearlyCompounding() {
        val zeroRate = FinanceEngine.calculateGrowth(
            startingCents = 500_000L,
            monthlyContributionCents = 10_000L,
            annualYieldRate = 0.0,
            years = 10,
            compounding = CompoundingMode.MONTHLY
        )
        assertEquals(1_700_000L, zeroRate.finalValueCents)
        assertEquals(0L, zeroRate.totalEarnedCents)

        val yearly = FinanceEngine.calculateGrowth(
            startingCents = 500_000L,
            monthlyContributionCents = 10_000L,
            annualYieldRate = 0.045,
            years = 10,
            compounding = CompoundingMode.YEARLY
        )
        assertEquals(1_700_000L, yearly.totalPutInCents)
        assertTrue(yearly.totalEarnedCents > 500_000L)
    }

    @Test
    fun scheduledClamping_jan31ToFeb28AndLeapFeb29_andIdempotentAutoPost() {
        val jan31NonLeap = DayDate(2025, 1, 31)
        assertEquals(DayDate(2025, 2, 28), FinanceEngine.advanceScheduleDate(jan31NonLeap, RepeatFrequency.MONTHLY))

        val jan31Leap = DayDate(2024, 1, 31)
        assertEquals(DayDate(2024, 2, 29), FinanceEngine.advanceScheduleDate(jan31Leap, RepeatFrequency.MONTHLY))

        val acc = Account("acc1", "Checking", AccountKind.CHECKING, "landmark", "sky", 500_000L, "USD", null, "", 0)
        val item = ScheduledItem(
            id = "sch_1",
            name = "Rent",
            kind = TransactionKind.EXPENSE,
            amountCents = 120_000L,
            accountId = acc.id,
            categoryId = "cat_home",
            repeat = RepeatFrequency.MONTHLY,
            nextDueDayKey = "2026-09-01",
            paused = false,
            autoPost = true
        )
        var counter = 0
        val outcome1 = FinanceEngine.processScheduledCatchUp(
            items = listOf(item),
            existingScheduledKeys = emptySet(),
            accountsById = mapOf(acc.id to acc),
            currencyCode = "USD",
            today = DayDate(2026, 10, 15),
            nowIso = "2026-10-15T09:00:00",
            idGenerator = { "id_${counter++}" }
        )
        // Should catch up 2026-09-01 and 2026-10-01, advancing nextDue to 2026-11-01
        assertEquals(2, outcome1.transactionsToCreate.size)
        assertEquals("2026-09-01", outcome1.transactionsToCreate[0].dayKey)
        assertEquals("2026-10-01", outcome1.transactionsToCreate[1].dayKey)
        assertEquals("2026-11-01", outcome1.updatedScheduledItems.first().nextDueDayKey)

        // Idempotency: re-running with existing keys creates zero duplicate transactions
        val existingKeys = outcome1.transactionsToCreate.map { "${it.scheduledId}|${it.dayKey}" }.toSet()
        val outcome2 = FinanceEngine.processScheduledCatchUp(
            items = listOf(item),
            existingScheduledKeys = existingKeys,
            accountsById = mapOf(acc.id to acc),
            currencyCode = "USD",
            today = DayDate(2026, 10, 15),
            nowIso = "2026-10-15T09:00:00",
            idGenerator = { "id_${counter++}" }
        )
        assertEquals(0, outcome2.transactionsToCreate.size)
    }

    @Test
    fun pinSecurity_saltedPbkdf2AndLockoutEscalation() {
        val salt = PinSecurity.generateSaltHex()
        val hash = PinSecurity.hashPin("4821", salt)
        assertTrue(PinSecurity.verifyPin("4821", salt, hash))
        assertFalse(PinSecurity.verifyPin("0000", salt, hash))
        assertEquals(0L, PinSecurity.lockoutDurationMsForFailures(4))
        assertEquals(30_000L, PinSecurity.lockoutDurationMsForFailures(5))
        assertEquals(60_000L, PinSecurity.lockoutDurationMsForFailures(6))
        assertEquals(300_000L, PinSecurity.lockoutDurationMsForFailures(7))
    }
}
