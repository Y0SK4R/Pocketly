package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalAtm
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RoomService
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

object PocketlyIcons {
    private val map: Map<String, ImageVector> = mapOf(
        "utensils" to Icons.Rounded.Restaurant,
        "dining" to Icons.Rounded.RoomService,
        "shopping_bag" to Icons.Rounded.ShoppingBag,
        "shopping_basket" to Icons.Rounded.ShoppingBasket,
        "car" to Icons.Rounded.DirectionsCar,
        "home" to Icons.Rounded.Home,
        "zap" to Icons.Rounded.Bolt,
        "heart_pulse" to Icons.Rounded.Favorite,
        "plane" to Icons.Rounded.Flight,
        "ticket" to Icons.Rounded.ConfirmationNumber,
        "landmark" to Icons.Rounded.AccountBalance,
        "piggy_bank" to Icons.Rounded.Savings,
        "trending_up" to Icons.Rounded.TrendingUp,
        "banknote" to Icons.Rounded.Payments,
        "wallet" to Icons.Rounded.AccountBalanceWallet,
        "credit_card" to Icons.Rounded.CreditCard,
        "cash" to Icons.Rounded.LocalAtm,
        "document" to Icons.Rounded.Description,
        "certificate" to Icons.Rounded.WorkspacePremium,
        "phone" to Icons.Rounded.PhoneIphone,
        "play" to Icons.Rounded.PlayCircle,
        "gym" to Icons.Rounded.FitnessCenter,
        "dots" to Icons.Rounded.MoreHoriz
    )

    val selectableKeys: List<String> = listOf(
        "utensils", "shopping_bag", "dining", "car", "home", "zap",
        "heart_pulse", "ticket", "plane", "wallet", "landmark", "credit_card",
        "piggy_bank", "trending_up", "banknote", "document", "certificate", "gym"
    )

    fun forKey(key: String): ImageVector = map[key.lowercase()] ?: Icons.Rounded.MoreHoriz
}
