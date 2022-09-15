package com.modicon;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public enum ModIcons
{
    PLAYER_MODERATOR("Player Moderator", "", false),
    JAGEX_MODERATOR("Jagex Moderator", "", false);

    private final String name;
    private final String imagePath;
    private final boolean header;

    public String toString()
    {
        return name;
    }
}