@file:Suppress("DuplicatedCode", "UNCHECKED_CAST")

package day5

import java.util.concurrent.atomic.*

// This implementation never stores `null` values.
class AtomicArrayWithCAS2<E : Any>(size: Int, initialValue: E) {
    private val array = AtomicReferenceArray<Any?>(size)

    init {
        // Fill array with the initial value.
        for (i in 0 until size) {
            array[i] = initialValue
        }
    }

    fun get(index: Int): E? {
        while (true) {
            val cur = array[index]
            if (cur is CAS2Descriptor<*>) {
                cur.complete()
            } else {
                return cur as E?
            }
        }
    }

    fun cas2(
        index1: Int, expected1: E?, update1: E?,
        index2: Int, expected2: E?, update2: E?
    ): Boolean {
        require(index1 != index2) { "The indices should be different" }
        // Order indices to avoid symmetric interference.
        var firstIndex = index1
        var firstExpected = expected1
        var firstUpdate = update1
        var secondIndex = index2
        var secondExpected = expected2
        var secondUpdate = update2
        if (firstIndex > secondIndex) {
            firstIndex = index2
            firstExpected = expected2
            firstUpdate = update2
            secondIndex = index1
            secondExpected = expected1
            secondUpdate = update1
        }

        while (true) {
            val curFirst = array[firstIndex]
            if (curFirst is CAS2Descriptor<*>) {
                curFirst.complete()
                continue
            }
            val curSecond = array[secondIndex]
            if (curSecond is CAS2Descriptor<*>) {
                curSecond.complete()
                continue
            }
            if (curFirst != firstExpected || curSecond != secondExpected) return false

            val descriptor = CAS2Descriptor(
                array,
                firstIndex, firstExpected, firstUpdate,
                secondIndex, secondExpected, secondUpdate
            )
            if (array.compareAndSet(firstIndex, curFirst, descriptor)) {
                descriptor.complete()
                return descriptor.isSuccessful()
            }
        }
    }

    private class CAS2Descriptor<E : Any>(
        private val array: AtomicReferenceArray<Any?>,
        private val index1: Int,
        private val expected1: E?,
        private val update1: E?,
        private val index2: Int,
        private val expected2: E?,
        private val update2: E?
    ) {
        private val state = AtomicReference(State.UNDECIDED)

        fun isSuccessful(): Boolean = state.get() == State.SUCCESS

        fun complete() {
            if (state.get() == State.UNDECIDED) {
                val decision = tryAcquireSecond()
                state.compareAndSet(State.UNDECIDED, decision)
            }
            if (state.get() == State.SUCCESS) {
                array.compareAndSet(index2, this, update2)
                array.compareAndSet(index1, this, update1)
            } else {
                array.compareAndSet(index1, this, expected1)
            }
        }

        private fun tryAcquireSecond(): State {
            while (true) {
                val cur = array[index2]
                when (cur) {
                    is CAS2Descriptor<*> -> {
                        if (cur === this) return State.SUCCESS
                        cur.complete()
                    }

                    else -> {
                        if (cur != expected2) return State.FAILED
                        if (array.compareAndSet(index2, cur, this)) return State.SUCCESS
                    }
                }
            }
        }

        private enum class State { UNDECIDED, SUCCESS, FAILED }
    }
}
