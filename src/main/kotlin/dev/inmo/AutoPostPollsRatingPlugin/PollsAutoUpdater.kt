package dev.inmo.AutoPostPollsRatingPlugin

import dev.inmo.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import dev.inmo.AutoPostPollsRatingPlugin.database.PollsRatingsTable
import dev.inmo.AutoPostTelegramBot.flowFilter
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import kotlinx.coroutines.*

internal fun CoroutineScope.enableRatingUpdatesByPolls(
    transformers: List<VariantTransformer>,
    pollsRatingsTable: PollsRatingsTable,
    pollsMessagesTable: PollsMessagesTable
): Job = launch {
    flowFilter.pollFlow.collectWithErrors {
        val poll = it.data
        pollsMessagesTable[poll.id] ?.let { postId ->
            pollsRatingsTable[postId] = poll.options.sumBy { option ->
                var pollRating = 0F
                for (transformer in transformers) {
                    pollRating = transformer(option.text) ?: continue
                    break
                }
                pollRating * option.votes
            }
        }
    }
}
