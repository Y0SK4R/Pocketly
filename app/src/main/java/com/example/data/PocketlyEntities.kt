package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.Account
import com.example.domain.AccountKind
import com.example.domain.ActivityItem
import com.example.domain.ActivityKind
import com.example.domain.Budget
import com.example.domain.Category
import com.example.domain.CategoryKind
import com.example.domain.Holding
import com.example.domain.HoldingContribution
import com.example.domain.HoldingMode
import com.example.domain.HoldingType
import com.example.domain.HoldingValuePoint
import com.example.domain.RepeatFrequency
import com.example.domain.ScheduledItem
import com.example.domain.Transaction
import com.example.domain.TransactionKind

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val icon: String,
    val color: String,
    val openingBalanceCents: Long,
    val currency: String,
    val archivedAt: String?,
    val createdAt: String,
    val sortOrder: Int,
    val isDemo: Boolean
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val kind: String,
    val archivedAt: String?,
    val sortOrder: Int,
    val isDemo: Boolean
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dayKey"]),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["scheduledId", "dayKey"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val amountCents: Long,
    val accountId: String,
    val toAccountId: String?,
    val categoryId: String?,
    val note: String?,
    val merchant: String?,
    val dateIso: String,
    val dayKey: String,
    val scheduledId: String?,
    val createdAt: String,
    val updatedAt: String,
    val isDemo: Boolean
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val amountCents: Long,
    val period: String,
    val rollover: Boolean,
    val createdAt: String,
    val isDemo: Boolean
)

@Entity(tableName = "scheduled_items")
data class ScheduledItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val amountCents: Long,
    val accountId: String,
    val categoryId: String?,
    val repeat: String,
    val nextDueDayKey: String,
    val paused: Boolean,
    val autoPost: Boolean,
    val isDemo: Boolean
)

@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val mode: String,
    val apy: Double?,
    val startDayKey: String?,
    val maturityDayKey: String?,
    val icon: String,
    val color: String,
    val archivedAt: String?,
    val isDemo: Boolean
)

@Entity(
    tableName = "holding_contributions",
    indices = [Index(value = ["holdingId"])]
)
data class HoldingContributionEntity(
    @PrimaryKey val id: String,
    val holdingId: String,
    val dayKey: String,
    val amountCents: Long
)

@Entity(
    tableName = "holding_value_points",
    indices = [Index(value = ["holdingId"])]
)
data class HoldingValuePointEntity(
    @PrimaryKey val id: String,
    val holdingId: String,
    val dayKey: String,
    val valueCents: Long
)

@Entity(
    tableName = "activity_items",
    indices = [Index(value = ["dedupeKey"], unique = true)]
)
data class ActivityItemEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val readAt: String?,
    val refId: String?,
    val dedupeKey: String?
)

// ─── Domain <-> Entity Mappers ───

fun AccountEntity.toDomain() = Account(
    id, name, AccountKind.fromKey(kind), icon, color, openingBalanceCents, currency, archivedAt, createdAt, sortOrder, isDemo
)

fun Account.toEntity() = AccountEntity(
    id, name, kind.key, icon, color, openingBalanceCents, currency, archivedAt, createdAt, sortOrder, isDemo
)

fun CategoryEntity.toDomain() = Category(
    id, name, icon, color, CategoryKind.fromKey(kind), archivedAt, sortOrder, isDemo
)

fun Category.toEntity() = CategoryEntity(
    id, name, icon, color, kind.key, archivedAt, sortOrder, isDemo
)

fun TransactionEntity.toDomain() = Transaction(
    id, TransactionKind.fromKey(kind), amountCents, accountId, toAccountId, categoryId, note, merchant, dateIso, dayKey, scheduledId, createdAt, updatedAt, isDemo
)

fun Transaction.toEntity() = TransactionEntity(
    id, kind.key, amountCents, accountId, toAccountId, categoryId, note, merchant, dateIso, dayKey, scheduledId, createdAt, updatedAt, isDemo
)

fun BudgetEntity.toDomain() = Budget(id, categoryId, amountCents, period, rollover, createdAt, isDemo)
fun Budget.toEntity() = BudgetEntity(id, categoryId, amountCents, period, rollover, createdAt, isDemo)

fun ScheduledItemEntity.toDomain() = ScheduledItem(
    id, name, TransactionKind.fromKey(kind), amountCents, accountId, categoryId, RepeatFrequency.fromKey(repeat), nextDueDayKey, paused, autoPost, isDemo
)

fun ScheduledItem.toEntity() = ScheduledItemEntity(
    id, name, kind.key, amountCents, accountId, categoryId, repeat.key, nextDueDayKey, paused, autoPost, isDemo
)

fun HoldingContributionEntity.toDomain() = HoldingContribution(id, holdingId, dayKey, amountCents)
fun HoldingContribution.toEntity() = HoldingContributionEntity(id, holdingId, dayKey, amountCents)

fun HoldingValuePointEntity.toDomain() = HoldingValuePoint(id, holdingId, dayKey, valueCents)
fun HoldingValuePoint.toEntity() = HoldingValuePointEntity(id, holdingId, dayKey, valueCents)

fun HoldingEntity.toDomain(
    contributions: List<HoldingContribution>,
    valueHistory: List<HoldingValuePoint>
) = Holding(
    id, name, HoldingType.fromKey(type), HoldingMode.fromKey(mode), apy, startDayKey, maturityDayKey,
    contributions, valueHistory, icon, color, archivedAt, isDemo
)

fun Holding.toEntity() = HoldingEntity(
    id, name, type.key, mode.key, apy, startDayKey, maturityDayKey, icon, color, archivedAt, isDemo
)

fun ActivityItemEntity.toDomain() = ActivityItem(
    id, ActivityKind.fromKey(kind), title, body, createdAt, readAt, refId, dedupeKey
)

fun ActivityItem.toEntity() = ActivityItemEntity(
    id, kind.key, title, body, createdAt, readAt, refId, dedupeKey
)
