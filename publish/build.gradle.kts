// Publishes the already-built jar (../build/libs) to CurseForge and Modrinth.
// Build the jar first with ../build.sh, then:
//   ./gradlew publishMods             -> dry run (nothing is uploaded)
//   ./gradlew publishMods -Prelease   -> real upload
// Tokens come from the environment only: CURSEFORGE_TOKEN, MODRINTH_TOKEN.
// -Pplatforms=curseforge (or modrinth) limits the upload to one platform.
import java.util.Properties

plugins {
    id("me.modmuss50.mod-publish-plugin") version "2.2.1"
}

val modProps = Properties().apply {
    rootDir.resolve("../mod.properties").reader(Charsets.UTF_8).use { load(it) }
}
val modVersion = modProps.getProperty("mod_version")
val mcVersion = modProps.getProperty("minecraft_version")
val fullVersion = "$modVersion+mc$mcVersion"
val jarFile = rootDir.resolve("../build/libs/axiom-utilities-$fullVersion.jar")

fun changelogFor(ver: String): String {
    val lines = rootDir.resolve("../CHANGELOG.md").readLines(Charsets.UTF_8)
    val start = lines.indexOfFirst { it.trim() == "## $ver" }
    if (start < 0) return "See CHANGELOG.md"
    val rest = lines.drop(start + 1)
    val end = rest.indexOfFirst { it.startsWith("## ") }.let { if (it < 0) rest.size else it }
    return rest.take(end).joinToString("\n").trim()
}

val targets = providers.gradleProperty("platforms").getOrElse("curseforge,modrinth").split(",").map { it.trim() }

publishMods {
    file = layout.file(provider { jarFile })
    displayName = "Axiom Utilities $modVersion (MC $mcVersion)"
    version = fullVersion
    changelog = changelogFor(fullVersion)
    type = STABLE
    modLoaders.add("fabric")
    dryRun = !providers.gradleProperty("release").isPresent

    if (targets.contains("curseforge")) curseforge {
        projectId = "1824561"
        projectSlug = "axiom-utilities"
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.add(mcVersion)
        javaVersions.add(JavaVersion.VERSION_25)
        client = true
        server = false
    }

    if (targets.contains("modrinth")) modrinth {
        projectId = "72svSPXp"
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.add(mcVersion)
        requires("axiom")
    }
}
