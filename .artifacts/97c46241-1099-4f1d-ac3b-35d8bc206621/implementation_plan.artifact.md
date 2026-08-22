# Implementation Plan - Power-Up Bubbles

This plan outlines the integration of four special Power-Up Bubbles into the TapTap game: Freeze, Rainbow, Shield, and Blast. Each bubble will have a unique visual representation and a specific gameplay effect.

## User Review Required

> [!IMPORTANT]
> - **Power-up Rarity**: Power-ups will spawn with a 15% cumulative chance (e.g., ~3.75% for each specific power-up). Does this feel balanced to you?
> - **Visual Feedback**: I will add a status indicator below the score to show active effects like "SHIELD ACTIVE" or "FREEZE".

## Proposed Changes

### Game Logic & UI

#### [MODIFY] [BubbleView.kt](file:///C:/Users/Kodabal/AndroidStudioProjects/TapTap/app/src/main/java/com/app/taptap/BubbleView.kt)
- Add `BubbleType` enum: `NORMAL`, `FREEZE`, `RAINBOW`, `SHIELD`, `BLAST`.
- Update `onDraw` to render unique colors and effects for each type:
    - **Freeze**: Cyan/Ice glow.
    - **Rainbow**: Multi-color shifting spectrum.
    - **Shield**: Golden/Yellow metallic sheen.
    - **Blast**: Pulsating red/orange "bomb" appearance.

#### [MODIFY] [GameActivity.kt](file:///C:/Users/Kodabal/AndroidStudioProjects/TapTap/app/src/main/java/com/app/bubblepop/GameActivity.kt)
- **State Management**:
    - `isShieldActive: Boolean`
    - `freezeTimeRemaining: Long`
- **Spawning Logic**:
    - Update `spawnBubble()` to roll for a `BubbleType`.
- **Power-up Effects**:
    - **Freeze**: Stop the timer and double the spawn interval for 5 seconds.
    - **Rainbow**: Instant +5 points and +20 combo.
    - **Shield**: Ignore the next nail tap.
    - **Blast**: A new `popAllBubbles()` function to clear the screen and gain points for each.
- **HUD Updates**:
    - Add a `powerUpStatus` TextView to show active effects.

#### [MODIFY] [activity_game.xml](file:///C:/Users/Kodabal/AndroidStudioProjects/TapTap/app/src/main/res/layout/activity_game.xml)
- Add a TextView for power-up status messages.

## Verification Plan

### Automated Tests
- N/A (Manual gameplay testing preferred for feel and timing).

### Manual Verification
1. Start the game and verify that special bubbles spawn occasionally.
2. Tap a **Freeze Bubble**: Verify the time bar stops and bubbles spawn slower.
3. Tap a **Rainbow Bubble**: Verify score jumps by 5 and combo increases.
4. Tap a **Shield Bubble**: Verify the "SHIELD ACTIVE" text appears. Then tap a nail and verify no score is lost and shield disappears.
5. Tap a **Blast Bubble**: Verify all current bubbles pop simultaneously.
