package com.blackboxpro.common.runtime.scheduler

object RuntimeTickScheduler {

    private data class ScheduledTask(
        var remainingTicks: Int,
        val task: () -> Unit
    )

    private val tasks = ArrayDeque<ScheduledTask>()

    fun tick() {
        val ready = mutableListOf<() -> Unit>()
        val iterator = tasks.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            entry.remainingTicks--
            if (entry.remainingTicks <= 0) {
                iterator.remove()
                ready.add(entry.task)
            }
        }
        ready.forEach { task ->
            try {
                task()
            } catch (e: Exception) {
                System.err.println("[BlackBoxPro] TickScheduler task exception: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun schedule(delayTicks: Int, task: () -> Unit) {
        if (delayTicks <= 0) {
            task()
        } else {
            tasks.addLast(ScheduledTask(delayTicks, task))
        }
    }

    fun clear(): Int {
        val count = tasks.size
        tasks.clear()
        return count
    }
}
