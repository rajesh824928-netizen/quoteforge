package com.example.domain.engine

data class PresetItem(
    val category: String,
    val name: String,
    val defaultSpec: String,
    val defaultUnit: String,
    val defaultRate: Double,
    val defaultTax: Double = 18.0
)

data class PresetTerm(
    val category: String,
    val title: String,
    val clauses: List<String>
)

object IndustryPresets {

    val UNITS = listOf(
        "sq.ft",
        "sq.m",
        "r.ft",
        "r.m",
        "nos",
        "kg",
        "litre",
        "set",
        "box",
        "hour",
        "day",
        "lump sum"
    )

    val ROOMS = listOf(
        "Living Room",
        "Dining Room",
        "Modular Kitchen",
        "Master Bedroom",
        "Bedroom 2",
        "Bedroom 3",
        "Kids Room",
        "Home Office",
        "Pooja Room",
        "Bathroom",
        "Utility Area",
        "Balcony",
        "Foyer",
        "Terrace",
        "Common Area",
        "Custom Room"
    )

    val PROJECT_TYPES = listOf(
        "Residential",
        "Commercial",
        "Office",
        "Villa",
        "Apartment",
        "Renovation",
        "Interior",
        "Construction",
        "Other"
    )

    val INTERIOR_CATEGORIES = listOf(
        "Modular Kitchen",
        "Wardrobes",
        "TV Unit",
        "False Ceiling",
        "Wall Panelling",
        "Electrical & Lighting",
        "Painting & Finishes",
        "Flooring & Tiling",
        "Loose Furniture",
        "Curtains & Blinds",
        "Civil Works",
        "Plumbing & Sanitary"
    )

    val CONSTRUCTION_CATEGORIES = listOf(
        "Excavation & Earthwork",
        "Foundation & Footing",
        "RCC Structural Works",
        "Brick & Block Masonry",
        "Plastering (Internal & External)",
        "Waterproofing",
        "Doors, Windows & Glazing",
        "Flooring & Wall Tiling",
        "Electrical Works",
        "Plumbing & Drainage",
        "Painting & Texturing",
        "External & Landscaping"
    )

    val PRESET_ITEMS = listOf(
        // Interior Items
        PresetItem("Modular Kitchen", "Base Cabinets (BWP Grade)", "18mm BWP marine plywood with inside 0.8mm off-white laminate, Hettich soft-close tandem boxes", "sq.ft", 1650.0),
        PresetItem("Modular Kitchen", "Wall Cabinets (Acrylic Finish)", "18mm BWP plywood carcass with 2mm high gloss anti-scratch acrylic shutters and Blum lift-up hinges", "sq.ft", 1850.0),
        PresetItem("Modular Kitchen", "Tall Unit with Pantry Pullout", "18mm BWP carcass with Hafele 6-tier stainless steel larder pullout system", "nos", 38000.0),
        PresetItem("Modular Kitchen", "Quartz Countertop with Edge Moulding", "18mm Kalinga quartz stone counter with bevelled edge and sink cutout", "r.ft", 2800.0),
        PresetItem("Wardrobes", "Floor-to-Ceiling Sliding Wardrobe", "Commercial BWP plywood carcass, 1mm Century textured laminate, Aristo slim aluminium profile frame", "sq.ft", 1450.0),
        PresetItem("Wardrobes", "Hinged Wardrobe with Loft", "18mm MR grade plywood with Greenlam matte laminate and Hafele soft-close hinges", "sq.ft", 1250.0),
        PresetItem("TV Unit", "Floating TV Console with Louvered Backing", "HDHMR with PU paint finish, fluted charcoal panels, and cable management ducts", "sq.ft", 1100.0),
        PresetItem("False Ceiling", "Gypsum Board False Ceiling with Cove", "Saint-Gobain Gypboard with GI channel framework and LED strip light perimeter cove", "sq.ft", 135.0),
        PresetItem("Wall Panelling", "Veneer Wall Panelling with Grooves", "Natural teak veneer with 12mm MDF backing and melamine matte polish", "sq.ft", 480.0),
        PresetItem("Electrical & Lighting", "Point Wiring & Switchboard Installation", "Polycab FR copper wires in concealed PVC conduits with Legrand Arteor switches", "nos", 950.0),
        PresetItem("Painting & Finishes", "Royal Luxury Emulsion Painting", "2 coats Asian Paints Royale with Birla white putty preparation and primer", "sq.ft", 38.0),
        PresetItem("Flooring & Tiling", "Italian Marble Flooring & Diamond Polishing", "Botticino / Dyna marble with cement mortar bed and 7-stage diamond polish", "sq.ft", 420.0),

        // Construction Items
        PresetItem("Excavation & Earthwork", "Mechanical Earth Excavation", "Excavation for footings and column trenches including disposal of soil up to 50m", "sq.m", 280.0),
        PresetItem("Foundation & Footing", "PCC 1:4:8 Bed Concrete", "Plain cement concrete using 40mm aggregate for foundation base layer 100mm thick", "sq.m", 4200.0),
        PresetItem("RCC Structural Works", "M25 Grade Ready-Mix Concrete", "Design mix M25 concrete for columns, beams and roof slabs including vibration and curing", "sq.m", 7200.0),
        PresetItem("RCC Structural Works", "Fe550D TMT Reinforcement Steel", "Tata Tiscon Fe550D cutting, bending, binding with 18-gauge GI wire in position", "kg", 88.0),
        PresetItem("Brick & Block Masonry", "AAC Block Masonry 200mm", "Autoclaved aerated concrete blocks with polymer-modified adhesive mortar", "sq.m", 1850.0),
        PresetItem("Plastering (Internal & External)", "Sand-Faced External Plastering", "Double coat cement plaster 20mm thick in 1:4 proportion with waterproofing compound", "sq.ft", 42.0),
        PresetItem("Waterproofing", "Bathroom & Balcony Polyurethane Waterproofing", "Fosroc Brushbond double-coat elastomeric coating with fibre mesh reinforcement", "sq.ft", 65.0)
    )

    val PRESET_TERMS = listOf(
        PresetTerm(
            "Interior",
            "Standard Interior Design Terms",
            listOf(
                "Quotation is valid for 30 days from the date of issue.",
                "Measurements are based on architectural drawings; final billing will reflect actual site dimensions.",
                "Electrical, civil, or plumbing works not explicitly listed in the scope are excluded.",
                "All materials used comply with standard ISI certifications (IS:710 for BWP Plywood, IS:303 for MR).",
                "Warranty covers manufacturer defects: Hardware 5 Years, Plywood Delamination 10 Years."
            )
        ),
        PresetTerm(
            "Construction",
            "General Civil & Construction Contract Terms",
            listOf(
                "Rates are based on current cement and steel market indices. Variations beyond 5% will be charged at actuals.",
                "Client to provide uninterrupted water and three-phase power supply at site.",
                "Municipal approvals, architect clearances, and sanction fees are the sole responsibility of the client.",
                "Standard structural guarantee of 10 years against major structural defects as per IS:456.",
                "Payment milestones are strictly linked to stage completion and inspection verification."
            )
        ),
        PresetTerm(
            "Payment",
            "Payment & Milestone Policy",
            listOf(
                "Payment must be disbursed within 5 business days of milestone completion notice.",
                "Cheques or NEFT/RTGS transfers should be made payable to the official company account specified.",
                "Taxes will be levied as per statutory GST rates at the time of invoicing."
            )
        )
    )
}
