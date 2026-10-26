package feca.client.registry;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class OnClientInitializeCallbackRegistry {
    public static List<Runnable> CALLBACKS = new ArrayList<>();

    public static void register(Runnable callback) {
        CALLBACKS.add(callback);
    }
}
