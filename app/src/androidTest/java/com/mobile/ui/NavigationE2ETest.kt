package com.mobile.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.mobile.data.FinanceRepository
import com.mobile.data.SettingsRepository
import com.mobile.ui.navigation.AppNavigation
import com.mobile.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end navigation tests — validates that tapping bottom nav items
 * and in-screen links navigates to the correct destinations.
 *
 * Key patterns:
 * - Use onNodeWithContentDescription for bottom nav icons (unique)
 * - Use onAllNodesWithText().onFirst() for text that appears in both
 *   the screen header AND the bottom nav label
 * - Use assertExists() for content below the scroll fold
 */
class NavigationE2ETest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SettingsRepository.init(context)
        FinanceRepository.clearAll()
    }

    // ── Bottom Navigation ────────────────────────────────────────────────

    @Test
    fun defaultScreen_isHome() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        // Home screen shows "Welcome back,"
        composeTestRule.onNodeWithText("Welcome back,").assertExists()
    }

    @Test
    fun navigateTo_analytics() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        // Tap the Analytics icon in the bottom nav
        composeTestRule.onNodeWithContentDescription("Analytics").performClick()
        composeTestRule.waitForIdle()
        // Verify analytics-specific content (NET WORTH is unique to analytics)
        composeTestRule.onNodeWithText("NET WORTH").assertExists()
    }

    @Test
    fun navigateTo_budget() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        composeTestRule.onNodeWithContentDescription("Budget").performClick()
        composeTestRule.waitForIdle()
        // "Budget & Expenses" is unique to the budget screen header
        composeTestRule.onNodeWithText("Budget & Expenses").assertExists()
    }

    @Test
    fun navigateTo_tools() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        composeTestRule.onNodeWithContentDescription("Tools").performClick()
        composeTestRule.waitForIdle()
        // "Workspace" is the tools screen header
        composeTestRule.onNodeWithText("Workspace").assertExists()
    }

    @Test
    fun navigateTo_settings() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        // Use SECURITY group header which is unique to the settings screen
        composeTestRule.onNodeWithText("SECURITY").assertExists()
    }

    @Test
    fun navigateBackTo_home() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        // Go to budget (no duplicate text issue)
        composeTestRule.onNodeWithContentDescription("Budget").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Budget & Expenses").assertExists()

        // Go back to home
        composeTestRule.onNodeWithContentDescription("Home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Welcome back,").assertExists()
    }

    // ── Tools Sub-Navigation ─────────────────────────────────────────────

    @Test
    fun tools_navigateToSupport() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        // Navigate to Tools
        composeTestRule.onNodeWithContentDescription("Tools").performClick()
        composeTestRule.waitForIdle()

        // Tap the support banner — use the Support icon's content description
        composeTestRule.onNodeWithContentDescription("Support").performClick()
        composeTestRule.waitForIdle()

        // Should be on support screen — "We're here to help!" is unique
        composeTestRule.onNodeWithText("We're here to help!").assertExists()
    }

    // ── Settings Content ─────────────────────────────────────────────────

    @Test
    fun settings_showsSecurityGroup() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SECURITY").assertExists()
        composeTestRule.onNodeWithText("Biometric Login").assertExists()
    }

    @Test
    fun settings_showsPreferencesGroup() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        // PREFERENCES might be below scroll fold — just check it exists in the tree
        composeTestRule.onNodeWithText("PREFERENCES").assertExists()
    }

    // ── Home Screen Content ──────────────────────────────────────────────

    @Test
    fun home_showsEmptyTransactionsMessage() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        // "No transactions found yet." may be below fold — assertExists checks tree, not visibility
        composeTestRule.onNodeWithText("No transactions found yet.").assertExists()
    }

    @Test
    fun home_showsAiSection() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        // "Habte AI" exists on the home screen AI card
        composeTestRule.onNodeWithText("Habte AI").assertExists()
    }

    @Test
    fun home_showsRecentActivitiesHeader() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        // May be below scroll fold on small screens
        composeTestRule.onNodeWithText("Recent Activities").assertExists()
    }

    // ── Bottom Nav Bar Visibility ────────────────────────────────────────

    @Test
    fun bottomNavBar_visibleOnMainScreens() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        // All nav items should be present via their content descriptions
        composeTestRule.onNodeWithContentDescription("Home").assertExists()
        composeTestRule.onNodeWithContentDescription("Analytics").assertExists()
        composeTestRule.onNodeWithContentDescription("Budget").assertExists()
        composeTestRule.onNodeWithContentDescription("Tools").assertExists()
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
    }

    // ── Round Trip Navigation ────────────────────────────────────────────

    @Test
    fun fullRoundTrip_allTabs() {
        composeTestRule.setContent {
            AppTheme { AppNavigation() }
        }
        // Home → Analytics → Budget → Tools → Settings → Home
        composeTestRule.onNodeWithText("Welcome back,").assertExists()

        composeTestRule.onNodeWithContentDescription("Analytics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("NET WORTH").assertExists()

        composeTestRule.onNodeWithContentDescription("Budget").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Budget & Expenses").assertExists()

        composeTestRule.onNodeWithContentDescription("Tools").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Workspace").assertExists()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SECURITY").assertExists()

        composeTestRule.onNodeWithContentDescription("Home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Welcome back,").assertExists()
    }
}
