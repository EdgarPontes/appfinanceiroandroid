package br.com.tecpontes.appfinanceiro.viewmodel;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.com.tecpontes.appfinanceiro.data.repository.ProfileRepository;
import br.com.tecpontes.appfinanceiro.model.ProfileDto;
import br.com.tecpontes.appfinanceiro.model.ProfileUpdateRequestDto;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/** Dados e ações do perfil de usuário. */
@HiltViewModel
public class ProfileViewModel extends ViewModel {
    private final ProfileRepository repository;
    private final MutableLiveData<ProfileDto> profile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> processing = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public final LiveData<ProfileDto> profileData = profile;
    public final LiveData<Boolean> isLoading = loading;
    public final LiveData<Boolean> isProcessing = processing;
    public final LiveData<String> errorMessage = error;

    @Inject public ProfileViewModel(ProfileRepository repository) { this.repository = repository; }

    public void loadProfile() {
        loading.setValue(true);
        error.setValue(null);
        repository.getProfile(new ProfileRepository.ResultCallback<ProfileDto>() {
            @Override public void onSuccess(@Nullable ProfileDto value) {
                profile.postValue(value);
                loading.postValue(false);
            }
            @Override public void onError(@NonNull String message) {
                error.postValue(message);
                loading.postValue(false);
            }
        });
    }

    public void updateProfile(@NonNull ProfileUpdateRequestDto request,
                              @NonNull ProfileRepository.ActionCallback callback) {
        perform(callback, action -> repository.updateProfile(request, action));
    }

    public void updatePassword(@NonNull String password,
                               @NonNull ProfileRepository.ActionCallback callback) {
        perform(callback, action -> repository.updatePassword(password, action));
    }

    public void uploadAvatar(@NonNull Uri uri, @NonNull ProfileRepository.ActionCallback callback) {
        perform(callback, action -> repository.uploadAvatar(uri, action));
    }

    public void deactivateAccount(@NonNull String password,
                                  @NonNull ProfileRepository.ActionCallback callback) {
        perform(callback, action -> repository.deactivateAccount(password, action));
    }

    private void perform(@NonNull ProfileRepository.ActionCallback callback, Action action) {
        processing.setValue(true);
        error.setValue(null);
        action.run(new ProfileRepository.ActionCallback() {
            @Override public void onSuccess() {
                processing.postValue(false);
                callback.onSuccess();
            }
            @Override public void onError(@NonNull String message) {
                error.postValue(message);
                processing.postValue(false);
                callback.onError(message);
            }
        });
    }

    private interface Action {
        void run(ProfileRepository.ActionCallback callback);
    }
}
