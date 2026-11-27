package be.maarten.woolworks.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public enum IndicatorStyle { SQUARE, LINE }
    public static final ModConfigSpec.EnumValue<IndicatorStyle> CABLE_EXTRACTION_INDICATOR_STYLE;
    public static final ModConfigSpec SPEC;
    static {
        var builder = new ModConfigSpec.Builder();
        CABLE_EXTRACTION_INDICATOR_STYLE = builder.translation("config.woolworks.cableExtractionIndicatorStyle")
                .comment("Style of the cable extraction indicator.")
                .defineEnum("cableExtractionIndicatorStyle", IndicatorStyle.SQUARE);
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
