package com.example.blockblast

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun localizedString(resId: Int): String {
    val locale = LocalLocale.current
    val context = LocalContext.current

    val configuration = android.content.res.Configuration(context.resources.configuration)
    configuration.setLocale(locale)

    val localizedContext = context.createConfigurationContext(configuration)

    return localizedContext.resources.getString(resId)
}