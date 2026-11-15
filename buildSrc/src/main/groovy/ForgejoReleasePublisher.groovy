import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class ForgejoReleasePublisher {
    private final String token
    private final HttpClient http = HttpClient.newHttpClient()

    ForgejoReleasePublisher(String token) {
        this.token = token
    }

    Map publish(String serverUrl, String repo, String tag, String sha, Collection<File> jars) {
        if (!(tag ==~ /v\d+\.\d+\.\d+/)) {
            throw new GradleException("Expected a shared mod-version tag, got: $tag")
        }
        if (jars.isEmpty() || jars*.name.toSet().size() != jars.size()) {
            throw new GradleException('Expected release jars with unique filenames')
        }
        for (File jar : jars) {
            if (!jar.isFile() || jar.length() == 0) throw new GradleException("Missing or empty jar: $jar")
        }

        String api = "${serverUrl.replaceAll('/+$', '')}/api/v1/repos/$repo"
        String releaseUrl = "$api/releases/tags/${URLEncoder.encode(tag, 'UTF-8')}"
        def response = call('GET', releaseUrl, null, 200, 404)
        if (response.statusCode() == 404) {
            String notes = 'Fabric and NeoForge builds for multiple Minecraft versions. Choose the JAR matching your Minecraft version and mod loader.'
            response = call('POST', "$api/releases", JsonOutput.toJson([
                    tag_name: tag, target_commitish: sha, name: tag, body: notes
            ]), 201, 409, 422)
            // Another branch may have created the shared release first.
            if (response.statusCode() != 201) response = call('GET', releaseUrl, null, 200)
        }
        Map release = new JsonSlurper().parseText(response.body()) as Map
        String assetsUrl = "$api/releases/${release.id}/assets"

        for (File jar : jars) {
            def existing = release.assets?.find { it.name == jar.name }
            if (existing) {
                // Keep the published file until its replacement has uploaded successfully.
                def uploaded = uploadAsset(assetsUrl, jar, "${jar.name}.${UUID.randomUUID()}.upload")
                call('DELETE', "$assetsUrl/${existing.id}", null, 204, 404)
                call('PATCH', "$assetsUrl/${uploaded.id}", JsonOutput.toJson([name: jar.name]), 200, 201)
            } else {
                uploadAsset(assetsUrl, jar, jar.name)
            }
        }
        return release
    }

    private HttpResponse<String> call(String method, String url, String json, int... ok) {
        def req = HttpRequest.newBuilder(URI.create(url))
                .header('Authorization', "token $token")
                .header('Accept', 'application/json')
        if (json != null) {
            req.header('Content-Type', 'application/json').method(method, HttpRequest.BodyPublishers.ofString(json))
        } else {
            req.method(method, HttpRequest.BodyPublishers.noBody())
        }
        return check(http.send(req.build(), HttpResponse.BodyHandlers.ofString()), method, url, ok)
    }

    private Map uploadAsset(String assetsUrl, File jar, String name) {
        String url = "$assetsUrl?name=${URLEncoder.encode(name, 'UTF-8')}"
        String boundary = "----stynast${UUID.randomUUID()}"
        def head = "--$boundary\r\nContent-Disposition: form-data; name=\"attachment\"; filename=\"$name\"\r\n" +
                "Content-Type: application/java-archive\r\n\r\n"
        def body = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(head),
                HttpRequest.BodyPublishers.ofFile(jar.toPath()),
                HttpRequest.BodyPublishers.ofString("\r\n--$boundary--\r\n"))
        def req = HttpRequest.newBuilder(URI.create(url))
                .header('Authorization', "token $token")
                .header('Content-Type', "multipart/form-data; boundary=$boundary")
                .POST(body).build()
        def response = check(http.send(req, HttpResponse.BodyHandlers.ofString()), 'POST', url, 201)
        return new JsonSlurper().parseText(response.body()) as Map
    }

    private static HttpResponse<String> check(HttpResponse<String> response, String method, String url, int... ok) {
        if (!(response.statusCode() in ok)) {
            throw new GradleException("$method $url -> ${response.statusCode()}: ${response.body()}")
        }
        return response
    }
}
