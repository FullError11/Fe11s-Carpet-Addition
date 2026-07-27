package feca.command.argument;

public abstract class NumberArgument<T extends Number> extends AbstractArgument<T> {
    public final T MIN;
    public final T MAX;

    protected NumberArgument(String name, Class<T> clazz, T min, T max) {
        super(name, clazz);
        this.MIN = min;
        this.MAX = max;
    }
}
