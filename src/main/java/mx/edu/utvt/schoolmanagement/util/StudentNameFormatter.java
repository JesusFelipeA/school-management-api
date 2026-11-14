package mx.edu.utvt.schoolmanagement.util;

import java.util.stream.Stream;

/**
 * Formatea los componentes del nombre de un estudiante en una sola cadena.
 *
 * <p>Clase utilitaria sin estado: no modifica entidades ni DTO y solo usa Java estándar.
 *
 * <p>Ejemplos:
 * <ul>
 *   <li>{@code format("  Ana   María ", " López  ", null)} → {@code "Ana María López"}</li>
 *   <li>{@code format("Luis", "", "  Hernández ")} → {@code "Luis Hernández"}</li>
 *   <li>{@code format(null, "   ", "")} → {@code ""}</li>
 * </ul>
 */
public final class StudentNameFormatter {

    private StudentNameFormatter() {
        // Clase utilitaria: no debe instanciarse.
    }

    /**
     * Une el nombre, el apellido paterno y el apellido materno, en ese orden,
     * omitiendo los valores nulos o vacíos y normalizando los espacios sobrantes.
     *
     * @param nombre          nombre del estudiante
     * @param apellidoPaterno apellido paterno del estudiante
     * @param apellidoMaterno apellido materno del estudiante
     * @return los componentes disponibles unidos por un espacio, o una cadena vacía
     *         si ninguno contiene texto
     */
    public static String format(String nombre, String apellidoPaterno, String apellidoMaterno) {
        return joinNonBlankParts(nombre, apellidoPaterno, apellidoMaterno);
    }

    /**
     * Limpia cada parte (recorta y colapsa espacios internos), descarta las vacías
     * y une el resto con un solo espacio.
     */
    private static String joinNonBlankParts(String... parts) {
        return Stream.of(parts)
                .filter(value -> value != null)
                .map(value -> value.strip().replaceAll("(?U)\\s+", " "))
                .filter(value -> !value.isEmpty())
                .reduce((first, second) -> first + " " + second)
                .orElse("");
    }
}
