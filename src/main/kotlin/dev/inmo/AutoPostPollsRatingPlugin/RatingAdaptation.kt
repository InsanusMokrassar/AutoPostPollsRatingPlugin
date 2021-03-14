package dev.inmo.AutoPostPollsRatingPlugin

import dev.inmo.AutoPostTelegramBot.base.plugins.abstractions.Rating

val Rating.forDatabase: Int
    get() = (this * 100).toInt()

val Int.asRating: Rating
    get() = this.toFloat() / 100F
