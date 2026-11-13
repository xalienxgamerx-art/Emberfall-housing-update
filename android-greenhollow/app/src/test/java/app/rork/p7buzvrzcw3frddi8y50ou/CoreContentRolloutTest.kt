package app.rork.p7buzvrzcw3frddi8y50ou

import app.rork.p7buzvrzcw3frddi8y50ou.data.BuildingDef
import app.rork.p7buzvrzcw3frddi8y50ou.data.BuildingDirectory
import app.rork.p7buzvrzcw3frddi8y50ou.data.ContentPack
import app.rork.p7buzvrzcw3frddi8y50ou.data.ContentRegistry
import app.rork.p7buzvrzcw3frddi8y50ou.procgen.BuildingCache
import app.rork.p7buzvrzcw3frddi8y50ou.render.BuildingMesh
import app.rork.p7buzvrzcw3frddi8y50ou.render.MeshBuilder
import app.rork.p7buzvrzcw3frddi8y50ou.sim.EntranceRegistry
import kotlinx.serialization.json.Json
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The core content rollout, tested against the real production packs (core.json +
 * emberfall.json), not synthetic fixtures: every ordinary settlement building opts into
 * procedural generation explicitly, the hand-authored exceptions are intentional and
 * documented, every grown plan converts to geometry, joins its interior and never falls
 * back silently.
 */
class CoreContentRolloutTest {

    /** The buildings grown from plans: six homes, the forge, the apothecary, the manor. */
    private val grown = setOf(
        "house_ne", "house_e", "house_se", "house_sw", "house_w", "house_nw",
        "smithy", "apothecary", "manor",
    )

    /** The buildings that keep a hand-authored shell on purpose, each with its reason below. */
    private val authored = setOf("inn", "shop", "well")

    private val registry: ContentRegistry by lazy {
        val dir = TestPacks.moduleDir()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        TestPacks.build(
            *listOf("core.json", "emberfall.json").map { name ->
                json.decodeFromString<ContentPack>(File(dir, "src/main/assets/content/$name").readText())
            }.toTypedArray(),
        )
    }

    // ---- the rollout: explicit opt-ins and explicit exceptions --------------------------

    @Test
    fun `every ordinary core building opts into procedural generation`() {
        assertTrue("no error issues: ${registry.issues.filter { it.severity == "error" }}", registry.issues.none { it.severity == "error" })
        assertEquals("the core world should hold exactly its twelve buildings", 12, registry.buildings.size)
        for (id in grown) assertTrue("'$id' should opt in with \"procedural\"", registry.buildings.getValue(id).procedural)
        for (id in authored) assertFalse("'$id' should keep its hand-authored shell", registry.buildings.getValue(id).procedural)
    }

    @Test
    fun `the hand-authored exceptions are exceptions on purpose`() {
        // The inn and the shop are the authored-room demos: their room programs (common
        // room, bar, cellar, staff quarters, private lodging; shop floor, stockroom,
        // bedchamber) are canonical content the generator would drop with a warning.
        assertTrue(registry.buildings.getValue("inn").rooms.size >= 7)
        assertTrue(registry.buildings.getValue("shop").rooms.map { it.id }.contains("bedchamber"))
        // The well is a civic landmark with no interior by design (it sits in the
        // no-interior set); a generated two-storey plan for it would be absurd.
        assertEquals("well", registry.buildings.getValue("well").type)
    }

    @Test
    fun `every procedural core building grew a sound plan and keeps its profile`() {
        for (id in grown) {
            val def = registry.buildings.getValue(id)
            val plan = BuildingCache.get(def)
            assertTrue("'$id' grew no plan", plan != null)
            val s = BuildingCache.summaries[id]!!
            assertTrue("'$id' plan is not sound: ${s.problems}", s.sound)
            assertTrue("'$id' should declare a profile", def.profile.isNotEmpty())
        }
    }

    @Test
    fun `every procedural core building converts to geometry, twice the same`() {
        for (id in grown) {
            val def = registry.buildings.getValue(id)
            val a = MeshBuilder()
            val b = MeshBuilder()
            assertTrue("'$id' did not convert to geometry", BuildingMesh.emit(a, def))
            assertTrue("'$id' did not convert to geometry on the second pass", BuildingMesh.emit(b, def))
            assertEquals("'$id' grows different geometry across passes", a.size, b.size)
        }
    }

    @Test
    fun `every procedural core building has an entrance joining shell to interior`() {
        val entrances = EntranceRegistry(registry)
        for (id in grown) {
            val e = entrances.entrances(id)
            assertTrue("'$id' has no entrance from shell to interior", e.isNotEmpty())
            assertTrue("'$id' entrances are not unique", e.map { it.id }.toSet().size == e.size)
        }
        assertTrue("the well has no interior by design", entrances.entrances("well").isEmpty())
    }

    @Test
    fun `procedural core buildings derive room metadata and authored rooms survive`() {
        val dir = BuildingDirectory(registry)
        for (id in grown) {
            val logical = dir.logical(id)!!
            assertTrue(logical.procedural)
            assertTrue("'$id' derived no rooms from its plan", logical.rooms.isNotEmpty())
            assertTrue("'$id' rooms report no usable tiles", logical.rooms.all { it.usableTiles > 0 })
        }
        val inn = dir.logical("inn")!!
        assertTrue(inn.rooms.map { it.ref }.contains("inn#room:common_room"))
        assertEquals("staff_only", inn.rooms.first { it.ref.endsWith("cellar") }.access)
    }

    // ---- nothing falls back silently ---------------------------------------------------

    @Test
    fun `no procedural core building falls back`() {
        for (id in grown) BuildingCache.get(registry.buildings.getValue(id))
        val cov = BuildingCache.coverage(registry.buildings.values)
        assertEquals(12, cov.total)
        assertEquals(grown.size, cov.procedural)
        assertEquals(authored, cov.authored.toSet())
        assertTrue("unexpected fallbacks: ${cov.fallbacks}", cov.fallbacks.isEmpty())
    }

    @Test
    fun `a failed plan is counted as a fallback, never hidden`() {
        val def = registry.buildings.getValue("house_ne")
        BuildingCache.summaries[def.id] =
            BuildingCache.Summary(def.id, def.name, 1L, 0, 0, false, "test failure", null, "cottage")
        try {
            val cov = BuildingCache.coverage(registry.buildings.values)
            assertTrue(cov.fallbacks.any { it.first == "house_ne" && "test failure" in it.second })
            val report = Diagnostics.report(emptyList(), emptyList(), registry.buildings)
            assertTrue("the report never named the fallback", "FALLBACK house_ne: test failure" in report)
        } finally {
            BuildingCache.summaries.remove(def.id)
        }
    }

    @Test
    fun `the diagnostics report counts the whole core world`() {
        for (id in grown) BuildingCache.get(registry.buildings.getValue(id))
        val report = Diagnostics.report(emptyList(), emptyList(), registry.buildings)
        assertTrue("12 buildings: ${grown.size} grown from plans, ${authored.size} hand-authored shells" in report)
        assertTrue("inn, shop, well" in report)
        assertTrue("Every procedural building grew a plan; nothing fell back." in report)
    }

    // ---- robustness across seeds -------------------------------------------------------

    @Test
    fun `every procedural core building holds up across seeds`() {
        var fallbacks = 0
        for (id in grown) {
            val def = registry.buildings.getValue(id)
            for (seed in 1L..20L) {
                if (BuildingCache.get(def.copy(genSeed = seed)) == null) fallbacks++
            }
        }
        assertEquals("some core buildings failed to grow under other seeds", 0, fallbacks)
    }

    // ---- the guard rail ----------------------------------------------------------------

    @Test
    fun `generation options without the opt-in are named, not silently ignored`() {
        val pack = TestPacks.base().copy(
            buildings = TestPacks.base().buildings + BuildingDef(
                id = "eager", name = "Eager House", type = "house", x = 60f, z = 0f,
                profile = "cottage", genFloors = 2,
            ),
        )
        val r = TestPacks.build(pack)
        // Building ids are pack-qualified in issues ("base:eager"), so match on the suffix.
        assertTrue(
            "no warning named the forgotten procedural flag: ${r.issues.filter { it.entry.endsWith("eager") }}",
            r.issues.any { it.severity == "warning" && it.entry.endsWith(":eager") && "procedural" in it.message },
        )
    }
}
