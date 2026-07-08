package fe11.carpetaddition.config;

import fe11.carpetaddition.config.utils.Configurator;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientConfigs {
    public static class Data {
        public boolean observerFreezeAreasHighlight = false;
    }

    private static Data data = new Data();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Configurator<Data> configurator =
            new Configurator<>("feca_config@client.json", Data.class);

    public static void load() {
        lock.writeLock().lock();
        data = configurator.loadOr(data);
        lock.writeLock().unlock();
    }
    public static void save() {
        lock.readLock().lock();
        configurator.save(data);
        lock.readLock().unlock();
    }

    public interface Accessor {
        void visit(Data data);
    }

    /**
     * @apiNote 禁止使用此方法写入内容，可能引发崩溃
     */
    public static void read(@NotNull Accessor accessor) {
        lock.readLock().lock();
        accessor.visit(data);
        lock.readLock().unlock();
    }
    /**
     * @apiNote 禁止使用此方法写入内容，可能引发崩溃
     */
    public static boolean tryRead(@NotNull Accessor accessor) {
        if (lock.readLock().tryLock()) {
            accessor.visit(data);
            lock.readLock().unlock();
            return true;
        }
        return false;
    }
    /**
     * @apiNote 禁止使用此方法写入内容，可能引发崩溃
     * @apiNote 此方法不安全
     */
    public static Data unsafeGet() {
        return data;
    }
    public static void write(@NotNull Accessor accessor) {
        lock.writeLock().lock();
        accessor.visit(data);
        save();
        lock.writeLock().unlock();
    }
}
