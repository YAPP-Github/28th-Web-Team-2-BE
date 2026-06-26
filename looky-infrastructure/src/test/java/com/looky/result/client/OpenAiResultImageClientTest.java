package com.looky.result.client;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiResultImageClientTest {

    @Test
    void buildsMultipartBodyWithEveryReferenceImageAsFilePart() throws Exception {
        byte[] body = invokeBuildMultipartBody(
                "looky-boundary",
                "gpt-image-1.5",
                "low",
                "abstract mascot prompt",
                List.of("base.png", "open-stars.png"),
                List.of("base-image".getBytes(StandardCharsets.UTF_8), "open-image".getBytes(StandardCharsets.UTF_8))
        );

        String multipart = new String(body, StandardCharsets.ISO_8859_1);

        assertTrue(multipart.contains("name=\"model\"\r\n\r\ngpt-image-1.5"));
        assertTrue(multipart.contains("name=\"quality\"\r\n\r\nlow"));
        assertTrue(multipart.contains("name=\"output_format\"\r\n\r\npng"));
        assertTrue(multipart.contains("name=\"n\"\r\n\r\n1"));
        assertTrue(multipart.contains("name=\"prompt\"\r\n\r\nabstract mascot prompt"));
        assertEquals(2, countOccurrences(multipart, "name=\"image[]\""));
        assertTrue(multipart.contains("filename=\"base.png\""));
        assertTrue(multipart.contains("filename=\"open-stars.png\""));
    }

    @Test
    void decodesFirstBase64ImageFromOpenAiResponse() throws Exception {
        String expected = "png-bytes";
        byte[] imageBytes = invokeExtractImageBytes("""
                {
                  "data": [
                    { "b64_json": "%s" }
                  ]
                }
                """.formatted(Base64.getEncoder().encodeToString(expected.getBytes(StandardCharsets.UTF_8))));

        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), imageBytes);
    }

    @Test
    void throwsWhenOpenAiResponseHasNoImageData() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> invokeExtractImageBytes("""
                        {
                          "data": []
                        }
                        """)
        );

        assertEquals("OpenAI image response is missing PNG data", exception.getMessage());
    }

    private static byte[] invokeBuildMultipartBody(
            String boundary,
            String model,
            String quality,
            String prompt,
            List<String> filenames,
            List<byte[]> contents
    ) throws Exception {
        Method method = OpenAiResultImageClient.class.getDeclaredMethod(
                "buildMultipartBody",
                String.class,
                String.class,
                String.class,
                String.class,
                List.class,
                List.class
        );
        method.setAccessible(true);
        return (byte[]) method.invoke(null, boundary, model, quality, prompt, filenames, contents);
    }

    private static byte[] invokeExtractImageBytes(String responseBody) throws Exception {
        Method method = OpenAiResultImageClient.class.getDeclaredMethod("extractImageBytes", String.class);
        method.setAccessible(true);
        try {
            return (byte[]) method.invoke(null, responseBody);
        } catch (InvocationTargetException exception) {
            if (exception.getTargetException() instanceof Exception targetException) {
                throw targetException;
            }
            throw exception;
        }
    }

    private static int countOccurrences(String value, String token) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
