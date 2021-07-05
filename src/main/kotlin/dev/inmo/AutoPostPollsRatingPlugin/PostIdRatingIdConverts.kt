package dev.inmo.AutoPostPollsRatingPlugin

import dev.inmo.AutoPostTelegramBot.base.models.PostId
import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.RatingId

internal inline val RatingId.asPostId: PostId
    get() = toInt()

internal inline val PostId.asRatingId: RatingId
    get() = toLong()
