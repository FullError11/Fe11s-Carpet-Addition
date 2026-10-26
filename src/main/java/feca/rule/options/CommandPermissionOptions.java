package feca.rule.options;

public enum CommandPermissionOptions {
    TRUE("true"),
    FALSE("false"),
    OPS("ops"),
    LEVEL0("0"),
    LEVEL1("1"),
    LEVEL2("2"),
    LEVEL3("3"),
    LEVEL4("4");

    private final String name;

    CommandPermissionOptions(String name) {
        this.name = name;
    }

    @Override public String toString() { return name; }
}
