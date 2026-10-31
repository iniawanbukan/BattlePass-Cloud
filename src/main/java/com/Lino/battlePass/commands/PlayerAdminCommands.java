package com.Lino.battlePass.commands;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.Lino.battlePass.BattlePass;
import com.Lino.battlePass.models.PlayerData;
import com.Lino.battlePass.utils.ExperienceProgression;
import com.Lino.battlePass.utils.GradientColorParser;

/** Handles player progress, premium access and leaderboard commands. */
final class PlayerAdminCommands {

    private final BattlePass plugin;

    PlayerAdminCommands(BattlePass plugin) {
        this.plugin = plugin;
    }

    boolean handleResetPlayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("battlepass.admin")) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.usage.reset-player"));
            return true;
        }

        Player targetReset = Bukkit.getPlayer(args[1]);
        if (targetReset == null) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.player-not-found"));
            return true;
        }

        PlayerData dataReset = plugin.getPlayerDataManager().getPlayerData(targetReset.getUniqueId());
        if (dataReset == null) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.data-loading"));
            return true;
        }
        dataReset.level = 1;
        dataReset.xp = 0;
        dataReset.claimedFreeRewards.clear();
        dataReset.claimedPremiumRewards.clear();
        dataReset.missionProgress.clear();

        plugin.getPlayerDataManager().markForSave(targetReset.getUniqueId());
        plugin.getMissionManager().clearPlayerActionbars(targetReset.getUniqueId());

        sender.sendMessage(plugin.getMessageManager().getPrefix() +
                plugin.getMessageManager().getMessage("messages.reset-player.sender",
                        "%player%", targetReset.getName()));
        targetReset.sendMessage(plugin.getMessageManager().getPrefix() +
                plugin.getMessageManager().getMessage("messages.reset-player.target"));
        return true;
    }

    boolean handlePremiumCommand(CommandSender sender, String[] args, boolean add) {
        if (!sender.hasPermission("battlepass.admin")) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (args.length < 2) {
            String usage = add ? "messages.usage.add-premium" : "messages.usage.remove-premium";
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage(usage));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.player-not-found"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.data-loading"));
            return true;
        }
        data.hasPremium = add;
        plugin.getPlayerDataManager().markForSave(target.getUniqueId());

        if (add) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.premium.given-sender",
                            "%target%", target.getName()));
            target.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.premium.given-target"));
            target.playSound(target.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.premium.removed-sender",
                            "%target%", target.getName()));
            target.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.premium.removed-target"));
        }

        return true;
    }
    boolean handleAddLevel(CommandSender sender, String[] args, boolean add) {
    if (!sender.hasPermission("battlepass.admin")) {
        sender.sendMessage(plugin.getMessageManager().getPrefix() +
                plugin.getMessageManager().getMessage("messages.no-permission"));
        return true;
    }

    if (args.length < 3) {
        String usage = add ? "messages.usage.add-level" : "messages.usage.remove-level";
        sender.sendMessage(plugin.getMessageManager().getPrefix() +
                plugin.getMessageManager().getMessage(usage));
        return true;
    }

    Player target = Bukkit.getPlayer(args[1]);
    if (target == null) {
        sender.sendMessage(plugin.getMessageManager().getPrefix() +
                plugin.getMessageManager().getMessage("messages.player-not-found"));
        return true;
    }

    try {
        int amount = Integer.parseInt(args[2]);
        if (amount <= 0) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.amount-must-be-positive"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.data-loading"));
            return true;
        }

        int maxLevel = plugin.getRewardManager().getMaxLevel();

        if (add) {
            int oldLevel = data.level;
            int newLevel = Math.min(maxLevel, data.level + amount);
            int levelsAdded = newLevel - oldLevel;

            data.level = newLevel;
            // Kalau sudah max level, XP di-reset ke 0
            if (newLevel >= maxLevel) {
                data.xp = 0;
            }

            plugin.getPlayerDataManager().markForSave(target.getUniqueId());

            // Feedback untuk target
            if (levelsAdded > 0) {
                target.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.level.added-target",
                                "%amount%", String.valueOf(levelsAdded),
                                "%level%", String.valueOf(newLevel)));
                target.playSound(target.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                int available = plugin.getRewardManager().countAvailableRewards(data);
                if (available > 0) {
                    target.sendMessage(plugin.getMessageManager().getPrefix() +
                            plugin.getMessageManager().getMessage("messages.new-rewards"));
                }
            }

            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.level.added-sender",
                            "%amount%", String.valueOf(levelsAdded),
                            "%target%", target.getName(),
                            "%level%", String.valueOf(newLevel)));

            if (levelsAdded < amount) {
                sender.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.level.max-reached",
                                "%target%", target.getName(),
                                "%level%", String.valueOf(newLevel)));
            }
        } else {
            int oldLevel = data.level;
            int newLevel = Math.max(1, data.level - amount);
            int levelsRemoved = oldLevel - newLevel;

            data.level = newLevel;
            if (newLevel <= 1) {
                data.xp = 0;
            }

            plugin.getPlayerDataManager().markForSave(target.getUniqueId());

            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.level.removed-sender",
                            "%amount%", String.valueOf(levelsRemoved),
                            "%target%", target.getName(),
                            "%level%", String.valueOf(newLevel)));
            target.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.level.removed-target",
                            "%amount%", String.valueOf(levelsRemoved),
                            "%level%", String.valueOf(newLevel)));

            if (levelsRemoved < amount) {
                sender.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.level.min-reached",
                                "%target%", target.getName(),
                                "%level%", String.valueOf(newLevel)));
            }
        }

        return true;

    } catch (NumberFormatException e) {
        sender.sendMessage(plugin.getMessageManager().getPrefix() +
                plugin.getMessageManager().getMessage("messages.invalid-amount"));
        return true;
    }
}
    boolean handleXPCommand(CommandSender sender, String[] args, boolean add) {
        if (!sender.hasPermission("battlepass.admin")) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (args.length < 3) {
            String usage = add ? "messages.usage.add-xp" : "messages.usage.remove-xp";
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage(usage));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.player-not-found"));
            return true;
        }

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                sender.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.amount-must-be-positive"));
                return true;
            }

            PlayerData data = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (data == null) {
                sender.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.data-loading"));
                return true;
            }
            int xpPerLevel = plugin.getConfigManager().getXpPerLevel();

            if (add) {
                try {
                    checkLevelUp(target, data, xpPerLevel, amount);
                } finally {
                    if (plugin.getPlayerDataManager().getPlayerData(target.getUniqueId()) == data) {
                        plugin.getPlayerDataManager().markForSave(target.getUniqueId());
                    }
                }
                sender.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.xp.added-sender",
                                "%amount%", String.valueOf(amount), "%target%", target.getName()));
                target.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.xp.added-target",
                                "%amount%", String.valueOf(amount)));
            } else {
                long totalXP = Math.max(0L, ((long) data.level - 1) * xpPerLevel + data.xp - amount);
                int newLevel = (int) Math.min(plugin.getRewardManager().getMaxLevel(), 1 + totalXP / xpPerLevel);
                totalXP -= (long) (newLevel - 1) * xpPerLevel;
                data.level = newLevel;
                data.xp = (int) Math.min(Integer.MAX_VALUE, totalXP);
                plugin.getPlayerDataManager().markForSave(target.getUniqueId());

                sender.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.xp.removed-sender",
                                "%amount%", String.valueOf(amount), "%target%", target.getName()));
                target.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.xp.removed-target",
                                "%amount%", String.valueOf(amount)));
            }

            return true;

        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.invalid-amount"));
            return true;
        }
    }

    boolean handleExcludeTopCommand(CommandSender sender, String[] args, boolean exclude) {
        if (!sender.hasPermission("battlepass.admin")) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (args.length < 2) {
            String usageMessage = plugin.getMessageManager().getMessagesConfig().getString(
                    exclude ? "messages.usage.exclude-top" : "messages.usage.include-top",
                    "&eUsage: /battlepass " + (exclude ? "excludefromtop" : "includetop") + " <player>");
            sender.sendMessage(plugin.getMessageManager().getPrefix() + GradientColorParser.parse(usageMessage));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.player-not-found"));
            return true;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.data-loading"));
            return true;
        }
        data.excludeFromTop = exclude;

        plugin.getPlayerDataManager().markForSave(target.getUniqueId());
        plugin.getPlayerDataManager().savePlayer(target.getUniqueId());

        String statusMessage = plugin.getMessageManager().getMessagesConfig().getString(
                exclude ? "messages.exclude-top.excluded" : "messages.exclude-top.included",
                exclude ? "&a%player% is now excluded from the top leaderboard." : "&a%player% is now included in the top leaderboard.");
        sender.sendMessage(plugin.getMessageManager().getPrefix() +
                GradientColorParser.parse(statusMessage.replace("%player%", target.getName())));

        return true;
    }

    private void checkLevelUp(Player player, PlayerData data, int xpPerLevel, int amount) {
        ExperienceProgression.addWithLimitedFeedback(data, amount, xpPerLevel, plugin.getRewardManager().getMaxLevel(), () -> {
            player.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.level-up",
                            "%level%", String.valueOf(data.level)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            int available = plugin.getRewardManager().countAvailableRewards(data);
            if (available > 0) {
                player.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.new-rewards"));
            }
        });
    }
}
