package com.vigyan.juniorcollege.util

/**
 * Fill these in with your own Supabase project's details after creating it
 * at https://supabase.com — see RELEASE.md (or the setup instructions given
 * alongside this file) for the exact steps: create a project, run the
 * provided SQL schema, create a "student-photos" storage bucket, then copy
 * your Project URL and key from Project Settings -> API Keys.
 *
 * Supabase now issues a "publishable" key (starts with sb_publishable_...)
 * instead of the older anon JWT key — either works here, just paste whichever
 * one your project's dashboard shows you. This key is DESIGNED to be embedded
 * in client apps like this one — Supabase's security model protects your data
 * via Row Level Security policies at the database level, not by keeping this
 * key secret. It is not the same as your database password or a "secret" key
 * (sb_secret_...), and a secret key should never be put in an app like this.
 */
object SupabaseConfig {
    const val SUPABASE_URL = "https://ozdpiubpgvgptizrwzfe.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_hBsBPskNyrWzVWd_QabRSA_m4ENdvgf"

    const val PHOTOS_BUCKET = "student-photos"

    val isConfigured: Boolean
        get() = SUPABASE_URL != "https://YOUR-PROJECT-REF.supabase.co" &&
            SUPABASE_ANON_KEY != "YOUR-ANON-PUBLIC-KEY-HERE" &&
            SUPABASE_URL.isNotBlank() && SUPABASE_ANON_KEY.isNotBlank()
}
