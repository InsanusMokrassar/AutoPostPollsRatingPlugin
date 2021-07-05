package dev.inmo.AutoPostPollsRatingPlugin.commands

import dev.inmo.AutoPostTelegramBot.base.database.tables.PostsBaseInfoTable
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.MutableRatingPlugin
import dev.inmo.AutoPostTelegramBot.utils.commands.buildCommandFlow
import dev.inmo.AutoPostTelegramBot.utils.flow.collectWithErrors
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
