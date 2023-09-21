package io.jenkins.update_center;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.update_center.util.HttpHelper;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

public class NxrmRepositoryImpl extends BaseMavenRepository {
    private static final Logger LOGGER = Logger.getLogger(NxrmRepositoryImpl.class.getName());

    // https://repo.sonatype.com/#browse/search=keyword%3Dnexus-jenkins-plugin%20AND%20attributes.maven2.artifactId%3Dnexus-jenkins-plugin:NX.coreui.model.Component-341

    // curl -X 'GET' \
    //  'http://localhost:1234/service/rest/v1/search?sort=version&maven.extension=hpi' \
    //  -H 'accept: application/json' \
    //  -H 'NX-ANTI-CSRF-TOKEN: 0.6829155349975051' \
    //  -H 'X-Nexus-UI: true'

    private static final String NXRM_API_URL = "https://repo.sonatype.com/service/rest/v1/";
    private static final String NXRM_PLUGIN_SEARCH_URL = NXRM_API_URL +
        "search?sort=version&direction=desc&" +
        "maven.artifactId=workflow-job";
    private static final String NXRM_CORE_WAR_SEARCH_URL = NXRM_API_URL +
        "search?sort=version&direction=desc&" +
        "maven.groupId=org.jenkins-ci.main&maven.artifactId=jenkins&maven.extension=war";

    private final String username;
    private final String password;
    private boolean initialized = false;

    private final Map<String, JsonAsset> files = new HashMap<>();
    private Set<ArtifactCoordinates> plugins;
    private Set<ArtifactCoordinates> wars;

    public NxrmRepositoryImpl(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected Set<ArtifactCoordinates> listAllJenkinsWars(String groupId) throws IOException {
        ensureInitialized();
        return wars;
    }

    private static boolean containsIllegalChars(String test) {
        return !test.chars().allMatch(c -> c >= 0x2B && c < 0x7B);
    }

    private static ArtifactCoordinates toGav(JsonAsset a) {
        return new ArtifactCoordinates(a.maven2.groupId, a.maven2.artifactId, a.maven2.version, a.maven2.extension);
    }

    @Override
    public Collection<ArtifactCoordinates> listAllPlugins() throws IOException {
        ensureInitialized();
        return plugins;
    }

    private static class JsonChecksum {
        public String sha1; // "83933f3f3312473afbe42a232392b3feffaadc36",
        public String sha256; // "05ee1e3f6e9b85a5ef90c1013e93b0bed92726376818e054d47382913201209f",
    }

    private static class JsonAsset {
        public String downloadUrl; // http://localhost:1234/repository/maven-central/bouncycastle/bcprov-jdk15/140/bcprov-jdk15-140.jar
        public String path; // "bouncycastle/bcprov-jdk15/140/bcprov-jdk15-140.jar",
        public JsonChecksum checksum;
        public Date lastModified; // "2008-09-29T14:23:43.000+00:00",
        public long fileSize; // 1593423,
        public JsonCoords maven2;
    }

    private static class JsonCoords {
        public String extension; // "jar",
        public String groupId; // "bouncycastle",
        public String artifactId; // "bcprov-jdk15",
        public String version;
    }

    private static class JsonItem {
        public List<JsonAsset> assets;
    }

    private static class JsonResponse {
        public List<JsonItem> items;
    }

    private void initialize() throws IOException {
        if (initialized) {
            throw new IllegalStateException("re-initialized");
        }
        LOGGER.log(Level.INFO, "Initializing " + this.getClass().getName());

        // collect plugins
        Request request = new Request.Builder().url(NXRM_PLUGIN_SEARCH_URL).addHeader("Authorization", Credentials.basic(username, password)).get().build();
        populateFiles(request);
        this.plugins = this.files.values().stream()
                .filter(it -> it.path.endsWith(".hpi"))
                .map(NxrmRepositoryImpl::toGav)
                .collect(Collectors.toSet());

        // collect wars
        request = new Request.Builder().url(NXRM_CORE_WAR_SEARCH_URL).addHeader("Authorization", Credentials.basic(username, password)).get().build();
        populateFiles(request);
        this.wars = this.files.values().stream()
            .filter(it -> it.path.endsWith(".war"))
            .map(NxrmRepositoryImpl::toGav)
            .collect(Collectors.toSet());
        LOGGER.log(Level.INFO, "Initialized " + this.getClass().getName());
    }

    private void populateFiles(Request request) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().build();
        try (final ResponseBody body = HttpHelper.body(client.newCall(request).execute())) {
            final MediaType mediaType = body.contentType();
            JsonResponse json = JSON.parseObject(body.byteStream(), mediaType == null ? StandardCharsets.UTF_8 : mediaType.charset(), JsonResponse.class);
            json.items.forEach(it -> it.assets.forEach(a -> this.files.put(a.path, a))
            );
        }
    }

    private String hexToBase64(String hex) throws IOException {
        try {
            byte[] decodedHex = Hex.decodeHex(hex);
            return Base64.encodeBase64String(decodedHex);
        } catch (DecoderException e) {
            throw new IOException("failed to convert hex to base64", e);
        }
    }

    @Override
    @SuppressFBWarnings(value="DCN_NULLPOINTER_EXCEPTION",
                        justification="Catching NPE is safer than trying to guard all cases")
    public ArtifactMetadata getMetadata(MavenArtifact artifact) throws IOException {
        ensureInitialized();
        ArtifactMetadata ret = new ArtifactMetadata();
        final JsonAsset jsonAsset = files.get("/" + getUri(artifact.artifact));
        try {
            ret.sha1 = hexToBase64(jsonAsset.checksum.sha1);
        } catch (NullPointerException e) {
            LOGGER.log(Level.WARNING, "No artifact: " + artifact);
            return null;
        }
        String hexSha256 = jsonAsset.checksum.sha256;
        if (hexSha256 != null) {
            ret.sha256 = hexToBase64(hexSha256);
        } else {
            LOGGER.log(Level.WARNING, "No SHA-256: " + artifact);
            return null;
        }
        ret.timestamp = jsonAsset.lastModified.getTime();
        ret.size = jsonAsset.fileSize;
        return ret;
    }

    private void ensureInitialized() throws IOException {
        if (!initialized) {
            initialize();
            initialized = true;
        }
    }

    private String getUri(ArtifactCoordinates a) {
        String basename = a.artifactId + "-" + a.version;
        String filename = basename + "." + a.packaging;
        return a.groupId.replace(".", "/") + "/" + a.artifactId + "/" + a.version + "/" + filename;
    }

    @Override
    public Manifest getManifest(MavenArtifact artifact) throws IOException {
        final JsonAsset jsonAsset = files.get("/" + getUri(artifact.artifact));
        try (InputStream is = getFileContent(jsonAsset.downloadUrl)) {
            return new Manifest(is);
        }
    }

    private InputStream getFileContent(String url) throws IOException {
        File manifestFile = getFile(url, "/META-INF/MANIFEST.MF");
        return Files.newInputStream(manifestFile.toPath());
    }

    private File getFile(final String url, String subPath) throws IOException {
        File file = Files.createTempFile("tmp", ".zip").toFile();
        File manifest = null;
        LOGGER.log(Level.INFO, "Downloading : " + url );

        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            OkHttpClient client = builder.build();
            Request request = new Request.Builder().url(url).addHeader("Authorization", Credentials.basic(username, password)).get().build();
            final Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                try (final ResponseBody body = HttpHelper.body(response)) {
                    try (InputStream inputStream = body.byteStream();
                         FileOutputStream fos = new FileOutputStream(file)) {
                        IOUtils.copy(inputStream, fos);
                    }
                    if (subPath == null) {
                        return file;
                    }
                    manifest = Files.createTempFile("tmp", ".MF").toFile();
                    TFile entry = new TFile(file, "/META-INF/MANIFEST.MF");
                    if (entry.exists()) {
                        try (TFileInputStream inputStream = new TFileInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(manifest)) {
                            IOUtils.copy(inputStream, fos);
                        }
                    }
                }
            } else {
                LOGGER.log(Level.INFO, "Received HTTP error response: " + response.code() + " for URL: " + url);
            }
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
        return manifest;
    }

    @Override
    public InputStream getZipFileEntry(MavenArtifact artifact, String path) throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public File resolve(ArtifactCoordinates artifact) throws IOException {
        /* Support loading files from local Maven repository to reduce redundancy */
        final String uri = getUri(artifact);
        final File localFile = new File(LOCAL_REPO, uri);
        if (localFile.exists()) {
            return localFile;
        }
        final JsonAsset jsonAsset = files.get("/" + getUri(artifact));
        return getFile(jsonAsset.downloadUrl, null);
    }

    private static final File LOCAL_REPO = new File(new File(System.getProperty("user.home")), ".m2/repository");
}
