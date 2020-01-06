package dev.brighten.anticheat.menu;

import cc.funkemunky.api.utils.Color;
import cc.funkemunky.api.utils.MathUtils;
import cc.funkemunky.api.utils.RunUtils;
import dev.brighten.anticheat.Kauri;
import dev.brighten.anticheat.check.api.Check;
import dev.brighten.anticheat.commands.LogCommand;
import dev.brighten.anticheat.logs.objects.Log;
import dev.brighten.anticheat.utils.ItemBuilder;
import dev.brighten.anticheat.utils.MiscUtils;
import dev.brighten.anticheat.utils.menu.button.Button;
import dev.brighten.anticheat.utils.menu.button.ClickAction;
import dev.brighten.anticheat.utils.menu.preset.button.FillerButton;
import dev.brighten.anticheat.utils.menu.type.impl.ChestMenu;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LogsGUI extends ChestMenu {

    private List<Log> logs = new ArrayList<>();
    private BukkitTask updaterTask;
    private AtomicInteger currentPage = new AtomicInteger(1);
    private OfflinePlayer player;
    private Player shown;

    public LogsGUI(OfflinePlayer player) {
        super("Violations", 6);

        this.player = player;
        updateLogs();

        setTitle(Color.Gray + player.getName() + "'s Logs");

        setButtons(1);
        buildInventory(true);
    }

    public LogsGUI(OfflinePlayer player, int page) {
        super(player.getName() + "'s Logs", 6);

        this.player = player;
        currentPage.set(page);
        updateLogs();
        setButtons(page);

        setTitle(Color.Gray + player.getName() + "'s Logs");

        buildInventory(true);
    }

    private void setButtons(int page, String... filtered) {
        if (page == 0 || getMenuDimension().getSize() <= 0) return;

        if (updaterTask == null || !Bukkit.getScheduler().isCurrentlyRunning(updaterTask.getTaskId())) {
            runUpdater();
        }

        List<Log> filteredLogs = (filtered.length > 0 ? logs.parallelStream()
                .filter(log -> {
                    for (String s : filtered) {
                        if (s.equalsIgnoreCase(log.checkName)) {
                            return true;
                        }
                    }
                    return false;
                }).sequential()
                .sorted(Comparator.comparing(log -> log.timeStamp, Comparator.reverseOrder()))
                .collect(Collectors.toList()) : logs).subList(Math.min((page - 1) * 45, logs.size()),
                Math.min(page * 45, logs.size()));

        for (int i = 0; i < filteredLogs.size(); i++) setItem(i, buttonFromLog(filteredLogs.get(i)));

        //Setting the next page option if possible.
        if (Math.min(page * 45, filteredLogs.size()) < filteredLogs.size()) {
            Button next = new Button(false, new ItemBuilder(Material.BOOK)
                    .amount(1).name(Color.Red + "Next Page &7(&e" + (page + 1) + "&7)").build(),
                    (player, info) -> {
                        if (info.getClickType().isLeftClick()) {
                            setButtons(page + 1, filtered);
                            buildInventory(false);
                            currentPage.set(page + 1);
                        }
                    });
            setItem(50, next);
        }

        val punishments = Kauri.INSTANCE.loggerManager.getPunishments(player.getUniqueId());

        Button getPastebin = new Button(false, new ItemBuilder(Material.SKULL_ITEM).amount(1)
                .durability(3)
                .owner(player.getName())
                .name(Color.Red + player.getName())
                .lore("", "&7Page: &f" + page, "", "&6Punishments&8: &f" + punishments.size(), "",
                        "&e&oLeft click &7&oto view a summary of logs.",
                        (shown == null || shown.hasPermission("kauri.logs.share")
                                ? "&e&oRight Click &7&oto get an &f&ounlisted &7&opastebin link of the logs."
                                : "&c&o(No Permission) &e&o&mRight Click &7&o&mto get an &f&o&munlisted &7&o&mpastebin link of the logs."),
                        (shown == null || shown.hasPermission("kauri.logs.clear")
                                ? "&e&oShift Left Click &7&oto &f&oclear &7&othe logs of " + player.getName()
                                : "&c&o(No Permission) &e&o&mShift Right Click &7&o&mto &f&o&mclear &7&o&mthe logs of " + player.getName())).build(),
                (player, info) -> {
                    if (player.hasPermission("kauri.logs.share")) {
                        if (info.getClickType().isRightClick()) {
                            runFunction(info, "kauri.logs.share", () -> {
                                close(player);
                                player.sendMessage(Color.Green + "Logs: "
                                        + LogCommand.getLogsFromUUID(LogsGUI.this.player.getUniqueId()));
                            });
                        } else if (info.getClickType().isLeftClick() && info.getClickType().isShiftClick()) {
                            runFunction(info, "kauri.logs.clear",
                                    () -> player.performCommand("kauri logs clear " + this.player.getName()));
                        } else if (info.getClickType().isLeftClick()) {
                            getSummary().showMenu(player);
                        }
                    }
                });

        setItem(49, getPastebin);

        //Setting the previous page option if possible.
        if (page > 1) {
            Button back = new Button(false, new ItemBuilder(Material.BOOK)
                    .amount(1).name(Color.Red + "Previous Page &7(&e" + (page - 1) + "&7)").build(),
                    (player, info) -> {
                        if (info.getClickType().isLeftClick()) {
                            setButtons(page - 1, filtered);
                            currentPage.set(page - 1);
                            buildInventory(false);
                        }
                    });
            setItem(48, back);
        }

        if(filtered.length > 0) {
            List<String> lore = new ArrayList<>(Arrays.asList("", "&eFilters:"));

            for (String s : filtered) {
                lore.add("&7- &f" + s);
            }
            Button stopFilter = new Button(false,
                    new ItemBuilder(Material.REDSTONE).amount(1)
                            .name(Color.Red + "Stop Filter").lore(lore).build(),
                    (player, info) -> {
                       setButtons(currentPage.get());
                    });

            setItem(47, stopFilter);
            setItem(51, stopFilter);
        }

        //Setting all empty slots with a filler.
        fill(new FillerButton());
    }

    private ChestMenu getSummary() {
        ChestMenu summary = new ChestMenu(player.getName() + "'s Summary", 6);

        summary.setParent(this);
        Map<String, List<Log>> sortedLogs = new HashMap<>();

        logs.forEach(log -> {
            List<Log> list = sortedLogs.getOrDefault(log.checkName, new ArrayList<>());

            list.add(log);
            sortedLogs.put(log.checkName, list);
        });

        sortedLogs.forEach((check, list) -> {
            Button button = new Button(false,
                    new ItemBuilder(Material.PAPER).amount(1).name(Color.Gold + check)
                            .lore("", "&7Alerts: &f" + list.size(),
                                    "&7Highest VL: &f" +
                                            list.stream()
                                                    .max(Comparator.comparing(log -> log.vl))
                                                    .map(log -> log.vl).orElse(0f),
                                    "&7Type: &f" + Check.getCheckSettings(check).type.name(),
                                    "", "&a&oLeft click &7&oto view vls from this check.").build(),
                    (player, info) -> {
                        setButtons(currentPage.get(), check);
                    });
            summary.addItem(button);
        });
        return summary;
    }

    private void updateLogs() {
        logs = Kauri.INSTANCE.loggerManager.getLogs(player.getUniqueId())
                .stream()
                .sorted(Comparator.comparing(log -> log.timeStamp, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private void runUpdater() {
        updaterTask = RunUtils.taskTimerAsync(() -> {
            if (shown != null
                    && shown.getOpenInventory() != null
                    && shown.getOpenInventory().getTopInventory() != null
                    && shown.getOpenInventory().getTopInventory().getTitle().equals(getTitle())) {
                updateLogs();
                setButtons(currentPage.get());
                buildInventory(false);
            } else cancelTask();
        }, Kauri.INSTANCE, 80L, 40L);
    }

    private void cancelTask() {
        if (updaterTask == null) return;
        updaterTask.cancel();
    }

    @Override
    public void showMenu(Player player) {
        this.shown = player;
        runUpdater();

        super.showMenu(player);
    }

    private void runFunction(ClickAction.InformationPair info, String permission, Runnable function) {
        if (shown == null) return;

        if (shown.hasPermission(permission)) {
            function.run();
        } else {
            String oldName = info.getButton().getStack().getItemMeta().getDisplayName();
            List<String> oldLore = info.getButton().getStack().getItemMeta().getLore();
            ItemMeta meta = info.getButton().getStack().getItemMeta();

            meta.setDisplayName(Color.Red + "No permission");
            meta.setLore(new ArrayList<>());
            info.getButton().getStack().setItemMeta(meta);
            RunUtils.taskLater(() -> {
                if (info.getButton() != null
                        && info.getButton().getStack().getItemMeta()
                        .getDisplayName().equals(Color.Red + "No permission")) {
                    ItemMeta newMeta = info.getButton().getStack().getItemMeta();
                    newMeta.setDisplayName(oldName);
                    newMeta.setLore(oldLore);
                    info.getButton().getStack().setItemMeta(newMeta);
                }
            }, Kauri.INSTANCE, 20L);
        }
    }

    @Override
    public void handleClose(Player player) {
        cancelTask();
    }

    private Button buttonFromLog(Log log) {
        return new Button(false, new ItemBuilder(Material.PAPER)
                .amount(1).name(Color.Gold + log.checkName)
                .lore("", "&eTime&8: &f" + MiscUtils.timeStampToDate(log.timeStamp),
                        "&eData&8: &f" + log.info,
                        "&eViolation Level&8: &f" + MathUtils.round(log.vl, 3),
                        "&ePing&8: &f" + log.ping,
                        "&eTPS&8: &f" + MathUtils.round(log.tps, 2))
                .build(), null);
    }
}