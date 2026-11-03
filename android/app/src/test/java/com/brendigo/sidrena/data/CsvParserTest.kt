package com.brendigo.sidrena.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CsvParserTest {
    @Test fun quotedSemicolonAndEmbeddedNewline() {
        val rows = CsvParser.parse("\uFEFFnaziv;maloprodajna_cijena\r\n\"Kava; velika\";2,50\r\n\"Više\nredaka\";3,00\r\n")
        assertEquals(3, rows.size)
        assertEquals("Kava; velika", rows[1][0])
        assertEquals("Više\nredaka", rows[2][0])
    }

    @Test fun escapedQuotes() {
        assertEquals("A \"B\"", CsvParser.parse("naziv\n\"A \"\"B\"\"\"")[1][0])
    }

    @Test fun rejectsTruncatedCsv() {
        assertThrows(IllegalArgumentException::class.java) { CsvParser.parse("naziv;sku\n\"nezatvoreno") }
    }

    @Test fun rejectsFormulaLikeMalformedHeaders() {
        assertThrows(IllegalArgumentException::class.java) { CsvParser.items("name;price\nX;2") }
    }

    @Test fun rejectsDuplicateOrEmptyColumnNamesToPreventMisleadingPrices() {
        for (source in listOf(
            "naziv;maloprodajna_cijena;maloprodajna_cijena\nArtikl;10,00;5,00\n",
            "naziv;maloprodajna_cijena;\nArtikl;10,00;5,00\n",
            "naziv;MALOPRODAJNA_CIJENA;maloprodajna_cijena\nArtikl;10,00;5,00\n",
        )) {
            assertThrows(IllegalArgumentException::class.java) { CsvParser.items(source) }
        }
    }

    @Test fun rejectsMalformedColumnCountsWithoutMixingAmounts() {
        assertThrows(IllegalArgumentException::class.java) {
            CsvParser.items("naziv;maloprodajna_cijena\nArtikl;10,00;5,00\n")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CsvParser.items("naziv;maloprodajna_cijena;sku\nArtikl;10,00\n")
        }
    }

    @Test fun parsesSidrenaColumnsByName() {
        val items = CsvParser.items("id;vrsta;naziv;maloprodajna_cijena;sidrena_cijena;sidrena_datum\r\nd7;usluga;Dostava;4,50;5,00;2026-10-09\r\n")
        assertEquals("Dostava", items.single().name)
        assertEquals("5,00", items.single().referencePrice)
    }
}
