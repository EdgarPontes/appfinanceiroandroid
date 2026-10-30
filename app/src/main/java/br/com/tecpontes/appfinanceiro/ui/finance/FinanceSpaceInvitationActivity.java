package br.com.tecpontes.appfinanceiro.ui.finance;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tecpontes.appfinanceiro.databinding.ActivityFinanceSpaceInvitationBinding;
import br.com.tecpontes.appfinanceiro.data.repository.FinanceSpaceRepository;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationAcceptResponseDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationPreviewDto;
import br.com.tecpontes.appfinanceiro.ui.auth.LoginActivity;
import br.com.tecpontes.appfinanceiro.ui.dashboard.DashboardActivity;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/** Prévia e aceite do convite aberto por e-mail ou mensageiro. */
@AndroidEntryPoint
public class FinanceSpaceInvitationActivity extends AppCompatActivity {
    public static final String EXTRA_INVITATION_TOKEN = "finance_invitation_token";

    @Inject FinanceSpaceRepository repository;
    @Inject TokenManager tokenManager;

    private ActivityFinanceSpaceInvitationBinding binding;
    private String invitationToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityFinanceSpaceInvitationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        SessionExpiredNavigation.observe(this, tokenManager);

        invitationToken = getIntent().getStringExtra(EXTRA_INVITATION_TOKEN);
        if (invitationToken == null) invitationToken = tokenFromIntent(getIntent());
        if (invitationToken == null || invitationToken.trim().isEmpty()) {
            showError("O link do convite está incompleto.");
            return;
        }

        binding.acceptInvitationButton.setOnClickListener(v -> acceptInvitation());
        binding.declineInvitationButton.setOnClickListener(v -> declineInvitation());
        binding.verifyInvitationEmailButton.setOnClickListener(v -> verifyInvitationEmail());
        if (!tokenManager.isLoggedIn()) {
            Intent login = new Intent(this, LoginActivity.class);
            login.putExtra(EXTRA_INVITATION_TOKEN, invitationToken);
            startActivity(login);
            finish();
            return;
        }
        loadPreview();
    }

    @Nullable
    private String tokenFromIntent(@Nullable Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data == null) return null;
        String token = data.getLastPathSegment();
        return token == null || token.trim().isEmpty() ? null : token;
    }

    private void loadPreview() {
        showLoading(true);
        repository.previewInvitation(invitationToken,
                new FinanceSpaceRepository.ResultCallback<FinanceSpaceInvitationPreviewDto>() {
                    @Override public void onSuccess(@Nullable FinanceSpaceInvitationPreviewDto preview) {
                        showLoading(false);
                        if (preview == null) {
                            showError("Não foi possível carregar este convite.");
                            return;
                        }
                        binding.invitationTitle.setText("Convite para " + preview.spaceName);
                        String inviter = preview.invitedByName == null
                                ? "Uma pessoa" : preview.invitedByName;
                        binding.invitationDetails.setText(inviter
                                + " convidou você para acessar as contas, cartões e lançamentos deste espaço.\n\n"
                                + "O convite foi enviado para " + preview.email + ". Ao aceitar, seus dados pessoais não serão mesclados automaticamente."
                                + (preview.requiresEmailVerification
                                    ? "\n\nDigite o código de 6 dígitos enviado para seu e-mail para confirmar que você controla essa conta."
                                    : ""));
                        binding.invitationEmailCodeInput.setVisibility(
                                preview.requiresEmailVerification ? View.VISIBLE : View.GONE);
                        binding.verifyInvitationEmailButton.setVisibility(
                                preview.requiresEmailVerification ? View.VISIBLE : View.GONE);
                        binding.acceptInvitationButton.setVisibility(
                                preview.requiresEmailVerification ? View.GONE : View.VISIBLE);
                        binding.declineInvitationButton.setVisibility(View.VISIBLE);
                    }

                    @Override public void onError(@NonNull String message) {
                        showLoading(false);
                        showError(message);
                    }
                });
    }

    private void verifyInvitationEmail() {
        String code = binding.invitationEmailCodeInput.getText() == null
                ? "" : binding.invitationEmailCodeInput.getText().toString().trim();
        if (!code.matches("\\d{6}")) {
            showError("Informe o código de 6 dígitos recebido por e-mail.");
            return;
        }
        showLoading(true);
        binding.verifyInvitationEmailButton.setEnabled(false);
        repository.verifyInvitationEmail(invitationToken, code,
                new FinanceSpaceRepository.ActionCallback() {
                    @Override public void onSuccess() {
                        binding.verifyInvitationEmailButton.setEnabled(true);
                        binding.invitationEmailCodeInput.setVisibility(View.GONE);
                        binding.verifyInvitationEmailButton.setVisibility(View.GONE);
                        binding.invitationError.setVisibility(View.GONE);
                        showLoading(false);
                        loadPreview();
                    }
                    @Override public void onError(@NonNull String message) {
                        binding.verifyInvitationEmailButton.setEnabled(true);
                        showLoading(false);
                        showError(message);
                    }
                });
    }

    private void acceptInvitation() {
        showLoading(true);
        binding.acceptInvitationButton.setEnabled(false);
        repository.acceptInvitation(invitationToken,
                new FinanceSpaceRepository.ResultCallback<FinanceSpaceInvitationAcceptResponseDto>() {
                    @Override public void onSuccess(@Nullable FinanceSpaceInvitationAcceptResponseDto result) {
                        showLoading(false);
                        if (result == null || !result.success) {
                            binding.acceptInvitationButton.setEnabled(true);
                            showError("Não foi possível confirmar o aceite.");
                            return;
                        }
                        Toast.makeText(FinanceSpaceInvitationActivity.this,
                                "Você entrou no espaço financeiro", Toast.LENGTH_LONG).show();
                        Intent dashboard = new Intent(FinanceSpaceInvitationActivity.this, DashboardActivity.class);
                        dashboard.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(dashboard);
                        finish();
                    }

                    @Override public void onError(@NonNull String message) {
                        showLoading(false);
                        binding.acceptInvitationButton.setEnabled(true);
                        showError(message);
                    }
                });
    }

    private void declineInvitation() {
        showLoading(true);
        repository.declineInvitation(invitationToken, new FinanceSpaceRepository.ActionCallback() {
            @Override public void onSuccess() {
                showLoading(false);
                Toast.makeText(FinanceSpaceInvitationActivity.this, "Convite recusado", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onError(@NonNull String message) {
                showLoading(false);
                showError(message);
            }
        });
    }

    private void showLoading(boolean loading) {
        binding.invitationProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(@NonNull String message) {
        binding.invitationError.setText(message);
        binding.invitationError.setVisibility(View.VISIBLE);
    }
}
