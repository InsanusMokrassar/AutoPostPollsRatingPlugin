package com.github.insanusmokrassar.AutoPostPollsRatingPlugin.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.buildCommandFlow
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.flow.collectWithErrors
import kotlinx.coroutines.*

private val enableRatingCommandRegex = Regex("enableRating")
private val disableRatingCommandRegex = Regex("disableRating")
private val reenableRatingCommandRegex = Regex("reenableRating")

internal fun CoroutineScope.enableEnableRatingCommand(
    ratingPlugin: MutableRatingPlugin,
    postsTable: PostsBaseInfoTable
): Job = launch {
    buildCommandFlow(
        enableRatingCommandRegex
    ).collectWithErrors {
        val repliedMessage = it.replyTo ?: return@collectWithErrors
        val postId = postsTable.findPost(repliedMessage.messageId)
        if (ratingPlugin.getPostRatings(postId).isEmpty()) {
            ratingPlugin.addRatingFor(postId)
        }
    }
}

internal fun CoroutineScope.enableDisableRatingCommand(
    ratingPlugin: MutableRatingPlugin,
    postsTable: PostsBaseInfoTable
): Job = launch {
    buildCommandFlow(
        disableRatingCommandRegex
    ).collectWithErrors {
        val repliedMessage = it.replyTo ?: return@collectWithErrors
        val postId = postsTable.findPost(repliedMessage.messageId)
        ratingPlugin.getPostRatings(postId).forEach { (ratingId, _) ->
            ratingPlugin.deleteRating(ratingId)
        }
    }
}
