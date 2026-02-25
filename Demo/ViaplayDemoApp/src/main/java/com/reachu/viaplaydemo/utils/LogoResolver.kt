package com.reachu.viaplaydemo.utils

object LogoResolver {
    fun resolveLogo(name: String): String = when {
        name.startsWith("http", ignoreCase = true) -> name
        name == "barcelona_logo" -> "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSQAP2IclQZqijpViZ-aTdyctkIPKaAhlHB_g&s"
        name == "psg_logo" -> "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSqtvnGXPcoRvTWf6W6cgMTFT0b-4cI1h8R9g&s"
        name == "bvb_logo" -> "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSr4NT3OoOehxsYnMa7Zlccdh7B39OinLFF3g&s"
        name == "athletic_logo" -> "https://city-png.b-cdn.net/preview/preview_public/uploads/preview/hd-official-athletic-bilbao-logo-transparent-png-701751712235012tyctv8zkm8.png"
        name == "city_logo" -> "https://download.logo.wine/logo/Manchester_City_F.C./Manchester_City_F.C.-Logo.wine.png"
        name == "madrid_logo" -> "https://crystalpng.com/wp-content/uploads/2025/02/logo-real-madrid-02.png"
        else -> "https://upload.wikimedia.org/wikipedia/commons/6/65/TV2_Norge_logo.svg"
    }

    fun resolveImage(name: String): String = when {
        name.startsWith("http", ignoreCase = true) -> name
        name == "barcelona_psg_bg" -> "https://images.unsplash.com/photo-1518098268026-4e89f1a2cd8e?auto=format&w=1600&q=80"
        name == "bg-card-1" -> "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&w=1600&q=80"
        name == "bg-card-2" -> "https://images.unsplash.com/photo-1505664194779-8beaceb93744?auto=format&w=1600&q=80"
        name == "bg-card-3" -> "https://images.unsplash.com/photo-1508979827776-710d407ced5d?auto=format&w=1600&q=80"
        else -> "https://images.unsplash.com/photo-1507679799987-c73779587ccf?auto=format&w=1600&q=80"
    }
}
