package io.whyzervellasskx.gifttreasurespro.service

import io.github.blackbaroness.boilerplate.adventure.parseMiniMessage
import io.github.blackbaroness.boilerplate.base.Service
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.kyori.adventure.text.Component

interface ProgressVisualizerService : Service {
    fun buildColoredProgressBar(
        current: Int,
        max: Int,
        length: Int = 10,
        filledChar: Char = '■',
        emptyChar: Char = '■',
        filledColor: String = "green",
        emptyColor: String = "red"
    ): Component
}

@Singleton
class BaseProgressVisualizerService @Inject constructor(
) : ProgressVisualizerService {

    override fun buildColoredProgressBar(
        current: Int,
        max: Int,
        length: Int,
        filledChar: Char,
        emptyChar: Char,
        filledColor: String,
        emptyColor: String
    ): Component {
        if (max <= 0) return emptyColor.repeat(length).parseMiniMessage()

        val progressFraction = (current.toDouble() / max).coerceIn(0.0, 1.0)
        val filledLength = (length * progressFraction).toInt()
        val emptyLength = length - filledLength

        val filledPart = "<$filledColor>" + filledChar.toString().repeat(filledLength)
        val emptyPart = "<$emptyColor>" + emptyChar.toString().repeat(emptyLength)

        return (filledPart + emptyPart).parseMiniMessage()
    }
}
