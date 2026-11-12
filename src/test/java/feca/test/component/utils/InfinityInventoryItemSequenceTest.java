package feca.test.component.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feca.function.utils.InfinityInventoryItemSequence;
import feca.test.support.GameBootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link InfinityInventoryItemSequence}（无限库存的固定展示顺序配置）。
 *
 * <p>覆盖配置解析与组的展开语义：注释、未知 / 非法 id、重复、嵌套组、占位符、
 * override 叠加、组引用循环与参数校验、未登记物品的 id 排序。</p>
 */
class InfinityInventoryItemSequenceTest {
    @BeforeAll
    static void bootstrap() {
        GameBootstrap.ensure();
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static InfinityInventoryItemSequence parse(String text) {
        return InfinityInventoryItemSequence.parse(json(text));
    }

    @Test
    @DisplayName("裸 id：注释跳过，未知 / 非法 id 忽略")
    void parsesPlainIds() {
        final var sequence = parse("""
                {
                    "sequence": [
                        "// 常用原料",
                        "minecraft:stone",
                        "minecraft:not_a_real_item",
                        "minecraft:Not A Valid Id",
                        "minecraft:dirt"
                    ]
                }
                """);

        assertEquals(List.of(Items.STONE, Items.DIRT), sequence.items());
        assertEquals(0, sequence.indexOf(Items.STONE));
        assertEquals(1, sequence.indexOf(Items.DIRT));
        assertEquals(-1, sequence.indexOf(Items.SAND), "未登记物品返回 -1");
        assertTrue(sequence.contains(Items.STONE));
        assertFalse(sequence.contains(Items.SAND));
        assertTrue(sequence.ignoredIds().contains("minecraft:not_a_real_item"), "不存在的 id 应记录在 ignoredIds");
        assertTrue(sequence.ignoredIds().contains("minecraft:Not A Valid Id"), "非法 id 应记录在 ignoredIds");
    }

    @Test
    @DisplayName("组展开：占位符替换 + 嵌套组")
    void expandsGroups() {
        final var sequence = parse("""
                {
                    "groups": {
                        "wood_parts": {
                            "input": ["namespace", "id"],
                            "output": [
                                "${namespace}:${id}_planks",
                                "${namespace}:${id}_stairs",
                                "${namespace}:${id}_slab"
                            ]
                        },
                        "wood": {
                            "input": ["namespace", "id"],
                            "output": [
                                "${namespace}:${id}_log",
                                "${namespace}:stripped_${id}_log",
                                { "wood_parts": ["${namespace}", "${id}"] }
                            ]
                        }
                    },
                    "sequence": [
                        { "wood": ["minecraft", "oak"] }
                    ]
                }
                """);

        assertEquals(
                List.of(
                        Items.OAK_LOG,
                        Items.STRIPPED_OAK_LOG,
                        Items.OAK_PLANKS,
                        Items.OAK_STAIRS,
                        Items.OAK_SLAB),
                sequence.items(),
                "组输出按声明顺序展开，嵌套组在原位置继续展开");
    }

    @Test
    @DisplayName("重复出现保留第一次的位置（不覆盖）")
    void duplicatesKeepFirstPosition() {
        final var sequence = parse("""
                {
                    "sequence": [
                        "minecraft:dirt",
                        "minecraft:stone",
                        "minecraft:dirt"
                    ]
                }
                """);

        assertEquals(List.of(Items.DIRT, Items.STONE), sequence.items());
        assertEquals(0, sequence.indexOf(Items.DIRT), "后面的重复不能把泥土挪到石头之后");
    }

    @Test
    @DisplayName("空气不能出现在序列中")
    void ignoresAir() {
        final var sequence = parse("""
                {
                    "sequence": [ "minecraft:air", "minecraft:stone" ]
                }
                """);

        assertEquals(List.of(Items.STONE), sequence.items());
        assertTrue(sequence.ignoredIds().contains("minecraft:air"), "空气应记录在 ignoredIds");
    }

    @Test
    @DisplayName("未登记物品排在已登记物品之后，按 id 字符顺序")
    void sortsUnlistedItemsById() {
        final var sequence = parse("""
                {
                    "sequence": [ "minecraft:stone" ]
                }
                """);

        assertEquals(
                List.of(Items.STONE, Items.DIRT, Items.SAND),
                sequence.sorted(List.of(Items.SAND, Items.STONE, Items.DIRT)),
                "石头按登记顺序在前，其余按 minecraft:dirt < minecraft:sand 排序");
    }

    @Test
    @DisplayName("override=true 清空此前叠加的配置")
    void overrideClearsPreviousSources() {
        final var builder = InfinityInventoryItemSequence.builder();
        builder.apply("base", json("""
                {
                    "sequence": [ "minecraft:stone" ]
                }
                """));
        builder.apply("addon", json("""
                {
                    "override": true,
                    "sequence": [ "minecraft:dirt" ]
                }
                """));

        assertEquals(List.of(Items.DIRT), builder.build().items());
    }

    @Test
    @DisplayName("override=false 追加到此前配置之后")
    void appendsWithoutOverride() {
        final var builder = InfinityInventoryItemSequence.builder();
        builder.apply("base", json("""
                {
                    "sequence": [ "minecraft:stone" ]
                }
                """));
        builder.apply("addon", json("""
                {
                    "sequence": [ "minecraft:dirt" ]
                }
                """));

        assertEquals(List.of(Items.STONE, Items.DIRT), builder.build().items());
    }

    @Test
    @DisplayName("组引用循环被截断，不会无限递归")
    void detectsGroupCycle() {
        final var sequence = parse("""
                {
                    "groups": {
                        "a": { "input": [], "output": [ { "b": [] } ] },
                        "b": { "input": [], "output": [ { "a": [] } ] }
                    },
                    "sequence": [ { "a": [] } ]
                }
                """);

        assertTrue(sequence.isEmpty(), "循环组不产生任何物品，也不应抛异常");
    }

    @Test
    @DisplayName("从模组资源加载内置配置：所有 id 有效，TODO 的生电材料已补全")
    void loadsBundledConfig() {
        final var sequence = InfinityInventoryItemSequence.load();

        assertFalse(sequence.isEmpty(), "feca 自带的 infinity_inventory_item_sequence.json 应能读到");
        assertEquals(Items.SPRUCE_LOG, sequence.items().getFirst(), "sequence 第一项是云杉系列");
        assertTrue(sequence.contains(Items.OAK_PLANKS));
        assertTrue(sequence.contains(Items.SPRUCE_PLANKS));

        assertTrue(sequence.contains(Items.STICK));
        assertTrue(sequence.contains(Items.STONE_BRICKS));
        assertTrue(sequence.contains(Items.SMOOTH_STONE));

        assertTrue(sequence.contains(Items.REDSTONE));
        assertTrue(sequence.contains(Items.REDSTONE_BLOCK));
        assertTrue(sequence.contains(Items.REPEATER));
        assertTrue(sequence.contains(Items.COMPARATOR));
        assertTrue(sequence.contains(Items.OBSERVER));
        assertTrue(sequence.contains(Items.PISTON));
        assertTrue(sequence.contains(Items.STICKY_PISTON));
        assertTrue(sequence.contains(Items.HOPPER));
        assertTrue(sequence.contains(Items.SLIME_BALL));
        assertTrue(sequence.contains(Items.SLIME_BLOCK));
        assertTrue(sequence.contains(Items.HONEY_BLOCK));

        assertTrue(sequence.ignoredIds().isEmpty(),
                () -> "内置配置包含无效 id: " + sequence.ignoredIds());
    }

    @Test
    @DisplayName("未定义组与参数个数不匹配被跳过，正常组照常展开")
    void skipsInvalidCalls() {
        final var sequence = parse("""
                {
                    "groups": {
                        "one": {
                            "input": [ "id" ],
                            "output": [ "minecraft:${id}" ]
                        }
                    },
                    "sequence": [
                        { "one": [ "stone" ] },
                        { "one": [] },
                        { "missing": [] }
                    ]
                }
                """);

        assertEquals(List.of(Items.STONE), sequence.items());
    }
}
