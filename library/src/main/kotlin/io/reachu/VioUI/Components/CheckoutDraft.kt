package io.reachu.VioUI.Components

/**
 * Kotlin port of Swift's `CheckoutDraft`. Maintains the same structure so the
 * checkout overlay can consume it directly once the UI is in place.
 */
class CheckoutDraft {

    // Contact
    var email: String = ""
    var phone: String = ""
    var phoneCountryCode: String = ""

    // Address
    var firstName: String = ""
    var lastName: String = ""
    var address1: String = ""
    var address2: String = ""
    var city: String = ""
    var province: String = ""
    var countryName: String = "United States"
    var countryCode: String = ""
    var zip: String = ""
    var company: String = ""

    // Flags & selections
    var shippingOptionRaw: String = "standard"
    var paymentMethodRaw: String = "stripe"
    var acceptsTerms: Boolean = false
    var acceptsPurchaseConditions: Boolean = false
    var appliedDiscount: Double = 0.0

    // URLs
    var successUrl: String = "vio-demo://checkout/success"
    var cancelUrl: String = "vio-demo://checkout/cancel"

    fun addressPayload(fallbackCountryISO2: String): Map<String, Any?> {
        val iso2 = resolveISO2(fallbackCountryISO2)
        val phoneCode = resolvePhoneCode(iso2)
        val provinceCode = resolveProvinceCode(iso2, province)
        return mapOf(
            "address1" to address1,
            "address2" to address2,
            "city" to city,
            "company" to company,
            "country" to resolveCountryName(countryName, iso2),
            "country_code" to iso2,
            "email" to email,
            "first_name" to firstName,
            "last_name" to lastName,
            "phone" to phone,
            "phone_code" to phoneCode,
            "province" to province,
            "province_code" to provinceCode,
            "zip" to zip,
        )
    }

    fun shippingAddressPayload(fallbackCountryISO2: String): Map<String, Any?> =
        addressPayload(fallbackCountryISO2)

    fun billingAddressPayload(fallbackCountryISO2: String): Map<String, Any?> =
        addressPayload(fallbackCountryISO2)

    fun resolveISO2(fallback: String): String {
        val provided = countryCode.nonEmpty()?.uppercase()
        if (provided != null && provided.length == 2) return provided
        val normalizedName = normalizeKey(countryName)
        GeoMaps.countryISO2ByName[normalizedName]?.let { return it }
        return fallback.uppercase()
    }

    fun resolvePhoneCode(effectiveISO2: String): String {
        phoneCountryCode.nonEmpty()?.let {
            return it.replace("+", "").trim()
        }
        return GeoMaps.phoneCodeByISO2[effectiveISO2] ?: ""
    }

    fun resolveProvinceCode(effectiveISO2: String, provinceName: String): String {
        val trimmed = provinceName.trim()
        if (trimmed.isEmpty()) return ""

        val map = GeoMaps.provinceCodeByCountry[effectiveISO2]
            ?: return trimmed.uppercase()

        val upper = trimmed.uppercase()
        if (map.values.contains(upper)) return upper

        map[normalizeKey(trimmed)]?.let { return it }

        val simplified = normalizeKey(
            trimmed
                .replace("state", "", ignoreCase = true)
                .replace("province", "", ignoreCase = true)
                .replace("provincia", "", ignoreCase = true)
                .replace("departamento", "", ignoreCase = true)
                .replace("region", "", ignoreCase = true)
                .replace("región", "", ignoreCase = true),
        )
        map[simplified]?.let { return it }

        if (upper.length in 2..5 && upper.matches(Regex("^[A-Z]{2,5}\$"))) {
            return upper
        }

        return ""
    }

    fun resolveCountryName(defaultName: String, iso2: String): String {
        defaultName.nonEmpty()?.let { return it }
        return GeoMaps.countryNameByISO2[iso2] ?: defaultName
    }

    fun resolveLocale(effectiveISO2: String): String {
        return GeoMaps.localeByISO2[effectiveISO2] ?: "en-US"
    }

    fun resolveFullPhoneNumber(effectiveISO2: String): String {
        val cleanPhoneCode = resolvePhoneCode(effectiveISO2)
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank()) return ""
        return cleanPhoneCode + cleanPhone
    }
}

// region Helpers

private fun String.nonEmpty(): String? = if (isBlank()) null else this

private fun normalizeKey(input: String): String {
    val normalized = input.trim().lowercase().map { c ->
        when {
            c.isLetterOrDigit() || c == ' ' || c == '.' || c == '-' -> c
            else -> ' '
        }
    }.joinToString("")
    return normalized.replace(Regex("\\s+"), " ")
}

// endregion

// region Static maps (direct port of Swift dictionaries)

private object GeoMaps {

    val countryNameByISO2: Map<String, String> = mapOf(
        "US" to "United States", "CA" to "Canada", "GB" to "United Kingdom", "IE" to "Ireland",
        "NO" to "Norway", "SE" to "Sweden", "DK" to "Denmark", "FI" to "Finland",
        "DE" to "Germany", "FR" to "France", "IT" to "Italy", "ES" to "Spain", "PT" to "Portugal",
        "NL" to "Netherlands", "BE" to "Belgium", "CH" to "Switzerland", "AT" to "Austria",
        "PL" to "Poland", "CZ" to "Czech Republic", "HU" to "Hungary", "RO" to "Romania",
        "GR" to "Greece", "TR" to "Turkey",
        "MX" to "Mexico", "AR" to "Argentina", "BR" to "Brazil", "CL" to "Chile",
        "CO" to "Colombia", "PE" to "Peru", "UY" to "Uruguay", "EC" to "Ecuador",
        "VE" to "Venezuela", "BO" to "Bolivia", "PY" to "Paraguay",
        "CR" to "Costa Rica", "PA" to "Panama", "DO" to "Dominican Republic",
        "GT" to "Guatemala", "HN" to "Honduras", "SV" to "El Salvador", "NI" to "Nicaragua",
        "AU" to "Australia", "NZ" to "New Zealand", "JP" to "Japan", "KR" to "South Korea", "CN" to "China",
        "IN" to "India",
    )

    val countryISO2ByName: Map<String, String> = buildMap {
        fun add(names: List<String>, iso: String) {
            names.forEach { put(normalizeKey(it), iso) }
        }

        add(listOf("United States", "USA", "US", "Estados Unidos", "EEUU", "EE.UU."), "US")
        add(listOf("United Kingdom", "UK", "Great Britain", "Britain", "Inglaterra"), "GB")
        add(listOf("Ireland", "Irlanda"), "IE")

        add(listOf("Norway", "Norge", "Noruega"), "NO")
        add(listOf("Sweden", "Sverige", "Suecia"), "SE")
        add(listOf("Denmark", "Danmark", "Dinamarca"), "DK")
        add(listOf("Finland", "Suomi", "Finlandia"), "FI")

        add(listOf("Germany", "Deutschland", "Alemania"), "DE")
        add(listOf("France", "Francia"), "FR")
        add(listOf("Italy", "Italia"), "IT")
        add(listOf("Spain", "España"), "ES")
        add(listOf("Portugal"), "PT")
        add(listOf("Netherlands", "Holland", "Países Bajos", "Paises Bajos"), "NL")
        add(listOf("Belgium", "België", "Belgie", "Bélgica"), "BE")
        add(listOf("Switzerland", "Schweiz", "Suisse", "Svizzera", "Suiza"), "CH")
        add(listOf("Austria", "Österreich"), "AT")
        add(listOf("Poland", "Polska", "Polonia"), "PL")
        add(listOf("Czech Republic", "Czechia", "Chequia"), "CZ")
        add(listOf("Hungary", "Magyarország", "Hungría"), "HU")
        add(listOf("Romania", "România", "Rumania"), "RO")
        add(listOf("Greece", "Hellas", "Grecia"), "GR")
        add(listOf("Turkey", "Türkiye", "Turquía"), "TR")

        add(listOf("Canada", "Canadá"), "CA")
        add(listOf("Mexico", "México"), "MX")
        add(listOf("Argentina"), "AR")
        add(listOf("Brazil", "Brasil"), "BR")
        add(listOf("Chile"), "CL")
        add(listOf("Colombia"), "CO")
        add(listOf("Peru", "Perú"), "PE")
        add(listOf("Uruguay"), "UY")
        add(listOf("Ecuador"), "EC")
        add(listOf("Venezuela"), "VE")
        add(listOf("Bolivia"), "BO")
        add(listOf("Paraguay"), "PY")
        add(listOf("Costa Rica"), "CR")
        add(listOf("Panama", "Panamá"), "PA")
        add(listOf("Dominican Republic", "República Dominicana"), "DO")
        add(listOf("Guatemala"), "GT")
        add(listOf("Honduras"), "HN")
        add(listOf("El Salvador"), "SV")
        add(listOf("Nicaragua"), "NI")

        add(listOf("Australia"), "AU")
        add(listOf("New Zealand", "Nueva Zelanda"), "NZ")
        add(listOf("Japan", "Japón"), "JP")
        add(listOf("South Korea", "Korea, Republic of", "Corea del Sur"), "KR")
        add(listOf("China", "PRC"), "CN")
        add(listOf("India"), "IN")
    }

    val phoneCodeByISO2: Map<String, String> = mapOf(
        "US" to "1", "CA" to "1", "GB" to "44", "IE" to "353",
        "NO" to "47", "SE" to "46", "DK" to "45", "FI" to "358",
        "DE" to "49", "FR" to "33", "IT" to "39", "ES" to "34", "PT" to "351",
        "NL" to "31", "BE" to "32", "CH" to "41", "AT" to "43",
        "PL" to "48", "CZ" to "420", "HU" to "36", "RO" to "40", "GR" to "30",
        "TR" to "90",
        "MX" to "52", "AR" to "54", "BR" to "55", "CL" to "56",
        "CO" to "57", "PE" to "51", "UY" to "598", "EC" to "593", "VE" to "58",
        "BO" to "591", "PY" to "595",
        "CR" to "506", "PA" to "507", "DO" to "1", "GT" to "502", "HN" to "504",
        "SV" to "503", "NI" to "505",
        "AU" to "61", "NZ" to "64", "JP" to "81", "KR" to "82", "CN" to "86", "IN" to "91",
    )

    val localeByISO2: Map<String, String> = mapOf(
        "US" to "en-US", "CA" to "en-CA", "GB" to "en-GB", "IE" to "en-IE",
        "NO" to "nb-NO", "SE" to "sv-SE", "DK" to "da-DK", "FI" to "fi-FI",
        "DE" to "de-DE", "FR" to "fr-FR", "IT" to "it-IT", "ES" to "es-ES", "PT" to "pt-PT",
        "NL" to "nl-NL", "BE" to "nl-BE", "CH" to "de-CH", "AT" to "de-AT",
        "PL" to "pl-PL", "CZ" to "cs-CZ", "HU" to "hu-HU", "RO" to "ro-RO", "GR" to "el-GR",
        "TR" to "tr-TR",
        "MX" to "es-MX", "AR" to "es-AR", "BR" to "pt-BR", "CL" to "es-CL",
        "CO" to "es-CO", "PE" to "es-PE", "UY" to "es-UY", "EC" to "es-EC", "VE" to "es-VE",
        "BO" to "es-BO", "PY" to "es-PY",
        "CR" to "es-CR", "PA" to "es-PA", "DO" to "es-DO", "GT" to "es-GT", "HN" to "es-HN",
        "SV" to "es-SV", "NI" to "es-NI",
        "AU" to "en-AU", "NZ" to "en-NZ", "JP" to "ja-JP", "KR" to "ko-KR", "CN" to "zh-CN", "IN" to "en-IN",
    )

    val provinceCodeByCountry: Map<String, Map<String, String>> = buildMap {
        put("US", mapOf(
            "alabama" to "AL", "alaska" to "AK", "arizona" to "AZ", "arkansas" to "AR",
            "california" to "CA", "colorado" to "CO", "connecticut" to "CT", "delaware" to "DE",
            "district of columbia" to "DC", "florida" to "FL", "georgia" to "GA", "hawaii" to "HI",
            "idaho" to "ID", "illinois" to "IL", "indiana" to "IN", "iowa" to "IA", "kansas" to "KS",
            "kentucky" to "KY", "louisiana" to "LA", "maine" to "ME", "maryland" to "MD",
            "massachusetts" to "MA", "michigan" to "MI", "minnesota" to "MN", "mississippi" to "MS",
            "missouri" to "MO", "montana" to "MT", "nebraska" to "NE", "nevada" to "NV",
            "new hampshire" to "NH", "new jersey" to "NJ", "new mexico" to "NM", "new york" to "NY",
            "north carolina" to "NC", "north dakota" to "ND", "ohio" to "OH", "oklahoma" to "OK",
            "oregon" to "OR", "pennsylvania" to "PA", "rhode island" to "RI", "south carolina" to "SC",
            "south dakota" to "SD", "tennessee" to "TN", "texas" to "TX", "utah" to "UT",
            "vermont" to "VT", "virginia" to "VA", "washington" to "WA", "west virginia" to "WV",
            "wisconsin" to "WI", "wyoming" to "WY",
        ))

        put("CA", mapOf(
            "alberta" to "AB", "british columbia" to "BC", "manitoba" to "MB", "new brunswick" to "NB",
            "newfoundland and labrador" to "NL", "nova scotia" to "NS", "ontario" to "ON",
            "prince edward island" to "PE", "quebec" to "QC", "saskatchewan" to "SK",
            "northwest territories" to "NT", "nunavut" to "NU", "yukon" to "YT",
        ))

        put("MX", mapOf(
            "ciudad de mexico" to "CDMX", "cdmx" to "CDMX", "estado de mexico" to "MEX", "méxico" to "MEX",
            "mexico" to "MEX", "nuevo leon" to "NL", "jalisco" to "JAL", "puebla" to "PUE",
            "guanajuato" to "GTO", "queretaro" to "QRO", "quintana roo" to "ROO", "veracruz" to "VER",
            "yucatan" to "YUC", "baja california" to "BCN", "baja california sur" to "BCS",
            "chihuahua" to "CHH", "coahuila" to "COA", "sonora" to "SON", "sinaloa" to "SIN",
            "tamaulipas" to "TAM", "michoacan" to "MIC", "oaxaca" to "OAX", "chiapas" to "CHP",
            "hidalgo" to "HID", "san luis potosi" to "SLP", "morelos" to "MOR", "tlaxcala" to "TLA",
            "nayarit" to "NAY", "durango" to "DUR", "zacatecas" to "ZAC", "aguascalientes" to "AGU",
            "colima" to "COL", "campeche" to "CAM",
        ))

        put("BR", mapOf(
            "sao paulo" to "SP", "rio de janeiro" to "RJ", "minas gerais" to "MG",
            "rio grande do sul" to "RS", "parana" to "PR", "santa catarina" to "SC", "bahia" to "BA",
            "pernambuco" to "PE", "ceara" to "CE", "distrito federal" to "DF", "goias" to "GO",
            "espirito santo" to "ES", "mato grosso" to "MT", "mato grosso do sul" to "MS",
            "para" to "PA", "amazonas" to "AM",
        ))

        put("AR", mapOf(
            "ciudad autonoma de buenos aires" to "C", "caba" to "C", "buenos aires" to "B",
            "cordoba" to "X", "santa fe" to "S", "mendoza" to "M", "tucuman" to "T",
        ))

        put("CL", mapOf(
            "region metropolitana" to "RM", "región metropolitana" to "RM", "santiago" to "RM",
        ))

        put("CO", mapOf(
            "bogota d.c." to "DC", "bogota" to "DC", "antioquia" to "ANT", "valle del cauca" to "VAC",
            "cundinamarca" to "CUN", "atlantico" to "ATL",
        ))

        put("PE", mapOf(
            "lima" to "LIM", "arequipa" to "ARE", "cusco" to "CUS", "la libertad" to "LAL",
            "piura" to "PIU", "loreto" to "LOR", "ancash" to "ANC",
        ))
    }
}

// endregion
