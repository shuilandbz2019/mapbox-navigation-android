package com.mapbox.navigation.instrumentation_tests.utils.assertions

import com.mapbox.navigation.instrumentation_tests.utils.assertions.ExpectedValue.Optional
import com.mapbox.navigation.instrumentation_tests.utils.assertions.ExpectedValue.Required
import org.junit.Assert

abstract class ValueTransitionAssertion<T>(
    expectedBlock: ValueTransitionAssertion<T>.() -> Unit
) {
    private val actualValues = mutableListOf<T>()
    private val expectedValues = mutableListOf<ExpectedValue<T>>()

    init {
        expectedBlock()
    }

    fun assert() {
        var minIndex = 0
        actualValues.forEach { state ->
            val minRequiredIndex =
                expectedValues.indexOfFirst { it is Required && expectedValues.indexOf(it) > minIndex }
            val availableStates =
                expectedValues.filterIndexed { index, _ ->
                    if (minRequiredIndex >= 0) {
                        index in minIndex..minRequiredIndex
                    } else {
                        index >= minIndex
                    }
                }
            when (val matching = availableStates.find { it.value == state }) {
                is Required -> {
                    minIndex = expectedValues.indexOf(matching)
                }
                is Optional -> {
                    // do nothing
                }
                null -> Assert.fail(
                    """$state shouldn't be returned right now. Expected are:
                    |$availableStates
                """.trimMargin()
                )
            }
        }
        Assert.assertEquals(
            """Not all of the expected states were matched. Below were not matched:
               |${expectedValues.drop(minIndex).filterIsInstance<Required<T>>()}
            """.trimMargin(),
            expectedValues.indexOfLast { it is Required }.let { if (it == -1) 0 else it },
            minIndex
        )
    }

    fun requiredState(value: T) {
        expectedValues.add(Required(value))
    }

    fun optionalState(value: T) {
        expectedValues.add(Optional(value))
    }

    protected fun onNewValue(value: T) = actualValues.add(value)
}

private sealed class ExpectedValue<T>(val value: T) {
    class Required<T>(value: T) : ExpectedValue<T>(value)

    class Optional<T>(value: T) : ExpectedValue<T>(value)

    override fun toString(): String {
        return "${this::class.simpleName}(value=$value)"
    }
}
