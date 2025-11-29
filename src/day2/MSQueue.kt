package day2

import java.util.concurrent.atomic.AtomicReference

class MSQueue<E> : Queue<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy = Node<E>(null)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        val newNode = Node(element)
        while (true) {
            val curTail = tail.get()
            val next = curTail.next.get()
            if (next == null) {
                // Try to link new node at the end
                if (curTail.next.compareAndSet(null, newNode)) {
                    // Try to move tail forward
                    tail.compareAndSet(curTail, newNode)
                    return
                }
            } else {
                // Tail is lagging behind, help it move
                tail.compareAndSet(curTail, next)
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val curHead = head.get()
            val next = curHead.next.get()
            if (next == null) return null
            if (head.compareAndSet(curHead, next))
                return next.element
        }
    }

    override fun validate() {
        check(tail.get().next.get() == null) {
            "At the end of the execution, `tail.next` must be `null`"
        }
        check(head.get().element == null) {
            "At the end of the execution, the dummy node shouldn't store an element"
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = AtomicReference<Node<E>?>(null)
    }
}