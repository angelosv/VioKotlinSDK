package io.reachu.VioEngagementUI

/**
 * VioEngagementUI Module
 * 
 * Provides reusable Compose components for engagement features including:
 * - Polls (cards and overlays)
 * - Contests (cards and overlays)
 * - Products (cards, grid cards, and overlays)
 * 
 * This module is optional but recommended for apps that want pre-built UI components.
 * 
 * Dependencies:
 * - VioEngagementSystem: For engagement data models and business logic
 * - VioDesignSystem: For design tokens and styling
 * - VioCore: For core utilities and configuration
 * 
 * Usage:
 * ```kotlin
 * import io.reachu.VioEngagementUI.Components.*
 * 
 * @Composable
 * fun MyScreen() {
 *     ReachuEngagementPollCard(poll = myPoll)
 *     ReachuEngagementContestCard(contest = myContest)
 * }
 * ```
 */
object VioEngagementUI {
    /**
     * Module version
     */
    const val VERSION = "1.0.0"
    
    /**
     * Initialize the VioEngagementUI module
     */
    fun configure() {
        println("🎨 VioEngagementUI module initialized (v$VERSION)")
    }
}
