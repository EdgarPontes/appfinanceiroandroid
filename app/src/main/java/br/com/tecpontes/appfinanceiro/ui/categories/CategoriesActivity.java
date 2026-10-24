package br.com.tecpontes.appfinanceiro.ui.categories;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.com.tecpontes.appfinanceiro.R;
import br.com.tecpontes.appfinanceiro.databinding.ActivityCategoriesBinding;
import br.com.tecpontes.appfinanceiro.model.CategoryDto;
import br.com.tecpontes.appfinanceiro.ui.BottomNavigationHelper;
import br.com.tecpontes.appfinanceiro.utils.SessionExpiredNavigation;
import br.com.tecpontes.appfinanceiro.utils.TokenManager;
import br.com.tecpontes.appfinanceiro.viewmodel.CategoriesViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/** Tela de categorias de despesas e receitas, com subcategorias. */
@AndroidEntryPoint
public class CategoriesActivity extends AppCompatActivity {

    private ActivityCategoriesBinding binding;
    private CategoriesViewModel viewModel;
    private CategoryAdapter adapter;
    private String currentType = CategoriesViewModel.TYPE_EXPENSE;

    @Inject TokenManager tokenManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCategoriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        BottomNavigationHelper.setup(this, R.id.navigation_menu);
        BottomNavigationHelper.applyEdgeToEdgeInsets(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CategoriesViewModel.class);

        if (savedInstanceState != null) {
            String savedType = savedInstanceState.getString("currentType");
            if (CategoriesViewModel.TYPE_INCOME.equals(savedType)) {
                currentType = CategoriesViewModel.TYPE_INCOME;
            }
        }

        adapter = new CategoryAdapter(new CategoryAdapter.Listener() {
            @Override public void onEdit(@NonNull CategoryDto category) {
                openCategoryDialog(category);
            }

            @Override public void onDelete(@NonNull CategoryDto category) {
                confirmDelete(category);
            }
        });

        binding.categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.categoriesRecyclerView.setAdapter(adapter);

        setupTabs();
        if (CategoriesViewModel.TYPE_INCOME.equals(currentType)) {
            binding.categoryTypeToggleGroup.check(R.id.incomeTypeButton);
        }
        setupListeners();
        setupObservers();
        SessionExpiredNavigation.observe(this, tokenManager);
        viewModel.load();
    }

    private void setupTabs() {
        binding.categoryTypeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            currentType = checkedId == R.id.incomeTypeButton
                    ? CategoriesViewModel.TYPE_INCOME
                    : CategoriesViewModel.TYPE_EXPENSE;
            renderCurrentTab();
            updateEmptyState();
        });
    }

    private void setupListeners() {
        binding.addCategoryButton.setOnClickListener(v -> openCategoryDialog(null));
        binding.newCategoryEmptyButton.setOnClickListener(v -> openCategoryDialog(null));
        binding.createDefaultsButton.setOnClickListener(v -> viewModel.createDefaults());
    }

    private void setupObservers() {
        viewModel.expenseCategoriesData.observe(this, categories -> {
            if (CategoriesViewModel.TYPE_EXPENSE.equals(currentType)) {
                adapter.setData(categories);
            }
            updateEmptyState();
        });

        viewModel.incomeCategoriesData.observe(this, categories -> {
            if (CategoriesViewModel.TYPE_INCOME.equals(currentType)) {
                adapter.setData(categories);
            }
            updateEmptyState();
        });

        viewModel.isLoading.observe(this, loading -> {
            binding.categoriesProgressBar.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            updateEmptyState();
            updateActionState();
        });

        viewModel.isSaving.observe(this, saving -> updateActionState());

        viewModel.errorMessage.observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                binding.categoriesErrorTextView.setVisibility(View.GONE);
            } else {
                binding.categoriesErrorTextView.setText(message);
                binding.categoriesErrorTextView.setVisibility(View.VISIBLE);
            }
        });

        viewModel.successMessage.observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderCurrentTab() {
        adapter.setData(currentCategories());
    }

    @NonNull
    private List<CategoryDto> currentCategories() {
        List<CategoryDto> categories = CategoriesViewModel.TYPE_INCOME.equals(currentType)
                ? viewModel.incomeCategoriesData.getValue()
                : viewModel.expenseCategoriesData.getValue();
        return categories == null ? new ArrayList<>() : categories;
    }

    private void updateEmptyState() {
        if (binding == null) return;
        boolean loading = Boolean.TRUE.equals(viewModel.isLoading.getValue());
        boolean empty = currentCategories().isEmpty();
        boolean showEmpty = empty && !loading;

        binding.categoriesEmptyLayout.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        binding.categoriesRecyclerView.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        binding.categoriesEmptyDescriptionTextView.setText(
                CategoriesViewModel.TYPE_INCOME.equals(currentType)
                        ? R.string.categories_empty_income
                        : R.string.categories_empty_expense);
    }

    private void updateActionState() {
        if (binding == null) return;
        boolean idle = !Boolean.TRUE.equals(viewModel.isLoading.getValue())
                && !Boolean.TRUE.equals(viewModel.isSaving.getValue());
        binding.addCategoryButton.setEnabled(idle);
        binding.newCategoryEmptyButton.setEnabled(idle);
        binding.createDefaultsButton.setEnabled(idle);
    }

    private void openCategoryDialog(@Nullable CategoryDto editing) {
        List<CategoryDto> allParents = new ArrayList<>();
        List<CategoryDto> expenses = viewModel.expenseCategoriesData.getValue();
        List<CategoryDto> incomes = viewModel.incomeCategoriesData.getValue();
        if (expenses != null) allParents.addAll(expenses);
        if (incomes != null) allParents.addAll(incomes);

        String type = editing != null && editing.getType() != null
                ? editing.getType() : currentType;

        CategoryDialog dialog = new CategoryDialog(this, editing, allParents, type,
                (request, currentDialog) -> {
                    currentDialog.setSaving(true);
                    viewModel.save(editing, request, new CategoriesViewModel.ActionCallback() {
                        @Override public void onSuccess() {
                            currentDialog.dismiss();
                        }

                        @Override public void onError(@NonNull String message) {
                            currentDialog.setSaving(false);
                            currentDialog.showError(message);
                        }
                    });
                });
        dialog.show();
    }

    private void confirmDelete(@NonNull CategoryDto category) {
        String name = category.getName() == null ? "" : category.getName();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.categories_delete_title)
                .setMessage(getString(R.string.categories_delete_message, name))
                .setNegativeButton(R.string.categories_cancel, null)
                .setPositiveButton(R.string.categories_delete_confirm,
                        (dialog, which) -> viewModel.delete(category))
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentType", currentType);
    }
}
