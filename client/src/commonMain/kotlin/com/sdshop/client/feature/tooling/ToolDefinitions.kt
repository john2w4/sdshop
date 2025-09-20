package com.sdshop.client.feature.tooling

import com.sdshop.client.data.model.ToolDefinition

object ToolDefinitions {
    val defaultTools = listOf(
        ToolDefinition(
            id = "compare",
            name = "规格对比",
            description = "对比主题内商品的关键参数与指标。",
            systemPrompt = "你是一个善于分析商品规格的专家，输出对比表格。",
            capabilities = listOf("spec_comparison", "table")
        ),
        ToolDefinition(
            id = "price_trend",
            name = "价格趋势",
            description = "分析价格变化、满减活动和组合优惠。",
            systemPrompt = "你擅长分析电商价格趋势，请结合优惠给出建议。",
            capabilities = listOf("price_analysis", "promotion")
        ),
        ToolDefinition(
            id = "review_guardian",
            name = "口碑守护",
            description = "总结评价与问大家热点。",
            systemPrompt = "总结评价、问大家的问题，输出风险与亮点。",
            capabilities = listOf("review_summary", "qa_cluster")
        )
    )
}
