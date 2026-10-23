package com.studyapp.decaycharge.data.importer

import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllabusParsersTest {
    @Test
    fun `parses subject colon chapter syntax`() {
        val draft = SyllabusTextParser.parse(
            """
            Physiology: Renal Regulation
            Physiology: Coronary Circulation
            Pathology: Acute Inflammation
            """.trimIndent(),
        )

        assertEquals(2, draft.subjects.size)
        assertEquals("Physiology", draft.subjects[0].name)
        assertEquals(2, draft.subjects[0].chapters.size)
        assertEquals("Renal Regulation", draft.subjects[0].chapters[0].title)
        assertEquals("Acute Inflammation", draft.subjects[1].chapters.single().title)
    }

    @Test
    fun `parses indented hierarchy with unit metadata and target`() {
        val draft = SyllabusTextParser.parse(
            """
            Physiology
              Cardiovascular
              - Coronary Circulation (Difficulty: Heavy, Target: 60m)
              - Cardiac Cycle (Light, 1h)

            Pathology
              General Pathology
              - Acute Inflammation (Target: 30m)
            """.trimIndent(),
        )

        assertEquals(2, draft.subjects.size)
        val physiology = draft.subjects[0]
        assertEquals("Cardiovascular", physiology.chapters[0].unitName)
        assertEquals(ChapterDifficulty.HEAVY, physiology.chapters[0].difficulty)
        assertEquals(60, physiology.chapters[0].targetMinutes)
        assertEquals(ChapterDifficulty.LIGHT, physiology.chapters[1].difficulty)
        assertEquals(60, physiology.chapters[1].targetMinutes)
        assertEquals(30, draft.subjects[1].chapters[0].targetMinutes)
    }

    @Test
    fun `parses bullet with unit prefix`() {
        val draft = SyllabusTextParser.parse(
            """
            Pharmacology
              - Autonomic Drugs: Beta Blockers (Difficulty: Medium, Target: 45m)
            """.trimIndent(),
        )

        val chapter = draft.subjects.single().chapters.single()
        assertEquals("Autonomic Drugs", chapter.unitName)
        assertEquals("Beta Blockers", chapter.title)
        assertEquals(ChapterDifficulty.MEDIUM, chapter.difficulty)
    }

    @Test
    fun `ignores empty input and creates no subjects`() {
        assertTrue(SyllabusTextParser.parse("  \n\n# comment\n").subjects.isEmpty())
    }

    @Test
    fun `parses csv with header and quoted commas`() {
        val draft = SyllabusCsv.parse(
            """
            Subject,Unit,Chapter,Difficulty,TargetMinutes
            Physiology,Cardiovascular,"Coronary Circulation, part 1",Heavy,60
            Physiology,Respiratory,Gas Exchange,Light,30
            """.trimIndent(),
        )

        assertEquals(1, draft.subjects.size)
        assertEquals(2, draft.subjects.single().chapters.size)
        assertEquals("Coronary Circulation, part 1", draft.subjects.single().chapters[0].title)
        assertEquals(ChapterDifficulty.HEAVY, draft.subjects.single().chapters[0].difficulty)
        assertEquals(60, draft.subjects.single().chapters[0].targetMinutes)
    }

    @Test
    fun `csv export escapes commas and round trips`() {
        val subject = Subject(
            id = 1,
            name = "Physiology",
            shortName = "PHY",
            colorHex = 0xFF0F5132,
            examWeight = 1f,
        )
        val chapter = Chapter(
            id = 1,
            subjectId = 1,
            subjectName = "Physiology",
            subjectShortName = "PHY",
            subjectColorHex = 0xFF0F5132,
            unitName = "Cardiovascular",
            title = "Coronary Circulation, part 1",
            status = ChapterStatus.ACTIVE,
            battery = 72f,
            stabilityDays = 4f,
            daysSinceCharge = 2,
            drainRatePerDay = 1f,
            difficultyWeight = 1.2f,
            targetMinutes = 60,
            reviewCount = 1,
            note = null,
        )

        val csv = SyllabusCsv.encode(listOf(subject), listOf(chapter))
        val parsed = SyllabusCsv.parse(csv)

        assertEquals("Coronary Circulation, part 1", parsed.subjects.single().chapters.single().title)
        assertEquals(ChapterDifficulty.HEAVY, parsed.subjects.single().chapters.single().difficulty)
        assertEquals(60, parsed.subjects.single().chapters.single().targetMinutes)
    }

    @Test
    fun `draft selection and batch edits only touch selected chapters`() {
        val draft = SyllabusDraft(
            title = "Test",
            subjects = listOf(
                DraftSubject(
                    name = "Physiology",
                    colorHex = "#0F5132",
                    chapters = listOf(
                        DraftChapter("One", selected = true),
                        DraftChapter("Two", selected = false),
                    ),
                ),
            ),
        )

        val updated = draft
            .applyDifficulty(ChapterDifficulty.HEAVY)
            .applyTarget(60)

        assertEquals(1, updated.chapterCount)
        assertEquals(ChapterDifficulty.HEAVY, updated.subjects[0].chapters[0].difficulty)
        assertEquals(ChapterDifficulty.MEDIUM, updated.subjects[0].chapters[1].difficulty)
        assertEquals(60, updated.subjects[0].chapters[0].targetMinutes)
        assertEquals(45, updated.subjects[0].chapters[1].targetMinutes)
    }
}
