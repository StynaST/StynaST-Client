package net.styna.stynastmc.clearspam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.styna.stynastmc.clearspam.config.ClearSpamConfig;

/**
 * Port of ClearSpam by Ruukas: collapses repeated chat messages into one line with a counter.
 */
public final class ClearSpam {
    private static final String PREFIX = ".clearspam";
    private static final Pattern COUNTER_SUFFIX = Pattern.compile(" \\[x\\d+]$");

    private static final Map<String, SpamCount> messages = new HashMap<>();
    private static final List<String> ignored = new ArrayList<>();
    private static boolean disabled = false;

    private ClearSpam() {
    }

    public static void syncConfig() {
        disabled = !ClearSpamConfig.ENABLED.get();
        ignored.clear();
        ignored.addAll(ClearSpamConfig.BLOCKED.get());
        messages.clear();
    }

    public static Component onChat(Component original, boolean overlay) {
        if (disabled || overlay) {
            return original;
        }

        Component message = original;
        String text = original.getString();
        for (String ig : ignored) {
            if (text.contains(ig)) {
                message = Component.literal("Ignored")
                        .withStyle(style -> style.withColor(ChatFormatting.RED).withHoverEvent(new HoverEvent.ShowText(original)));
                break;
            }
        }

        String key = stripCounter(message.getString());
        long now = System.currentTimeMillis();
        SpamCount spam = messages.get(key);
        if (spam == null) {
            messages.put(key, new SpamCount(now));
            return message;
        }

        if (now - spam.time < ClearSpamConfig.SPAM_WINDOW_SECONDS.get() * 1000L) {
            spam.counter++;
            message = message.copy().append(counter(spam.counter));
        } else {
            spam.counter = 1;
        }
        spam.time = now;

        ChatComponent chat = Minecraft.getInstance().gui.hud.getChat();
        if (chat.allMessages.removeIf(line -> stripCounter(line.content().getString()).equals(key))) {
            chat.refreshTrimmedMessages();
        }
        return message;
    }

    public static void onDisconnect() {
        messages.clear();
    }

    public static boolean onSendChat(String mes) {
        if (!mes.startsWith(PREFIX)) {
            return false;
        }

        Minecraft.getInstance().gui.hud.getChat().addRecentChat(mes);
        switch (mes) {
            case PREFIX + " toggle" -> {
                disabled = !disabled;
                sendMessage("ClearSpam has been " + (disabled ? "disabled." : "enabled."));
            }
            case PREFIX + " on" -> {
                disabled = false;
                sendMessage("ClearSpam has been enabled.");
            }
            case PREFIX + " off" -> {
                disabled = true;
                sendMessage("ClearSpam has been disabled.");
            }
            case PREFIX + " reset" -> {
                messages.clear();
                sendMessage("ClearSpam counter has been reset.");
            }
            case PREFIX, PREFIX + " help" -> sendCommandList();
            default -> {
                String blockPrefix = PREFIX + " block ";
                if (mes.startsWith(blockPrefix) && mes.length() > blockPrefix.length()) {
                    String ignoreStr = mes.substring(blockPrefix.length());
                    ignored.add(ignoreStr);
                    sendMessage("Messages containing \"" + ignoreStr + "\" will no longer reach chat.");
                } else {
                    sendMessage(Component.literal("That ClearSpam command was not recognized.").withStyle(ChatFormatting.RED));
                    sendMessage("Type \".clearspam help\" for a list of commands.");
                }
            }
        }
        return true;
    }

    private static void sendCommandList() {
        sendMessage(Component.literal("-=-=-=-=-= ClearSpam Help =-=-=-=-=-").withStyle(ChatFormatting.DARK_GREEN));
        sendCommandHelp(PREFIX + " toggle", "Toggles ClearSpam");
        sendCommandHelp(PREFIX + " on/off", "Turns ClearSpam on or off");
        sendCommandHelp(PREFIX + " reset", "Resets the ClearSpam counters");
        sendCommandHelp(PREFIX + " block <string>", "Block messages that contain the string");
    }

    private static void sendCommandHelp(String command, String description) {
        sendMessage(Component.literal(command).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" - " + description).withStyle(ChatFormatting.GRAY)));
    }

    private static void sendMessage(String message) {
        sendMessage(Component.literal(message).withStyle(ChatFormatting.GOLD));
    }

    private static void sendMessage(Component message) {
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
    }

    private static Component counter(int count) {
        return Component.literal(" [").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("x").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.RED))
                .append(Component.literal("]").withStyle(ChatFormatting.GOLD));
    }

    private static String stripCounter(String text) {
        return COUNTER_SUFFIX.matcher(text).replaceFirst("");
    }

    private static final class SpamCount {
        private int counter = 1;
        private long time;

        private SpamCount(long time) {
            this.time = time;
        }
    }
}
