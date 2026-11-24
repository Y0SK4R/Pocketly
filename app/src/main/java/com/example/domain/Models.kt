package com.example.domain

enum class AccountKind(val key: String, val label: String) {
    WALLET("wallet", "Wallet"),
    CHECKING("checking", "Checking"),
    CREDIT("credit", "Credit card"),
    SAVINGS("savings", "Savings"),
    CASH("cash", "Cash");

    companion object {
        fun fromKey(key: String): AccountKind = entries.firstOrNull { it.key == key } ?: WALLET
    }
}

enum class CategoryKind(val key: String) {
    EXPENSE("expense"),
    INCOME("income");

    companion object {
        fun fromKey(key: String): CategoryKind = entries.firstOrNull { it.key == key } ?: EXPENSE
    }
}

enum class TransactionKind(val key: String, val label: String) {
    EXPENSE("expense", "Expense"),
    INCOME("income", "Income"),
    TRANSFER("transfer", "Transfer");

    companion object {
        fun fromKey(key: String): TransactionKind = entries.firstOrNull { it.key == key } ?: EXPENSE
    }
}

enum class RepeatFrequency(val key: String, val label: String) {
    WEEKLY("weekly", "weekly"),
    MONTHLY("monthly", "monthly"),
    YEARLY("yearly", "yearly"),
    ONCE("once", "once");

    companion object {
        fun fromKey(key: String): RepeatFrequency = entries.firstOrNull { it.key == key } ?: MONTHLY
    }
}

enum class HoldingType(val key: String, val label: String) {
    SAVINGS("savings", "High-yield savings"),
    TBILL("tbill", "Treasury bills"),
    CD("cd", "Certificate of deposit"),
    BOND("bond", "Bond"),
    INDEX("index", "Index fund"),
    OTHER("other", "Other");

    companion object {
        fun fromKey(key: String): HoldingType = entries.firstOrNull { it.key == key } ?: SAVINGS
    }
}

enum class HoldingMode(val key: String, val label: String) {
    YIELD("yield", "Auto-grows from a rate"),
    MANUAL("manual", "I'll enter the value");

    companion object {
        fun fromKey(key: String): HoldingMode = entries.firstOrNull { it.key == key } ?: YIELD
    }
}

enum class ActivityKind(val key: String) {
    BUDGET_90("budget90"),
    BUDGET_100("budget100"),
    SCHEDULED_PAID("scheduledPaid"),
    SCHEDULED_DUE("scheduledDue"),
    MILESTONE("milestone");

    companion object {
        fun fromKey(key: String): ActivityKind = entries.firstOrNull { it.key == key } ?: MILESTONE
    }
}

enum class CompoundingMode(val key: String, val label: String) {
    MONTHLY("monthly", "Monthly"),
    YEARLY("yearly", "Yearly")
}

enum class InvestRange(val label: String, val days: Int?) {
    ONE_MONTH("1M", 30),
    SIX_MONTHS("6M", 182),
    ONE_YEAR("1Y", 365),
    ALL("All", null)
}

data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind,
    val icon: String,
    val color: String,
    val openingBalanceCents: Long,
    val currency: String = "USD",
    val archivedAt: String? = null,
    val createdAt: String,
    val sortOrder: Int = 0,
    val isDemo: Boolean = false
)

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val kind: CategoryKind,
    val archivedAt: String? = null,
    val sortOrder: Int = 0,
    val isDemo: Boolean = false
)

data class Transaction(
    val id: String,
    val kind: TransactionKind,
    val amountCents: Long, // always positive; sign comes from kind
    val accountId: String,
    val toAccountId: String? = null, // transfers only
    val categoryId: String? = null,
    val note: String? = null,
    val merchant: String? = null,
    val dateIso: String,
    val dayKey: String, // YYYY-MM-DD
    val scheduledId: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val isDemo: Boolean = false
)

data class Budget(
    val id: String,
    val categoryId: String, // "all" or a Category.id
    val amountCents: Long,
    val period: String = "monthly",
    val rollover: Boolean = false,
    val createdAt: String,
    val isDemo: Boolean = false
)

data class ScheduledItem(
    val id: String,
    val name: String,
    val kind: TransactionKind, // EXPENSE or INCOME
    val amountCents: Long,
    val accountId: String,
    val categoryId: String? = null,
    val repeat: RepeatFrequency,
    val nextDueDayKey: String, // YYYY-MM-DD
    val paused: Boolean = false,
    val autoPost: Boolean = false,
    val isDemo: Boolean = false
)

data class HoldingContribution(
    val id: String,
    val holdingId: String,
    val dayKey: String, // YYYY-MM-DD
    val amountCents: Long // positive = deposit, negative = withdrawal
)

data class HoldingValuePoint(
    val id: String,
    val holdingId: String,
    val dayKey: String, // YYYY-MM-DD
    val valueCents: Long
)

data class Holding(
    val id: String,
    val name: String,
    val type: HoldingType,
    val mode: HoldingMode,
    val apy: Double? = null, // e.g. 0.045 for 4.5%
    val startDayKey: String? = null,
    val maturityDayKey: String? = null,
    val contributions: List<HoldingContribution> = emptyList(),
    val valueHistory: List<HoldingValuePoint> = emptyList(),
    val icon: String,
    val color: String,
    val archivedAt: String? = null,
    val isDemo: Boolean = false
)

data class ActivityItem(
    val id: String,
    val kind: ActivityKind,
    val title: String,
    val body: String,
    val createdAt: String,
    val readAt: String? = null,
    val refId: String? = null,
    val dedupeKey: String? = null
)

data class UserSettings(
    val currency: String = "USD",
    val theme: String = "system", // light | dark | system
    val reduceMotion: Boolean = false,
    val soundEffects: Boolean = true,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val failedPinAttempts: Int = 0,
    val lockoutUntilEpochMs: Long = 0L,
    val biometricEnabled: Boolean = false,
    val lockAfter: String = "30s", // immediate | 30s | 5m
    val onboarded: Boolean = false,
    val demoLoaded: Boolean = false,
    val profileName: String = "Alex",
    val avatarColor: String = "mint",
    val lastAccountId: String? = null,
    val lastCategoryId: String? = null
) {
    val hasPin: Boolean get() = !pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()

    val lockAfterMs: Long
        get() = when (lockAfter) {
            "immediate" -> 0L
            "5m" -> 5 * 60 * 1000L
            else -> 30 * 1000L
        }
}
