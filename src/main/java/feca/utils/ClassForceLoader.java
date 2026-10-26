package feca.utils;

public final class ClassForceLoader {
    private ClassForceLoader() {
    }

    /**
     * 确保指定类被初始化
     * <p>若类已初始化过，本次调用无副作用。</p>
     */
    public static void loadBy(Class<?> clazz, ClassLoader loader) {
        if (clazz == null || clazz.isPrimitive() || clazz.isArray()) {
            return; // 基本类型、数组没有静态初始化器
        }
        try {
            // initialize=true：走完整 加载 → 链接 → 初始化
            // 用 clazz 自己的加载器，保证命中同一个 Class 对象，避免误初始化同名的另一份类
            Class.forName(clazz.getName(), true, loader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to initialize class: " + clazz.getName(), e);
        }
    }

    /**
     * 确保指定类被初始化
     * <p>若类已初始化过，本次调用无副作用。</p>
     * <p>使用类默认加载器</p>
     */
    public static void load(Class<?> clazz) {
        loadBy(clazz, clazz.getClassLoader());
    }
}