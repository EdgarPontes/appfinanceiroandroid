package br.com.tecpontes.appfinanceiro.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.ui.help.FeatureTour;
import br.com.tecpontes.appfinanceiro.ui.help.HelpGuide;
import br.com.tecpontes.appfinanceiro.ui.help.HelpPreferences;
import br.com.tecpontes.appfinanceiro.ui.help.HelpStep;
import br.com.tecpontes.appfinanceiro.ui.categories.CategoriesActivity;
import br.com.tecpontes.appfinanceiro.ui.notifications.NotificationsActivity;
import br.com.tecpontes.appfinanceiro.ui.reports.ReportsActivity;
import br.com.tecpontes.appfinanceiro.ui.settings.SettingsActivity;
import br.com.tecpontes.appfinanceiro.ui.transactions.TransactionsActivity;
import br.com.tecpontes.appfinanceiro.ui.profile.ProfileActivity;
import br.com.tecpontes.appfinanceiro.ui.finance.FinanceSpaceActivity;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Menu para destinos secundários do aplicativo. */
@AndroidEntryPoint
public class MenuActivity extends AppCompatActivity {

    @Inject HelpPreferences helpPreferences;
    @Nullable private FeatureTour activeHelpTour;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu);
        BottomNavigationHelper.setup(this, R.id.navigation_menu);
        BottomNavigationHelper.applyEdgeToEdgeInsets(findViewById(android.R.id.content));

        configureRow(R.id.menuCategories, R.drawable.ic_menu_categories,
                "Categorias", "Organize receitas e despesas", FeatureDestinationActivity.CATEGORIES);
        configureRow(R.id.menuReports, R.drawable.ic_menu_reports,
                "Relatórios", "Acompanhe seus resultados", FeatureDestinationActivity.REPORTS);
        configureRow(R.id.menuImport, R.drawable.ic_menu_import,
                "Importar dados", "Importe um arquivo OFX", null);
        configureRow(R.id.menuProfile, R.drawable.ic_menu_profile,
                "Perfil", "Seus dados pessoais", FeatureDestinationActivity.PROFILE);
        configureRow(R.id.menuSettings, R.drawable.ic_menu_settings,
                "Configurações", "Preferências do aplicativo", FeatureDestinationActivity.SETTINGS);
        configureRow(R.id.menuNotifications, R.drawable.ic_menu_notifications,
                "Notificações", "Avisos e lembretes", FeatureDestinationActivity.NOTIFICATIONS);
        configureRow(R.id.menuFinanceSpace, R.drawable.ic_menu_profile,
                "Espaço financeiro", "Compartilhe suas finanças", "finance_space");
        scheduleMenuGuide();
    }

    private void scheduleMenuGuide() {
        getWindow().getDecorView().postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (activeHelpTour != null && activeHelpTour.isShowing()) return;
            if (!helpPreferences.shouldShow(HelpGuide.MENU)) return;

            List<HelpStep> steps = new ArrayList<>();
            steps.add(new HelpStep(
                    findViewById(R.id.menuReports),
                    getString(R.string.help_menu_reports_title),
                    getString(R.string.help_menu_reports_message)));
            steps.add(new HelpStep(
                    findViewById(R.id.menuImport),
                    getString(R.string.help_menu_import_title),
                    getString(R.string.help_menu_import_message)));
            steps.add(new HelpStep(
                    findViewById(R.id.menuSettings),
                    getString(R.string.help_menu_settings_title),
                    getString(R.string.help_menu_settings_message)));
            steps.add(new HelpStep(
                    findViewById(R.id.menuFinanceSpace),
                    getString(R.string.help_menu_finance_space_title),
                    getString(R.string.help_menu_finance_space_message)));

            activeHelpTour = FeatureTour.show(this, steps, new FeatureTour.Listener() {
                @Override public void onCompleted() {
                    helpPreferences.markCompleted(HelpGuide.MENU);
                }

                @Override public void onSkipped() {
                    helpPreferences.markCompleted(HelpGuide.MENU);
                }

                @Override public void onTargetClicked() {
                    // O guia do menu apenas apresenta os atalhos.
                }
            });
        }, 700);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scheduleMenuGuide();
    }

    @Override
    protected void onDestroy() {
        if (activeHelpTour != null) activeHelpTour.dismiss();
        super.onDestroy();
    }

    private void configureRow(
            int viewId, int icon, String title, String subtitle, @Nullable String destination) {
        View row = findViewById(viewId);
        ((android.widget.ImageView) row.findViewById(R.id.menuOptionIcon)).setImageResource(icon);
        ((android.widget.TextView) row.findViewById(R.id.menuOptionTitle)).setText(title);
        ((android.widget.TextView) row.findViewById(R.id.menuOptionSubtitle)).setText(subtitle);
        row.setOnClickListener(view -> {
            if (destination == null) {
                openImport(view);
                return;
            }
            Intent intent;
            if ("finance_space".equals(destination)) {
                intent = new Intent(this, FinanceSpaceActivity.class);
            } else if (FeatureDestinationActivity.PROFILE.equals(destination)) {
                intent = new Intent(this, ProfileActivity.class);
            } else if (FeatureDestinationActivity.SETTINGS.equals(destination)) {
                intent = new Intent(this, SettingsActivity.class);
            } else if (FeatureDestinationActivity.NOTIFICATIONS.equals(destination)) {
                intent = new Intent(this, NotificationsActivity.class);
            } else if (FeatureDestinationActivity.CATEGORIES.equals(destination)) {
                intent = new Intent(this, CategoriesActivity.class);
            } else if (FeatureDestinationActivity.REPORTS.equals(destination)) {
                intent = new Intent(this, ReportsActivity.class);
            } else {
                intent = FeatureDestinationActivity.createIntent(this, destination);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private void openImport(View ignored) {
        Intent intent = new Intent(this, TransactionsActivity.class);
        intent.putExtra(TransactionsActivity.EXTRA_OPEN_IMPORT, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
