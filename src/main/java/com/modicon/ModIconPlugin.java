package com.modicon;

import com.google.common.base.Splitter;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.IconID;
import net.runelite.api.IndexedSprite;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;


// CREDITS TO https://github.com/ThatGamerBlue/fake-iron-icon

@Slf4j
@PluginDescriptor(name = "Mod Icons", enabledByDefault = true)
public class ModIconPlugin extends Plugin
{
	@Inject
	@Getter
	private Client client;
	@Inject
	private ClientThread clientThread;
	private final HashMap<ModIcons, Integer> iconIds = new HashMap<>();
	@Getter
	private static List<String> players = new ArrayList<>();
	private ModIcons selectedIcon = null;
	@Inject
	@Getter
	private ModIconConfig pluginConfig;
	private static final Splitter NEWLINE_SPLITTER = Splitter
			.on("\n")
			.omitEmptyStrings()
			.trimResults();

	private void updateSelectedIcon()
	{
		if (selectedIcon != pluginConfig.icon())
		{
			selectedIcon = pluginConfig.icon();
		}
	}

	@Provides
	ModIconConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ModIconConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("modicon"))
		{
			if (pluginConfig.icon().isHeader())
			{
				pluginConfig.icon(ModIcons.valueOf(event.getOldValue()));
				return;
			}

			clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
			players = NEWLINE_SPLITTER.splitToList(pluginConfig.otherPlayers().toLowerCase());
			updateSelectedIcon();
		}
	}

	@Override
	public void startUp()
	{
		updateSelectedIcon();

		if (client.getModIcons() == null)
		{
			iconIds.clear();
			return;
		}

		loadSprites();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		updateSelectedIcon();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		updateSelectedIcon();

		if (event.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if ((Stream.of(ModIcons.values())
				.noneMatch((icon) -> (!icon.getImagePath().equals("")) && iconIds.getOrDefault(icon, -1) == -1)))
		{
			return;
		}

		loadSprites();
	}

	private void loadSprites()
	{
		IndexedSprite[] modIcons = client.getModIcons();
		IndexedSprite[] newAry = Arrays.copyOf(modIcons, Math.toIntExact(
				modIcons.length +
						Stream.of(ModIcons.values()).filter(icon -> !icon.getImagePath().equals("")).count()));
		int modIconsStart = modIcons.length - 1;

		// Establishes the base moderator icons
		iconIds.put(ModIcons.PLAYER_MODERATOR, IconID.PLAYER_MODERATOR.getIndex());
		iconIds.put(ModIcons.JAGEX_MODERATOR, IconID.JAGEX_MODERATOR.getIndex());

		for (ModIcons icon : ModIcons.values())
		{
			if (icon.getImagePath().equals(""))
			{
				continue;
			}

			final IndexedSprite sprite = getIndexedSprite(icon.getImagePath());
			newAry[++modIconsStart] = sprite;
			iconIds.put(icon, modIconsStart);
		}

		client.setModIcons(newAry);
	}

	@Override
	public void shutDown()
	{
		iconIds.clear();

		clientThread.invoke(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getName() == null || client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return;
		}

		boolean isLocalPlayer =
				Text.standardize(event.getName()).equalsIgnoreCase(Text.standardize(client.getLocalPlayer().getName()));

		if (isLocalPlayer || players.contains(Text.standardize(event.getName().toLowerCase())))
		{
			event.getMessageNode().setName(
					getImgTag(iconIds.getOrDefault(selectedIcon, IconID.NO_ENTRY.getIndex())) +
							event.getName());
		}
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!event.getEventName().equals("setChatboxInput"))
		{
			return;
		}

		updateChatbox();
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		updateChatbox(); // this stops flickering when typing
	}

	private void updateChatbox()
	{
		Widget chatboxTypedText = client.getWidget(WidgetInfo.CHATBOX_INPUT);

		if (getIconIdx() == -1)
		{
			return;
		}

		if (chatboxTypedText == null || chatboxTypedText.isHidden())
		{
			return;
		}

		String[] chatbox = chatboxTypedText.getText().split(":", 2);
		String rsn = Objects.requireNonNull(client.getLocalPlayer()).getName();

		// Determines whether to show the crown next to the username permanently.
		if (pluginConfig.showCrown())
		{
			chatboxTypedText.setText(getImgTag(getIconIdx()) + rsn + ":" + chatbox[1]);
		}
	}

	private IndexedSprite getIndexedSprite(String file)
	{
		try
		{
			log.debug("Loading: {}", file);
			BufferedImage image = ImageUtil.loadImageResource(this.getClass(), file);
			return ImageUtil.getImageIndexedSprite(image, client);
		}
		catch (RuntimeException ex)
		{
			log.debug("Unable to load image: ", ex);
		}

		return null;
	}

	private String getImgTag(int i)
	{
		return "<img=" + i + ">";
	}

	private int getIconIdx()
	{
		if (selectedIcon == null)
		{
			updateSelectedIcon();
		}

		return iconIds.getOrDefault(selectedIcon, -1);
	}
}
