package com.example

import com.example.domain.engine.ChangelogRepository
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testChangelogRepository_hasCurrentVersion() {
    val current = ChangelogRepository.getCurrentVersion()
    assertEquals("2.2.0", current.version)
    assertTrue(current.isCurrent)
    assertTrue(current.categories.isNotEmpty())
    assertTrue(current.headline.contains("Quick Share"))
  }

  @Test
  fun testChangelogRepository_allReleasesHaveCategories() {
    val releases = ChangelogRepository.releases
    assertTrue(releases.size >= 4)
    for (rel in releases) {
      assertFalse(rel.version.isBlank())
      assertFalse(rel.headline.isBlank())
      assertTrue(rel.categories.isNotEmpty())
      for (cat in rel.categories) {
        assertTrue(cat.features.isNotEmpty())
      }
    }
  }
}
