package net.kingscraft.eclipseSMP.environment;

public enum LightState {
    SUNLIGHT("&e☀ Sunlight"),
    SHADOW("&7Shadow"),
    DARKNESS("&8Darkness"),
    ECLIPSE("&c☀☾ Eclipse");

    private final String label;

    LightState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
