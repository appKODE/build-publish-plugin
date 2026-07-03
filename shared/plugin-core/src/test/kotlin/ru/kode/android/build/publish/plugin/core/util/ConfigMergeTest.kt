package ru.kode.android.build.publish.plugin.core.util

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Inject

class ConfigMergeTest {
    private val objects: ObjectFactory = ProjectBuilder.builder().build().objects

    private fun container(): NamedDomainObjectContainer<MergeTestConfig> = objects.domainObjectContainer(MergeTestConfig::class.java)

    @BeforeEach
    fun setup() {
        // no-op: each test builds its own container
    }

    @Test
    fun `unset scalar inherits from common`() {
        val container = container()
        container.common { it.title.set("common-title") }
        container.buildVariant("release") { it.subtitle.set("release-subtitle") }

        val release = container.getByName("release")
        assertEquals("common-title", release.title.get())
        assertEquals("release-subtitle", release.subtitle.get())
    }

    @Test
    fun `set scalar overrides common`() {
        val container = container()
        container.common { it.title.set("common-title") }
        container.buildVariant("release") { it.title.set("release-title") }

        assertEquals("release-title", container.getByName("release").title.get())
    }

    @Test
    fun `collection replaces common by default`() {
        val container = container()
        container.common { it.tags.addAll("a", "b") }
        container.buildVariant("release") { it.tags.add("c") }

        assertEquals(setOf("c"), container.getByName("release").tags.get())
    }

    @Test
    fun `untouched collection inherits common`() {
        val container = container()
        container.common { it.tags.addAll("a", "b") }
        container.buildVariant("release") { it.title.set("release-title") }

        assertEquals(setOf("a", "b"), container.getByName("release").tags.get())
    }

    @Test
    fun `collection with append strategy unions with common`() {
        val container = container()
        container.common { it.tags.addAll("a", "b") }
        container.buildVariant("release") { it.tags(CollectionStrategy.APPEND, "c") }

        assertEquals(setOf("a", "b", "c"), container.getByName("release").tags.get())
    }

    @Test
    fun `nested object merges field by field`() {
        val container = container()
        container.common {
            it.auth.username.set("common-user")
            it.auth.password.set("common-pass")
        }
        container.buildVariant("release") { it.auth.password.set("release-pass") }

        val auth = container.getByName("release").auth
        assertEquals("common-user", auth.username.get())
        assertEquals("release-pass", auth.password.get())
    }

    @Test
    fun `nested container unions by element name`() {
        val container = container()
        container.common {
            it.child("a") { child -> child.value.set("common-a") }
            it.child("b") { child -> child.value.set("common-b") }
        }
        container.buildVariant("release") {
            it.child("b") { child -> child.value.set("release-b") }
        }

        val children = container.getByName("release").children
        assertEquals(setOf("a", "b"), children.names.toSet())
        assertEquals("common-a", children.getByName("a").value.get())
        assertEquals("release-b", children.getByName("b").value.get())
    }

    @Test
    fun `replace strategy inherits nothing from common`() {
        val container = container()
        container.common {
            it.title.set("common-title")
            it.tags.addAll("a", "b")
        }
        container.buildVariant("release", MergeStrategy.REPLACE) { it.subtitle.set("release-subtitle") }

        val release = container.getByName("release")
        assertFalse(release.title.isPresent)
        assertTrue(release.tags.get().isEmpty())
        assertEquals("release-subtitle", release.subtitle.get())
    }

    @Test
    fun `common lookup returns null when absent so variant stands alone`() {
        val container = container()
        container.buildVariant("release") { it.title.set("release-title") }

        val release = container.getByName("release")
        assertEquals("release-title", release.title.get())
        assertNull(container.findByName(COMMON_CONTAINER_NAME))
    }
}

abstract class MergeTestConfig
    @Inject
    constructor(
        objects: ObjectFactory,
    ) : CommonConfigMergeable<MergeTestConfig> {
        abstract val name: String

        @get:Input
        @get:Optional
        abstract val title: Property<String>

        @get:Input
        @get:Optional
        abstract val subtitle: Property<String>

        @get:Input
        @get:Optional
        internal abstract val tags: SetProperty<String>

        @get:Nested
        val auth: MergeTestAuth = objects.newInstance(MergeTestAuth::class.java)

        internal val children: NamedDomainObjectContainer<MergeTestChild> =
            objects.domainObjectContainer(MergeTestChild::class.java)

        private var tagsStrategy: CollectionStrategy = CollectionStrategy.REPLACE

        fun tags(
            strategy: CollectionStrategy,
            vararg values: String,
        ) {
            tagsStrategy = strategy
            tags.addAll(values.toList())
        }

        fun child(
            name: String,
            action: Action<MergeTestChild>,
        ) {
            children.register(name, action)
        }

        override fun inheritFrom(common: MergeTestConfig) {
            title.convention(common.title)
            subtitle.convention(common.subtitle)
            tags.inheritFrom(common.tags, tagsStrategy)
            auth.inheritFrom(common.auth)
            children.inheritNamedFrom(common.children)
        }
    }

abstract class MergeTestAuth : CommonConfigMergeable<MergeTestAuth> {
    @get:Input
    @get:Optional
    abstract val username: Property<String>

    @get:Input
    @get:Optional
    abstract val password: Property<String>

    override fun inheritFrom(common: MergeTestAuth) {
        username.convention(common.username)
        password.convention(common.password)
    }
}

abstract class MergeTestChild : CommonConfigMergeable<MergeTestChild> {
    abstract val name: String

    @get:Input
    @get:Optional
    abstract val value: Property<String>

    override fun inheritFrom(common: MergeTestChild) {
        value.convention(common.value)
    }
}
