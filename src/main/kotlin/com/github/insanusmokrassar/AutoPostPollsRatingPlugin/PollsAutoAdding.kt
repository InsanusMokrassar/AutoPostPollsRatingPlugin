package com.github.insanusmokrassar.AutoPostPollsRatingPlugin

import com.github.insanusmokrassar.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.requests.send.SendPoll
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatId
import com.github.insanusmokrassar.TelegramBotAPI.types.message.abstracts.ContentMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.message.content.PollContent
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.executeUnsafe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow

internal fun CoroutineScope.enableAutoEnablingOfPolls(
    postsTable: PostsTable,
    ratingPlugin: MutableRatingPlugin
): Job = launch {
    postsTable.postMessageRegisteredChannel.asFlow().collectWithErrors {
        ratingPlugin.addRatingFor(it.first)
    }
}

internal fun CoroutineScope.enableAutoaddingOfPolls(
    executor: RequestsExecutor,
    chatId: ChatId,
    ratingPlugin: RatingPlugin,
    options: List<String>,
    text: String,
    pollsMessagesTable: PollsMessagesTable,
    postsMessagesTable: PostsMessagesTable
): Job = launch {
    val sendPoll = SendPoll(
        chatId,
        text,
        options
    )
    ratingPlugin.allocateRatingAddedFlow().collectWithErrors { (postId, _) ->
        val firstPostMessage = postsMessagesTable.getMessagesOfPost(postId).firstOrNull()

        if (firstPostMessage == null || postId in pollsMessagesTable) {
            return@collectWithErrors
        }

        (executor.executeUnsafe(
            sendPoll.copy(replyToMessageId = firstPostMessage.messageId),
            3
        ) as? ContentMessage<*>) ?.let {
            val pollContent = it.content as? PollContent ?: return@collectWithErrors
            val poll = pollContent.poll

            pollsMessagesTable.registerPoll(postId, it.messageId, poll.id)
        }
    }
}
