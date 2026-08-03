package com.vsk.orders.data

object Constants {
    // Voice notes need Cloud Storage for Firebase, which Google now gates
    // behind the paid Blaze plan. With this off the recording UI is hidden
    // everywhere and no upload is attempted, so the rest of the app runs on
    // the free tier. Flip to true once the project has billing enabled.
    const val VOICE_NOTES_ENABLED = false

    // Hardcoded bootstrap super admins — mirrored in firestore.rules so the
    // very first sign-in for these emails is allowed to self-elevate.
    val SUPER_ADMIN_EMAILS = listOf("ashisha340d@gmail.com")

    data class StationSeed(val id: String, val name: String)

    val SEED_STATIONS = listOf(
        StationSeed("mangarh", "Mangarh"),
        StationSeed("vrindavan", "Vrindavan"),
        StationSeed("barsana", "Barsana"),
        StationSeed("mussoorie", "Mussoorie")
    )

    // "For demo" accounts requested for the walkthrough. Seeding only creates
    // their Firestore profile (approved, PIN 1234, all stations) — each still
    // has to actually sign up once with this email in the app to create the
    // real Firebase Auth account before they can sign in.
    val DEMO_USER_EMAILS = listOf("dummy1@gmail.com", "dummy2@gmail.com")
    const val DEMO_PIN = "1234"
}
