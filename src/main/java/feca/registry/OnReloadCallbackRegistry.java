package feca.registry;

import java.util.ArrayList;
import java.util.List;

public class OnReloadCallbackRegistry {
    public static List<Runnable> CALLBACKS = new ArrayList<>();

    public static void register(Runnable callback) {
        CALLBACKS.add(callback);
    }
}
