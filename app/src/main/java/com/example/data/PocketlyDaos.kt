package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PocketlyDao {

    // Accounts
    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAccountsSnapshot(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    // Categories
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getCategoriesSnapshot(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY dayKey DESC, createdAt DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dayKey DESC, createdAt DESC")
    suspend fun getTransactionsSnapshot(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionsIgnoreConflict(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    // Budgets
    @Query("SELECT * FROM budgets ORDER BY createdAt ASC")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getBudgetsSnapshot(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteBudgetByCategory(categoryId: String)

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    // Scheduled items
    @Query("SELECT * FROM scheduled_items ORDER BY nextDueDayKey ASC")
    fun observeScheduledItems(): Flow<List<ScheduledItemEntity>>

    @Query("SELECT * FROM scheduled_items ORDER BY nextDueDayKey ASC")
    suspend fun getScheduledItemsSnapshot(): List<ScheduledItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduledItem(item: ScheduledItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduledItems(items: List<ScheduledItemEntity>)

    @Query("DELETE FROM scheduled_items WHERE id = :id")
    suspend fun deleteScheduledItem(id: String)

    @Query("DELETE FROM scheduled_items")
    suspend fun clearScheduledItems()

    // Holdings
    @Query("SELECT * FROM holdings ORDER BY name ASC")
    fun observeHoldings(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings")
    suspend fun getHoldingsSnapshot(): List<HoldingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHolding(holding: HoldingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHoldings(holdings: List<HoldingEntity>)

    @Query("DELETE FROM holdings WHERE id = :id")
    suspend fun deleteHolding(id: String)

    @Query("DELETE FROM holdings")
    suspend fun clearHoldings()

    // Holding contributions
    @Query("SELECT * FROM holding_contributions ORDER BY dayKey ASC")
    fun observeContributions(): Flow<List<HoldingContributionEntity>>

    @Query("SELECT * FROM holding_contributions ORDER BY dayKey ASC")
    suspend fun getContributionsSnapshot(): List<HoldingContributionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContribution(contribution: HoldingContributionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContributions(contributions: List<HoldingContributionEntity>)

    @Query("DELETE FROM holding_contributions WHERE holdingId = :holdingId")
    suspend fun deleteContributionsForHolding(holdingId: String)

    @Query("DELETE FROM holding_contributions")
    suspend fun clearContributions()

    // Holding value points
    @Query("SELECT * FROM holding_value_points ORDER BY dayKey ASC")
    fun observeValuePoints(): Flow<List<HoldingValuePointEntity>>

    @Query("SELECT * FROM holding_value_points ORDER BY dayKey ASC")
    suspend fun getValuePointsSnapshot(): List<HoldingValuePointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertValuePoint(point: HoldingValuePointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertValuePoints(points: List<HoldingValuePointEntity>)

    @Query("DELETE FROM holding_value_points WHERE holdingId = :holdingId")
    suspend fun deleteValuePointsForHolding(holdingId: String)

    @Query("DELETE FROM holding_value_points")
    suspend fun clearValuePoints()

    // Activity
    @Query("SELECT * FROM activity_items ORDER BY createdAt DESC")
    fun observeActivity(): Flow<List<ActivityItemEntity>>

    @Query("SELECT * FROM activity_items ORDER BY createdAt DESC")
    suspend fun getActivitySnapshot(): List<ActivityItemEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivityIgnoreConflict(item: ActivityItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivitiesIgnoreConflict(items: List<ActivityItemEntity>)

    @Query("UPDATE activity_items SET readAt = :readAt WHERE id = :id")
    suspend fun markActivityRead(id: String, readAt: String)

    @Query("UPDATE activity_items SET readAt = :readAt WHERE readAt IS NULL")
    suspend fun markAllActivityRead(readAt: String)

    @Query("DELETE FROM activity_items")
    suspend fun clearActivity()
}
