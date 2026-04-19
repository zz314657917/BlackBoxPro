package com.blackboxpro.common.action.composite

import com.blackboxpro.common.runtime.LogHandler
import com.blackboxpro.common.runtime.LoggerSupplier
import com.blackboxpro.common.runtime.action.ActionExecutor
import com.blackboxpro.common.runtime.action.ActionResult
import com.blackboxpro.common.runtime.config.RuntimeBlackBoxConfig
import com.blackboxpro.common.runtime.dispatcher.RuntimeCommandDispatcher
import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.google.gson.JsonObject

class BatchAction : ActionExecutor {

    private lateinit var logger: LogHandler

    fun bind(loggerSupplier: LoggerSupplier) {
        this.logger = loggerSupplier.getLogger("BlackBoxPro-Batch")
    }

    override fun execute(params: JsonObject): ActionResult {
        if (!::logger.isInitialized) {
            return ActionResult.fail("BatchAction not initialized: logger not bound")
        }

        val actionsArray = params.getAsJsonArray("actions")
            ?: return ActionResult.fail("Missing required field: actions")

        val maxBatchSize = RuntimeBlackBoxConfig.current.execution.maxBatchSize
        if (actionsArray.size() > maxBatchSize) {
            return ActionResult.fail("Batch too large: ${actionsArray.size()} > $maxBatchSize")
        }

        if (actionsArray.size() == 0) {
            return ActionResult.ok("Empty batch, nothing to do")
        }

        executeBatchStep(actionsArray.map { it.asJsonObject }, 0)
        return ActionResult.ok("Batch started (${actionsArray.size()} actions)")
    }

    private fun executeBatchStep(actions: List<JsonObject>, startIndex: Int) {
        var index = startIndex
        while (index < actions.size) {
            val item = actions[index]
            val action = item.get("action")?.asString
            if (action == null) {
                logger.warn("Batch step {} missing 'action' field, skipping", index)
                index++
                continue
            }
            val actionParams = item.getAsJsonObject("params") ?: JsonObject()

            if (action == "batch") {
                logger.warn("Nested batch is not allowed, skipping step {}", index)
                index++
                continue
            }

            if (action == "wait") {
                val maxDelay = RuntimeBlackBoxConfig.current.execution.maxDelayTicks
                val ticks = (actionParams.get("ticks")?.asInt ?: 0).coerceAtLeast(0).coerceAtMost(maxDelay)
                RuntimeTickScheduler.schedule(ticks) {
                    executeBatchStep(actions, index + 1)
                }
                return
            }

            val executor = RuntimeCommandDispatcher.resolveAction(action)
            if (executor != null) {
                // safety 检查：batch 内子 action 也需遵守 blockedActions 规则
                val safety = RuntimeBlackBoxConfig.current.safety
                if (safety.enabled) {
                    if (action in safety.blockedActions || (safety.allowedActions.isNotEmpty() && action !in safety.allowedActions)) {
                        logger.warn("Batch step {} ({}) blocked by safety config, skipping", index, action)
                        index++
                        continue
                    }
                }
                val result = try {
                    executor.execute(actionParams)
                } catch (e: Exception) {
                    logger.warn("Batch step {} ({}) threw exception: {}", index, action, e.message)
                    ActionResult.fail("Exception: ${e.message}")
                }
                if (!result.success) {
                    logger.warn("Batch step {} ({}) failed: {}", index, action, result.message)
                }
            } else {
                logger.warn("Batch step {} unknown action: {}", index, action)
            }

            index++
        }
    }
}
