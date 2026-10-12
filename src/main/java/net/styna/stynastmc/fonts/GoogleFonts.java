package net.styna.stynastmc.fonts;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.styna.stynastmc.StynaSTMC;

/**
 * Google Fonts catalog and downloads.
 * The official Developer API needs a key, so the family list comes from the keyless metadata feed that fonts.google.com uses,
 * and TTF URLs come from the CSS2 API, which serves TrueType to legacy user agents.
 */
public final class GoogleFonts {
    public static final Path DIR = StynaSTMC.modConfigDir().resolve("fonts");
    private static final Path CATALOG = DIR.resolve("google_fonts.json");
    private static final Path CACHE = DIR.resolve("cache");
    private static final long CATALOG_MAX_AGE = TimeUnit.DAYS.toMillis(7);
    private static final String METADATA_URL = "https://fonts.google.com/metadata/fonts";
    private static final String LEGACY_USER_AGENT = "Mozilla/4.0";
    private static final Pattern FONT_URL = Pattern.compile("url\\((https://[^)]+)\\)");
    private static final Gson GSON = new Gson();

    private static volatile CompletableFuture<List<Family>> catalog;
    private static final ConcurrentHashMap<String, CompletableFuture<Path>> DOWNLOADS = new ConcurrentHashMap<>();

    private GoogleFonts() {
    }

    /** {@code variants} are weights with an i suffix for italic, e.g. 400, 700i. */
    public static final class Family {
        public final String family;
        public final String category;
        public final List<String> variants;
        public final List<String> designers;
        public final int popularity;

        Family(String family, String category, List<String> variants, List<String> designers, int popularity) {
            this.family = family;
            this.category = category;
            this.variants = variants;
            this.designers = designers;
            this.popularity = popularity;
        }

        public String defaultVariant() {
            return variants.contains("400") ? "400" : variants.get(0);
        }
    }

    public static CompletableFuture<List<Family>> catalog(boolean refresh) {
        if (refresh || catalog == null || catalog.isCompletedExceptionally()) {
            catalog = CompletableFuture.supplyAsync(() -> loadCatalog(refresh));
        }
        return catalog;
    }

    private static List<Family> loadCatalog(boolean refresh) {
        try {
            if (!refresh && Files.exists(CATALOG)
                    && Files.getLastModifiedTime(CATALOG).toMillis() > System.currentTimeMillis() - CATALOG_MAX_AGE) {
                return parseCatalog(readJson(CATALOG).getAsJsonArray());
            }
            String body = new String(get(METADATA_URL), StandardCharsets.UTF_8);
            // The feed may carry the XSSI guard Google puts in front of JSON.
            int start = body.indexOf('{');
            JsonArray families = new JsonArray();
            for (JsonElement element : new JsonParser().parse(body.substring(Math.max(0, start))).getAsJsonObject().getAsJsonArray("familyMetadataList")) {
                JsonObject in = element.getAsJsonObject();
                JsonObject out = new JsonObject();
                out.addProperty("family", in.get("family").getAsString());
                out.addProperty("category", in.get("category").getAsString());
                out.addProperty("popularity", in.get("popularity").getAsInt());
                JsonArray variants = new JsonArray();
                for (Map.Entry<String, JsonElement> font : in.getAsJsonObject("fonts").entrySet()) {
                    variants.add(new JsonPrimitive(font.getKey()));
                }
                out.add("variants", variants);
                out.add("designers", in.get("designers"));
                families.add(out);
            }
            Files.createDirectories(DIR);
            Files.write(CATALOG, GSON.toJson(families).getBytes(StandardCharsets.UTF_8));
            return parseCatalog(families);
        } catch (IOException | RuntimeException e) {
            // Fall back to a stale catalog rather than nothing.
            try {
                if (Files.exists(CATALOG)) {
                    return parseCatalog(readJson(CATALOG).getAsJsonArray());
                }
            } catch (IOException | RuntimeException ignored) {
            }
            throw new RuntimeException("Could not load the Google Fonts catalog", e);
        }
    }

    private static JsonElement readJson(Path file) throws IOException {
        return new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    private static List<Family> parseCatalog(JsonArray families) {
        List<Family> out = new ArrayList<>();
        for (JsonElement element : families) {
            JsonObject o = element.getAsJsonObject();
            List<String> variants = new ArrayList<>();
            o.getAsJsonArray("variants").forEach(v -> variants.add(v.getAsString()));
            variants.sort(Comparator.comparing((String v) -> v.endsWith("i")).thenComparing(GoogleFonts::weight));
            List<String> designers = new ArrayList<>();
            o.getAsJsonArray("designers").forEach(d -> designers.add(d.getAsString()));
            if (!variants.isEmpty()) {
                out.add(new Family(o.get("family").getAsString(), o.get("category").getAsString(), variants, designers, o.get("popularity").getAsInt()));
            }
        }
        out.sort(Comparator.comparingInt(f -> f.popularity));
        return Collections.unmodifiableList(out);
    }

    public static int weight(String variant) {
        return Integer.parseInt(variant.endsWith("i") ? variant.substring(0, variant.length() - 1) : variant);
    }

    public static String variantName(String variant) {
        String name = switch (weight(variant)) {
            case 100 -> "Thin";
            case 200 -> "Extra Light";
            case 300 -> "Light";
            case 400 -> "Regular";
            case 500 -> "Medium";
            case 600 -> "Semi Bold";
            case 700 -> "Bold";
            case 800 -> "Extra Bold";
            case 900 -> "Black";
            default -> variant;
        };
        return variant.endsWith("i") ? name + " Italic" : name;
    }

    private static Path cacheFile(String family, String variant) {
        return CACHE.resolve(family.replaceAll("[^A-Za-z0-9]+", "_") + "-" + variant + ".ttf");
    }

    public static CompletableFuture<Path> download(String family, String variant) {
        Path file = cacheFile(family, variant);
        if (Files.exists(file)) {
            return CompletableFuture.completedFuture(file);
        }
        return DOWNLOADS.computeIfAbsent(file.toString(), k -> CompletableFuture.supplyAsync(() -> {
            try {
                String query = URLEncoder.encode(family, "UTF-8").replace("%20", "+")
                        + ":ital,wght@" + (variant.endsWith("i") ? 1 : 0) + "," + weight(variant);
                Matcher url = FONT_URL.matcher(new String(get("https://fonts.googleapis.com/css2?family=" + query), StandardCharsets.UTF_8));
                if (!url.find()) {
                    throw new IOException("No font file for " + family + " " + variant);
                }
                byte[] font = get(url.group(1));
                Files.createDirectories(CACHE);
                Path tmp = Files.createTempFile(CACHE, "download", ".tmp");
                Files.write(tmp, font);
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                return file;
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                DOWNLOADS.remove(file.toString());
            }
        }));
    }

    public static boolean isCached(String family, String variant) {
        return Files.exists(cacheFile(family, variant));
    }

    private static byte[] get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", LEGACY_USER_AGENT);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        int status = connection.getResponseCode();
        if (status != 200) {
            throw new IOException("HTTP " + status + " from " + url);
        }
        try (InputStream in = connection.getInputStream()) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
