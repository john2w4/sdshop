package com.sdshop.client.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun SdShopApp(environmentFactory: () -> AppEnvironment = { AppEnvironmentFactory.create() }) {
    val environment = remember(environmentFactory) { environmentFactory() }
    AppRoot(environment)
}
