package app.cash.cdf.generation

import app.cash.cdf.generation.compiled.BuiltInType
import app.cash.cdf.generation.compiled.BusinessUnitContext
import app.cash.cdf.generation.compiled.CdfPackageInfo
import app.cash.cdf.generation.compiled.Destination
import app.cash.cdf.generation.compiled.Entity
import app.cash.cdf.generation.compiled.Enum
import app.cash.cdf.generation.compiled.Event
import app.cash.cdf.generation.compiled.TagGroup
import app.cash.cdf.utils.compileResource
import app.cash.cdf.utils.copyResource
import app.cash.cdf.utils.execute
import com.samskivert.mustache.Mustache
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import java.time.format.DateTimeFormatter

class MkdocsGenerator(
  override val fileSystem: FileSystem = FileSystem.SYSTEM,
  private val mustache: Mustache.Compiler,
) : CdfGenerator, FileSystemAware {
  private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm zzz")
  override fun generate(
    config: CdfPackageInfo,
    entities: List<Entity>,
    enums: List<Enum>,
    tagGroups: List<TagGroup>,
    destinations: List<Destination>, // Unused
    businessUnitContext: BusinessUnitContext,
    output: Path,
  ) {
    fileSystem.createDirectories(output)

    // Write mkdocs config

    val orgSpecificMkdocsTemplate = if (businessUnitContext.isSquare) "sq_mkdocs.yml.mustache" else "mkdocs.yml.mustache"
    val mkdocsTemplate = mustache.compileResource(MKDOCS_RESOURCES_PATH / orgSpecificMkdocsTemplate)
    mkdocsTemplate.execute(
      object {
        val entities = entities
        val tagGroups = tagGroups
        val hasTagGroups = tagGroups.isNotEmpty()
        val update_time = dateFormatter.format(config.packageTime)
      },
      output / "mkdocs.yml",
    )

    val entityTemplate = mustache.compileResource(MKDOCS_RESOURCES_PATH / "entity.md.mustache")
    val actionTemplate = mustache.compileResource(MKDOCS_RESOURCES_PATH / "action.md.mustache")
    val eventTemplate = mustache.compileResource(MKDOCS_RESOURCES_PATH / "event.md.mustache")

    // Write Entity pages
    entities.forEach { entity ->
      val entityDir = (output / MKDOCS_DOCS_PATH / entity.name.default).also { fileSystem.createDirectories(it) }

      entityTemplate.execute(
        object {
          val render_entity = entity
          val git_branch = config.git_branch
        },
        entityDir / "index.md",
      )

      // Write Action pages
      entity.actions.forEach { action ->
        val actionDir = (entityDir / action.name).also { fileSystem.createDirectories(it) }
        actionTemplate.execute(
          object {
            val render_action = action
            val git_branch = config.git_branch
            val entity_name = entity.name
            val entity_owners = entity.entity_owners
            val entity_name_lower_snake_case = entity.entity_name_lower_snake_case
          },
          actionDir / "index.md",
        )

        // Write Event pages
        action.events.forEach { event ->
          eventTemplate.execute(
            object {
              val render_event = event
              val git_branch = config.git_branch
              val entity_name = entity.name
              val entity_owners = entity.entity_owners
              val action_owners = action.action_owners
              val entity_name_lower_snake_case = entity.entity_name_lower_snake_case
            },
            actionDir / "${event.event_name}.md",
          )
        }
      }
    }

    // Write Tag pages
    val tagDir = (output / MKDOCS_DOCS_PATH / "tags").also { fileSystem.createDirectories(it) }
    val tagTemplate = mustache.compileResource(MKDOCS_RESOURCES_PATH / "tag.md.mustache")
    tagGroups.forEach { tagGroup ->
      tagTemplate.execute(tagGroup, tagDir / "${tagGroup.identifier}.md")
    }

    // Write Global Types page
    val globalTypesTemplate = mustache.compileResource(MKDOCS_RESOURCES_PATH / "global_types.md.mustache")
    globalTypesTemplate.execute(
      object {
        val git_branch = config.git_branch
        val built_in_types = BuiltInType.values()
        val custom_types = enums
        val has_custom_types = enums.isNotEmpty()
      },
      output / MKDOCS_DOCS_PATH / "global_types.md",
    )

    // Write Event Audit  page
    val eventAuditTemplate = mustache.compileResource(MKDOCS_RESOURCES_PATH / "event_audit.md.mustache")
    eventAuditTemplate.execute(
      object {
        val event_count = entities.countFromEvents { it.size }
        val ios_event_tracked_count = entities.countFromEvents { events -> events.count { it.event_usage.ios != null } }
        val android_event_tracked_count = entities.countFromEvents { events -> events.count { it.event_usage.android != null } }

        val entities = entities
      },
      output / MKDOCS_DOCS_PATH / "event_audit.md",
    )

    // Copy static files
    copyResource(MKDOCS_RESOURCES_PATH / "css/app.css", output / MKDOCS_DOCS_PATH / "css")

    val orgSpecificLoc: String
    if (businessUnitContext.isSquare) {
      orgSpecificLoc = "square"
    } else {
      orgSpecificLoc = "cash"
    }

    val imgPath = MKDOCS_RESOURCES_PATH / ("img/$orgSpecificLoc")
    FileSystem.RESOURCES.list(imgPath).forEach { static ->
      copyResource(static, output / MKDOCS_DOCS_PATH / "img")
    }

    val staticPath = MKDOCS_RESOURCES_PATH / "static"
    FileSystem.RESOURCES.list(staticPath)
      .filter { FileSystem.RESOURCES.metadata(it).isRegularFile }
      .forEach { static ->
        copyResource(static, output / MKDOCS_DOCS_PATH)
      }

    val staticOrgPath = staticPath / orgSpecificLoc
    FileSystem.RESOURCES.list(staticOrgPath).forEach { static ->
      copyResource(static, output / MKDOCS_DOCS_PATH)
    }
  }

  private fun List<Entity>.countFromEvents(sum: (List<Event>) -> Int): Int =
    fold(0) { acc, entity -> acc + entity.actions.fold(0) { count, action -> count + sum(action.events) } }

  companion object {
    private val MKDOCS_RESOURCES_PATH = "/templates/mkdocs".toPath()
    private const val MKDOCS_DOCS_PATH = "docs"
  }
}
