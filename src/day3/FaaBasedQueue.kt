package day3

import day2.*
import java.util.concurrent.atomic.*

class FAABasedQueue<E> : Queue<E> {
    private val head: AtomicReference<Segment>
    private val tail: AtomicReference<Segment>
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)

    init {
        val dummy = Segment(0)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    override fun enqueue(element: E) {
        while (true) {
            val curTail = tail.get()
            val i = enqIdx.getAndIncrement()
            val s = findSegment(curTail, i / SEGMENT_SIZE)
            moveTailForward(s)

            if (s.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, element)) {
                return
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (!shouldTryToDequeue()) return null

            val curHead = head.get()
            val i = deqIdx.getAndIncrement()
            val s = findSegment(curHead, i / SEGMENT_SIZE)
            moveHeadForward(s)

            if (s.cells.compareAndSet((i % SEGMENT_SIZE).toInt(), null, BROKEN)) {
                continue
            }
            return s.cells.get((i % SEGMENT_SIZE).toInt()) as E?
        }
    }

    private fun shouldTryToDequeue(): Boolean {
        while (true) {
            val curDeqIdx = deqIdx.get()
            val curEnqIdx = enqIdx.get()
            if (curDeqIdx != deqIdx.get()) continue
            return curDeqIdx < curEnqIdx
        }
    }

    private fun findSegment(start: Segment, id: Long): Segment {
        var cur = start
        while (cur.id < id) {
            val next = cur.next.get()
            if (next != null) {
                cur = next
            } else {
                val newSegment = Segment(cur.id + 1)
                if (cur.next.compareAndSet(null, newSegment)) {
                    cur = newSegment
                } else {
                    cur = cur.next.get()!!
                }
            }
        }
        return cur
    }

    private fun moveTailForward(s: Segment) {
        while (true) {
            val curTail = tail.get()
            if (curTail.id >= s.id) return
            if (tail.compareAndSet(curTail, s)) return
        }
    }

    private fun moveHeadForward(s: Segment) {
        while (true) {
            val curHead = head.get()
            if (curHead.id >= s.id) return
            if (head.compareAndSet(curHead, s)) return
        }
    }
}

private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
}

private val BROKEN = Any()

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2