package uk.ac.warwick.plus

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import uk.ac.warwick.plus.data.ProbeEndpoint

/** Opt-in, read-only check. Never included in ordinary emulator acceptance. */
class LiveSessionSmokeTest {
    @Test fun existingSessionCanReadCourseworkAndLibrary() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveSession") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PlusApplication
        val coursework = app.api.probe(ProbeEndpoint.COURSEWORK)
        val library = app.api.probe(ProbeEndpoint.LIBRARY)
        val timetable = app.api.timetable(app.api.user())
        for (result in listOf(coursework, library)) {
            assertEquals(200, result.httpStatus)
            assertTrue(result.success)
            assertNotNull(result.itemCount)
        }
        // Only aggregate counts are logged. No field values or credentials leave the phone.
        android.util.Log.i("MyWarwickPlus", "Native API probes succeeded; coursework=${coursework.itemCount}; library=${library.itemCount}")
        InstrumentationRegistry.getInstrumentation().sendStatus(2, android.os.Bundle().apply {
            putString("summary", "Native API probes: timetable=${timetable.size}, namedModules=${timetable.count { it.moduleName.isNotBlank() }}, coursework=${coursework.itemCount}, library=${library.itemCount}; success")
        })
    }
}
