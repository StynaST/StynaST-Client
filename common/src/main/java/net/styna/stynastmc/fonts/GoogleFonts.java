package net.styna.stynastmc.fonts;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.styna.stynastmc.platform.Platform;
import net.styna.stynastmc.StynaSTMC;

/**
 * Google Fonts catalog and downloads.
 * The official Developer API needs a key, so the family list comes from the keyless metadata feed that fonts.google.com uses,
 * and TTF URLs come from the CSS2 API, which serves TrueType to legacy user agents.
 */
public final class GoogleFonts {
    public static final Path DIR = Platform.INSTANCE.configDir().resolve(StynaSTMC.MODID).resolve("fonts");
    private static final Path CATALOG = DIR.resolve("google_fonts.json");
    private static final Path CACHE = DIR.resolve("cache");
    private static final Duration CATALOG_MAX_AGE = Duration.ofDays(7);
    private static final String METADATA_URL = "https://fonts.google.com/metadata/fonts";
    private static final String LEGACY_USER_AGENT = "Mozilla/4.0";
    private static final Pattern FONT_URL = Pattern.compile("url\\((https://[^)]+)\\)");
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15)).build();
    private static final Gson GSON = new Gson();

    private static volatile CompletableFuture<List<Family>> catalog;
    private static final ConcurrentHashMap<String, CompletableFuture<Path>> DOWNLOADS = new ConcurrentHashMap<>();

    private GoogleFonts() {
    }

    /**
     * @param variants weights with an i suffix for italic, e.g. 400, 700i
     */
    public record Family(String family, String category, List<String> variants, List<String> designers, int popularity) {
        public String defaultVariant() {
            return variants.contains("400") ? "400" : variants.getFirst();
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
                    && Files.getLastModifiedTime(CATALOG).toInstant().isAfter(Instant.now().minus(CATALOG_MAX_AGE))) {
                return parseCatalog(JsonParser.parseString(Files.readString(CATALOG)).getAsJsonArray());
            }
            JsonArray families = new JsonArray();
            for (JsonElement element : JsonParser.parseString(get(METADATA_URL)).getAsJsonObject().getAsJsonArray("familyMetadataList")) {
                JsonObject in = element.getAsJsonObject();
                JsonObject out = new JsonObject();
                out.addProperty("family", in.get("family").getAsString());
                out.addProperty("category", in.get("category").getAsString());
                out.addProperty("popularity", in.get("popularity").getAsInt());
                JsonArray variants = new JsonArray();
                in.getAsJsonObject("fonts").keySet().forEach(variants::add);
                out.add("variants", variants);
                out.add("designers", in.get("designers"));
                families.add(out);
            }
            Files.createDirectories(DIR);
            Files.writeString(CATALOG, GSON.toJson(families));
            return parseCatalog(families);
        } catch (IOException | InterruptedException e) {
            // Fall back to a stale catalog rather than nothing.
            try {
                if (Files.exists(CATALOG)) {
                    return parseCatalog(JsonParser.parseString(Files.readString(CATALOG)).getAsJsonArray());
                }
            } catch (IOException ignored) {
            }
            throw new RuntimeException("Could not load the Google Fonts catalog", e);
        }
    }

    private static List<Family> parseCatalog(JsonArray families) {
        List<Family> out = new ArrayList<>();
        for (JsonElement element : families) {
            JsonObject o = element.getAsJsonObject();
            List<String> variants = o.getAsJsonArray("variants").asList().stream().map(JsonElement::getAsString)
                    .sorted(Comparator.comparing((String v) -> v.endsWith("i")).thenComparing(GoogleFonts::weight)).toList();
            List<String> designers = o.getAsJsonArray("designers").asList().stream().map(JsonElement::getAsString).toList();
            if (!variants.isEmpty()) {
                out.add(new Family(o.get("family").getAsString(), o.get("category").getAsString(), variants, designers, o.get("popularity").getAsInt()));
            }
        }
        out.sort(Comparator.comparingInt(Family::popularity));
        return List.copyOf(out);
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

    public static CompletableFuture<Path> download(String family, String variant) {
        Path file = CACHE.resolve(family.replaceAll("[^A-Za-z0-9]+", "_") + "-" + variant + ".ttf");
        if (Files.exists(file)) {
            return CompletableFuture.completedFuture(file);
        }
        return DOWNLOADS.computeIfAbsent(file.toString(), k -> CompletableFuture.supplyAsync(() -> {
            try {
                String query = URLEncoder.encode(family, StandardCharsets.UTF_8).replace("%20", "+")
                        + ":ital,wght@" + (variant.endsWith("i") ? 1 : 0) + "," + weight(variant);
                Matcher url = FONT_URL.matcher(get("https://fonts.googleapis.com/css2?family=" + query));
                if (!url.find()) {
                    throw new IOException("No font file for " + family + " " + variant);
                }
                HttpResponse<byte[]> font = HTTP.send(request(url.group(1)), HttpResponse.BodyHandlers.ofByteArray());
                if (font.statusCode() != 200) {
                    throw new IOException("HTTP " + font.statusCode() + " downloading " + family);
                }
                Files.createDirectories(CACHE);
                Path tmp = Files.createTempFile(CACHE, "download", ".tmp");
                Files.write(tmp, font.body());
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return file;
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                DOWNLOADS.remove(file.toString());
            }
        }));
    }

    public static boolean isCached(String family, String variant) {
        return Files.exists(CACHE.resolve(family.replaceAll("[^A-Za-z0-9]+", "_") + "-" + variant + ".ttf"));
    }

    private static String get(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = HTTP.send(request(url), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }
        return response.body();
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url)).header("User-Agent", LEGACY_USER_AGENT).timeout(Duration.ofSeconds(30)).build();
    }
}
