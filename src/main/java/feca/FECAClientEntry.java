package feca;

import feca.config.ClientConfigs;
import feca.entry.FECAEventsRegistry;
import feca.network.Network;
import net.fabricmc.api.ClientModInitializer;

public class FECAClientEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FECAEventsRegistry.registerClientEvents();
        ClientConfigs.load();
        Network.registerClient();
    }
}
