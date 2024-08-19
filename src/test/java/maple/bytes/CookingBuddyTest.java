package maple.bytes;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CookingBuddyTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CookingBuddyPlugin.class);
		RuneLite.main(args);
	}
}