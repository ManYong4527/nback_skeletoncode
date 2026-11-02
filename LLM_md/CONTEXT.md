# CONTEXT: KTH HI1033 / CM2023 Course Constraints

DO NOT add third-party game engines, reactive frameworks (RxJava), or external sequence libraries.

## 1. Oral Exam Defense Requirements
- Always explain the "Why": Why Room over SharedPreferences? Why StateFlow? Why Coroutines?
- Avoid overly clever, unreadable Kotlin shorthand (e.g., obscure bitwise tricks or convoluted chained higher-order functions) where a clear, well-structured function is easier to explain.

## 2. Unit Test Coverage Requirements
- Must provide passing, deterministic JUnit tests for:
  1. `SequenceGeneratorTest`: Sequence length, match rate verification (~20-40%), and strict verification that indices `0 until n` contain zero matches.
  2. `DoublePressPreventionTest`: Rapid successive taps within the same event tick only increment matches once.
  3. `ScoringEngineTest`: Calculation of hits/false alarms, boundary verification preventing negative scores.