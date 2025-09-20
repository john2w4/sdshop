package com.sdshop.client.app

import com.sdshop.client.core.state.StateStore
import kotlinx.coroutines.flow.StateFlow

class AppNavigator(stateStore: StateStore) {
    private val stateHandle = stateStore.getState(AppScaffoldKey) { AppScaffoldState() }
    val uiState: StateFlow<AppScaffoldState> = stateHandle.flow

    fun switchTab(tab: AppTab) {
        stateHandle.update { it.copy(currentTab = tab) }
    }
}
