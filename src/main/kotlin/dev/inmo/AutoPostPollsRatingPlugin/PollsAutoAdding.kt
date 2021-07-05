package dev.inmo.AutoPostPollsRatingPlugin

import dev.inmo.AutoPostPollsRatingPlugin.database.PollsMessagesTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsMessagesInfoTable
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.RatingPlugin
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
import dev.inmo.tgbotapi.bot.RequestsExecutor
import dev.inmo.tgbotapi.extensions.utils.shortcuts.executeUnsafe
import dev.inmo.tgbotapi.requests.send.polls.SendRegularPoll
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.PollContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow

internal fun CoroutineScope.enableAutoEnablingOfPolls(
    postsTable: PostsBaseInfoTable,
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
    postsMessagesTable: PostsMessagesInfoTable
): Job = launch {
    val sendPoll = SendRegularPoll(
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
