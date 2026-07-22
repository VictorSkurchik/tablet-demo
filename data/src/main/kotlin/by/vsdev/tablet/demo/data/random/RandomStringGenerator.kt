package by.vsdev.tablet.demo.data.random

import kotlin.random.Random

internal class RandomStringGenerator(
    private val random: Random,
) {
    fun generate(): String {
        val length = random.nextInt(MIN_LENGTH, MAX_LENGTH + 1)
        return buildString(length) {
            repeat(length) { append(CHARACTERS.random(random)) }
        }
    }

    private companion object {
        const val MIN_LENGTH = 5
        const val MAX_LENGTH = 10
        const val CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }
}
