package lei.greg.utils

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

// schedules tasks x amount of ingame ticks in the future
// usage: Scheduler.schedule(20) { println("meow") }
object Scheduler {
    private val tasks = mutableListOf<Pair<Int, () -> Unit>>()

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { tick() }
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
