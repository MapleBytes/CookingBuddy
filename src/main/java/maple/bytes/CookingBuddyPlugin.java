package maple.bytes;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;

import java.awt.*;
import java.util.Set;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.Player;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.Notification;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.Notifier;

import java.util.Objects;






@Slf4j
@PluginDescriptor(
	/*
	I wrote this entire plugin, because my friend burned 8 Angler Fish (Angry Fish as he calls them)
	So yeah, May nobody ever burn another Angler(Angry fish) / Raw moonlight antelope / Raw manta ray /
	Raw sea turtle / Raw dark crab / Raw shark / Moonlight antelope If they have 99 cooking... :)
	*/
	name = "CookingBuddy"
)
public class CookingBuddyPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private CookingBuddyConfig config;

	@Inject
	private Notifier notifier;

	//Global Booleans
	public boolean 	hasCookingCape = false;
	public boolean 	hasRawFood = false;
	public boolean 	inCookingRegion = false;
	public boolean 	playerMaxCooking = false;

	public int 	 	playerCookingLevel;
	public long 	lastNotificationTime;

	//Boolean Flags
	public boolean  valuesInitialized;
	public boolean 	checkPlayerLevelFlag;
	public boolean 	checkPlayerContainerFlag;
	public boolean 	onGameTickNeededFlag;


	//Notification in-game message string
	public String gameMessageNotificationString =
			"A friendly reminder from Cooking Buddy: Your Cooking Cape effect is missing";

	//Build a list of cooking cape ID's
	private final Set<Integer> COOKING_CAPE_IDS = ImmutableSet.<Integer>builder()
			.addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.COOKING_CAPE)))
			.addAll(ItemVariationMapping.getVariations(ItemVariationMapping.map(ItemID.MAX_CAPE)))
			.build();

	//Built in list of locations where notifications are enabled
	private static final Set<Integer> GOOD_REGION_IDS = ImmutableSet.of(
			9772,  	// Myths Guild
			12109 	// Rogues Den
	);




	/*
	In future versions, I plan on dynamically scraping the wiki to build a list of burnable foods based on the active
	players levels, achievement diary status and region (EG: Hosidius Range, Lumbridge Range, Cooking Gauntlets.)

	Currently, for the first version of cooking buddy, I wanted to keep things simple, so I use the below set, and assume
	that the player is level 99 cooking
	 */

	private static final Set<Integer> TARGETED_BURNABLE_FOODS = ImmutableSet.of(
			29113, 	// Raw moonlight antelope
//			29140,	// Raw sunlight antelope *Burns on fire, but not on range at level 99*
			389, 	// Raw manta
			395, 	// Raw sea turtles
			11934, 	// Raw dark crabs
			383, 	// Raw shark
			13439	// Raw angler fish
	);


	public void checkContainers()
	{
		//This snip of code is adapted from the Ammo Plugin, so credit to them!

		//Check for null equipment & update hasCookingCape
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null) {
			hasCookingCape = false;
		} else {
			final Item cape = equipment.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
			hasCookingCape = cape != null && COOKING_CAPE_IDS.contains(cape.getId());
		}

		//Sets global hasRawFood to false, then we iterate over the inventory (if it's not null)
		hasRawFood = false;

		//check if the player's inventory is NULL (Empty) and escapes if so.
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null) {
			return;
		}

		//Iterates over player inventory checking for any item in the TARGETED_BURNABLE_FOODS set.
		for (Item item : inventory.getItems()) {
			if (TARGETED_BURNABLE_FOODS.contains(item.getId())) {
				hasRawFood = true;
				break;
			}
		}

	}


	public void updatePlayerRegion()
	{
		//Fetch player region ID.
		Player player = client.getLocalPlayer();
		int regionId = player.getWorldLocation().getRegionID();

		//If the player is not in a cooking region, update the boolean
		if(!GOOD_REGION_IDS.contains(regionId))
		{
			inCookingRegion = false;
			return;
		}

		inCookingRegion = true;
	}



	public void checkPlayerCookingEligibility()
	{
		//Check if the player is max cooking.
		if(playerCookingLevel != 99)
		{
			playerMaxCooking = false;
			return;
		}

		playerMaxCooking = true;
	}

	public void warnForUnequippedCookingCape()
	{
		// Get the current system time
		long currentTime 		 = System.currentTimeMillis();

		// Convert the cooldown from seconds to milliseconds
		long cooldownMillis 	 = config.notificationCooldown() * 1000L;

		// Check if lastNotificationTime is uninitialized or if enough time has passed since the last notification
		if (lastNotificationTime == 0 || (currentTime - lastNotificationTime) >= cooldownMillis)
		{
			//Add Log info message (or show it in-game, depending on how you want to notify the player)
			log.info("You are about to cook without your Cooking Cape equipped! - Cooking Buddy Notification");

			Notification notification;

			//Set the notification variable to be the cooking buddy default
			notification 		= config.defaultCookingCapeEffectNotification();


			//If the end user has enabled a custom configuration, and confirmed the notification warning
			//set notification to be their custom implementation instead of the cooking buddy default
			if(		  			  config.missingCookingCapeEffectNotification().isEnabled()
								& config.notificationWarning()
								& config.missingCookingCapeEffectNotification().isOverride())
			{
				notification 	= config.missingCookingCapeEffectNotification();
			}

			notifier.notify(	  notification
								, gameMessageNotificationString);




			//Update lastNotificationTime to the current time
			lastNotificationTime = currentTime;
		}
	}
	//Event Hooks
	@Override
	protected void startUp() throws Exception
	{
		log.info("Cooking Buddy Started.");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Cooking Buddy Stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		//Check player region on login.
		if(gameStateChanged.getGameState() != GameState.LOGGED_IN) return;

		//Update Player Region (As it is accessible on LOGGED_IN.)
		updatePlayerRegion();

		//Set the global flags on Login, so the next onGameTick will fetch the needed information
		//As the information is not actually available on LOGGED_IN when the client first launches.

		if(valuesInitialized) return;

		checkPlayerLevelFlag = true;
		checkPlayerContainerFlag = true;
		onGameTickNeededFlag = true;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{

		//Since player container, and player levels don't seem to be available on LOGGED_IN gamestate
		//to get around this I set a flag so that on the next gametick we check for them instead.
		if(!onGameTickNeededFlag) return;

		if(checkPlayerLevelFlag)
		{
			checkPlayerCookingEligibility();
			checkPlayerLevelFlag = false;
		}

		if(checkPlayerContainerFlag)
		{
			checkContainers();
			checkPlayerContainerFlag = false;
		}

		onGameTickNeededFlag = false;
		valuesInitialized = true;
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed widgetClosed)
	{
		if(widgetClosed.getGroupId() != InterfaceID.BANK) return;

		updatePlayerRegion();
		checkContainers();
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		//Check if stat changed is COOKING.
		if(statChanged.getSkill() != Skill.COOKING) return;

		//Check if player has gained a level since we first checked, and escape if not.
		if(statChanged.getLevel() == playerCookingLevel) return;

		//Checks for and then updates the Global
		checkPlayerCookingEligibility();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged itemContainerChanged)
	{
		if(itemContainerChanged.getContainerId() != InventoryID.INVENTORY.getId()) return;

		checkContainers();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked)
	{
		//if we're not in a pre-determined cooking region, just escape.
		if(!inCookingRegion) return;

		//Check and escape if the menu option is not Cook.
		if(!Objects.equals(menuOptionClicked.getMenuOption(), "Cook")) return;

		//If player is not max cooking, escape.
		if(!playerMaxCooking) return;

		//If player has cooking cape, they will not burn food, so escape.
		if(hasCookingCape) return;

		//If player doesn't have raw food, escape.
		if(!hasRawFood) return;


		//Should only reach here, if player does NOT have cooking cape, and play HAS raw food.
		warnForUnequippedCookingCape();


	}

	@Provides
	CookingBuddyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CookingBuddyConfig.class);
	}
}
