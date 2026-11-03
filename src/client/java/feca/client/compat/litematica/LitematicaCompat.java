package feca.client.compat.litematica;

import fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventHandler;

/// Litematica 软依赖入口：仅在检测到 Litematica 时加载
public final class LitematicaCompat {
    private LitematicaCompat() {}

    public static void register() {
        SchematicPickBlockEventHandler.getInstance()
                .registerSchematicPickBlockEventListener(new LitematicaSchematicPickBlockListener());
    }
}
