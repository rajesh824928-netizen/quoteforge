package com.example.domain.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

data class FeatureCategory(
    val title: String,
    val tag: String = "NEW", // NEW, ENHANCEMENT, CORE
    val features: List<String>
)

data class ReleaseVersion(
    val version: String,
    val releaseDate: String,
    val isCurrent: Boolean = false,
    val highlightBadge: String = "",
    val headline: String,
    val summary: String,
    val categories: List<FeatureCategory>
)

object ChangelogRepository {

    val releases: List<ReleaseVersion> = listOf(
        ReleaseVersion(
            version = "2.2.0",
            releaseDate = "September 2026",
            isCurrent = true,
            highlightBadge = "CURRENT STABLE",
            headline = "Quick Share Offline APK Distribution & Live Architectural PDF Studio",
            summary = "Direct peer-to-peer APK sharing via Quick Share for on-site contractors, real-time responsive A4 PDF design preview, and Credential Manager Google Sign-In.",
            categories = listOf(
                FeatureCategory(
                    title = "Quick Share & Offline Distribution",
                    tag = "NEW",
                    features = listOf(
                        "Direct APK Sharing via Android Quick Share / Nearby Share without requiring Google Play Store or internet.",
                        "One-tap APK packaging (QuoteForge-v2.2.0.apk) with package size and checksum metadata display.",
                        "Universal Android Sharesheet integration for WhatsApp, Google Drive, and local file storage backup.",
                        "Zero-configuration peer-to-peer distribution designed for architects, site supervisors, and contractors."
                    )
                ),
                FeatureCategory(
                    title = "Live Architectural PDF & A4 Studio",
                    tag = "ENHANCEMENT",
                    features = listOf(
                        "Synchronous live paper preview reflecting style and branding customizations immediately.",
                        "3 distinct architectural layout templates: Minimal Studio (editorial hairline), Professional Corporate (bold banner), and Premium Architectural (slate framed with project site blocks).",
                        "Live accent color swatches with dynamic tinting across header bars, table borders, and totals.",
                        "Rotated watermark overlays (Draft, Urgent, Confidential, Company Logo) with opacity & angle calibration.",
                        "Site specification blocks including Project Site Address, Area (sq.ft), Project Type, and Principal Architect signature block."
                    )
                ),
                FeatureCategory(
                    title = "Firebase Cloud Auth & Workspace Onboarding",
                    tag = "NEW",
                    features = listOf(
                        "Google Sign-In integration powered by Android Credential Manager & Firebase Authentication.",
                        "Frictionless developer & emulator fallback mode for offline testing environments.",
                        "Post-login Workspace Onboarding flow configuring company name, business domain, and default currency.",
                        "User account drawer in More tab showing current sign-in provider and one-tap account switching."
                    )
                )
            )
        ),
        ReleaseVersion(
            version = "2.1.0",
            releaseDate = "August 2026",
            isCurrent = false,
            highlightBadge = "CALCULATIONS",
            headline = "Executive Estimation Engine & Indian GST Compliance",
            summary = "Dual calculation modes (Selling Price vs Cost + Profit Margin), GST tax engine (CGST/SGST/IGST), and direct client WhatsApp delivery.",
            categories = listOf(
                FeatureCategory(
                    title = "Indian GST & Currency Engine",
                    tag = "CORE",
                    features = listOf(
                        "Full Indian GST compliance: Intra-state (CGST 9% + SGST 9%) and Inter-state (IGST 18%) split calculations.",
                        "Indian Rupee (₹) formatting with Lakhs/Crores grouping and Amount in Words converter.",
                        "Tax-inclusive vs tax-exclusive toggle at line-item and overall document level.",
                        "Custom tax rates support (0%, 5%, 12%, 18%, 28%) for diverse construction materials."
                    )
                ),
                FeatureCategory(
                    title = "Estimation & Cost-Plus Margin Engine",
                    tag = "CORE",
                    features = listOf(
                        "Dual pricing modes: Direct Selling Price vs Cost + Profit Margin % calculation.",
                        "Milestone payment schedule generator (Advance, Civil completion, Handover).",
                        "Discount management supporting both flat amount deductions and percentage concessions.",
                        "Line-item unit presets: sq.ft, running ft, nos, lump sum, metric ton, and hours."
                    )
                ),
                FeatureCategory(
                    title = "Client Communication & WhatsApp",
                    tag = "ENHANCEMENT",
                    features = listOf(
                        "Formatted professional WhatsApp text generator with quotation number, total amount, and validity date.",
                        "Single-click WhatsApp dispatch attaching the rendered PDF directly to the client's phone number.",
                        "Validity expiration reminder tracker (15, 30, 45, 60 days presets)."
                    )
                )
            )
        ),
        ReleaseVersion(
            version = "2.0.0",
            releaseDate = "July 2026",
            isCurrent = false,
            highlightBadge = "SAAS ARCHITECTURE",
            headline = "Multi-Tenant Workspaces, Team Roles & Revision History",
            summary = "Complete SaaS transformation featuring multi-organization data isolation, 6 granular team permissions, and audit logging.",
            categories = listOf(
                FeatureCategory(
                    title = "Multi-Tenant Workspaces",
                    tag = "CORE",
                    features = listOf(
                        "Multi-tenant data isolation: User → Workspace → Team → Clients → Projects → Quotations.",
                        "Multi-organization switching with instant context reload.",
                        "Granular role-based access control: Owner, Admin, Manager, Designer, Accountant, and Viewer.",
                        "Immutable team activity audit logs tracking document revisions, approvals, and PDF exports."
                    )
                ),
                FeatureCategory(
                    title = "Document Revision Control",
                    tag = "CORE",
                    features = listOf(
                        "Full revision tree (Rev.00, Rev.01, Rev.02) preserving original drafts when revising estimates.",
                        "Comprehensive status lifecycle: Draft → Sent → Viewed → Accepted → Revised → Cancelled.",
                        "Revision comparison indicators highlighting amount deltas between revisions."
                    )
                ),
                FeatureCategory(
                    title = "Google Drive Cloud Storage",
                    tag = "ENHANCEMENT",
                    features = listOf(
                        "Google Drive API integration using secure drive.file scope.",
                        "Automated cloud backup for generated PDF documents organized into dedicated workspace folders.",
                        "Cloud synchronization status tracker with retry queues for offline resilience."
                    )
                )
            )
        ),
        ReleaseVersion(
            version = "1.0.0",
            releaseDate = "May 2026",
            isCurrent = false,
            highlightBadge = "FOUNDATION",
            headline = "Initial Architectural & Interior Quotation Tool",
            summary = "Local SQLite Room database, item catalog presets, and foundational Material Design 3 architecture.",
            categories = listOf(
                FeatureCategory(
                    title = "Core Capabilities",
                    tag = "FOUNDATION",
                    features = listOf(
                        "Modular Kitchen, Interior Carpentry, Civil, Electrical, and False Ceiling item presets.",
                        "Fully offline Room Database storage with reactive StateFlow streams.",
                        "Executive dark and light styling with high-contrast architectural palette."
                    )
                )
            )
        )
    )

    fun getCurrentVersion(): ReleaseVersion = releases.first { it.isCurrent }
}
