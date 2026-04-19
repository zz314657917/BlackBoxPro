package com.blackboxpro.common.runtime.dispatcher

import com.blackboxpro.common.runtime.LogHandler
import com.blackboxpro.common.runtime.action.ActionExecutor

class RuntimeActionRegistry(
    private val logger: LogHandler
) {
    private val mutableExecutors = mutableMapOf<String, ActionExecutor>()
    @Volatile
    private var executors: Map<String, ActionExecutor> = emptyMap()
    private var frozen = false

    fun register(actionId: String, executor: ActionExecutor) {
        check(!frozen) { "ActionRegistry is frozen, cannot register new actions" }
        if (mutableExecutors.containsKey(actionId)) {
            logger.warn("Overriding executor for action: {}", actionId)
        }
        mutableExecutors[actionId] = executor
        logger.debug("Registered action: {}", actionId)
    }

    fun find(actionId: String): ActionExecutor? = executors[actionId]

    fun size(): Int = executors.size

    /**
     * 第三方 mod 注册自定义 Action 的公开 API。
     * 可在 BBP 初始化完成（frozen）后调用，注册后立即生效。
     */
    @Synchronized
    fun registerExternal(actionId: String, executor: ActionExecutor) {
        if (mutableExecutors.containsKey(actionId)) {
            logger.warn("Overriding executor for action: {}", actionId)
        }
        mutableExecutors[actionId] = executor
        executors = mutableExecutors.toMap()
        logger.info("External action registered: {}", actionId)
    }

    fun ensureNotInitialized() {
        check(!frozen) { "ActionRegistry already initialized" }
    }

    fun freeze() {
        executors = mutableExecutors.toMap()
        frozen = true
        logger.info("All actions registered. Total: {}", executors.size)
    }

    fun freezeAndValidate(expectedActionIds: Set<String>) {
        executors = mutableExecutors.toMap()
        frozen = true

        val actual = executors.keys
        val missing = expectedActionIds - actual
        val extra = actual - expectedActionIds

        if (missing.isNotEmpty()) {
            logger.warn("Action catalog mismatch, missing executors: {}", missing.joinToString(", "))
        }
        if (extra.isNotEmpty()) {
            logger.warn("Action catalog mismatch, untracked executors: {}", extra.joinToString(", "))
        }

        logger.info("All actions registered. Total: {}", executors.size)
    }
}
