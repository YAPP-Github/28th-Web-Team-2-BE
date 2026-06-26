package com.looky.result.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.looky.result.application.ResultImageClient;
import com.looky.result.application.ResultImageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
@Profile("!test & !local")
public class OpenAiResultImageClient implements ResultImageClient {

    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1";
    private static final String IMAGE_EDIT_PATH = "/images/edits";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final S3Client s3Client;
    private final String bucket;
    private final String imageModel;
    private final String imageQuality;
    private final HttpClient httpClient;
    private final URI openAiBaseUrl;

    @Autowired
    public OpenAiResultImageClient(
            S3Client s3Client,
            @Value("${looky.result-generation.s3.bucket}") String bucket,
            @Value("${looky.result-generation.image-model}") String imageModel,
            @Value("${looky.result-generation.image-quality}") String imageQuality
    ) {
        this(s3Client, bucket, imageModel, imageQuality, HttpClient.newHttpClient(), resolveOpenAiBaseUrl());
    }

    OpenAiResultImageClient(
            S3Client s3Client,
            String bucket,
            String imageModel,
            String imageQuality,
            HttpClient httpClient,
            URI openAiBaseUrl
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.imageModel = imageModel;
        this.imageQuality = imageQuality;
        this.httpClient = httpClient;
        this.openAiBaseUrl = openAiBaseUrl;
    }

    @Override
    public byte[] generate(ResultImageRequest request) {
        List<String> filenames = new ArrayList<>();
        List<byte[]> contents = new ArrayList<>();
        for (int index = 0; index < request.referenceAssetKeys().size(); index++) {
            String assetKey = request.referenceAssetKeys().get(index);
            filenames.add(extractFilename(assetKey, index));
            contents.add(readAssetBytes(assetKey));
        }
        String boundary = "looky-" + UUID.randomUUID();
        byte[] requestBody = buildMultipartBody(
                boundary,
                imageModel,
                imageQuality,
                request.imagePrompt(),
                filenames,
                contents
        );
        HttpRequest httpRequest = HttpRequest.newBuilder(openAiImageEditUri())
                .header("Authorization", "Bearer " + requireOpenAiApiKey())
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();
        HttpResponse<String> response = send(httpRequest);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalArgumentException(
                    "OpenAI image edit request failed: status=%s, body=%s"
                            .formatted(response.statusCode(), truncate(response.body()))
            );
        }
        return extractImageBytes(response.body());
    }

    @Override
    public String modelName() {
        return imageModel;
    }

    private byte[] readAssetBytes(String assetKey) {
        try (ResponseInputStream<GetObjectResponse> assetStream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(assetKey)
                .build())) {
            return assetStream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read reference asset from S3: key=" + assetKey, exception);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("OpenAI image edit request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI image edit request interrupted", exception);
        }
    }

    private URI openAiImageEditUri() {
        String baseUrl = openAiBaseUrl.toString();
        if (baseUrl.endsWith("/")) {
            return URI.create(baseUrl.substring(0, baseUrl.length() - 1) + IMAGE_EDIT_PATH);
        }
        return URI.create(baseUrl + IMAGE_EDIT_PATH);
    }

    private static URI resolveOpenAiBaseUrl() {
        String configuredBaseUrl = System.getenv("OPENAI_BASE_URL");
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            return URI.create(DEFAULT_OPENAI_BASE_URL);
        }
        return URI.create(configuredBaseUrl);
    }

    private static String requireOpenAiApiKey() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is not set");
        }
        return apiKey;
    }

    private static String extractFilename(String assetKey, int index) {
        if (assetKey == null || assetKey.isBlank()) {
            return "reference-image-%d.png".formatted(index + 1);
        }
        int slashIndex = assetKey.lastIndexOf('/');
        String filename = slashIndex >= 0 ? assetKey.substring(slashIndex + 1) : assetKey;
        if (filename.isBlank()) {
            return "reference-image-%d.png".formatted(index + 1);
        }
        return filename;
    }

    private static byte[] buildMultipartBody(
            String boundary,
            String model,
            String quality,
            String prompt,
            List<String> filenames,
            List<byte[]> contents
    ) {
        if (filenames.size() != contents.size()) {
            throw new IllegalArgumentException("Reference image filenames and contents must have the same size");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writeTextPart(outputStream, boundary, "model", model);
            writeTextPart(outputStream, boundary, "quality", quality);
            writeTextPart(outputStream, boundary, "output_format", "png");
            writeTextPart(outputStream, boundary, "n", "1");
            writeTextPart(outputStream, boundary, "prompt", prompt);
            for (int index = 0; index < filenames.size(); index++) {
                writeFilePart(outputStream, boundary, "image[]", filenames.get(index), contents.get(index));
            }
            outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to build OpenAI image multipart body", exception);
        }
    }

    private static void writeTextPart(ByteArrayOutputStream outputStream, String boundary, String name, String value) throws IOException {
        outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFilePart(ByteArrayOutputStream outputStream, String boundary, String name, String filename, byte[] contents) throws IOException {
        outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: " + detectContentType(filename) + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(contents);
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String detectContentType(String filename) {
        String contentType = URLConnection.guessContentTypeFromName(filename);
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType;
    }

    private static byte[] extractImageBytes(String responseBody) {
        try {
            OpenAiImageResponse response = OBJECT_MAPPER.readValue(responseBody, OpenAiImageResponse.class);
            String image = response.data == null ? null : response.data.stream()
                    .map(imageData -> imageData.b64Json)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
            if (image == null) {
                throw new IllegalArgumentException("OpenAI image response is missing PNG data");
            }
            return Base64.getDecoder().decode(image);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("OpenAI image response is invalid JSON", exception);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= 1_000) {
            return value;
        }
        return value.substring(0, 1_000) + "...";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenAiImageResponse {
        public List<OpenAiImageData> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenAiImageData {
        @JsonProperty("b64_json")
        public String b64Json;
    }
}
