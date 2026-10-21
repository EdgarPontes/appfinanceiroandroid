package br.com.tecpontes.appfinanceiro.ui.menu;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;

/** Destino visual para seções que ainda não possuem implementação funcional no app Android. */
public class FeatureDestinationActivity extends AppCompatActivity {

    public static final String EXTRA_DESTINATION = "feature_destination";
    public static final String CARDS = "cards";
    public static final String ACCOUNTS = "accounts";
    public static final String CATEGORIES = "categories";
    public static final String REPORTS = "reports";
    public static final String PROFILE = "profile";
    public static final String SETTINGS = "settings";
    public static final String NOTIFICATIONS = "notifications";

    public static Intent createIntent(Context context, String destination) {
        return new Intent(context, FeatureDestinationActivity.class)
                .putExtra(EXTRA_DESTINATION, destination);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feature_destination);
        renderDestination();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        renderDestination();
    }

    private void renderDestination() {
        String destination = getIntent().getStringExtra(EXTRA_DESTINATION);
        String title;
        int selectedNavigationItem;

        if (CARDS.equals(destination)) {
            title = "Cartões";
            selectedNavigationItem = R.id.navigation_cards;
        } else if (ACCOUNTS.equals(destination)) {
            title = "Contas";
            selectedNavigationItem = R.id.navigation_accounts;
        } else {
            title = menuTitle(destination);
            selectedNavigationItem = R.id.navigation_menu;
        }

        ((TextView) findViewById(R.id.featureTitle)).setText(title);
        BottomNavigationHelper.setup(this, selectedNavigationItem);
    }

    private String menuTitle(@Nullable String destination) {
        if (CATEGORIES.equals(destination)) return "Categorias";
        if (REPORTS.equals(destination)) return "Relatórios";
        if (PROFILE.equals(destination)) return "Perfil";
        if (SETTINGS.equals(destination)) return "Configurações";
        if (NOTIFICATIONS.equals(destination)) return "Notificações";
        return "Seção";
    }
}
