package com.Lino.battlePass.gui;

import com.Lino.battlePass.BattlePass;
import com.Lino.battlePass.utils.GradientColorParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardGui extends BaseGui {

    private final Player player;

    public LeaderboardGui(BattlePass plugin, Player player) {
        super(plugin, plugin.getMessageManager().getMessage("gui.leaderboard"), 54);
        this.player = player;
    }

    @Override
    protected BaseHolder.MenuContext getMenuContext() {
        return BaseHolder.MenuContext.menu(BaseHolder.MenuType.LEADERBOARD);
    }

    public void open() {
        Inventory gui = createInventory();

        setupTitleItem(gui);
        setupLeaderboard(gui);

        gui.setItem(49, createBackButton());

        player.openInventory(gui);
    }

    private void setupTitleItem(Inventory gui) {
        ItemStack titleItem = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = titleItem.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().getMessage("items.leaderboard-title.name"));

        List<String> lore = new ArrayList<>();
        for (String line : plugin.getMessageManager().getMessagesConfig().getStringList("items.leaderboard-title.lore")) {
            String processedLine = line.replace("%season_time%", plugin.getMissionManager().getTimeUntilSeasonEnd());
            lore.add(GradientColorParser.parse(processedLine));
        }

        meta.setLore(lore);
        titleItem.setItemMeta(meta);
        gui.setItem(4, titleItem);
    }

    private void setupLeaderboard(Inventory gui) {
        new LeaderboardResults(plugin, player, gui).request();
    }

}