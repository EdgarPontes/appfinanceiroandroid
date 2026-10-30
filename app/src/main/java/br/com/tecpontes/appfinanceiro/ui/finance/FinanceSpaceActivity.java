package br.com.tecpontes.appfinanceiro.ui.finance;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Seleção do espaço financeiro e gestão inicial de convites. */
@AndroidEntryPoint
public class FinanceSpaceActivity extends AppCompatActivity {
    @Inject FinanceSpaceRepository repository;
    @Inject TokenManager tokenManager;

    private ActivityFinanceSpaceBinding binding;
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
            }

            @Override public void onError(@NonNull String message) {
                binding.financeSpaceProgress.setVisibility(View.GONE);
                showError(message);
            }
        });
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
        binding.financeSpaceMembersContainer.removeAllViews();
        binding.financeSpaceInvitationsContainer.removeAllViews();
        binding.deletionRequestsContainer.removeAllViews();
        binding.deletionRequestsCard.setVisibility(View.VISIBLE);

        repository.getMembers(space.id, new FinanceSpaceRepository.ResultCallback<List<FinanceSpaceMemberDto>>() {
            @Override public void onSuccess(@Nullable List<FinanceSpaceMemberDto> result) {
                if (result == null || result.isEmpty()) {
                    addMessage(binding.financeSpaceMembersContainer, "Nenhum membro ativo.");
                    return;
                }
                for (FinanceSpaceMemberDto member : result) {
                    addMessage(binding.financeSpaceMembersContainer,
                            member.displayName() + ("OWNER".equalsIgnoreCase(member.role)
                                    ? "  •  Proprietário" : "  •  Membro"));
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
    }

    private void showInviteDialog() {
        FinanceSpaceDto space = selectedSpace;
        if (space == null || !space.isOwner()) return;

        EditText emailInput = new EditText(this);
        emailInput.setHint("E-mail do convidado");
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        int horizontal = dp(24);
        emailInput.setPadding(horizontal, dp(12), horizontal, dp(12));

        new AlertDialog.Builder(this)
                .setTitle("Convidar pessoa")
                .setMessage("Ao aceitar, a pessoa verá as contas, cartões e lançamentos deste espaço. O convite também poderá ser compartilhado pelo WhatsApp.")
                .setView(emailInput)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar convite", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Informe o e-mail do convidado.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    createInvitation(space, email);
                })
                .show();
    }

    private void createInvitation(FinanceSpaceDto space, String email) {
        binding.inviteFinanceSpaceButton.setEnabled(false);
        repository.createInvitation(space.id, email,
                new FinanceSpaceRepository.ResultCallback<FinanceSpaceInvitationDto>() {
                    @Override public void onSuccess(@Nullable FinanceSpaceInvitationDto invitation) {
                        binding.inviteFinanceSpaceButton.setEnabled(true);
                        if (invitation == null || invitation.shareUrl == null) {
                            Toast.makeText(FinanceSpaceActivity.this,
                                    "Convite criado, mas o link não foi retornado.", Toast.LENGTH_LONG).show();
                            loadSpaceDetails();
                            return;
                        }
                        shareInvitation(invitation, space.name);
                        loadSpaceDetails();
                    }

                    @Override public void onError(@NonNull String message) {
                        binding.inviteFinanceSpaceButton.setEnabled(true);
                        showError(message);
                    }
        });
    }

    private void shareInvitation(FinanceSpaceInvitationDto invitation, String spaceName) {
        if (!invitation.emailSent) {
            Toast.makeText(this,
                    "O e-mail com o código de confirmação não foi enviado. Verifique o serviço de e-mail e reenvie o convite.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String message = "Você foi convidado para compartilhar o espaço financeiro \""
                + spaceName + "\". Entre no App Financeiro com " + invitation.email
                + " para aceitar: " + invitation.shareUrl;
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Convite do App Financeiro");
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(sendIntent, "Compartilhar convite"));
        Toast.makeText(this, "Convite enviado por e-mail. Você também pode encaminhar o link.",
                Toast.LENGTH_LONG).show();
    }

    private void addInvitation(FinanceSpaceInvitationDto invitation, String spaceId) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView email = new TextView(this);
        email.setText(invitation.email + "  •  Pendente");
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
                            shareInvitation(updated, selectedSpace == null ? "Finanças" : selectedSpace.name);
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
}
