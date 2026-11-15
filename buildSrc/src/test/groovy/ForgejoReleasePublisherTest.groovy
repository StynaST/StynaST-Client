import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.junit.Assert.*

class ForgejoReleasePublisherTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder()

    private HttpServer server
    private String serverUrl
    private final Map<Long, Map> assets = [:]
    private final List<String> requests = []
    private Map release
    private long nextId = 1
    private int createConflict
    private boolean failUploads

    @Before
    void startServer() {
        server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
        server.createContext('/api/v1/repos/Minecraft/StynaST-Client') { HttpExchange exchange ->
            handle(exchange)
        }
        server.start()
        serverUrl = "http://127.0.0.1:${server.address.port}"
    }

    @After
    void stopServer() {
        server.stop(0)
    }

    @Test
    void combinesAllMinecraftAndLoaderBuildsInOneRelease() {
        for (String version : ['1.21.1', '26.1.2', '26.2', '26.3']) {
            publish(jar(version, 'fabric'), jar(version, 'neoforge'))
        }

        assertEquals(8, assets.size())
        assertEquals('v1.2.3', release.tag_name)
        assertEquals('test-commit', release.target_commitish)
        assertEquals(1, requests.count { it == 'POST /api/v1/repos/Minecraft/StynaST-Client/releases' })
        assertTrue(requests.every { !it.startsWith('DELETE ') })
        assertTrue(assets.values().every { it.name.endsWith('.jar') })
    }

    @Test
    void republishingReplacesOnlyMatchingFilesAndPreservesReleaseMetadata() {
        File current = jar('26.3', 'fabric')
        File other = jar('26.2', 'neoforge')
        publish(current, other)
        Map originalOther = assets.values().find { it.name == other.name }.clone() as Map
        long originalId = assets.values().find { it.name == current.name }.id
        release.body = 'Hand-written release notes'
        current.text = 'replacement jar contents'
        requests.clear()

        publish(current)

        assertEquals(2, assets.size())
        assertEquals(originalOther, assets.values().find { it.name == other.name })
        assertFalse(assets.containsKey(originalId))
        assertTrue(assets.values().find { it.name == current.name }.body.contains(current.text))
        assertEquals('Hand-written release notes', release.body)
        assertEquals('test-commit', release.target_commitish)
        assertEquals(['GET', 'POST', 'DELETE', 'PATCH'], requests.collect { it.tokenize(' ')[0] })
    }

    @Test
    void failedReplacementUploadKeepsPublishedFile() {
        File current = jar('26.3', 'fabric')
        publish(current)
        Map original = assets.values().first().clone() as Map
        failUploads = true
        requests.clear()

        assertThrows(GradleException) { publish(current) }

        assertEquals([original], assets.values().toList())
        assertTrue(requests.every { !it.startsWith('DELETE ') })
    }

    @Test
    void missingJarFailsBeforeAnyRemoteChanges() {
        File valid = jar('26.3', 'fabric')

        assertThrows(GradleException) { publish(valid, new File(temporary.root, 'missing.jar')) }

        assertTrue(requests.isEmpty())
    }

    @Test
    void emptyJarFailsBeforeAnyRemoteChanges() {
        File empty = temporary.newFile('empty.jar')

        assertThrows(GradleException) { publish(empty) }

        assertTrue(requests.isEmpty())
    }

    @Test
    void reusesReleaseCreatedByAnotherPublisherOnConflict() {
        createConflict = 409
        publish(jar('26.3', 'fabric'))

        assertEquals(1, assets.size())
        assertEquals(['GET', 'POST', 'GET', 'POST'], requests.collect { it.tokenize(' ')[0] })
    }

    @Test
    void reusesReleaseCreatedByAnotherPublisherOnValidationConflict() {
        createConflict = 422
        publish(jar('26.3', 'fabric'))

        assertEquals(1, assets.size())
        assertEquals(['GET', 'POST', 'GET', 'POST'], requests.collect { it.tokenize(' ')[0] })
    }

    @Test
    void noFreecamBuildsAreKeptAlongsideRegularBuilds() {
        publish(jar('26.1.2', 'fabric'), jar('26.1.2', 'fabric-nofreecam'))

        assertEquals(2, assets.size())
        assertTrue(assets.values().any { it.name.endsWith('-nofreecam.jar') })
    }

    @Test
    void rejectsMinecraftSpecificReleaseTag() {
        File current = jar('26.3', 'fabric')

        assertThrows(GradleException) {
            new ForgejoReleasePublisher('test-token').publish(serverUrl, 'Minecraft/StynaST-Client',
                    'v1.2.3+26.3', 'test-commit', [current])
        }

        assertTrue(requests.isEmpty())
    }

    private void publish(File... jars) {
        new ForgejoReleasePublisher('test-token').publish(serverUrl, 'Minecraft/StynaST-Client',
                'v1.2.3', 'test-commit', jars.toList())
    }

    private File jar(String version, String loader) {
        File file = temporary.newFile("StynaSTMC-1.2.3+${version}-${loader}.jar")
        file.text = "$version $loader jar contents"
        return file
    }

    private void handle(HttpExchange exchange) {
        String method = exchange.requestMethod
        String path = exchange.requestURI.path
        requests.add("$method $path".toString())
        if (exchange.requestHeaders.getFirst('Authorization') != 'token test-token') {
            respond(exchange, 401, [:])
            return
        }
        if (method == 'GET' && path.endsWith('/releases/tags/v1.2.3')) {
            respond(exchange, release ? 200 : 404, release ? release + [assets: assets.values().toList()] : [:])
        } else if (method == 'POST' && path.endsWith('/releases')) {
            release = new JsonSlurper().parseText(exchange.requestBody.text) as Map
            release.id = 7
            release.html_url = "$serverUrl/Minecraft/StynaST-Client/releases/tag/v1.2.3"
            respond(exchange, createConflict ?: 201, release + [assets: []])
        } else if (method == 'POST' && path.endsWith('/releases/7/assets')) {
            String name = URLDecoder.decode(exchange.requestURI.rawQuery.substring('name='.length()), 'UTF-8')
            String body = exchange.requestBody.text
            if (failUploads) {
                respond(exchange, 500, [message: 'upload failed'])
                return
            }
            Map asset = [id: nextId++, name: name, body: body]
            assets[asset.id as Long] = asset
            respond(exchange, 201, asset)
        } else if (path ==~ /.*\/releases\/7\/assets\/\d+/) {
            Long id = path.tokenize('/').last().toLong()
            if (method == 'DELETE') {
                assets.remove(id)
                respond(exchange, 204, null)
            } else if (method == 'PATCH') {
                assets[id].name = new JsonSlurper().parseText(exchange.requestBody.text).name
                respond(exchange, 201, assets[id])
            } else {
                respond(exchange, 405, [:])
            }
        } else {
            respond(exchange, 404, [:])
        }
    }

    private static void respond(HttpExchange exchange, int status, Object body) {
        byte[] data = body == null ? new byte[0] : JsonOutput.toJson(body).getBytes('UTF-8')
        exchange.responseHeaders.set('Content-Type', 'application/json')
        exchange.sendResponseHeaders(status, status == 204 ? -1 : data.length)
        if (status != 204) exchange.responseBody.write(data)
        exchange.close()
    }
}
