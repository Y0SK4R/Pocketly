package com.example.data

import com.example.domain.Account
import com.example.domain.AccountKind
import com.example.domain.ActivityItem
import com.example.domain.ActivityKind
import com.example.domain.Budget
import com.example.domain.Category
import com.example.domain.CategoryKind
import com.example.domain.DateUtils
import com.example.domain.Holding
import com.example.domain.HoldingContribution
import com.example.domain.HoldingMode
import com.example.domain.HoldingType
import com.example.domain.HoldingValuePoint
import com.example.domain.MoneyMath
import com.example.domain.RepeatFrequency
import com.example.domain.ScheduledItem
import com.example.domain.Transaction
import com.example.domain.TransactionKind
import org.json.JSONArray
import org.json.JSONObject

data class FullBackupSnapshot(
    val currency: String,
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val scheduledItems: List<ScheduledItem>,
    val holdings: List<Holding>,
    val activityItems: List<ActivityItem>
)

object BackupCodec {

    fun exportTransactionsCsv(
        transactions: List<Transaction>,
        accountsById: Map<String, Account>,
        categoriesById: Map<String, Category>,
        currencyCode: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Kind,Category,Account,ToAccount,Merchant,Note,Amount,Currency")
        for (tx in transactions.sortedByDescending { it.dayKey }) {
            val signed = when (tx.kind) {
                TransactionKind.EXPENSE -> -tx.amountCents
                TransactionKind.INCOME -> tx.amountCents
                TransactionKind.TRANSFER -> tx.amountCents
            }
            val amountStr = String.format(java.util.Locale.US, "%.2f", signed / 100.0)
            val cols = listOf(
                tx.dayKey,
                tx.kind.key,
                tx.categoryId?.let { categoriesById[it]?.name } ?: "",
                accountsById[tx.accountId]?.name ?: tx.accountId,
                tx.toAccountId?.let { accountsById[it]?.name } ?: "",
                tx.merchant ?: "",
                tx.note ?: "",
                amountStr,
                currencyCode
            )
            sb.appendLine(cols.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (needsQuotes) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    fun exportFullBackupJson(snapshot: FullBackupSnapshot): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("app", "Pocketly")
        root.put("exportedAt", DateUtils.nowIso())
        root.put("currency", snapshot.currency)

        root.put("accounts", JSONArray().apply {
            snapshot.accounts.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("kind", a.kind.key)
                    put("icon", a.icon)
                    put("color", a.color)
                    put("openingBalanceCents", a.openingBalanceCents)
                    put("currency", a.currency)
                    put("archivedAt", a.archivedAt)
                    put("createdAt", a.createdAt)
                    put("sortOrder", a.sortOrder)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            snapshot.categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("icon", c.icon)
                    put("color", c.color)
                    put("kind", c.kind.key)
                    put("archivedAt", c.archivedAt)
                    put("sortOrder", c.sortOrder)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            snapshot.transactions.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("kind", t.kind.key)
                    put("amountCents", t.amountCents)
                    put("accountId", t.accountId)
                    put("toAccountId", t.toAccountId)
                    put("categoryId", t.categoryId)
                    put("note", t.note)
                    put("merchant", t.merchant)
                    put("dateIso", t.dateIso)
                    put("dayKey", t.dayKey)
                    put("scheduledId", t.scheduledId)
                    put("createdAt", t.createdAt)
                    put("updatedAt", t.updatedAt)
                })
            }
        })

        root.put("budgets", JSONArray().apply {
            snapshot.budgets.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("categoryId", b.categoryId)
                    put("amountCents", b.amountCents)
                    put("period", b.period)
                    put("createdAt", b.createdAt)
                })
            }
        })

        root.put("scheduledItems", JSONArray().apply {
            snapshot.scheduledItems.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("kind", s.kind.key)
                    put("amountCents", s.amountCents)
                    put("accountId", s.accountId)
                    put("categoryId", s.categoryId)
                    put("repeat", s.repeat.key)
                    put("nextDueDayKey", s.nextDueDayKey)
                    put("paused", s.paused)
                    put("autoPost", s.autoPost)
                })
            }
        })

        root.put("holdings", JSONArray().apply {
            snapshot.holdings.forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id)
                    put("name", h.name)
                    put("type", h.type.key)
                    put("mode", h.mode.key)
                    if (h.apy != null) put("apy", h.apy)
                    put("startDayKey", h.startDayKey)
                    put("maturityDayKey", h.maturityDayKey)
                    put("icon", h.icon)
                    put("color", h.color)
                    put("archivedAt", h.archivedAt)
                    put("contributions", JSONArray().apply {
                        h.contributions.forEach { c ->
                            put(JSONObject().apply {
                                put("id", c.id)
                                put("dayKey", c.dayKey)
                                put("amountCents", c.amountCents)
                            })
                        }
                    })
                    put("valueHistory", JSONArray().apply {
                        h.valueHistory.forEach { v ->
                            put(JSONObject().apply {
                                put("id", v.id)
                                put("dayKey", v.dayKey)
                                put("valueCents", v.valueCents)
                            })
                        }
                    })
                })
            }
        })

        return root.toString(2)
    }

    fun parseFullBackupJson(rawJson: String): Result<FullBackupSnapshot> = runCatching {
        val root = JSONObject(rawJson)
        require(root.optString("app") == "Pocketly" || root.has("accounts")) {
            "This file isn't a Pocketly backup."
        }
        val currency = root.optString("currency", "USD").ifBlank { "USD" }
        val nowIso = DateUtils.nowIso()

        val accounts = mutableListOf<Account>()
        val accArr = root.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accArr.length()) {
            val o = accArr.getJSONObject(i)
            accounts.add(
                Account(
                    id = o.getString("id"),
                    name = o.getString("name").trim().ifEmpty { "Account" },
                    kind = AccountKind.fromKey(o.optString("kind", "wallet")),
                    icon = o.optString("icon", "wallet"),
                    color = o.optString("color", "mint"),
                    openingBalanceCents = o.optLong("openingBalanceCents", 0L),
                    currency = o.optString("currency", currency),
                    archivedAt = o.optString("archivedAt").takeIf { it.isNotBlank() && it != "null" },
                    createdAt = o.optString("createdAt", nowIso),
                    sortOrder = o.optInt("sortOrder", i),
                    isDemo = false
                )
            )
        }

        val categories = mutableListOf<Category>()
        val catArr = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            categories.add(
                Category(
                    id = o.getString("id"),
                    name = o.getString("name").trim().ifEmpty { "Category" },
                    icon = o.optString("icon", "dots"),
                    color = o.optString("color", "slate"),
                    kind = CategoryKind.fromKey(o.optString("kind", "expense")),
                    archivedAt = o.optString("archivedAt").takeIf { it.isNotBlank() && it != "null" },
                    sortOrder = o.optInt("sortOrder", i),
                    isDemo = false
                )
            )
        }

        val transactions = mutableListOf<Transaction>()
        val txArr = root.optJSONArray("transactions") ?: JSONArray()
        for (i in 0 until txArr.length()) {
            val o = txArr.getJSONObject(i)
            val amt = o.optLong("amountCents", 0L).coerceIn(0L, MoneyMath.MAX_AMOUNT_CENTS)
            if (amt <= 0L) continue
            transactions.add(
                Transaction(
                    id = o.getString("id"),
                    kind = TransactionKind.fromKey(o.optString("kind", "expense")),
                    amountCents = amt,
                    accountId = o.getString("accountId"),
                    toAccountId = o.optString("toAccountId").takeIf { it.isNotBlank() && it != "null" },
                    categoryId = o.optString("categoryId").takeIf { it.isNotBlank() && it != "null" },
                    note = o.optString("note").takeIf { it.isNotBlank() && it != "null" },
                    merchant = o.optString("merchant").takeIf { it.isNotBlank() && it != "null" },
                    dateIso = o.optString("dateIso", nowIso),
                    dayKey = o.getString("dayKey"),
                    scheduledId = o.optString("scheduledId").takeIf { it.isNotBlank() && it != "null" },
                    createdAt = o.optString("createdAt", nowIso),
                    updatedAt = o.optString("updatedAt", nowIso),
                    isDemo = false
                )
            )
        }

        val budgets = mutableListOf<Budget>()
        val bArr = root.optJSONArray("budgets") ?: JSONArray()
        for (i in 0 until bArr.length()) {
            val o = bArr.getJSONObject(i)
            budgets.add(
                Budget(
                    id = o.getString("id"),
                    categoryId = o.getString("categoryId"),
                    amountCents = o.optLong("amountCents", 0L).coerceAtLeast(0L),
                    period = o.optString("period", "monthly"),
                    rollover = false,
                    createdAt = o.optString("createdAt", nowIso),
                    isDemo = false
                )
            )
        }

        val scheduledItems = mutableListOf<ScheduledItem>()
        val sArr = root.optJSONArray("scheduledItems") ?: JSONArray()
        for (i in 0 until sArr.length()) {
            val o = sArr.getJSONObject(i)
            scheduledItems.add(
                ScheduledItem(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    kind = TransactionKind.fromKey(o.optString("kind", "expense")),
                    amountCents = o.optLong("amountCents", 0L).coerceAtLeast(0L),
                    accountId = o.getString("accountId"),
                    categoryId = o.optString("categoryId").takeIf { it.isNotBlank() && it != "null" },
                    repeat = RepeatFrequency.fromKey(o.optString("repeat", "monthly")),
                    nextDueDayKey = o.getString("nextDueDayKey"),
                    paused = o.optBoolean("paused", false),
                    autoPost = o.optBoolean("autoPost", false),
                    isDemo = false
                )
            )
        }

        val holdings = mutableListOf<Holding>()
        val hArr = root.optJSONArray("holdings") ?: JSONArray()
        for (i in 0 until hArr.length()) {
            val o = hArr.getJSONObject(i)
            val hId = o.getString("id")
            val contribs = mutableListOf<HoldingContribution>()
            val cArr = o.optJSONArray("contributions") ?: JSONArray()
            for (j in 0 until cArr.length()) {
                val co = cArr.getJSONObject(j)
                contribs.add(HoldingContribution(co.getString("id"), hId, co.getString("dayKey"), co.getLong("amountCents")))
            }
            val vals = mutableListOf<HoldingValuePoint>()
            val vArr = o.optJSONArray("valueHistory") ?: JSONArray()
            for (j in 0 until vArr.length()) {
                val vo = vArr.getJSONObject(j)
                vals.add(HoldingValuePoint(vo.getString("id"), hId, vo.getString("dayKey"), vo.getLong("valueCents")))
            }
            holdings.add(
                Holding(
                    id = hId,
                    name = o.getString("name"),
                    type = HoldingType.fromKey(o.optString("type", "savings")),
                    mode = HoldingMode.fromKey(o.optString("mode", "yield")),
                    apy = if (o.has("apy") && !o.isNull("apy")) o.getDouble("apy") else null,
                    startDayKey = o.optString("startDayKey").takeIf { it.isNotBlank() && it != "null" },
                    maturityDayKey = o.optString("maturityDayKey").takeIf { it.isNotBlank() && it != "null" },
                    contributions = contribs,
                    valueHistory = vals,
                    icon = o.optString("icon", "piggy_bank"),
                    color = o.optString("color", "orange"),
                    archivedAt = o.optString("archivedAt").takeIf { it.isNotBlank() && it != "null" },
                    isDemo = false
                )
            )
        }

        FullBackupSnapshot(
            currency = currency,
            accounts = accounts,
            categories = categories.ifEmpty { DemoSeeder.defaultCategories(nowIso) },
            transactions = transactions,
            budgets = budgets,
            scheduledItems = scheduledItems,
            holdings = holdings,
            activityItems = emptyList()
        )
    }
}
