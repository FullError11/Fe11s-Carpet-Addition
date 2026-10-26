package feca.rule.options;

public enum CollectibleSpawnEggOptions {
    FALSE,  // 禁用
    KILL,   // 当玩家 副手/主手 物品带有 精准采集 附魔时，击杀生物
    BUCKET, // 使用 空桶(Items.BUCKET) 对生物右键
}
