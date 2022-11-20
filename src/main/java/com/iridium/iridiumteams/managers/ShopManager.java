package com.iridium.iridiumteams.managers;

import com.iridium.iridiumcore.utils.InventoryUtils;
import com.iridium.iridiumcore.utils.StringUtils;
import com.iridium.iridiumteams.IridiumTeams;
import com.iridium.iridiumteams.configs.Shop;
import com.iridium.iridiumteams.database.IridiumUser;
import com.iridium.iridiumteams.database.Team;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ShopManager<T extends Team, U extends IridiumUser<T>> {
    private final IridiumTeams<T, U> iridiumTeams;

    public ShopManager(IridiumTeams<T, U> iridiumTeams) {
        this.iridiumTeams = iridiumTeams;
    }

    public void buy(Player player, Shop.ShopItem shopItem, int amount) {
        double moneyCost = calculateCost(amount, shopItem.defaultAmount, shopItem.buyCost.money);
        if (!canPurchase(moneyCost, player)) {
            player.sendMessage(StringUtils.color(iridiumTeams.getMessages().cannotAfford
                    .replace("%prefix%", iridiumTeams.getConfiguration().prefix)));
            iridiumTeams.getShop().failSound.play(player);
            return;
        }

        iridiumTeams.getEconomy().withdrawPlayer(player, moneyCost);

        if (shopItem.command == null) {
            // Add item to the player Inventory
            if (!iridiumTeams.getShop().dropItemWhenFull && !InventoryUtils.hasEmptySlot(player.getInventory())) {
                player.sendMessage(StringUtils.color(iridiumTeams.getMessages().inventoryFull
                        .replace("%prefix%", iridiumTeams.getConfiguration().prefix)
                ));
                return;
            }

            ItemStack itemStack = shopItem.type.parseItem();
            itemStack.setAmount(amount);

            for (ItemStack dropItem : player.getInventory().addItem(itemStack).values()) {
                player.getWorld().dropItem(player.getEyeLocation(), dropItem);
            }
        } else {
            // Run the command
            String command = shopItem.command
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(amount));

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        iridiumTeams.getShop().successSound.play(player);

        player.sendMessage(StringUtils.color(iridiumTeams.getMessages().successfullyBought
                        .replace("%prefix%", iridiumTeams.getConfiguration().prefix)
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%item%", StringUtils.color(shopItem.name))
                        .replace("%vault_cost%", String.valueOf(moneyCost))
                //TODO add bank placeholders
        ));
    }

    public void sell(Player player, Shop.ShopItem shopItem, int amount) {
        double moneyReward = calculateCost(amount, shopItem.defaultAmount, shopItem.sellCost.money);
        int inventoryAmount = InventoryUtils.getAmount(player.getInventory(), shopItem.type);
        if (inventoryAmount == 0) {
            player.sendMessage(StringUtils.color(iridiumTeams.getMessages().noSuchItem
                    .replace("%prefix%", iridiumTeams.getConfiguration().prefix)
            ));
            iridiumTeams.getShop().failSound.play(player);
            return;
        }

        int soldAmount = Math.min(inventoryAmount, amount);

        InventoryUtils.removeAmount(player.getInventory(), shopItem.type, soldAmount);

        iridiumTeams.getEconomy().depositPlayer(player, moneyReward);

        player.sendMessage(StringUtils.color(iridiumTeams.getMessages().successfullySold
                .replace("%prefix%", iridiumTeams.getConfiguration().prefix)
                .replace("%amount%", String.valueOf(amount))
                .replace("%item%", StringUtils.color(shopItem.name))
                .replace("%vault_reward%", String.valueOf(moneyReward))
        ));
        iridiumTeams.getShop().successSound.play(player);
    }

    private boolean canPurchase(double money, Player player) {
        Economy economy = iridiumTeams.getEconomy();
        //TODO add bank costs

        return money == 0 || economy != null && economy.getBalance(player) >= money;
    }

    private double calculateCost(int amount, int defaultAmount, double defaultPrice) {
        double costPerItem = defaultPrice / defaultAmount;
        return round(costPerItem * amount, 2);
    }

    private double round(double value, int places) {
        BigDecimal bigDecimal = BigDecimal.valueOf(value);
        bigDecimal = bigDecimal.setScale(places, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }
}