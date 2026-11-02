# STYLE: Kotlin & Jetpack Compose Architectural Guidelines

## 1. Architectural Pattern
- **Strict MVVM (Model-View-ViewModel)**:
  - **Model**: Pure Kotlin domain models, immutable data classes (`GameRoundState`, `StimulusEvent`, `ScoreRecord`).
  - **Domain / Logic**: Pure Kotlin engines/use-cases independent of Android SDK (e.g., `SequenceGenerator`, `ScoringEngine`). Must be 100% unit-testable on JVM without mocks or Robolectric.
  - **ViewModel**: Manages UI state using `StateFlow<T>`. Handles coroutine scopes (`viewModelScope`), audio triggers, and DataStore/Room interactions. Never stores Context references.
  - **View (Compose)**: Stateless composables preferred. State hoisting for interactivity. View models injected at screen level.
- **Navigation**:
  - ViewModel must NOT handle navigation directly. Navigation events exposed via single-shot events or hoisted callbacks to the NavHost.

## 2. Naming Conventions & Documentation
- Self-explanatory class, function, and parameter names (`generateSequence`, `registerVisualMatch`, `isFirstNTick`).
- Every non-trivial function and class must include concise KDoc explaining purpose, parameters, and invariants.
- No "God Classes": Separate timer/engine, audio playback, storage, and UI layers into decoupled components.

## 3. Asynchronous & Threading Model
- Kotlin Coroutines & Flow:
  - Database (Room) and Preferences (DataStore) operations dispatched strictly on `Dispatchers.IO`.
  - Pure calculation/generation on `Dispatchers.Default`.
  - UI State updates collected on `Dispatchers.Main` via `collectAsStateWithLifecycle()`.

## 4. UI Design & Styling
- Minimalist, clean aesthetics with soft rounded corners and subtle shadows (Material 3).
- Strict separation of visual styling into reusable themes/tokens.