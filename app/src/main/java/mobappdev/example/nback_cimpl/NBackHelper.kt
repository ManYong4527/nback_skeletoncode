package mobappdev.example.nback_cimpl

/**
 * Historical JNI bridge to the C sequence generator.
 *
 * As of 2026-11 this class is INACTIVE. The sequence logic now lives in
 * the pure-Kotlin [mobappdev.example.nback_cimpl.domain.SequenceGenerator],
 * which keeps the domain layer JVM-testable (see SPEC §2 and STYLE.md §1).
 *
 * The file is retained — alongside the original C source under
 * `app/src/main/cpp/` — purely as a reference point: it documents how the
 * earlier skeleton delegated generation to native code, so an oral exam
 * discussion can contrast the two approaches. Loading the native library
 * and the `external fun` declaration are both commented out so no `.so`
 * is required at runtime (also resolves the 16KB ELF-alignment warning).
 *
 * Original author: Yeetivity (2023-08-25)
 */
class NBackHelper {

    // private external fun createNBackString(
    //     size: Int, combinations: Int, percentMatch: Int, nBack: Int,
    // ): IntArray

    /**
     * Preserved only so references to this symbol compile. Throws on
     * call — the pure-Kotlin [SequenceGenerator] is the live
     * implementation and should be used instead.
     */
    @Suppress("UNUSED_PARAMETER")
    fun generateNBackString(
        size: Int,
        combinations: Int,
        percentMatch: Int,
        nBack: Int,
    ): IntArray {
        throw UnsupportedOperationException(
            "NBackHelper's native implementation is disabled. " +
                "Use mobappdev.example.nback_cimpl.domain.SequenceGenerator instead.",
        )
    }

    // companion object {
    //     init {
    //         System.loadLibrary("JniBridge")
    //     }
    // }
}
