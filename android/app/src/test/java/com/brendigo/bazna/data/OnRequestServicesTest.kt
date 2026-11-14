package com.brendigo.bazna.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OnRequestServicesTest {
    private val outlets = listOf(Outlet(
        "Rijeka", "RI-01",
        "https://primjer.hr/ri-p.csv", "https://primjer.hr/ri-s.csv",
    ))
    private fun example() = JSONObject(
        """{"schema":1,"items":[
          {"id":"Q-1","name":"Montaža","criteria":"Opseg radova","outlet":""},
          {"id":"Q-2","name":"Dostava","criteria":"Mjesto dostave","outlet":"RI-01"}
        ]}"""
    )

    @Test fun readsPublishedDescriptionsWithoutFabricatingNumericPrices() {
        val rows = OnRequestServices.parse(example(), outlets)
        assertEquals(2, rows.size)
        assertEquals("Opseg radova", rows[0].criteria)
        assertEquals(listOf("Montaža"), OnRequestServices.visible(rows, "", "", 0).map { it.name })
        assertEquals(listOf("Dostava"), OnRequestServices.visible(rows, "RI-01", "mjesto", 0).map { it.name })
        assertTrue(OnRequestServices.visible(rows, "RI-01", "", 1).isEmpty())
        assertTrue(OnRequestServices.visible(rows, "", "nepostojeće", 0).isEmpty())
    }

    @Test fun rejectsInvalidOutletsDuplicatesAndBogusPrices() {
        for (mutation in listOf(
            """{"schema":1,"items":[{"id":"X","name":"X","criteria":"Y","outlet":"UNKNOWN"}]}""",
            """{"schema":1,"items":[{"id":"1","name":"X","criteria":"Y","outlet":"",
                "maloprodajna_cijena":"19,99"}]}""",
            """{"schema":1,"items":[{"id":"1","name":"X","criteria":"","outlet":""}]}""",
            """{"schema":1,"items":[{"id":"1","name":"X","criteria":"Y","outlet":""},
                {"id":"1","name":"X","criteria":"Y","outlet":""}]}""",
            """{"schema":2,"items":[]}""",
            """{"schema":1,"items":"not an array"}""",
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                OnRequestServices.parse(JSONObject(mutation), outlets)
            }
        }
    }
}
