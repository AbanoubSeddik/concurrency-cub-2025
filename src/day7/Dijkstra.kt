package day7

import java.util.*
import java.util.concurrent.*
import kotlin.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = PriorityBlockingQueue<NodeEntry>(workers, Comparator<NodeEntry> { a, b ->
        Integer.compare(a.distance, b.distance)
    }) // TODO replace me with a multi-queue based PQ!
    q.add(NodeEntry(start, 0))
    val active = AtomicInteger(0)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (true) {
                val entry = q.poll(10, TimeUnit.MILLISECONDS)
                if (entry == null) {
                    if (active.get() == 0 && q.isEmpty()) break
                    continue
                }
                active.incrementAndGet()
                if (entry.node.distance != entry.distance) {
                    active.decrementAndGet()
                    continue
                }
                for (e in entry.node.outgoingEdges) {
                    val newDistance = entry.distance + e.weight
                    while (true) {
                        val cur = e.to.distance
                        if (newDistance >= cur) break
                        if (e.to.casDistance(cur, newDistance)) {
                            q.add(NodeEntry(e.to, newDistance))
                            break
                        }
                    }
                }
                active.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

private data class NodeEntry(val node: Node, val distance: Int)
