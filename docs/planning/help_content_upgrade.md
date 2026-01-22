# Help Interface Content Upgrade Plan

## 1. Goal
Upgrade the existing `HelpScreen` to be a comprehensive "Operator's Manual" that covers all current gameplay features, including the new Endless Mode, Combo/Rage systems, and updated item/weapon rosters.

## 2. User Review Required
> [!IMPORTANT]
> This plan proposes a significant restructuring of the Help sections. Please confirm if the proposed section structure aligns with your vision.

## 3. Proposed Structure & Content

The Help Interface will be divided into the following sections:

### 3.1. Basic Controls (Updated)
**Current:** Movement, Combat, Other.
**Additions/Refinements:**
- Verify Control Keys match actual `InputManager` or `GameSettings`.
- Add **Endless Mode Specific Controls** if any (e.g., viewing stats).
- Clarify "Sprint" mechanics (Stamina usage?).

### 3.2. Game Modes (NEW)
**Content:**
- **Adventure Mode (Level Mode):**
    - Explanation of Zones (1-5).
    - Goal: Find the Key -> Find the Exit.
    - Difficulty progression.
- **Endless Mode:**
    - Goal: Survive as long as possible.
    - **Combo System:** Explain how killing enemies continuously increases score multiplier. mention the 5s decay.
    - **Rage System:** Explain how killing *too fast* increases Enemy Rage (Speed/Damage up).
        - Strategy Tip: "Balance your killing speed to manage Enemy Rage."
    - Wave Mechanics: Enemies spawn in waves.

### 3.3. Combat Systems (Expanded)
**Content:**
- **Weapon Types:**
    - **Melee**: Sword (High dmg, short range).
    - **Ranged**: Bow, Crossbow, Wand, Staff.
    - **Damage Types**: Physical vs Magical.
- **Armor System:**
    - Explain Physical Armor vs Magical Armor.
    - Matching armor to enemy damage type is crucial.
- **status Effects:**
    - Burn, Freeze, Poison (explain what they do).

### 3.4. Bestiary (Enemies) (Refined)
**Current:** Embedded in "Zones".
**Change:** Create a dedicated "Enemies" or "Threats" section or keep in World but expand details.
- List distinct enemy types (Slime, Skeleton, Spider, etc.) and their:
    - Damage Type (Phys/Mag).
    - Behavior (Chasing, Ranged, etc.).
    - Weakness (if implemented).

### 3.5. Items & Chests (NEW)
**Content:**
- **Treasure Chests:**
    - Explain they contain weapons, armor, or potions.
    - **Chest Puzzles:** Mention some chests are locked behind puzzles.
- **Consumables:**
    - Health Potions (Hearts).
    - Power-ups (if any).
- **Dual Coin System:** (If applicable, explain Gold/Score).

### 3.6. Developer Console
**Current:** Detailed list of commands.
**Update:**
- Ensure list is up-to-date with recent additions (e.g., `combo`, `rage` commands).

## 4. Implementation Details

### 4.1. `HelpScreen.java` Refactor
- Update `NAV_ITEMS` array to include new sections: `{"Controls", "Game Modes", "Combat", "Equipment", "World", "Console", "Tips"}`.
- Refactor `refreshContent()` switch case.
- Create new builder methods:
    - `buildGameModesContent()`
    - `buildEquipmentContent()` (Combining Weapons/Armor)
    - `buildCombatContent()` (Mechanics like Combo/Rage)

### 4.2. Data Management
- Instead of hardcoded strings in `HelpScreen`, consider extracting text to a `HelpConstants` or `Bundle` class if localization is a future goal. For now, keep hardcoded but organized.

## 5. Verification Plan
- **Manual Verification:**
    - Launch game, open Help.
    - Click through ALL tabs.
    - Verify text accuracy against actual gameplay.
    - Check formatting (text wrapping, table alignment).
