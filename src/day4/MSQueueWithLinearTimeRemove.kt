package day4

import java.util.concurrent.atomic.*

class MSQueueWithLinearTimeRemove<E> : QueueWithRemove<E> {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val dummy = Node(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        val newNode = Node(element)
        while (true) {
            val curTail = tail.get()
            val next = curTail.next.get()
            if (next == null) {
                if (curTail.next.compareAndSet(null, newNode)) {
                    tail.compareAndSet(curTail, newNode)
                    return
                }
            } else {
                tail.compareAndSet(curTail, next)
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val curHead = head.get()
            val next = curHead.next.get() ?: return null
            if (head.compareAndSet(curHead, next)) {
                if (next.markExtractedOrRemoved()) return next.element
            }
        }
    }

    override fun remove(element: E): Boolean {
        var node = head.get()
        while (true) {
            val next = node.next.get()
            if (next == null) return false
            node = next
            if (node.element == element && node.remove()) return true
        }
    }

    override fun validate() {
        check(tail.get().next.get() == null) { "tail.next must be null" }
        var node = head.get()
        while (true) {
            if (node !== head.get() && node !== tail.get()) {
                check(!node.extractedOrRemoved) {
                    "Removed node with element ${node.element} found in the middle"
                }
            }
            node = node.next.get() ?: break
        }
    }

    private inner class Node(var element: E?) {
        val next = AtomicReference<Node?>(null)
        private val removed = AtomicBoolean(false)
        val extractedOrRemoved get() = removed.get()
        fun markExtractedOrRemoved() = removed.compareAndSet(false, true)

        fun remove(): Boolean {
            if (!markExtractedOrRemoved()) return false
            var prev = head.get()
            while (true) {
                val nxt = prev.next.get() ?: return false
                if (nxt == this) {
                    prev.next.compareAndSet(this, this.next.get())
                    return true
                }
                prev = nxt
            }
        return true
        }
    }
}
