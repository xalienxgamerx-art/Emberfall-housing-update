package app.rork.p7buzvrzcw3frddi8y50ou.procgen

/**
 * Building Roadmap Phase 3 — architectural profiles.
 *
 * A profile is a bundle of generation characteristics the shared tile generator reads: how
 * large a building may grow, how many floors it may stand, how wide its hallway runs, which
 * rooms its bands may hold and which it must hold, how eagerly partitions merge into open
 * halls, and whether upper-floor variations (jetties, terraces, balconies) fit the style.
 * The generator stays one generator; profiles are configuration, not code paths.
 *
 * Every word is a closed vocabulary: [NAMES] is what content packs may declare, validated by
 * ContentBuilder like classifications and zones — an unknown word is reported and falls back
 * to [DEFAULT], never guessed. The default profile reproduces the pre-profile generator
 * exactly (same draws, same ranges, same pools), so legacy packs and existing saves grow the
 * same buildings they always did.
 *
 * Independent of the exterior profile, a building may declare interior-only configuration
 * (see [BuildingDef]'s int* fields): a compact tavern shell around a spacious common room,
 * or a cottage interior a little roomier than its walls. Same profiles, separate cache key.
 */
data class ArchProfile(
    val name: String,
    /** Floor-count range the generator draws from. */
    val floorsMin: Int,
    val floorsMax: Int,
    /** Preferred footprint range, in tiles, and the hard clamp the draw is coerced into. */
    val wMin: Int,
    val wMax: Int,
    val wCapMin: Int = 7,
    val wCapMax: Int = 30,
    val dMin: Int,
    val dMax: Int,
    /** Hallway width in tiles (1 = snug cottage passage, 2 = the classic hall). */
    val hallW: Int = 2,
    /** How deep a room band may run (rows of tiles on one side of the hall). */
    val bandMin: Int = 3,
    val bandMax: Int = 4,
    /** Room-kind pools per band and floor; drawn shuffled, so order never biases a seed. */
    val groundNorth: List<String>,
    val groundSouth: List<String>,
    val upper: List<String>,
    /** The ground-floor room holding the front door: the building's face to the world. */
    val frontGreat: String,
    val frontSingle: String,
    /** The ground-floor room holding the back door (kitchen, workshop, loading bay…). */
    val backRoom: String,
    /** How upper-floor multi-bay rooms are named. */
    val upperGreat: String = "great chamber",
    /** Partition-merge chances: open halls are a profile trait, not a dice roll of the style. */
    val groundOpenChance: Double = 0.7,
    val groundOpenNorth: Double = 0.0,
    val upperOpenChance: Double = 0.25,
    /** Jetties, setbacks, terraces and loggias — off for modest or industrial buildings. */
    val allowFeatures: Boolean = true,
    /** Windows every Nth bay on each facade (1 = the classic one-per-bay). */
    val windowEvery: Int = 1,
    /** Room kinds that must appear somewhere in the plan, or generation retries. */
    val required: List<String> = emptyList(),
)

object ArchitecturalProfiles {

    /** The closed set of profile names a content pack may declare. */
    val NAMES = setOf(
        "default", "cottage", "townhouse", "tavern", "workshop", "manor", "warehouse", "civic",
    )

    /**
     * The pre-profile generator, word for word: same floor, width and depth ranges, the same
     * band depths, room pools, hall-opening and upper-floor variation chances. Buildings that
     * declare no profile grow exactly what they always grew.
     */
    val DEFAULT = ArchProfile(
        name = "default",
        floorsMin = 3, floorsMax = 4,
        wMin = 17, wMax = 23, wCapMin = 17, wCapMax = 30,
        dMin = 12, dMax = 14,
        hallW = 2, bandMin = 3, bandMax = 4,
        groundNorth = listOf("pantry", "storeroom", "larder", "scullery", "cold cellar"),
        groundSouth = listOf("parlor", "guardroom", "workshop", "storeroom", "buttery"),
        upper = listOf(
            "bedchamber", "solar", "study", "guest chamber", "nursery", "armory", "scriptorium", "linen room",
        ),
        frontGreat = "great hall", frontSingle = "entry hall", backRoom = "kitchen",
        groundOpenChance = 0.7,
    )

    val COTTAGE = ArchProfile(
        name = "cottage",
        floorsMin = 1, floorsMax = 2,
        wMin = 8, wMax = 11,
        dMin = 7, dMax = 9,
        hallW = 1, bandMin = 1, bandMax = 2,
        groundNorth = listOf("pantry", "larder"),
        groundSouth = listOf("storeroom", "buttery"),
        upper = listOf("bedchamber", "study", "nursery"),
        frontGreat = "parlor", frontSingle = "parlor", backRoom = "kitchen",
        groundOpenChance = 0.9,
        allowFeatures = false,
    )

    val TOWNHOUSE = ArchProfile(
        name = "townhouse",
        floorsMin = 2, floorsMax = 3,
        wMin = 8, wMax = 12,
        dMin = 10, dMax = 13,
        hallW = 1, bandMin = 1, bandMax = 3,
        groundNorth = listOf("pantry", "storeroom", "scullery"),
        groundSouth = listOf("stockroom", "office"),
        upper = listOf("bedchamber", "guest chamber", "study", "nursery"),
        frontGreat = "shop", frontSingle = "shop", backRoom = "kitchen",
        upperGreat = "bedchamber", // a townhouse's big upstairs room is still a bedchamber
        groundOpenChance = 0.3,
    )

    val TAVERN = ArchProfile(
        name = "tavern",
        floorsMin = 2, floorsMax = 3,
        wMin = 19, wMax = 25,
        dMin = 13, dMax = 15,
        hallW = 2, bandMin = 3, bandMax = 4,
        groundNorth = listOf("pantry", "buttery", "scullery", "larder"),
        groundSouth = listOf("guardroom", "storeroom"),
        upper = listOf("guest chamber", "bedchamber", "solar", "linen room"),
        frontGreat = "common room", frontSingle = "common room", backRoom = "kitchen",
        groundOpenChance = 0.85,
        required = listOf("common room", "kitchen"),
    )

    val WORKSHOP = ArchProfile(
        name = "workshop",
        floorsMin = 1, floorsMax = 2,
        wMin = 13, wMax = 18,
        dMin = 10, dMax = 12,
        hallW = 1, bandMin = 1, bandMax = 2,
        groundNorth = listOf("storeroom", "armory"),
        groundSouth = listOf("storeroom"),
        upper = listOf("bedchamber", "solar", "study"),
        frontGreat = "workshop", frontSingle = "workshop", backRoom = "forge",
        groundOpenChance = 0.6,
        allowFeatures = false,
        required = listOf("workshop", "forge"),
    )

    /** The manor grows exactly as it always has; the profile makes that shape declarative. */
    val MANOR = ArchProfile(
        name = "manor",
        floorsMin = 3, floorsMax = 4,
        wMin = 17, wMax = 25, wCapMin = 17, wCapMax = 30,
        dMin = 12, dMax = 15,
        hallW = 2, bandMin = 3, bandMax = 4,
        groundNorth = listOf("pantry", "storeroom", "larder", "scullery", "cold cellar"),
        groundSouth = listOf("parlor", "guardroom", "workshop", "storeroom", "buttery"),
        upper = listOf(
            "bedchamber", "solar", "study", "guest chamber", "nursery", "armory", "scriptorium", "linen room",
        ),
        frontGreat = "great hall", frontSingle = "entry hall", backRoom = "kitchen",
        required = listOf("great hall", "kitchen"),
    )

    val WAREHOUSE = ArchProfile(
        name = "warehouse",
        floorsMin = 1, floorsMax = 2,
        wMin = 24, wMax = 30,
        dMin = 14, dMax = 16,
        hallW = 1, bandMin = 3, bandMax = 9,
        groundNorth = listOf("storeroom", "armory"),
        groundSouth = listOf("storeroom"),
        upper = listOf("storeroom", "office"),
        frontGreat = "storeroom", frontSingle = "storeroom", backRoom = "storeroom",
        upperGreat = "storeroom",
        groundOpenChance = 1.0, groundOpenNorth = 1.0, upperOpenChance = 1.0,
        allowFeatures = false,
        windowEvery = 2,
        required = listOf("storeroom"),
    )

    val CIVIC = ArchProfile(
        name = "civic",
        floorsMin = 2, floorsMax = 3,
        wMin = 20, wMax = 26,
        dMin = 13, dMax = 16,
        hallW = 2, bandMin = 3, bandMax = 5,
        groundNorth = listOf("office", "linen room", "guardroom", "pantry"),
        groundSouth = listOf("meeting room", "office", "study"),
        upper = listOf("meeting room", "office", "study", "scriptorium", "linen room"),
        frontGreat = "great hall", frontSingle = "great hall", backRoom = "office",
        upperGreat = "meeting room",
        groundOpenChance = 0.8,
        required = listOf("great hall", "office"),
    )

    private val ALL = mapOf(
        "default" to DEFAULT, "cottage" to COTTAGE, "townhouse" to TOWNHOUSE,
        "tavern" to TAVERN, "workshop" to WORKSHOP, "manor" to MANOR,
        "warehouse" to WAREHOUSE, "civic" to CIVIC,
    )

    /** The profile for a declared word; anything else falls back to the default, never guessed. */
    fun resolve(name: String): ArchProfile = ALL[name.trim().lowercase()] ?: DEFAULT

    /**
     * A profile for a declared word, honouring the declaration when it is empty: "" means
     * [fallback] (the building's own profile, or the default). An unknown word also lands on
     * the fallback — ContentBuilder reports it; generation itself never fails on vocabulary.
     */
    fun resolve(name: String, fallback: ArchProfile): ArchProfile {
        val n = name.trim().lowercase()
        return if (n.isEmpty()) fallback else ALL[n] ?: fallback
    }
}
