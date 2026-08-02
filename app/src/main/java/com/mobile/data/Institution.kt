package com.mobile.data

import com.mobile.R

/**
 * Broad category of a financial institution. Lets the catalog describe entities
 * beyond traditional banks (wallets, SACCOs, fintechs, government services, ...)
 * without any call site needing to change when a new category of institution
 * shows up — only InstitutionCatalog.ALL grows.
 */
enum class InstitutionType {
    GOVERNMENT_BANK,
    PRIVATE_BANK,
    DIGITAL_WALLET,
    MICROFINANCE,
    SACCO,
    CREDIT_UNION,
    FINTECH,
    INTERNATIONAL_PROVIDER,
    GOVERNMENT_SERVICE
}

/**
 * Static catalog entry for one financial institution — the single place a bank
 * or wallet is registered. Data.PRESET_BANKS and SmsParser both derive from
 * InstitutionCatalog.ALL, so adding an entry here is enough to make the
 * institution selectable in "Add Bank" and detectable from incoming SMS,
 * with no other code changes required.
 *
 * `smsContains`/`smsExact` are lowercase matchers checked against the SMS
 * sender address: `smsContains` for substring matches (bank names), `smsExact`
 * for short numeric/shortcode senders where a substring match would be too loose.
 */
data class InstitutionProfile(
    val id: String,
    val name: String,
    val shortName: String,
    val type: InstitutionType,
    val colorFrom: String,
    val colorTo: String,
    val logoText: String,
    val logoResId: Int? = null,
    val domain: String? = null,
    val smsContains: List<String> = emptyList(),
    val smsExact: List<String> = emptyList(),
    val supportedAccountTypes: List<AccountType>,
    val website: String? = domain?.let { "https://$it" },
    val supportPhone: String? = null,
    val supportEmail: String? = null
) {
    fun matchesSmsSender(lowerSender: String): Boolean =
        smsContains.any { lowerSender.contains(it) } || smsExact.any { lowerSender == it }
}

fun InstitutionProfile.toBank(): Bank = Bank(
    id = id,
    name = name,
    shortName = shortName,
    accounts = emptyList(),
    colorFrom = colorFrom,
    colorTo = colorTo,
    logoText = logoText,
    logoResId = logoResId,
    domain = domain
)

object InstitutionCatalog {

    private val standardBankAccountTypes = listOf(
        AccountType.SAVINGS, AccountType.CURRENT, AccountType.SALARY,
        AccountType.BUSINESS, AccountType.YOUTH, AccountType.STUDENT,
        AccountType.FIXED_DEPOSIT, AccountType.LOAN, AccountType.FOREIGN_CURRENCY
    )

    private val digitalWalletAccountTypes = listOf(
        AccountType.MOBILE_WALLET, AccountType.DIGITAL_WALLET
    )

    // Every institution the app currently recognizes. Migrated 1:1 from the
    // previous hardcoded Bank list + SmsParser's when-chain — matching behavior
    // is preserved exactly (see SmsParserTest), not re-tuned, in this pass.
    val ALL: List<InstitutionProfile> = listOf(
        // CBE Birr must precede the plain CBE entry below: its sender IDs contain
        // "cbe" as a substring, and findBySmsSender() returns the first match.
        InstitutionProfile(
            id = "cbebirr", name = "CBE Birr", shortName = "CBEBirr",
            type = InstitutionType.DIGITAL_WALLET,
            colorFrom = "#3730A3", colorTo = "#1E1B4B", logoText = "CBEB",
            logoResId = R.drawable.logo_cbe, // CBE Birr is CBE's own mobile money product
            smsContains = listOf("cbebirr", "cbe birr"),
            supportedAccountTypes = digitalWalletAccountTypes
        ),
        InstitutionProfile(
            id = "cbe", name = "Commercial Bank of Ethiopia", shortName = "CBE",
            type = InstitutionType.GOVERNMENT_BANK,
            colorFrom = "#3730A3", colorTo = "#1E1B4B", logoText = "CBE",
            logoResId = R.drawable.logo_cbe,
            smsContains = listOf("cbe"), smsExact = listOf("1000", "8008"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "boa", name = "Bank of Abyssinia", shortName = "BOA",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#F59E0B", colorTo = "#B45309", logoText = "BOA",
            logoResId = R.drawable.logo_boa,
            domain = "bankofabyssinia.com",
            smsContains = listOf("boa", "abyssinia"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "awash", name = "Awash Bank", shortName = "AWA",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#1E40AF", colorTo = "#1D4ED8", logoText = "AWB",
            logoResId = R.drawable.logo_awash,
            domain = "awashbank.com",
            smsContains = listOf("awash"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "dashen", name = "Dashen Bank", shortName = "DAS",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#1E3A8A", colorTo = "#EF4444", logoText = "DSB",
            logoResId = R.drawable.logo_dashen,
            domain = "dashenbanksc.com",
            smsContains = listOf("dashen"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "hibret", name = "Hibret Bank", shortName = "HIB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#059669", colorTo = "#D97706", logoText = "HBT",
            logoResId = R.drawable.logo_hibret,
            domain = "hibretbank.com.et",
            smsContains = listOf("hibret", "united"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "zemen", name = "Zemen Bank", shortName = "ZEM",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#111827", colorTo = "#D97706", logoText = "ZMN",
            logoResId = R.drawable.logo_zemen,
            domain = "zemenbank.com",
            smsContains = listOf("zemen"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "nib", name = "Nib International Bank", shortName = "NIB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#1E40AF", colorTo = "#FACC15", logoText = "NIB",
            logoResId = R.drawable.logo_nib,
            domain = "nibbanksc.com",
            smsContains = listOf("nib"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "coop", name = "Cooperative Bank of Oromia", shortName = "COO",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#047857", colorTo = "#F59E0B", logoText = "CPB",
            logoResId = R.drawable.logo_coop,
            domain = "coopbankoromia.com.et",
            smsContains = listOf("coop"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "abay", name = "Abay Bank", shortName = "ABY",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#1D4ED8", colorTo = "#059669", logoText = "ABY",
            logoResId = R.drawable.logo_abay,
            domain = "abaybank.com.et",
            smsContains = listOf("abay"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "berhan", name = "Berhan Bank", shortName = "BER",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#B91C1C", colorTo = "#F59E0B", logoText = "BRH",
            logoResId = R.drawable.logo_berhan,
            domain = "berhanbanksc.com",
            smsContains = listOf("berhan"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "bunna", name = "Bunna Bank", shortName = "BUN",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#451A03", colorTo = "#D97706", logoText = "BNA",
            logoResId = R.drawable.logo_bunna,
            domain = "bunnabanksc.com",
            smsContains = listOf("bunna"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "wegagen", name = "Wegagen Bank", shortName = "WEG",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#1D4ED8", colorTo = "#F59E0B", logoText = "WGN",
            logoResId = R.drawable.logo_wegagen,
            domain = "wegagen.com",
            smsContains = listOf("wegagen"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "oromia", name = "Oromia Bank", shortName = "ORO",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#059669", colorTo = "#B91C1C", logoText = "ORB",
            logoResId = R.drawable.logo_oromia,
            domain = "oromiabank.com",
            smsContains = listOf("oromia"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "lion", name = "Lion International Bank", shortName = "LIO",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#F59E0B", colorTo = "#B91C1C", logoText = "LIB",
            logoResId = R.drawable.logo_lion,
            domain = "lionbanksc.com",
            smsContains = listOf("lion"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "enat", name = "Enat Bank", shortName = "ENA",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#DB2777", colorTo = "#1D4ED8", logoText = "ENB",
            logoResId = R.drawable.logo_enat,
            domain = "enatbanksc.com",
            smsContains = listOf("enat"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "tele", name = "Telebirr", shortName = "TEL",
            type = InstitutionType.DIGITAL_WALLET,
            colorFrom = "#0E7490", colorTo = "#164E63", logoText = "TEL",
            logoResId = R.drawable.logo_tele,
            smsContains = listOf("telebirr"), smsExact = listOf("tele", "127", "*127#"),
            supportedAccountTypes = digitalWalletAccountTypes
        ),
        InstitutionProfile(
            id = "dbe", name = "Development Bank of Ethiopia", shortName = "DBE",
            type = InstitutionType.GOVERNMENT_BANK,
            colorFrom = "#065F46", colorTo = "#047857", logoText = "DBE",
            logoResId = R.drawable.logo_dbe,
            smsContains = listOf("dbe", "development bank"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "addis", name = "Addis International Bank", shortName = "AIB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#7C2D12", colorTo = "#C2410C", logoText = "AIB",
            logoResId = R.drawable.logo_addis,
            smsContains = listOf("addis international", "addisintl"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "hijra", name = "Hijra Bank", shortName = "HIJ",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#065F46", colorTo = "#10B981", logoText = "HIJ",
            logoResId = R.drawable.logo_hijra,
            smsContains = listOf("hijra"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "global", name = "Global Bank Ethiopia", shortName = "GLB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#1E3A8A", colorTo = "#3B82F6", logoText = "GLB",
            logoResId = R.drawable.logo_global,
            smsContains = listOf("global bank", "globalbank"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "zamzam", name = "ZamZam Bank", shortName = "ZZB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#0F766E", colorTo = "#14B8A6", logoText = "ZZB",
            logoResId = R.drawable.logo_zamzam,
            smsContains = listOf("zamzam"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "tsehay", name = "Tsehay Bank", shortName = "TSB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#B45309", colorTo = "#F59E0B", logoText = "TSB",
            logoResId = R.drawable.logo_tsehay,
            smsContains = listOf("tsehay"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "goh", name = "Goh Betoch Bank", shortName = "GBB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#374151", colorTo = "#6B7280", logoText = "GBB",
            smsContains = listOf("goh betoch", "gohbetoch"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "ahadu", name = "Ahadu Bank", shortName = "AHB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#4C1D95", colorTo = "#7C3AED", logoText = "AHB",
            logoResId = R.drawable.logo_ahadu,
            smsContains = listOf("ahadu"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "amhara", name = "Amhara Bank", shortName = "AMB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#166534", colorTo = "#22C55E", logoText = "AMB",
            logoResId = R.drawable.logo_amhara,
            smsContains = listOf("amhara bank", "amharabank"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "gadaa", name = "Gadaa Bank", shortName = "GDB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#92400E", colorTo = "#D97706", logoText = "GDB",
            logoResId = R.drawable.logo_gadaa,
            smsContains = listOf("gadaa"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "omo", name = "Omo Bank", shortName = "OMB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#155E75", colorTo = "#0891B2", logoText = "OMB",
            logoResId = R.drawable.logo_omo,
            smsContains = listOf("omo bank", "omobank"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "sidama", name = "Sidama Bank", shortName = "SDB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#7F1D1D", colorTo = "#DC2626", logoText = "SDB",
            logoResId = R.drawable.logo_sidama,
            smsContains = listOf("sidama"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "rammis", name = "Rammis Bank", shortName = "RMB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#581C87", colorTo = "#9333EA", logoText = "RMB",
            logoResId = R.drawable.logo_rammis,
            smsContains = listOf("rammis"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "siket", name = "Siket Bank", shortName = "SKB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#134E4A", colorTo = "#0D9488", logoText = "SKB",
            logoResId = R.drawable.logo_siket,
            smsContains = listOf("siket"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "shabelle", name = "Shabelle Bank", shortName = "SHB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#78350F", colorTo = "#B45309", logoText = "SHB",
            logoResId = R.drawable.logo_shabelle,
            smsContains = listOf("shabelle"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "siinqee", name = "Siinqee Bank", shortName = "SIB",
            type = InstitutionType.PRIVATE_BANK,
            colorFrom = "#1E3A8A", colorTo = "#2563EB", logoText = "SIB",
            smsContains = listOf("siinqee", "sinqee"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "ecwc", name = "Ethiopian Construction Works Corporation Bank", shortName = "ECWC",
            type = InstitutionType.GOVERNMENT_BANK,
            colorFrom = "#1F2937", colorTo = "#4B5563", logoText = "ECWC",
            logoResId = R.drawable.logo_ecwc,
            smsContains = listOf("ecwc", "construction works"),
            supportedAccountTypes = standardBankAccountTypes
        ),
        InstitutionProfile(
            id = "mpesa", name = "M-PESA Ethiopia", shortName = "MPESA",
            type = InstitutionType.DIGITAL_WALLET,
            colorFrom = "#00A651", colorTo = "#007A3D", logoText = "MPESA",
            logoResId = R.drawable.logo_mpesa,
            smsContains = listOf("mpesa", "m-pesa"),
            supportedAccountTypes = digitalWalletAccountTypes
        ),
        InstitutionProfile(
            id = "amole", name = "Amole", shortName = "AMOLE",
            type = InstitutionType.DIGITAL_WALLET,
            colorFrom = "#DC2626", colorTo = "#7C2D12", logoText = "AMOLE",
            smsContains = listOf("amole"),
            supportedAccountTypes = digitalWalletAccountTypes
        ),
        InstitutionProfile(
            id = "hellocash", name = "HelloCash", shortName = "HELLO",
            type = InstitutionType.DIGITAL_WALLET,
            colorFrom = "#F97316", colorTo = "#EA580C", logoText = "HELLO",
            logoResId = R.drawable.logo_hellocash,
            smsContains = listOf("hellocash", "hello cash"),
            supportedAccountTypes = digitalWalletAccountTypes
        )
    )

    /** Data-driven replacement for SmsParser's old hardcoded when-chain. */
    fun findBySmsSender(rawSender: String): InstitutionProfile? {
        val lowerSender = rawSender.lowercase().trim()
        return ALL.find { it.matchesSmsSender(lowerSender) }
    }
}
