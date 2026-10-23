package br.com.tecpontes.appfinanceiro.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.data.repository.ProfileRepository;
import br.com.tecpontes.appfinanceiro.databinding.ActivityProfileBinding;
import br.com.tecpontes.appfinanceiro.di.NetworkModule;
import br.com.tecpontes.appfinanceiro.model.ProfileDto;
import br.com.tecpontes.appfinanceiro.model.ProfileUpdateRequestDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.ui.auth.LoginActivity;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.ProfileViewModel;
import com.bumptech.glide.Glide;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/** Perfil do usuário, foto, senha e desativação permanente da conta. */
@AndroidEntryPoint
public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private ProfileViewModel viewModel;
    private ActivityResultLauncher<String[]> avatarPicker;
    private Uri selectedAvatarUri;
    private boolean formattingPhone;

    @Inject TokenManager tokenManager;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BottomNavigationHelper.setup(this, R.id.navigation_menu);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        setupAvatarPicker();
        setupObservers();
        setupPhoneFormatting();
        setupListeners();
        SessionExpiredNavigation.observe(this, tokenManager);
        viewModel.loadProfile();
    }

    private void setupAvatarPicker() {
        avatarPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            String mimeType = getContentResolver().getType(uri);
            if (!isSupportedImage(mimeType)) {
                Toast.makeText(this, "Escolha uma imagem JPEG, PNG, GIF ou WebP.", Toast.LENGTH_LONG).show();
                return;
            }
            selectedAvatarUri = uri;
            renderAvatar(uri, null);
            binding.uploadAvatarButton.setVisibility(View.VISIBLE);
        });
    }

    private void setupObservers() {
        viewModel.profileData.observe(this, this::renderProfile);
        viewModel.isLoading.observe(this, loading ->
                binding.profileProgressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.isProcessing.observe(this, processing -> {
            boolean enabled = !Boolean.TRUE.equals(processing);
            binding.saveProfileButton.setEnabled(enabled);
            binding.changePasswordButton.setEnabled(enabled);
            binding.uploadAvatarButton.setEnabled(enabled);
            binding.deactivateAccountButton.setEnabled(enabled);
            binding.selectAvatarButton.setEnabled(enabled);
        });
        viewModel.errorMessage.observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                binding.profileErrorTextView.setVisibility(View.GONE);
            } else {
                binding.profileErrorTextView.setText(message);
                binding.profileErrorTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupPhoneFormatting() {
        binding.cellphoneEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable editable) {
                if (formattingPhone) return;
                String digits = digitsOnly(editable.toString());
                if (digits.length() > 11) digits = digits.substring(0, 11);
                String formatted = formatPhone(digits);
                if (!formatted.equals(editable.toString())) {
                    formattingPhone = true;
                    binding.cellphoneEditText.setText(formatted);
                    binding.cellphoneEditText.setSelection(formatted.length());
                    formattingPhone = false;
                }
            }
        });
    }

    private void setupListeners() {
        binding.selectAvatarButton.setOnClickListener(v ->
                avatarPicker.launch(new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"}));
        binding.uploadAvatarButton.setOnClickListener(v -> uploadAvatar());
        binding.saveProfileButton.setOnClickListener(v -> saveProfile());
        binding.changePasswordButton.setOnClickListener(v -> changePassword());
        binding.logoutButton.setOnClickListener(v -> logout());
        binding.deactivateAccountButton.setOnClickListener(v -> showDeactivateDialog());
    }

    private void logout() {
        tokenManager.clearToken();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(this, "Logout realizado com sucesso!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void renderProfile(@Nullable ProfileDto profile) {
        if (profile == null) return;
        binding.profileEmailTextView.setText(profile.email == null ? "" : profile.email);
        binding.firstNameEditText.setText(profile.firstName == null ? "" : profile.firstName);
        binding.lastNameEditText.setText(profile.lastName == null ? "" : profile.lastName);
        binding.cellphoneEditText.setText(formatStoredPhone(profile.cellphone));
        binding.telegramIdEditText.setText(profile.telegramId == null ? "" : profile.telegramId);
        if (selectedAvatarUri == null) {
            if (profile.profileImageUrl == null || profile.profileImageUrl.trim().isEmpty()) {
                renderAvatar(null, profile.initials());
            } else {
                String avatarUrl = profile.profileImageUrl.startsWith("http")
                        ? profile.profileImageUrl : NetworkModule.SERVER_BASE_URL + profile.profileImageUrl;
                renderAvatar(Uri.parse(avatarUrl + (avatarUrl.contains("?") ? "&" : "?") + "v=" + System.currentTimeMillis()), null);
            }
        }
    }

    private void renderAvatar(@Nullable Uri uri, @Nullable String initials) {
        if (uri == null) {
            Glide.with(this).clear(binding.avatarImageView);
            binding.avatarImageView.setImageDrawable(null);
            binding.avatarInitialsTextView.setText(initials == null ? "?" : initials);
            binding.avatarInitialsTextView.setVisibility(View.VISIBLE);
        } else {
            binding.avatarInitialsTextView.setVisibility(View.GONE);
            Glide.with(this).load(uri).circleCrop().placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder).into(binding.avatarImageView);
        }
    }

    private void saveProfile() {
        String firstName = binding.firstNameEditText.getText().toString().trim();
        if (firstName.isEmpty()) {
            binding.firstNameEditText.setError("O nome não pode estar em branco.");
            return;
        }
        ProfileUpdateRequestDto request = new ProfileUpdateRequestDto();
        request.firstName = firstName;
        request.lastName = binding.lastNameEditText.getText().toString().trim();
        request.telegramId = binding.telegramIdEditText.getText().toString().trim();
        String cellphone = normalizeCellphone(binding.cellphoneEditText.getText().toString());
        request.cellphone = cellphone.isEmpty() ? null : cellphone;
        viewModel.updateProfile(request, actionCallback("Seu perfil foi atualizado.", true));
    }

    private void changePassword() {
        String password = binding.newPasswordEditText.getText().toString();
        String confirmation = binding.confirmPasswordEditText.getText().toString();
        if (password.length() < 6) {
            binding.newPasswordEditText.setError("A senha deve ter no mínimo 6 caracteres.");
            return;
        }
        if (!password.equals(confirmation)) {
            binding.confirmPasswordEditText.setError("As senhas não coincidem.");
            return;
        }
        viewModel.updatePassword(password, new ProfileRepository.ActionCallback() {
            @Override public void onSuccess() {
                binding.newPasswordEditText.setText("");
                binding.confirmPasswordEditText.setText("");
                Toast.makeText(ProfileActivity.this, "Sua senha foi alterada.", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(@NonNull String message) { showErrorToast(message); }
        });
    }

    private void uploadAvatar() {
        if (selectedAvatarUri == null) return;
        viewModel.uploadAvatar(selectedAvatarUri, new ProfileRepository.ActionCallback() {
            @Override public void onSuccess() {
                selectedAvatarUri = null;
                binding.uploadAvatarButton.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "Sua foto de perfil foi atualizada.", Toast.LENGTH_SHORT).show();
                viewModel.loadProfile();
            }
            @Override public void onError(@NonNull String message) { showErrorToast(message); }
        });
    }

    private void showDeactivateDialog() {
        EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint("Sua senha atual");
        password.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128)});
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        password.setPadding(padding, padding, padding, padding);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Desativar conta permanentemente")
                .setMessage("Esta ação apagará definitivamente seu perfil, contas, cartões, transações e demais dados. Digite sua senha para confirmar.")
                .setView(password)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Apagar conta", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = password.getText().toString();
            if (currentPassword.isEmpty()) {
                password.setError("A senha é obrigatória.");
                return;
            }
            dialog.dismiss();
            viewModel.deactivateAccount(currentPassword, new ProfileRepository.ActionCallback() {
                @Override public void onSuccess() {
                    tokenManager.clearToken();
                    Toast.makeText(ProfileActivity.this, "Conta desativada.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                @Override public void onError(@NonNull String message) { showErrorToast(message); }
            });
        }));
        dialog.show();
    }

    private ProfileRepository.ActionCallback actionCallback(String successMessage, boolean reloadProfile) {
        return new ProfileRepository.ActionCallback() {
            @Override public void onSuccess() {
                Toast.makeText(ProfileActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                if (reloadProfile) viewModel.loadProfile();
            }
            @Override public void onError(@NonNull String message) { showErrorToast(message); }
        };
    }

    private void showErrorToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private boolean isSupportedImage(@Nullable String mimeType) {
        return "image/jpeg".equalsIgnoreCase(mimeType) || "image/png".equalsIgnoreCase(mimeType)
                || "image/gif".equalsIgnoreCase(mimeType) || "image/webp".equalsIgnoreCase(mimeType);
    }

    private String normalizeCellphone(String value) {
        String digits = digitsOnly(value);
        if (digits.length() == 11) {
            String ddd = digits.substring(0, 2);
            String number = digits.substring(2);
            if (number.length() == 9 && number.startsWith("9")) return "55" + ddd + number.substring(1);
            return "55" + digits;
        }
        return digits;
    }

    private String formatPhone(String digits) {
        if (digits.length() >= 2) {
            String ddd = digits.substring(0, 2);
            String rest = digits.substring(2);
            if (rest.isEmpty()) return "(" + ddd + ") ";
            if (rest.length() <= 4) return "(" + ddd + ") " + rest;
            if (rest.length() <= 8) return "(" + ddd + ") " + rest.substring(0, 4) + "-" + rest.substring(4);
            return "(" + ddd + ") " + rest.substring(0, 5) + "-" + rest.substring(5, Math.min(9, rest.length()));
        }
        return digits;
    }

    private String formatStoredPhone(@Nullable String value) {
        String digits = digitsOnly(value == null ? "" : value);
        if (digits.length() == 13 && digits.startsWith("55")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 12 && digits.startsWith("55")) {
            String ddd = digits.substring(2, 4);
            String localNumber = digits.substring(4);
            if (localNumber.length() == 8) localNumber = "9" + localNumber;
            digits = ddd + localNumber;
        }
        if (digits.length() == 10 || digits.length() == 11) return formatPhone(digits);
        return value == null ? "" : value;
    }

    private String digitsOnly(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }

    @Override protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
