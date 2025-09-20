package com.sdshop.client.feature.single

import com.sdshop.client.app.SingleProductKey
import com.sdshop.client.app.SingleProductUiState
import com.sdshop.client.core.container.FlowContext
import com.sdshop.client.core.container.FlowHandler
import com.sdshop.client.core.container.FlowRequest

class SingleProductFlowHandler : FlowHandler<FlowRequest.SingleProductFlow> {
    override suspend fun FlowContext.handle(request: FlowRequest.SingleProductFlow) {
        val singleState = stateStore.getState(SingleProductKey) { SingleProductUiState() }
        singleState.set(
            SingleProductUiState(
                product = request.payload.product,
                inquiries = emptyList(),
                toolRecords = emptyList()
            )
        )
    }
}
