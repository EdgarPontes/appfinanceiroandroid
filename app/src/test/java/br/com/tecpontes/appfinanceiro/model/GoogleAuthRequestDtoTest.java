package br.com.tecpontes.appfinanceiro.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.junit.Test;

public class GoogleAuthRequestDtoTest {

    @Test
    public void serializesIdTokenAndRememberFlag() {
        GoogleAuthRequestDto request = new GoogleAuthRequestDto("google-id-token", true);

        String json = new Gson().toJson(request);

        assertTrue(json.contains("\"idToken\":\"google-id-token\""));
        assertTrue(json.contains("\"remember\":true"));
    }

    @Test
    public void toStringDoesNotExposeIdToken() {
        GoogleAuthRequestDto request = new GoogleAuthRequestDto("google-id-token", false);

        String text = request.toString();

        assertFalse(text.contains("google-id-token"));
        assertTrue(text.contains("idToken='[PROTECTED]'"));
        assertEquals(Boolean.FALSE, request.getRemember());
    }
}
