package io.whyzervellasskx.gifttreasurespro.configuration

import dev.rollczi.litecommands.invalidusage.InvalidUsage
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.MiniMessageComponent
import io.github.blackbaroness.boilerplate.kotlinx.serialization.type.asMiniMessageComponent
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class MessagesConfiguration(
    val errors: Errors = Errors(),
    val common: Commons = Commons(),

) {

    @Serializable
    data class Commons(
        val reload: String = "<green>Плагин <plugin> перезапущен за <duration>",
        val withDraw: String = "<green>Вы сняли деньги <amount> с банка",
    )

    @Serializable
    data class Errors(
        val generic: @Contextual MiniMessageComponent = "<red>Произошла ошибка! Сообщите администратору!".asMiniMessageComponent,
        val noPermission: String = "<red>У вас недостаточно прав (требуется <permission>).",

        val invalidUsageHeader: String = "<red>[<cause>: <input>]</red> <gray>Использование:</gray>",
        val invalidUsageEntry: String = "<gray><schematic>",
        val invalidUsageCauses: Map<InvalidUsage.Cause, String> = mapOf(
            InvalidUsage.Cause.INVALID_ARGUMENT to "Неправильный аргумент",
            InvalidUsage.Cause.MISSING_ARGUMENT to "Аргумент отсутствует",
            InvalidUsage.Cause.UNKNOWN_COMMAND to "Неизвестная команда",
            InvalidUsage.Cause.MISSING_PART_OF_ARGUMENT to "Отсутствует часть аргумента",
            InvalidUsage.Cause.TOO_MANY_ARGUMENTS to "Слишком много аргументов"
        ),

        val noEnoughMoney: String = "Недостаточно денег (есть: <bank>) а вы пытались снять <amount>",
        val noNextLevel: String = "Нету некст левела",
    )
}
