package com.example.solesense.ui.navigation

import androidx.annotation.DrawableRes
import com.example.solesense.R

sealed class Screen(val route: String, val title: String, @DrawableRes val icon: Int) {
    object Home : Screen("home", "Trang chủ", R.drawable.ic_menu_home)
    object Explore : Screen("explore", "Khám phá", R.drawable.ic_menu_explore)
    object Cart : Screen("cart", "Giỏ hàng", R.drawable.ic_menu_cart)
    object Chat : Screen("chat", "Chat", R.drawable.ic_menu_message)
    object Profile : Screen("profile", "Tôi", R.drawable.ic_menu_info_1)
}