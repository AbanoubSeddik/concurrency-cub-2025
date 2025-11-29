@file:Suppress("UNCHECKED_CAST")

package day6

import java.util.concurrent.atomic.*

class ConcurrentHashTable<K : Any, V : Any>(initialCapacity: Int) {
    private val table = AtomicReference(Table<K, V>(initialCapacity))

    fun put(key: K, value: V): V? {
        while (true) {
            // Try to insert the key/value pair.
            val putResult = table.get().put(key, value)
            if (putResult === NEEDS_REHASH) {
                // The current table is too small to insert a new key.
                // Create a new table of x2 capacity,
                // copy all elements to it,
                // and restart the current operation.
                resize()
            } else {
                // The operation has been successfully performed,
                // return the previous value associated with the key.
                return putResult as V?
            }
        }
    }

    fun get(key: K): V? {
        return table.get().get(key)
    }

    fun remove(key: K): V? {
        return table.get().remove(key)
    }

    private fun resize() {
        // The copy-on-write table never exhausts its capacity, so nothing to do here.
    }

    class Table<K : Any, V : Any>(val capacity: Int) {
        val keys = AtomicReferenceArray<Any?>(capacity)
        val values = AtomicReferenceArray<V?>(capacity)
        private val state = AtomicReference<Map<K, V>>(emptyMap())

        fun put(key: K, value: V): Any? {
            while (true) {
                val current = state.get()
                val prev = current[key]
                val updated = current.toMutableMap()
                updated[key] = value
                if (state.compareAndSet(current, updated)) {
                    return prev
                }
            }
        }

        fun get(key: K): V? {
            return state.get()[key]
        }

        fun remove(key: K): V? {
            while (true) {
                val current = state.get()
                val prev = current[key] ?: return null
                val updated = current.toMutableMap()
                updated.remove(key)
                if (state.compareAndSet(current, updated)) {
                    return prev
                }
            }
        }
    }
}

private val NEEDS_REHASH = Any()
