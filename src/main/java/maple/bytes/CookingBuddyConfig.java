package maple.bytes;

import net.runelite.client.Notifier;
import net.runelite.client.config.*;
import net.runelite.client.config.FlashNotification;

import java.awt.*;

@ConfigGroup("CookingBuddy")
public interface CookingBuddyConfig extends Config
{

	@ConfigItem(
			keyName = "notificationCooldown",
			name = "Notification Cooldown",
			description = "Time in seconds between cooking cape warnings"
	)
	default int notificationCooldown()
	{
		return 60; // Default to 60 seconds
	}


	@ConfigSection(
			name = "Customizable Notification",
			description = "Disabling GameMessageNotification, and SendNotificationWhileFocused is not recommended",
			closedByDefault = true,
			position = 2
	)
	String  missingCookingCapeEffectNotificationSection = "missingCookingCapeEffectNotificationSection";



	@ConfigItem(
			position 	= 11,
			keyName 	= "enableCustomNotificationWarning",
			name 		= "Enable Custom Notification",
			description = "Check this box, and customize your notification to override the default notification",
			warning 	= "If enabling custom notifications, we HIGHLY recommend you to enable: \n - Send notifications when focused \n - Game Message Notification!",
			section     = missingCookingCapeEffectNotificationSection
	)
	default boolean notificationWarning(){ return false; }





	@ConfigItem(
			position = 12,
			keyName = "missingCookingCapeEffectNotification",
			name = "Missing Cooking Cape Notification",
			description = "Send a notification when a attempting to cook without the cooking cape effect",
			section = missingCookingCapeEffectNotificationSection
	)

	default Notification missingCookingCapeEffectNotification()
	{
		return new Notification();
	}

	@ConfigItem(
			name 		= "Default Cooking Cape Effect Notification",
			keyName 	= "defaultCookingCapeEffectNotification",
			description = "A hidden configuration to use in the event that the end-user has not enabled custom notifications",
			hidden = true,
			secret = true
	)
	//I hope this is allowed, it saved me from implementing an entirely separate flash thing.
	//The whole point of the plugin is to notify you, and if the end-user really doesn't like the settings
	//They can change them if they so choose.
	default Notification defaultCookingCapeEffectNotification()
	{
		return Notification.ON
				.withInitialized(true)
				.withOverride(true)
				.withTray(false)
				.withTrayIconType(TrayIcon.MessageType.INFO)
				.withRequestFocus(RequestFocusType.OFF)
				.withSound(Notifier.NativeCustomOff.OFF)
				.withVolume(0)
				.withGameMessage(true)
				.withTimeout(2000)
				.withFlash(FlashNotification.FLASH_TWO_SECONDS)
				.withFlashColor(new Color(255, 0, 0, 130))
				.withSendWhenFocused(true);
	}


}
