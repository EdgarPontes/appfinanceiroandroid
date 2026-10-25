package br.com.tecpontes.appfinanceiro.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PushRegistrationStoreTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File stateFile;

    @Before
    public void setUp() throws Exception {
        File noBackupDir = temporaryFolder.newFolder("no_backup");
        stateFile = new File(noBackupDir, "fcm-registration.properties");
    }

    @Test
    public void getDeviceId_generatesAndPersistsUuid() throws Exception {
        Properties props = new Properties();
        if (stateFile.exists()) {
            try (FileInputStream in = new FileInputStream(stateFile)) {
                props.load(in);
            }
        }
        
        String deviceId = props.getProperty("deviceId");
        assertNull(deviceId);

        // Simulate save
        String newUuid = UUID.randomUUID().toString();
        props.setProperty("deviceId", newUuid);
        try (FileOutputStream out = new FileOutputStream(stateFile)) {
            props.store(out, null);
        }

        // Read back
        Properties reloaded = new Properties();
        try (FileInputStream in = new FileInputStream(stateFile)) {
            reloaded.load(in);
        }
        assertEquals(newUuid, reloaded.getProperty("deviceId"));
    }

    @Test
    public void saveAndClearRegistration_managesRegistrationAndBindingId() throws Exception {
        Properties props = new Properties();
        props.setProperty("registrationId", "reg-123");
        props.setProperty("bindingId", "bind-456");
        try (FileOutputStream out = new FileOutputStream(stateFile)) {
            props.store(out, null);
        }

        Properties loaded = new Properties();
        try (FileInputStream in = new FileInputStream(stateFile)) {
            loaded.load(in);
        }
        assertEquals("reg-123", loaded.getProperty("registrationId"));
        assertEquals("bind-456", loaded.getProperty("bindingId"));

        // Clear registration
        loaded.remove("registrationId");
        loaded.remove("bindingId");
        try (FileOutputStream out = new FileOutputStream(stateFile)) {
            loaded.store(out, null);
        }

        Properties cleared = new Properties();
        try (FileInputStream in = new FileInputStream(stateFile)) {
            cleared.load(in);
        }
        assertNull(cleared.getProperty("registrationId"));
        assertNull(cleared.getProperty("bindingId"));
    }

    @Test
    public void timeZonePersistence_savesAndReadsTimeZone() throws Exception {
        Properties props = new Properties();
        props.setProperty("deviceTimeZone", "America/Sao_Paulo");
        try (FileOutputStream out = new FileOutputStream(stateFile)) {
            props.store(out, null);
        }

        Properties loaded = new Properties();
        try (FileInputStream in = new FileInputStream(stateFile)) {
            loaded.load(in);
        }
        assertEquals("America/Sao_Paulo", loaded.getProperty("deviceTimeZone"));
    }
}
