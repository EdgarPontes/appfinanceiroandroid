package br.com.tecpontes.appfinanceiro.ui.finance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Patterns;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.data.repository.FinanceSpaceRepository;
import br.com.tecpontes.appfinanceiro.databinding.ActivityFinanceSpaceBinding;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceDeletionRequestDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceInvitationDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceMemberDto;
import br.com.tecpontes.appfinanceiro.model.FinanceSpaceMigrationRequestDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.ui.help.FeatureTour;
import br.com.tecpontes.appfinanceiro.ui.help.HelpGuide;
import br.com.tecpontes.appfinanceiro.ui.help.HelpPreferences;
import br.com.tecpontes.appfinanceiro.ui.help.HelpStep;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Seleção do espaço financeiro e gestão inicial de convites. */
@AndroidEntryPoint
public class FinanceSpaceActivity extends AppCompatActivity {
    @Inject FinanceSpaceRepository repository;
    @Inject TokenManager tokenManager;
    @Inject HelpPreferences helpPreferences;

    private ActivityFinanceSpaceBinding binding;
    @Nullable private FeatureTour activeHelpTour;
    private final List<FinanceSpaceDto> spaces = new ArrayList<>();
    @Nullable private FinanceSpaceDto selectedSpace;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityFinanceSpaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        BottomNavigationHelper.setup(this, R.id.navigation_menu);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());
        binding.inviteFinanceSpaceButton.setOnClickListener(v -> showInviteDialog());
        binding.leaveFinanceSpaceButton.setOnClickListener(v -> confirmLeaveSpace());
        binding.requestMigrationButton.setOnClickListener(v -> confirmMigrationRequest());
        SessionExpiredNavigation.observe(this, tokenManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) loadSpaces();
    }

    private void loadSpaces() {
        binding.financeSpaceProgress.setVisibility(View.VISIBLE);
        binding.financeSpaceError.setVisibility(View.GONE);
        repository.getSpaces(new FinanceSpaceRepository.ResultCallback<List<FinanceSpaceDto>>() {
            @Override public void onSuccess(@Nullable List<FinanceSpaceDto> result) {
                binding.financeSpaceProgress.setVisibility(View.GONE);
                spaces.clear();
                if (result != null) spaces.addAll(result);
                if (spaces.isEmpty()) {
                    showError("Nenhum espaço financeiro está disponível para esta conta.");
                    renderSpaces();
                    return;
                }

                String selectedId = repository.getSelectedSpaceId();
                selectedSpace = findSpace(selectedId);
                if (selectedSpace == null) {
                    selectedSpace = spaces.get(0);
                    repository.selectSpace(selectedSpace.id);
                }
                renderSpaces();
                loadSpaceDetails();
                scheduleFinanceSpaceGuide();
            }

            @Override public void onError(@NonNull String message) {
                binding.financeSpaceProgress.setVisibility(View.GONE);
                showError(message);
            }
        });
    }

    private void scheduleFinanceSpaceGuide() {
        if (binding == null) return;
        binding.getRoot().postDelayed(() -> {
            if (binding == null || spaces.isEmpty() || isFinishing() || isDestroyed()) return;
            if (activeHelpTour != null && activeHelpTour.isShowing()) return;
            if (!helpPreferences.shouldShow(HelpGuide.FINANCE_SPACE)) return;

            View spaceTarget = binding.financeSpacesContainer.getChildCount() > 0
                    ? binding.financeSpacesContainer.getChildAt(0)
                    : binding.financeSpacesContainer;
            List<HelpStep> steps = new ArrayList<>();
            steps.add(new HelpStep(
                    spaceTarget,
                    getString(R.string.help_finance_space_select_title),
                    getString(R.string.help_finance_space_select_message)));
            if (binding.financeSpaceMembersContainer.getChildCount() > 0) {
                steps.add(new HelpStep(
                        binding.financeSpaceMembersContainer,
                        getString(R.string.help_finance_space_members_title),
                        getString(R.string.help_finance_space_members_message)));
            }
            if (binding.inviteFinanceSpaceButton.getVisibility() == View.VISIBLE) {
                steps.add(new HelpStep(
                        binding.inviteFinanceSpaceButton,
                        getString(R.string.help_finance_space_invite_title),
                        getString(R.string.help_finance_space_invite_message)));
            }
            if (binding.leaveFinanceSpaceButton.getVisibility() == View.VISIBLE) {
                steps.add(new HelpStep(
                        binding.leaveFinanceSpaceButton,
                        getString(R.string.help_finance_space_leave_title),
                        getString(R.string.help_finance_space_leave_message)));
            }
            if (binding.requestMigrationButton.getVisibility() == View.VISIBLE) {
                steps.add(new HelpStep(
                        binding.requestMigrationButton,
                        getString(R.string.help_finance_space_migration_title),
                        getString(R.string.help_finance_space_migration_message)));
            }

            activeHelpTour = FeatureTour.show(this, steps, new FeatureTour.Listener() {
                @Override public void onCompleted() {
                    helpPreferences.markCompleted(HelpGuide.FINANCE_SPACE);
                }

                @Override public void onSkipped() {
                    helpPreferences.markCompleted(HelpGuide.FINANCE_SPACE);
                }

                @Override public void onTargetClicked() {
                    // O guia do espaço financeiro apenas apresenta as ações.
                }
            });
        }, 1000);
    }

    @Nullable
    private FinanceSpaceDto findSpace(@Nullable String id) {
        if (id == null) return null;
        for (FinanceSpaceDto space : spaces) {
            if (id.equals(space.id)) return space;
        }
        return null;
    }

    private void renderSpaces() {
        binding.financeSpacesContainer.removeAllViews();
        for (FinanceSpaceDto space : spaces) {
            boolean isSelected = selectedSpace != null && space.id.equals(selectedSpace.id);
            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(8);
            card.setLayoutParams(cardParams);
            card.setClickable(true);
            card.setFocusable(true);
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(getColor(isSelected
                    ? R.color.cta_primary : R.color.outline));
            card.setCardBackgroundColor(getColor(isSelected
                    ? R.color.success_container : R.color.surface));

            TextView label = new TextView(this);
            label.setPadding(dp(16), dp(14), dp(16), dp(14));
            label.setText(space.name + (space.isOwner() ? "  •  Proprietário" : "  •  Membro")
                    + (isSelected ? "  •  Ativo" : ""));
            label.setTextColor(getColor(isSelected ? R.color.on_success_container : R.color.on_surface));
            label.setTextSize(15);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(label);
            card.setOnClickListener(v -> {
                selectedSpace = space;
                repository.selectSpace(space.id);
                renderSpaces();
                loadSpaceDetails();
                Toast.makeText(this, "Espaço selecionado", Toast.LENGTH_SHORT).show();
            });
            binding.financeSpacesContainer.addView(card);
        }
    }

    private void loadSpaceDetails() {
        FinanceSpaceDto space = selectedSpace;
        if (space == null) return;
        binding.inviteFinanceSpaceButton.setVisibility(space.isOwner() ? View.VISIBLE : View.GONE);
        binding.leaveFinanceSpaceButton.setVisibility(space.isOwner() ? View.GONE : View.VISIBLE);
        binding.financeSpaceMembersContainer.removeAllViews();
        binding.financeSpaceInvitationsContainer.removeAllViews();
        binding.deletionRequestsContainer.removeAllViews();
        binding.migrationRequestsContainer.removeAllViews();
        binding.deletionRequestsCard.setVisibility(View.VISIBLE);
        binding.migrationRequestsCard.setVisibility(View.VISIBLE);
        binding.requestMigrationButton.setVisibility(View.GONE);

        repository.getMembers(space.id, new FinanceSpaceRepository.ResultCallback<List<FinanceSpaceMemberDto>>() {
            @Override public void onSuccess(@Nullable List<FinanceSpaceMemberDto> result) {
                if (result == null || result.isEmpty()) {
                    addMessage(binding.financeSpaceMembersContainer, "Nenhum membro ativo.");
                    return;
                }
                for (FinanceSpaceMemberDto member : result) {
                    addMember(member, space);
                }
            }

            @Override public void onError(@NonNull String message) {
                addMessage(binding.financeSpaceMembersContainer, message);
            }
        });

        if (!space.isOwner()) {
            addMessage(binding.financeSpaceInvitationsContainer, "Somente o proprietário pode gerenciar convites.");
        } else {
            repository.getInvitations(space.id,
                    new FinanceSpaceRepository.ResultCallback<List<FinanceSpaceInvitationDto>>() {
                        @Override public void onSuccess(@Nullable List<FinanceSpaceInvitationDto> result) {
                            boolean anyPending = false;
                            if (result != null) {
                                for (FinanceSpaceInvitationDto invitation : result) {
                                    if (!"PENDING".equalsIgnoreCase(invitation.status)) continue;
                                    anyPending = true;
                                    addInvitation(invitation, space.id);
                                }
                            }
                            if (!anyPending) addMessage(binding.financeSpaceInvitationsContainer,
                                    "Nenhum convite pendente.");
                        }

                        @Override public void onError(@NonNull String message) {
                            addMessage(binding.financeSpaceInvitationsContainer, message);
                        }
                    });
        }
        repository.getDeletionRequests(space.id,
                new FinanceSpaceRepository.ResultCallback<List<FinanceSpaceDeletionRequestDto>>() {
                    @Override public void onSuccess(@Nullable List<FinanceSpaceDeletionRequestDto> result) {
                        boolean hasPending = false;
                        if (result != null) {
                            for (FinanceSpaceDeletionRequestDto request : result) {
                                if (!"PENDING".equalsIgnoreCase(request.status)) continue;
                                hasPending = true;
                                addDeletionRequest(request, space);
                            }
                        }
                        if (!hasPending) addMessage(binding.deletionRequestsContainer,
                                "Nenhum pedido pendente.");
                    }

                    @Override public void onError(@NonNull String message) {
                        addMessage(binding.deletionRequestsContainer, message);
                    }
                });
        loadMigrationRequests(space);
    }

    private void loadMigrationRequests(@NonNull FinanceSpaceDto space) {
        binding.migrationRequestsContainer.removeAllViews();
        binding.requestMigrationButton.setVisibility(View.GONE);
        repository.getMigrationRequests(space.id,
                new FinanceSpaceRepository.ResultCallback<List<FinanceSpaceMigrationRequestDto>>() {
                    @Override public void onSuccess(
                            @Nullable List<FinanceSpaceMigrationRequestDto> result) {
                        boolean hasPending = false;
                        int shown = 0;
                        if (result != null) {
                            for (FinanceSpaceMigrationRequestDto request : result) {
                                if (request.isPending()) hasPending = true;
                                if (shown >= 5) break;
                                addMigrationRequest(request, space);
                                shown++;
                            }
                        }
                        if (shown == 0) {
                            addMessage(binding.migrationRequestsContainer,
                                    space.isOwner()
                                            ? "Nenhuma solicitação de migração."
                                            : "Nenhuma migração pendente.");
                        }
                        binding.requestMigrationButton.setVisibility(
                                !space.isOwner() && !hasPending ? View.VISIBLE : View.GONE);
                    }

                    @Override public void onError(@NonNull String message) {
                        addMessage(binding.migrationRequestsContainer, message);
                    }
                });
    }

    private void addMigrationRequest(
            @NonNull FinanceSpaceMigrationRequestDto request,
            @NonNull FinanceSpaceDto space) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dp(8), 0, dp(8));

        TextView details = new TextView(this);
        details.setText("Status: " + migrationStatusLabel(request.status)
                + "  •  " + migrationSummaryText(request.summary));
        details.setTextColor(getColor(R.color.on_surface));
        details.setTextSize(13);
        item.addView(details);

        if (request.rejectionReason != null && !request.rejectionReason.trim().isEmpty()) {
            TextView reason = new TextView(this);
            reason.setText("Motivo: " + request.rejectionReason);
            reason.setTextColor(getColor(R.color.on_surface_variant));
            reason.setTextSize(12);
            item.addView(reason);
        }

        if (request.isPending()) {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            if (space.isOwner()) {
                MaterialButton reject = new MaterialButton(this);
                reject.setText("Recusar");
                reject.setTextSize(11);
                reject.setOnClickListener(v -> confirmRejectMigration(space, request));
                MaterialButton approve = new MaterialButton(this);
                approve.setText("Aprovar migração");
                approve.setTextSize(11);
                approve.setOnClickListener(v -> confirmApproveMigration(space, request));
                actions.addView(reject,
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                actions.addView(approve,
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            } else {
                TextView waiting = new TextView(this);
                waiting.setText("Aguardando aprovação do proprietário");
                waiting.setTextColor(getColor(R.color.on_surface_variant));
                waiting.setTextSize(12);
                actions.addView(waiting,
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                MaterialButton cancel = new MaterialButton(this);
                cancel.setText("Cancelar");
                cancel.setTextSize(11);
                cancel.setOnClickListener(v -> confirmCancelMigration(space, request));
                actions.addView(cancel);
            }
            item.addView(actions);
        }
        binding.migrationRequestsContainer.addView(item);
    }

    @NonNull
    private String migrationStatusLabel(@Nullable String status) {
        if ("APPROVED".equalsIgnoreCase(status)) return "Migração concluída";
        if ("REJECTED".equalsIgnoreCase(status)) return "Recusada";
        if ("CANCELLED".equalsIgnoreCase(status)) return "Cancelada";
        if ("FAILED".equalsIgnoreCase(status)) return "Falhou";
        return "Pendente";
    }

    @NonNull
    private String migrationSummaryText(@Nullable FinanceSpaceMigrationRequestDto.Summary summary) {
        if (summary == null) return "sem resumo";
        return summary.accounts + " contas, " + summary.cards + " cartões, "
                + summary.categories + " categorias, " + summary.transactions + " lançamentos";
    }

    private void confirmMigrationRequest() {
        FinanceSpaceDto space = selectedSpace;
        if (space == null || space.isOwner()) return;
        new AlertDialog.Builder(this)
                .setTitle("Trazer meus dados?")
                .setMessage("Os dados do seu espaço pessoal serão movidos para este espaço "
                        + "compartilhado após a aprovação do proprietário. Nada será apagado "
                        + "se a solicitação for recusada.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Solicitar", (dialog, which) -> requestMigration(space))
                .show();
    }

    private void requestMigration(@NonNull FinanceSpaceDto space) {
        binding.requestMigrationButton.setEnabled(false);
        repository.createMigrationRequest(space.id,
                new FinanceSpaceRepository.ResultCallback<FinanceSpaceMigrationRequestDto>() {
                    @Override public void onSuccess(
                            @Nullable FinanceSpaceMigrationRequestDto request) {
                        binding.requestMigrationButton.setEnabled(true);
                        Toast.makeText(FinanceSpaceActivity.this,
                                "Solicitação enviada ao proprietário.", Toast.LENGTH_LONG).show();
                        loadMigrationRequests(space);
                    }

                    @Override public void onError(@NonNull String message) {
                        binding.requestMigrationButton.setEnabled(true);
                        showError(message);
                    }
                });
    }

    private void confirmApproveMigration(
            @NonNull FinanceSpaceDto space,
            @NonNull FinanceSpaceMigrationRequestDto request) {
        new AlertDialog.Builder(this)
                .setTitle("Aprovar migração?")
                .setMessage("Os dados pessoais do membro serão movidos para este espaço. "
                        + "A operação será concluída por completo ou não alterará nada.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Aprovar", (dialog, which) ->
                        decideMigration(space, request, true))
                .show();
    }

    private void confirmRejectMigration(
            @NonNull FinanceSpaceDto space,
            @NonNull FinanceSpaceMigrationRequestDto request) {
        new AlertDialog.Builder(this)
                .setTitle("Recusar migração?")
                .setMessage("Os dados continuarão no espaço pessoal do membro e não serão apagados.")
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Recusar", (dialog, which) ->
                        decideMigration(space, request, false))
                .show();
    }

    private void confirmCancelMigration(
            @NonNull FinanceSpaceDto space,
            @NonNull FinanceSpaceMigrationRequestDto request) {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar solicitação?")
                .setMessage("A solicitação deixará de aparecer para o proprietário.")
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Cancelar solicitação", (dialog, which) ->
                        repository.cancelMigrationRequest(space.id, request.id,
                                new FinanceSpaceRepository.ActionCallback() {
                                    @Override public void onSuccess() {
                                        Toast.makeText(FinanceSpaceActivity.this,
                                                "Solicitação cancelada.", Toast.LENGTH_SHORT).show();
                                        loadMigrationRequests(space);
                                    }
                                    @Override public void onError(@NonNull String message) {
                                        showError(message);
                                    }
                                }))
                .show();
    }

    private void decideMigration(
            @NonNull FinanceSpaceDto space,
            @NonNull FinanceSpaceMigrationRequestDto request,
            boolean approve) {
        FinanceSpaceRepository.ActionCallback callback =
                new FinanceSpaceRepository.ActionCallback() {
                    @Override public void onSuccess() {
                        Toast.makeText(FinanceSpaceActivity.this,
                                approve ? "Migração aprovada." : "Migração recusada.",
                                Toast.LENGTH_LONG).show();
                        loadMigrationRequests(space);
                    }

                    @Override public void onError(@NonNull String message) {
                        showError(message);
                    }
                };
        if (approve) {
            repository.approveMigrationRequest(space.id, request.id, callback);
        } else {
            repository.rejectMigrationRequest(space.id, request.id, callback);
        }
    }

    private void showInviteDialog() {
        FinanceSpaceDto space = selectedSpace;
        if (space == null || !space.isOwner()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_invite_finance_space, null);
        TextInputLayout emailLayout = dialogView.findViewById(R.id.inviteEmailLayout);
        TextInputEditText emailInput = dialogView.findViewById(R.id.inviteEmailEditText);
        RadioGroup channelGroup = dialogView.findViewById(R.id.inviteChannelGroup);
        if (emailLayout == null || emailInput == null || channelGroup == null) return;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Convidar pessoa")
                .setMessage("Ao aceitar, a pessoa verá as contas, cartões e lançamentos deste espaço.")
                .setView(dialogView)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar convite", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    emailLayout.setError(null);
                    String email = emailInput.getText() == null
                            ? "" : emailInput.getText().toString().trim();
                    if (email.isEmpty()) {
                        emailLayout.setError("Informe o e-mail do convidado.");
                        return;
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailLayout.setError("Informe um e-mail válido.");
                        return;
                    }
                    String channel = selectedChannel(channelGroup);
                    dialog.dismiss();
                    createInvitation(space, email, channel);
                }));
        dialog.show();
    }

    @NonNull
    private String selectedChannel(@NonNull RadioGroup group) {
        if (group.getCheckedRadioButtonId() == R.id.inviteChannelWhatsapp) {
            return FinanceSpaceInvitationDto.CHANNEL_WHATSAPP;
        }
        if (group.getCheckedRadioButtonId() == R.id.inviteChannelPush) {
            return FinanceSpaceInvitationDto.CHANNEL_PUSH;
        }
        return FinanceSpaceInvitationDto.CHANNEL_EMAIL;
    }

    private void createInvitation(
            @NonNull FinanceSpaceDto space,
            @NonNull String email,
            @NonNull String channel) {
        binding.inviteFinanceSpaceButton.setEnabled(false);
        repository.createInvitation(space.id, email, channel,
                new FinanceSpaceRepository.ResultCallback<FinanceSpaceInvitationDto>() {
                    @Override public void onSuccess(@Nullable FinanceSpaceInvitationDto invitation) {
                        binding.inviteFinanceSpaceButton.setEnabled(true);
                        if (invitation == null || invitation.shareUrl == null) {
                            Toast.makeText(FinanceSpaceActivity.this,
                                    "Convite criado, mas o link não foi retornado.", Toast.LENGTH_LONG).show();
                            loadSpaceDetails();
                            return;
                        }
                        handleInvitationDelivery(invitation, space.name);
                        loadSpaceDetails();
                    }

                    @Override public void onError(@NonNull String message) {
                        binding.inviteFinanceSpaceButton.setEnabled(true);
                        showError(message);
                    }
        });
    }

    private void handleInvitationDelivery(
            @NonNull FinanceSpaceInvitationDto invitation,
            @NonNull String spaceName) {
        String channel = invitation.deliveryChannel == null
                ? FinanceSpaceInvitationDto.CHANNEL_EMAIL : invitation.deliveryChannel;

        if (!invitation.emailSent) {
            Toast.makeText(this,
                    "O e-mail com o código de confirmação não foi enviado. "
                            + "Verifique o serviço de e-mail e reenvie o convite.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (FinanceSpaceInvitationDto.CHANNEL_EMAIL.equals(channel)) {
            Toast.makeText(this, "Convite enviado por e-mail.", Toast.LENGTH_LONG).show();
            return;
        }
        if (FinanceSpaceInvitationDto.CHANNEL_PUSH.equals(channel)) {
            Toast.makeText(this, invitation.pushSent
                            ? "Convite enviado por notificação push."
                            : "Convite criado, mas o push não foi entregue. Reenvie ou escolha outro canal.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        shareInvitation(invitation, spaceName);
    }

    private void shareInvitation(FinanceSpaceInvitationDto invitation, String spaceName) {
        String message = "Você foi convidado para compartilhar o espaço financeiro \""
                + spaceName + "\". Entre no FluxoCerto com " + invitation.email
                + " para aceitar: " + invitation.shareUrl;
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Convite do FluxoCerto");
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(sendIntent, "Compartilhar convite no WhatsApp"));
        Toast.makeText(this, "Convite criado. O código de confirmação foi enviado por e-mail.",
                Toast.LENGTH_LONG).show();
    }

    @NonNull
    private String channelLabel(@Nullable String channel) {
        if (FinanceSpaceInvitationDto.CHANNEL_PUSH.equals(channel)) return "Push";
        if (FinanceSpaceInvitationDto.CHANNEL_WHATSAPP.equals(channel)) return "WhatsApp";
        return "E-mail";
    }

    private void addInvitation(FinanceSpaceInvitationDto invitation, String spaceId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView email = new TextView(this);
        email.setText(invitation.email + "  •  Pendente  •  "
                + channelLabel(invitation.deliveryChannel));
        email.setTextColor(getColor(R.color.on_surface));
        email.setTextSize(14);
        row.addView(email, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton resend = new MaterialButton(this);
        resend.setText("Reenviar");
        resend.setTextSize(12);
        resend.setOnClickListener(v -> repository.resendInvitation(spaceId, invitation.id,
                new FinanceSpaceRepository.ResultCallback<FinanceSpaceInvitationDto>() {
                    @Override public void onSuccess(@Nullable FinanceSpaceInvitationDto updated) {
                        if (updated != null && updated.shareUrl != null) {
                            handleInvitationDelivery(updated,
                                    selectedSpace == null ? "Finanças" : selectedSpace.name);
                        }
                        loadSpaceDetails();
                    }
                    @Override public void onError(@NonNull String message) { showError(message); }
                }));
        row.addView(resend);

        MaterialButton cancel = new MaterialButton(this);
        cancel.setText("Cancelar");
        cancel.setTextSize(12);
        cancel.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Cancelar convite?")
                .setMessage("O link deixará de funcionar.")
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Cancelar convite", (dialog, which) ->
                        repository.revokeInvitation(spaceId, invitation.id,
                                new FinanceSpaceRepository.ActionCallback() {
                                    @Override public void onSuccess() {
                                        Toast.makeText(FinanceSpaceActivity.this,
                                                "Convite cancelado", Toast.LENGTH_SHORT).show();
                                        loadSpaceDetails();
                                    }
                                    @Override public void onError(@NonNull String message) {
                                        showError(message);
                                    }
                                }))
                .show());
        row.addView(cancel);
        binding.financeSpaceInvitationsContainer.addView(row);
    }

    private void addMember(FinanceSpaceMemberDto member, FinanceSpaceDto space) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dp(6), 0, dp(6));

        TextView name = new TextView(this);
        name.setText(member.displayName() + ("OWNER".equalsIgnoreCase(member.role)
                ? "  •  Proprietário" : "  •  Membro"));
        name.setTextColor(getColor(R.color.on_surface));
        name.setTextSize(14);
        item.addView(name);

        if (space.isOwner() && !"OWNER".equalsIgnoreCase(member.role)) {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            MaterialButton transfer = new MaterialButton(this);
            transfer.setText("Transferir propriedade");
            transfer.setTextSize(11);
            transfer.setOnClickListener(v -> confirmTransferOwnership(space, member));
            MaterialButton remove = new MaterialButton(this);
            remove.setText("Remover");
            remove.setTextSize(11);
            remove.setOnClickListener(v -> confirmRemoveMember(space, member));
            actions.addView(transfer,
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            actions.addView(remove,
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            item.addView(actions);
        }
        binding.financeSpaceMembersContainer.addView(item);
    }

    private void confirmTransferOwnership(
            @NonNull FinanceSpaceDto space,
            @NonNull FinanceSpaceMemberDto member) {
        new AlertDialog.Builder(this)
                .setTitle("Transferir propriedade?")
                .setMessage("A propriedade do espaço será transferida para " + member.displayName()
                        + ". Você continuará como membro e perderá as ações de proprietário.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Transferir", (dialog, which) ->
                        transferOwnership(space, member))
                .show();
    }

    private void transferOwnership(
            @NonNull FinanceSpaceDto space,
            @NonNull FinanceSpaceMemberDto member) {
        repository.transferOwnership(space.id, member.userId,
                new FinanceSpaceRepository.ActionCallback() {
                    @Override public void onSuccess() {
                        Toast.makeText(FinanceSpaceActivity.this,
                                "Propriedade transferida para " + member.displayName() + ".",
                                Toast.LENGTH_LONG).show();
                        loadSpaces();
                    }

                    @Override public void onError(@NonNull String message) {
                        showError(message);
                    }
                });
    }

    private void confirmRemoveMember(
            @NonNull FinanceSpaceDto space,
            @NonNull FinanceSpaceMemberDto member) {
        new AlertDialog.Builder(this)
                .setTitle("Remover membro?")
                .setMessage(member.displayName() + " perderá o acesso a este espaço imediatamente. "
                        + "Os lançamentos já feitos serão mantidos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Remover", (dialog, which) -> removeMember(space, member))
                .show();
    }

    private void removeMember(
            @NonNull FinanceSpaceDto space,
            @NonNull FinanceSpaceMemberDto member) {
        repository.removeMember(space.id, member.id, new FinanceSpaceRepository.ActionCallback() {
            @Override public void onSuccess() {
                Toast.makeText(FinanceSpaceActivity.this,
                        "Membro removido do espaço.", Toast.LENGTH_LONG).show();
                loadSpaceDetails();
            }

            @Override public void onError(@NonNull String message) {
                showError(message);
            }
        });
    }

    private void confirmLeaveSpace() {
        FinanceSpaceDto space = selectedSpace;
        if (space == null || space.isOwner()) return;
        new AlertDialog.Builder(this)
                .setTitle("Sair deste espaço?")
                .setMessage("Você perderá o acesso às contas, cartões e lançamentos compartilhados. "
                        + "Os lançamentos já feitos serão mantidos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sair", (dialog, which) -> leaveSpace(space))
                .show();
    }

    private void leaveSpace(@NonNull FinanceSpaceDto space) {
        binding.leaveFinanceSpaceButton.setEnabled(false);
        repository.leaveSpace(space.id, new FinanceSpaceRepository.ActionCallback() {
            @Override public void onSuccess() {
                binding.leaveFinanceSpaceButton.setEnabled(true);
                Toast.makeText(FinanceSpaceActivity.this,
                        "Você saiu do espaço financeiro.", Toast.LENGTH_LONG).show();
                loadSpaces();
            }

            @Override public void onError(@NonNull String message) {
                binding.leaveFinanceSpaceButton.setEnabled(true);
                showError(message);
            }
        });
    }

    private void addDeletionRequest(FinanceSpaceDeletionRequestDto request, FinanceSpaceDto space) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(0, dp(8), 0, dp(8));

        TextView details = new TextView(this);
        String description = request.description == null || request.description.trim().isEmpty()
                ? "Lançamento" : request.description;
        String requester = request.requesterName() == null ? "Membro" : request.requesterName();
        String reason = request.reason == null || request.reason.trim().isEmpty()
                ? "" : "\nMotivo: " + request.reason;
        details.setText(requester + " solicita excluir \"" + description + "\"." + reason);
        details.setTextColor(getColor(R.color.on_surface));
        details.setTextSize(14);
        item.addView(details);

        if (space.isOwner()) {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            MaterialButton reject = new MaterialButton(this);
            reject.setText("Recusar");
            reject.setTextSize(12);
            reject.setOnClickListener(v -> decideDeletionRequest(space, request, false));
            MaterialButton approve = new MaterialButton(this);
            approve.setText("Aprovar exclusão");
            approve.setTextSize(12);
            approve.setOnClickListener(v -> decideDeletionRequest(space, request, true));
            actions.addView(reject, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            actions.addView(approve, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            item.addView(actions);
        } else {
            TextView pending = new TextView(this);
            pending.setText("Aguardando aprovação do proprietário");
            pending.setTextColor(getColor(R.color.on_surface_variant));
            pending.setTextSize(12);
            item.addView(pending);
        }
        binding.deletionRequestsContainer.addView(item);
    }

    private void decideDeletionRequest(
            FinanceSpaceDto space,
            FinanceSpaceDeletionRequestDto request,
            boolean approve) {
        String title = approve ? "Aprovar exclusão?" : "Recusar pedido?";
        String message = approve
                ? "O lançamento será removido para todos os membros do espaço."
                : "O lançamento será mantido no espaço.";
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Voltar", null)
                .setPositiveButton(approve ? "Aprovar" : "Recusar", (dialog, which) ->
                        repository.decideDeletionRequest(space.id, request.id, approve,
                                new FinanceSpaceRepository.ActionCallback() {
                                    @Override public void onSuccess() {
                                        Toast.makeText(FinanceSpaceActivity.this,
                                                approve ? "Exclusão aprovada" : "Pedido recusado",
                                                Toast.LENGTH_SHORT).show();
                                        loadSpaceDetails();
                                    }
                                    @Override public void onError(@NonNull String message) {
                                        showError(message);
                                    }
                                }))
                .show();
    }

    private void addMessage(LinearLayout parent, String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextColor(getColor(R.color.on_surface_variant));
        view.setTextSize(14);
        view.setPadding(0, dp(6), 0, dp(6));
        parent.addView(view);
    }

    private void showError(String message) {
        binding.financeSpaceError.setText(message);
        binding.financeSpaceError.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (activeHelpTour != null) activeHelpTour.dismiss();
        super.onDestroy();
        binding = null;
    }
}
