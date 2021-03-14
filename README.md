# AutoPostPollsRatingPlugin

 [ ![Download](https://api.bintray.com/packages/insanusmokrassar/StandardRepository/AutoPostPollsRatingPlugin/images/download.svg) ](https://bintray.com/insanusmokrassar/StandardRepository/AutoPostPollsRatingPlugin/_latestVersion)
 [![Build Status](https://travis-ci.com/InsanusMokrassar/AutoPostPollsRatingPlugin.svg?branch=master)](https://travis-ci.com/InsanusMokrassar/AutoPostPollsRatingPlugin)

This plugin was created for [AutoPostTelegramBot](https://github.com/InsanusMokrassar/AutoPostTelegramBot)

## Why?

The main idea of this plugin is to use default polls in Telegram as a tool for rating the posts.
Any answer in poll have its own rating value and will affect to result rating of post.

## Config

Add as dependencies this plugin to your project:

### Build system

#### Gradle

##### Modern

```groovy
implementation "com.github.insanusmokrassar:AutoPostPollsRatingPlugin:$polls_rating_plugin_version"
```

##### Old

```groovy
compile "com.github.insanusmokrassar:AutoPostPollsRatingPlugin:$polls_rating_plugin_version"
```

#### Maven

```xml
<dependency>
    <groupId>com.github.insanusmokrassar</groupId>
    <artifactId>AutoPostPollsRatingPlugin</artifactId>
    <version>${polls_rating_plugin_version}</version>
</dependency>
```

### Bot config

In plugins section add:

```json
[
    "dev.inmo.AutoPostPollsRatingPlugin.PollRatingPlugin",
    {
        "ratingVariants": {
            "Left for near": 1,
            "Left for last": -1,
            "Drop": -3
        },
        "autoAttach": false,
        "text": "How do you like this plugin?:)",
        "variantsRatings": false
    }
]
```

Options:

* `ratingVariants` (Required) - Key-value object where keys - texts of variants (up to 10) and values as ratings for post
* `text` (Default: `How do you like it?`) - Text for question
* `autoAttach` (Default: `false`) - flag to attach/not attach rating by default (`true`/`false`)
* `variantsRatings` (Default: `false`) - flag to include/exclude rating value to the answer variant (`true` - include.`false` - exclude)
