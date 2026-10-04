package com.chaduvukondi.firstu

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.chaduvukondi.firstu.data.model.Difficulty
import com.chaduvukondi.firstu.data.model.ExpectedOutput
import com.chaduvukondi.firstu.data.model.Question
import com.chaduvukondi.firstu.data.model.isNew
import com.chaduvukondi.firstu.data.repository.QuestionSource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class QuestionSourceTest {

    private lateinit var context: Context
    private lateinit var questionSource: QuestionSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        questionSource = QuestionSource(context)
    }

    @Test
    fun testLoadBundledQuestions() {
        val questions = questionSource.loadAllQuestions()
        assertTrue("Bundled questions should not be empty", questions.isNotEmpty())
        val q1 = questionSource.loadQuestion("sql_001")
        assertNotNull("sql_001 should load from bundled assets", q1)
        assertEquals("sql_001", q1?.id)
    }

    @Test
    fun testAtomicSaveAndLayer1Precedence() {
        val customQuestion = Question(
            id = "sql_999",
            contentVersion = 1,
            title = "Test Dynamic Question",
            difficulty = Difficulty.EASY,
            description = "A dynamically downloaded question",
            tables = emptyList(),
            expectedOutput = ExpectedOutput(columns = listOf("val"), rows = emptyList()),
            solutionQuery = "SELECT 1 AS val;",
            addedAt = "2026-09-28"
        )

        val rawJson = Json.encodeToString(customQuestion)
        val saved = questionSource.saveQuestionAtomically("sql_999", rawJson.toByteArray())
        assertTrue("Atomic save should succeed", saved)
        assertTrue("hasInternalFile should return true", questionSource.hasInternalFile("sql_999"))

        val loaded = questionSource.loadQuestion("sql_999")
        assertNotNull("Loaded question should not be null", loaded)
        assertEquals("Test Dynamic Question", loaded?.title)
        assertEquals(1, questionSource.getLocalContentVersion("sql_999"))

        // Clean up
        val file = File(context.filesDir, "questions/sql_999.json")
        if (file.exists()) file.delete()
    }

    @Test
    fun testIsNewCalculation() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = sdf.format(Date())

        val newQ = Question(
            id = "new_q",
            title = "New Question",
            difficulty = Difficulty.EASY,
            description = "Test",
            tables = emptyList(),
            expectedOutput = ExpectedOutput(columns = emptyList(), rows = emptyList()),
            solutionQuery = "SELECT 1;",
            addedAt = todayStr
        )
        assertTrue("Question added today should be marked isNew", newQ.isNew())

        val oldQ = newQ.copy(addedAt = "2020-01-01")
        assertFalse("Question added years ago should not be isNew", oldQ.isNew())

        val noDateQ = newQ.copy(addedAt = null)
        assertFalse("Question with null date should not be isNew", noDateQ.isNew())
    }
}
