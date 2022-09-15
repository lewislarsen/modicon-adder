package com.modicon;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("modicon")
public interface ModIconConfig extends Config {

    @ConfigItem(
            keyName = "icon",
            name = "Crown type",
            description = "What icon should we show? This icon will be used for you and other players."
    )
    default ModIcons icon() {
        return ModIcons.PLAYER_MODERATOR;
    }
    void icon(ModIcons icon);

    @ConfigItem(
            keyName = "players",
            name = "Other players",
            description = "List of other players RSNs to show a crown for, please separate by a new line (press enter)."
    )
    default String otherPlayers() {
        return "";
    }

    @ConfigItem(
            keyName = "showCrown",
            name = "Show crown next to username",
            description = "Permanently show the crown next to your name like ironman mode does."
    )
    default boolean showCrown() {
        return true;
    }
}
