package br.com.tecpontes.appfinanceiro.notifications;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Non-backed-up installation identity and current server association. */
@Singleton
public class PushRegistrationStore {
    private static final String DEVICE_ID = "deviceId";
    private static final String REGISTRATION_ID = "registrationId";
    private static final String BINDING_ID = "bindingId";
    private static final String DEVICE_TIME_ZONE = "deviceTimeZone";

    private final File stateFile;

    @Inject
    public PushRegistrationStore(@ApplicationContext Context context) {
        stateFile = new File(context.getNoBackupFilesDir(), "fcm-registration.properties");
    }

    @NonNull
    public synchronized String getDeviceId() {
        Properties state = read();
        String current = state.getProperty(DEVICE_ID);
        if (current != null && !current.isEmpty()) return current;
        String generated = UUID.randomUUID().toString();
        state.setProperty(DEVICE_ID, generated);
        write(state);
        return generated;
    }

    @Nullable
    public synchronized String getRegistrationId() {
        return read().getProperty(REGISTRATION_ID);
    }

    @Nullable
    public synchronized String getBindingId() {
        return read().getProperty(BINDING_ID);
    }

    public synchronized void saveRegistration(@NonNull String id, @NonNull String bindingId) {
        Properties state = read();
        state.setProperty(REGISTRATION_ID, id);
        state.setProperty(BINDING_ID, bindingId);
        write(state);
    }

    public synchronized void clearRegistration() {
        Properties state = read();
        state.remove(REGISTRATION_ID);
        state.remove(BINDING_ID);
        write(state);
    }

    @Nullable
    public synchronized String getDeviceTimeZone() {
        return read().getProperty(DEVICE_TIME_ZONE);
    }

    public synchronized void setDeviceTimeZone(@NonNull String timeZone) {
        Properties state = read();
        state.setProperty(DEVICE_TIME_ZONE, timeZone);
        write(state);
    }

    private Properties read() {
        Properties state = new Properties();
        if (!stateFile.exists()) return state;
        try (FileInputStream input = new FileInputStream(stateFile)) {
            state.load(input);
        } catch (Exception ignored) {
            // A new association will be safely created if local state is unreadable.
        }
        return state;
    }

    private void write(Properties state) {
        File temporary = new File(stateFile.getParentFile(), stateFile.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            state.store(output, null);
            if (!temporary.renameTo(stateFile)) {
                try (FileInputStream input = new FileInputStream(temporary);
                     FileOutputStream target = new FileOutputStream(stateFile)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = input.read(buffer)) != -1) target.write(buffer, 0, read);
                }
                //noinspection ResultOfMethodCallIgnored
                temporary.delete();
            }
        } catch (Exception ignored) {
            // Registration is retried on next authenticated startup.
        }
    }
}
