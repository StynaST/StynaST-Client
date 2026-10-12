package net.styna.stynastmc.clearspam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.styna.stynastmc.clearspam.config.ClearSpamConfig;
import net.styna.stynastmc.mixin.GuiNewChatAccessor;

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

    public static ITextComponent onChat(ITextComponent original) {
        if (disabled) {
            return original;
        }

        ITextComponent message = original;
        String text = original.getUnformattedText();
        for (String ig : ignored) {
            if (text.contains(ig)) {
                message = new TextComponentString("Ignored");
                message.getStyle().setColor(TextFormatting.RED).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, original));
                break;
            }
        }

        String key = stripCounter(message.getUnformattedText());
        long now = System.currentTimeMillis();
        SpamCount spam = messages.get(key);
        if (spam == null) {
            messages.put(key, new SpamCount(now));
            return message;
        }

        if (now - spam.time < ClearSpamConfig.SPAM_WINDOW_SECONDS.get() * 1000L) {
            spam.counter++;
            message = message.createCopy().appendSibling(counter(spam.counter));
        } else {
            spam.counter = 1;
        }
        spam.time = now;

        GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
        List<ChatLine> lines = ((GuiNewChatAccessor) chat).stynastmc$chatLines();
        if (lines.removeIf(line -> stripCounter(line.getChatComponent().getUnformattedText()).equals(key))) {
            chat.refreshChat();
        }
        return message;
    }

    public static void onDisconnect() {
        messages.clear();
    }

    /** True when the message was a .clearspam command and must not be sent. */
    public static boolean onSendChat(String mes) {
        if (!mes.startsWith(PREFIX)) {
            return false;
        }

        Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(mes);
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
                    sendMessage(styled("That ClearSpam command was not recognized.", TextFormatting.RED));
                    sendMessage("Type \".clearspam help\" for a list of commands.");
                }
            }
        }
        return true;
    }

    private static void sendCommandList() {
        sendMessage(styled("-=-=-=-=-= ClearSpam Help =-=-=-=-=-", TextFormatting.DARK_GREEN));
        sendCommandHelp(PREFIX + " toggle", "Toggles ClearSpam");
        sendCommandHelp(PREFIX + " on/off", "Turns ClearSpam on or off");
        sendCommandHelp(PREFIX + " reset", "Resets the ClearSpam counters");
        sendCommandHelp(PREFIX + " block <string>", "Block messages that contain the string");
    }

    private static void sendCommandHelp(String command, String description) {
        sendMessage(styled(command, TextFormatting.GOLD).appendSibling(styled(" - " + description, TextFormatting.GRAY)));
    }

    private static void sendMessage(String message) {
        sendMessage(styled(message, TextFormatting.GOLD));
    }

    private static void sendMessage(ITextComponent message) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(message);
    }

    private static ITextComponent styled(String text, TextFormatting color) {
        ITextComponent component = new TextComponentString(text);
        component.getStyle().setColor(color);
        return component;
    }

    private static ITextComponent counter(int count) {
        return styled(" [", TextFormatting.GOLD)
                .appendSibling(styled("x", TextFormatting.GRAY))
                .appendSibling(styled(String.valueOf(count), TextFormatting.RED))
                .appendSibling(styled("]", TextFormatting.GOLD));
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
