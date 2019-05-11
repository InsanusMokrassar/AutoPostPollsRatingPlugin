package com.github.insanusmokrassar.AutoPostPollsRatingPlugin

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingPlugin
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.DeleteMessage
import com.github.insanusmokrassar.TelegramBotAPI.requests.StopPoll
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeUnsafe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect

internal fun CoroutineScope.enableAutoremovingOnPostRemoved(
    ratingPlugin: MutableRatingPlugin,
    postsTable: PostsTable
): Job = launch {
    postsTable.postRemovedChannel.asFlow().collect {
        ratingPlugin.getPostRatings(it).forEach {
            ratingPlugin.deleteRating(it.first)
        }
    }
}

internal fun CoroutineScope.enableAutoremovingOfPolls(
    executor: RequestsExecutor,
    chatId: ChatId,
    ratingPlugin: RatingPlugin,
    pollsMessagesTable: PollsMessagesTable,
    closeIfUnsuccess: Boolean = true
): Job = launch {
    ratingPlugin.allocateRatingRemovedFlow().collect {
        val pollInfo = pollsMessagesTable[it.first.asPostId] ?: return@collect

        executor.executeUnsafe(
            DeleteMessage(
                chatId,
                pollInfo.first
            )
        ) ?: if (closeIfUnsuccess) {
            executor.executeUnsafe(
                StopPoll(
                    chatId,
                    pollInfo.first
                )
            )
        }
    }
}
