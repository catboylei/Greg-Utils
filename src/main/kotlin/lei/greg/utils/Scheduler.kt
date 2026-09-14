package lei.greg.utils

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

object Scheduler {
    private val tasks = mutableListOf<Pair<Int, () -> Unit>>()

    fun register() {
        ServerTickEvents.END_SERVER_TICK.register {
            Scheduler.tick()
        }
    }

    fun schedule(delayTicks: Int, task: () -> Unit) {
        tasks += delayTicks to task
    }

    private fun tick() {
        val iterator = tasks.listIterator()

        while (iterator.hasNext()) {
            val (ticks, task) = iterator.next()

            if (ticks <= 1) {
                task()
                iterator.remove()
            } else {
                iterator.set((ticks - 1) to task)
            }
        }
    }
}
