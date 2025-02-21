package org.tems.ttotem.Punishments

import okhttp3.*
import org.json.JSONObject

class DiscordWebhook {
    private val client = OkHttpClient()

    fun sendWebhook(url: String, payload: JSONObject) {
        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload.toString())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute()
    }

    fun createEmbed(player: String, type: String, punishment: String = "", ticks: String = "", ping: String = "", popped: String = "", flagged: String = ""): JSONObject {
        val embed = JSONObject()
        val config = plugin.config.getConfigurationSection("webhook.embed_template.$type")

        embed.put("title", config.getString("title"))
        embed.put("description", config.getString("description")
            .replace("{player}", player)
            .replace("{punishment}", punishment)
            .replace("{ticks}", ticks)
            .replace("{ping}", ping)
            .replace("{popped}", popped)
            .replace("{flagged}", flagged)
        )
        embed.put("color", config.getInt("color"))

        val payload = JSONObject()
        payload.put("embeds", listOf(embed))

        return payload
    }
}
