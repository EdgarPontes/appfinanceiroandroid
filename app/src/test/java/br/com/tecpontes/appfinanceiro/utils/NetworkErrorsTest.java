package br.com.tecpontes.appfinanceiro.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import org.junit.Test;

public class NetworkErrorsTest {

    @Test
    public void detectsOfflineHostResolutionErrors() {
        UnknownHostException error =
                new UnknownHostException("Unable to resolve host \"api.example.com\"");

        assertTrue(NetworkErrors.isOffline(error));
        assertEquals(NetworkErrors.OFFLINE_MESSAGE, NetworkErrors.message(error, "Falhou"));
    }

    @Test
    public void keepsBusinessMessagesForOtherFailures() {
        RuntimeException error = new RuntimeException("Categoria inválida");

        assertFalse(NetworkErrors.isOffline(error));
        assertEquals("Categoria inválida", NetworkErrors.message(error, "Categoria inválida"));
    }
}
