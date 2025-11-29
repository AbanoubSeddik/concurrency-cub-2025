package day4

import java.util.concurrent.atomic.*

class MSQueueWithConstantTimeRemove<E> : QueueWithRemove<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy = Node<E>(null, null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        val newNode = Node(element, null)
        while (true) {
            val curTail = tail.get()
            val next = curTail.next.get()
            if (next == null) {
                newNode.prev.set(curTail)
                if (curTail.next.compareAndSet(null, newNode)) {
                    tail.compareAndSet(curTail, newNode)
                    return
                }
            } else tail.compareAndSet(curTail, next)
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val curHead = head.get()
            val next = curHead.next.get() ?: return null
            if (head.compareAndSet(curHead, next)) {
                if (next.markExtractedOrRemoved()) {
                    next.prev.set(null)
                    return next.element
                }
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
        check(head.get().prev.get() == null) { "`head.prev` must be null" }
        check(tail.get().next.get() == null) { "tail.next must be null" }
        var node = head.get()
        while (true) {
            if (node !== head.get() && node !== tail.get()) {
                check(!node.extractedOrRemoved) {
                    "Removed node ${node.element} found in middle"
                }
            }
            val next = node.next.get() ?: break
            val nextPrev = next.prev.get()
            check(nextPrev != null && nextPrev == node) {
                "Broken prev/next link"
            }
            node = next
        }
    }

    private class Node<E>(var element: E?, prev: Node<E>?) {
        val next = AtomicReference<Node<E>?>(null)
        val prev = AtomicReference(prev)
        private val removed = AtomicBoolean(false)
        val extractedOrRemoved get() = removed.get()

        fun markExtractedOrRemoved() = removed.compareAndSet(false, true)

        fun remove(): Boolean {
            if (!markExtractedOrRemoved()) return false
            val p = prev.get()
            val n = next.get()
            if (p != null) p.next.compareAndSet(this, n)
            if (n != null) n.prev.compareAndSet(this, p)
            return true
        }
    }
}