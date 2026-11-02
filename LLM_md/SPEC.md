# SPEC: Native Dual N-Back Mobile App (Android)

## 1. Project Overview
- Target Platform: Android
- Language & UI: Kotlin, Jetpack Compose
- Target: Must implement Dual N-Back (Visual + Auditory) and at least 3 advanced features.

## 2. Core Game Mechanics & Logic
- **N-Back Task**:
  - Stimuli presented sequentially with configurable tick duration.
  - Visual Stimulus: Highlighted cell in an $M \times M$ grid (default 3x3).
  - Auditory Stimulus: Spoken letter from a fixed, phonetically distinct set (e.g., C, D, G, K, P, T, V).
  - Dual N-Back: Both visual and auditory stimuli occur simultaneously and independently.
- **Sequence Generation**:
  - Custom deterministic sequence generator (NO external game libraries).
  - Target match rate enforced strictly between 20% and 40% for each modality independently.
  - Hard constraint: The first `n` events MUST NEVER contain a match.
- **Input & Event Guards**:
  - Separate match buttons for Visual and Audio.
  - Touches ignored: before round starts, after round ends, and during the first `n` events.
  - Double-press prevention: Only the first tap per stimulus per modality is registered; subsequent taps within the same tick are ignored.
- **Scoring System**:
  - Explicit tracking of Hits, Misses, and False Alarms.
  - Score formula must prevent negative results: `Score = max(0, (Hits * 10) - (FalseAlarms * 5))`.
  - Non-offensive feedback animation on incorrect tap (e.g., subtle button shake or color pulse).
- **Lifecycle & Timer Clean-up**:
  - Robust coroutine-based game loop (`delay` / flow ticker).
  - Game timer is cancelled cleanly when the round ends or when the user navigates away mid-round.

## 3. Advanced Features (Aiming for 3+)
1. **Persistent Settings**:
   - Configurable: $n$ value (1-5), event duration (1000ms-3000ms), events per round (15-30), grid size (3x3 or 5x5), letter set size.
   - Backed by Jetpack DataStore Preferences.
2. **Score History & Local Database**:
   - Player inputs name upon game completion.
   - Scores persisted in a local Room database (`id`, `playerName`, `score`, `nValue`, `timestamp`).
   - History screen displays entries with sorting capabilities (by score, name, date).
3. **Responsive Layout & Accessibility**:
   - Grid maintains an exact 1:1 aspect ratio across portrait, landscape, phone, and tablet screen sizes.
   - Touch targets meet accessibility guidelines (minimum 48dp x 48dp).
   - TTS fallback mechanism (visual toast/snack or UI text cue) if engine is unavailable or language data is missing.