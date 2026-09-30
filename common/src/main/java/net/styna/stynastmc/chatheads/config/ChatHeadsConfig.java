/*
 * Ported from Chat Heads by dzwdz (https://github.com/dzwdz/chat_heads).
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package net.styna.stynastmc.chatheads.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.styna.stynastmc.config.StynaConfigSpec;

public final class ChatHeadsConfig {
    public static final StynaConfigSpec SPEC;
    public static final StynaConfigSpec.EnumValue<RenderPosition> RENDER_POSITION;
    public static final StynaConfigSpec.BooleanValue OFFSET_NON_PLAYER_TEXT;
    public static final StynaConfigSpec.EnumValue<SenderDetection> SENDER_DETECTION;
    public static final StynaConfigSpec.BooleanValue SMART_HEURISTICS;
    public static final StynaConfigSpec.BooleanValue HANDLE_SYSTEM_MESSAGES;
    public static final StynaConfigSpec.BooleanValue DRAW_SHADOW;
    public static final StynaConfigSpec.DoubleValue THREE_DEE_NESS;
    public static final StynaConfigSpec.IntValue RIGHT_PADDING;
    public static final StynaConfigSpec.BooleanValue DETECT_NAME_ALIASES;
    public static final StynaConfigSpec.ConfigValue<List<? extends String>> NAME_ALIASES;

    static {
        StynaConfigSpec.Builder builder = new StynaConfigSpec.Builder();
        builder.push("chat_heads");
        RENDER_POSITION = builder.comment("BEFORE_NAME puts the head next to the sender's name, BEFORE_LINE at the start of the line.")
                .defineEnum("render_position", RenderPosition.BEFORE_NAME);
        OFFSET_NON_PLAYER_TEXT = builder.comment("Indent lines without a head so all text lines up. Only for BEFORE_LINE.")
                .define("offset_non_player_text", true);
        SENDER_DETECTION = builder.comment("How the sender is found. Use HEURISTIC_ONLY on servers that send fake UUIDs.")
                .defineEnum("sender_detection", SenderDetection.UUID_AND_HEURISTIC);
        SMART_HEURISTICS = builder.comment("Stop guessing senders once the server is known to send UUIDs.")
                .define("smart_heuristics", true);
        HANDLE_SYSTEM_MESSAGES = builder.comment("Add heads to server messages. Many chat plugins send player chat this way.")
                .define("handle_system_messages", true);
        DRAW_SHADOW = builder.comment("Draw a shadow under the head.")
                .define("draw_shadow", true);
        THREE_DEE_NESS = builder.comment("Draws the hat layer slightly bigger so the head looks less flat.")
                .defineInRange("three_dee_ness", 0.0, 0.0, 1.0);
        RIGHT_PADDING = builder.comment("Empty pixels after a head in chat and books.")
                .defineInRange("right_padding", 1, 0, 16);
        DETECT_NAME_ALIASES = builder.comment("Learn nicknames from EssentialsX /realname replies.")
                .define("detect_name_aliases", true);
        NAME_ALIASES = builder.comment("Nicknames matched to profile names, as nickname=profilename.")
                .defineList("name_aliases", () -> List.<String>of(), () -> "nickname=profilename", String.class::isInstance);
        builder.pop();
        SPEC = builder.build();
    }

    private static boolean loaded() {
        return SPEC.isLoaded();
    }

    public RenderPosition renderPosition() {
        return loaded() ? RENDER_POSITION.get() : RENDER_POSITION.getDefault();
    }

    public boolean offsetNonPlayerText() {
        return loaded() ? OFFSET_NON_PLAYER_TEXT.get() : OFFSET_NON_PLAYER_TEXT.getDefault();
    }

    public SenderDetection senderDetection() {
        return loaded() ? SENDER_DETECTION.get() : SENDER_DETECTION.getDefault();
    }

    public boolean smartHeuristics() {
        return loaded() ? SMART_HEURISTICS.get() : SMART_HEURISTICS.getDefault();
    }

    public boolean handleSystemMessages() {
        return loaded() ? HANDLE_SYSTEM_MESSAGES.get() : HANDLE_SYSTEM_MESSAGES.getDefault();
    }

    public boolean drawShadow() {
        return loaded() ? DRAW_SHADOW.get() : DRAW_SHADOW.getDefault();
    }

    public float threeDeeNess() {
        return (float) (double) (loaded() ? THREE_DEE_NESS.get() : THREE_DEE_NESS.getDefault());
    }

    public int rightPadding() {
        return loaded() ? RIGHT_PADDING.get() : RIGHT_PADDING.getDefault();
    }

    public boolean detectNameAliases() {
        return loaded() ? DETECT_NAME_ALIASES.get() : DETECT_NAME_ALIASES.getDefault();
    }

    /** nickname -> profile name */
    public Map<String, String> getNameAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        if (!loaded()) {
            return aliases;
        }
        for (String entry : NAME_ALIASES.get()) {
            int split = entry.indexOf('=');
            if (split > 0 && split < entry.length() - 1) {
                aliases.put(entry.substring(0, split).trim(), entry.substring(split + 1).trim());
            }
        }
        return aliases;
    }

    public void addNameAlias(String nickname, String profileName) {
        if (!loaded() || profileName.equals(getNameAliases().get(nickname))) {
            return;
        }
        List<String> next = new ArrayList<>();
        for (String entry : NAME_ALIASES.get()) {
            if (!entry.startsWith(nickname + "=")) {
                next.add(entry);
            }
        }
        next.add(nickname + "=" + profileName);
        NAME_ALIASES.set(next);
        SPEC.save();
    }
}
