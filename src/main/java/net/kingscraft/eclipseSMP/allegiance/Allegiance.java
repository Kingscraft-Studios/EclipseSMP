package net.kingscraft.eclipseSMP.allegiance;

public enum Allegiance {
    SOL("Sol", "☀"),
    LUNA("Luna", "☾");

    private final String displayName;
    private final String symbol;

    Allegiance(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public Allegiance opposite() {
        return this == SOL ? LUNA : SOL;
    }
}
