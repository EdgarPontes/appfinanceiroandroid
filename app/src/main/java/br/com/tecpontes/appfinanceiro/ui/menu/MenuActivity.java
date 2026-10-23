package br.com.tecpontes.appfinanceiro.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.ui.notifications.NotificationsActivity;
import br.com.tecpontes.appfinanceiro.ui.settings.SettingsActivity;
import br.com.tecpontes.appfinanceiro.ui.transactions.TransactionsActivity;
import br.com.tecpontes.appfinanceiro.ui.profile.ProfileActivity;

/** Menu para destinos secundários do aplicativo. */
public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        BottomNavigationHelper.setup(this, R.id.navigation_menu);

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
            if (FeatureDestinationActivity.PROFILE.equals(destination)) {
                intent = new Intent(this, ProfileActivity.class);
            } else if (FeatureDestinationActivity.SETTINGS.equals(destination)) {
                intent = new Intent(this, SettingsActivity.class);
            } else if (FeatureDestinationActivity.NOTIFICATIONS.equals(destination)) {
                intent = new Intent(this, NotificationsActivity.class);
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
