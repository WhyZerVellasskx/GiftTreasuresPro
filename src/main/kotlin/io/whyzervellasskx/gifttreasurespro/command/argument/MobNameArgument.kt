package io.whyzervellasskx.gifttreasurespro.command.argument

import dev.rollczi.litecommands.argument.Argument
import dev.rollczi.litecommands.argument.parser.ParseResult
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver
import dev.rollczi.litecommands.invocation.Invocation
import dev.rollczi.litecommands.suggestion.SuggestionContext
import dev.rollczi.litecommands.suggestion.SuggestionResult
import io.whyzervellasskx.gifttreasurespro.model.MobNameArgumentProvider
import io.whyzervellasskx.gifttreasurespro.service.KamlConfigurationService
import jakarta.inject.Inject
import org.bukkit.command.CommandSender

class MobNameArgument @Inject constructor(
    private val kamlConfigurationService: KamlConfigurationService,
) : ArgumentResolver<CommandSender, MobNameArgumentProvider>() {

    private val config get() = kamlConfigurationService.config.eggs

    override fun parse(
        invocation: Invocation<CommandSender>,
        context: Argument<MobNameArgumentProvider>,
        argument: String
    ): ParseResult<MobNameArgumentProvider> {
        return ParseResult.success(MobNameArgumentProvider(argument))
    }

    override fun suggest(
        invocation: Invocation<CommandSender>,
        argument: Argument<MobNameArgumentProvider>,
        context: SuggestionContext
    ): SuggestionResult = SuggestionResult.of(config.keys)
}
