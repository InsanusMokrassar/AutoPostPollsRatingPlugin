package com.github.insanusmokrassar.AutoPostPollsRatingPlugin

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsRatingsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.flowFilter
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import kotlinx.coroutines.*

internal fun CoroutineScope.enableRatingUpdatesByPolls(
    adaptedVariants: RatingsVariants,
    pollsRatingsTable: PollsRatingsTable,
    pollsMessagesTable: PollsMessagesTable
): Job = launch {
    flowFilter.pollFlow.collectWithErrors {
        val poll = it.data
        pollsMessagesTable[poll.id] ?.let { postId ->
            pollsRatingsTable[postId] = poll.options.sumBy {
                (adaptedVariants[it.text] ?: 0F) * it.votes
            }
        }
    }
}
