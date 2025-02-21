package org.tems.ttotem.Punishments

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import okhttp3.*
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.tems.ttotem.Tetotem
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class FlagsPunish(private var plugin: Tetotem) {
    var lastTotem = HashMap<UUID, Long>()
    var totemsPopped = HashMap<UUID, Int>()
    var totemsFlagged = HashMap<UUID, Int>()
    private var flags = HashMap<UUID, Int>()
    private val client = OkHttpClient()

    fun addTotem(player: Player) {
        if (totemsPopped[player.uniqueId] == null) {
            totemsPopped[player.uniqueId] = 1
        } else {
            totemsPopped[player.uniqueId] = totemsPopped[player.uniqueId]!! + 1
        }
    }

    fun addFlag(player: Player) {
        if (totemsFlagged[player.uniqueId] == null) {
            totemsFlagged[player.uniqueId] = 1
        } else {
            totemsFlagged[player.uniqueId] = totemsFlagged[player.uniqueId]!! + 1
        }
    }

    private fun sendWebhook(url: String, payload: String) {
        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload)
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute()
    }

    private fun createEmbed(player: Player, type: String, punishment: String = "", ticks: String = "", ping: Int = -99): JSONObject {
        val embed = JSONObject()
        val config = plugin.config.getConfigurationSection("webhook.embed_template.$type")

        embed.put("title", config.getString("title"))
        embed.put("description", config.getString("description")
            .replace("{player}", player.name)
            .replace("{punishment}", punishment)
            .replace("{ticks}", ticks)
            .replace("{ping}", ping.toString())
            .replace("{popped}", totemsPopped[player.uniqueId].toString())
            .replace("{flagged}", totemsFlagged[player.uniqueId].toString())
        )
        embed.put("color", config.getInt("color"))

        val payload = JSONObject()
        payload.put("embeds", listOf(embed))

        return payload
    }

    fun logFlag(player: HumanEntity, ticks: String) {
        if (player is Player) addFlag(player)
        if (plugin.config.getString("adminMessage") != null) {
            val mm = MiniMessage.miniMessage()
            val message = plugin.config.getString("adminMessage").toString().replace("%player%", player.name)
                .replace("%ticks%", ticks)
            val parsed: Component = mm.deserialize(message)
            for (lp in plugin.server.onlinePlayers) {
                if (lp.hasPermission("ttotem.admin")) {
                    lp.sendMessage(parsed)
                }
            }
        }
        if (plugin.config.getString("consoleMessage") != null) {
            val p = plugin.server.getPlayer(player.uniqueId)
            var ping = -99
            if (p != null) ping = p.ping

            val consoleMessage = plugin.config.getString("consoleMessage").toString()
                .replace("%player%", player.name)
                .replace("%ticks%", ticks)
                .replace("%ping%", ping.toString())
                .replace("%popped%", totemsPopped[player.uniqueId].toString())
                .replace("%flagged%", totemsFlagged[player.uniqueId].toString())

            plugin.logger.info(consoleMessage)

            if (player is Player) {
                val embed = createEmbed(player, "alert", ticks = ticks, ping = ping)
                sendWebhook(plugin.config.getString("webhook.alert_url"), embed.toString())
            }
        }
    }

    fun punishAndRegisterFlag(player: HumanEntity) {
        val tps = plugin.config.getDouble("minimumTpsForFlagsCount")
        if (plugin.server.tps[0] < tps) return
        if (flags[player.uniqueId] == null) {
            flags[player.uniqueId] = 1
        } else {
            flags[player.uniqueId] = flags[player.uniqueId]!! + 1
        }

        val commands = plugin.config.getList("punishments") ?: return
        if (flags[player.uniqueId]!! < plugin.config.getInt("punishAfterFlags")) return

        lastTotem = HashMap<UUID, Long>()
        flags = HashMap<UUID, Int>()
        for (command in commands) {
            plugin.server.dispatchCommand(
                plugin.server.consoleSender,
                command.toString().replace("%player%", player.name)
            )
            plugin.i = 0
        }

        if (player is Player) {
            val embed = createEmbed(player, "punishment", "kick")
            sendWebhook(plugin.config.getString("webhook.punishment_url"), embed.toString())
        }
    }
}
