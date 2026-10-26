package feca.client.utils;

import feca.utils.ClassForceLoader;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class ForceLoadClass {
    public static void load(Class<?> @NonNull ... classes) {
        for (Class<?> clazz : classes) {
            ClassForceLoader.load(clazz);
        }
    }
    public static void loadBy(ClassLoader loader, Class<?> @NonNull ... classes) {
        for (Class<?> clazz : classes) {
            ClassForceLoader.loadBy(clazz, loader);
        }
    }
}
