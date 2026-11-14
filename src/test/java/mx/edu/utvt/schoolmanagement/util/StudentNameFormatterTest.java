package mx.edu.utvt.schoolmanagement.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentNameFormatterTest {

    @Test
    void formatsNameAndBothSurnames() {
        assertEquals("Ana María López Díaz",
                StudentNameFormatter.format("  Ana   María ", " López  ", "Díaz"));
    }

    @Test
    void omitsNullMaternalSurname() {
        assertEquals("Ana María López",
                StudentNameFormatter.format("  Ana   María ", " López  ", null));
    }

    @Test
    void omitsNullAndBlankParts() {
        assertEquals("Luis Hernández",
                StudentNameFormatter.format("Luis", "", "  Hernández "));
    }

    @Test
    void returnsEmptyStringWhenAllPartsAreBlank() {
        assertEquals("", StudentNameFormatter.format(null, "   ", ""));
    }

    @Test
    void collapsesTabsAndLineBreaks() {
        assertEquals("José Pérez", StudentNameFormatter.format("\tJosé\n", "Pérez", null));
    }
}
