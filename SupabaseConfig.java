package com.faizmart.india;

public final class SupabaseConfig {
    public static final String URL = BuildConfig.SUPABASE_URL;
    public static final String ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;
    public static final String STORAGE_BUCKET = "product-images";
    private SupabaseConfig() {}

    public static boolean isConfigured() {
        return URL != null && ANON_KEY != null
                && URL.startsWith("https://")
                && !URL.contains("YOUR-PROJECT")
                && !ANON_KEY.contains("YOUR_SUPABASE");
    }
}
