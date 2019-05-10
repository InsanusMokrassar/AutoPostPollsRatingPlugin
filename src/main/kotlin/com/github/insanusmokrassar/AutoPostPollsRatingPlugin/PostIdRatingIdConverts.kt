package com.github.insanusmokrassar.AutoPostPollsRatingPlugin

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingId

internal inline val RatingId.asPostId: PostId
    get() = toInt()

internal inline val PostId.asRatingId: RatingId
    get() = toLong()
