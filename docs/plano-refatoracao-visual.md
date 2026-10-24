# Plano de Refatoração Visual — Modern Tech & Sofisticado

Documento vivo para acompanhar a modernização visual do app, com etapas, critérios de aceite e conclusões.

- **Status geral:** Planejamento concluído; implementação não iniciada.
- **Última atualização:** 2026-10-24.
- **Próximo passo:** `F1-01` — criar os tokens de cor do tema claro.
- **Responsável:** equipe de desenvolvimento Android.

## Como atualizar este documento

1. Atualizar o campo **Status** do passo antes de iniciar e ao concluir.
2. Preencher a coluna **Conclusão** com evidências objetivas: arquivos alterados, decisões, validações executadas e pendências.
3. Atualizar a tabela **Visão geral** sempre que uma fase mudar de status.
4. Adicionar uma linha no **Registro de execução** ao final de cada etapa relevante.
5. Nunca marcar um passo como concluído sem executar a validação correspondente.
6. Manter o **Próximo passo** apontando para o primeiro item pendente.

## Legenda de status

| Status | Significado |
| :--- | :--- |
| Pendente | Ainda não iniciado. |
| Em andamento | Iniciado, sem conclusão validada. |
| Concluído | Implementado e validado. |
| Bloqueado | Dependência externa ou decisão pendente. |
| Cancelado | Não será executado nesta refatoração. |

## Visão geral

| Fase | Objetivo | Status | Progresso |
| :--- | :--- | :--- | :--- |
| Fase 0 | Diagnóstico técnico e definição do plano | Concluído | 100% |
| Fase 1 | Fundação visual: tokens, temas, estilos e drawables | Pendente | 0% |
| Fase 2 | Migração global de cores fixas e preparação do dark mode | Pendente | 0% |
| Fase 3 | Redesenho estrutural das telas | Pendente | 0% |
| Fase 4 | Validação final, dark mode e QA | Pendente | 0% |

## Decisões consolidadas

| ID | Decisão | Justificativa |
| :--- | :--- | :--- |
| D-01 | Iniciar pela fundação visual antes de redesenhar telas | Reduz retrabalho e evita inconsistência entre componentes. |
| D-02 | Dark mode automático, seguindo o sistema Android | Decisão do produto; evita configuração manual. |
| D-03 | Manter XML Views como base principal | O app usa XML Views; o tema Compose não altera essas telas. |
| D-04 | Manter aliases temporários de cores antigas | Evita quebrar dezenas de layouts durante a transição. |
| D-05 | Exportações de relatórios não seguem a máscara de privacidade | A máscara é apenas visual; arquivos devem conter valores reais. |
| D-06 | Telas com cores fixas serão migradas na Fase 2 | A fundação sozinha não garante dark mode completo. |

## Fase 0 — Diagnóstico e planejamento

| ID | Passo | Status | Conclusão |
| :--- | :--- | :--- | :--- |
| F0-01 | Mapear estrutura de temas e recursos atuais | Concluído | App usa `values/themes.xml`, sem `values-night`. |
| F0-02 | Identificar estratégia de UI | Concluído | Predominância de XML Views; Compose presente apenas no tema. |
| F0-03 | Levantar bloqueios para dark mode | Concluído | Muitas cores fixas (`@android:color/white`, `@android:color/black`, `glass_*`) nos layouts. |
| F0-04 | Definir fases e critérios de validação | Concluído | Plano documentado neste arquivo. |

**Conclusões da Fase 0**

- O visual atual é baseado em glassmorphism com branco, roxo e azul.
- `Theme.Appfinanceiro.Dark` existe, mas não é aplicado e não há recursos noturnos.
- Componentes compartilhados (`GlassCard`, `GlassButton`, `GlassTextInput`, `bg_outlined_rounded`, `bg_dialog`) concentram parte importante do visual.
- A modernização exige primeiro tokens/temas, depois migração de cores e, por último, redesenho das telas.

## Fase 1 — Fundação visual

Objetivo: criar a base de design system sem redesenhar a estrutura das telas.

| ID | Passo | Arquivos principais | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F1-01 | Criar tokens de cor do tema claro | `res/values/colors.xml` | Tokens de marca, CTA, fundo, superfície, texto, borda e finanças definidos. | Pendente | Aguardando execução. |
| F1-02 | Criar variantes noturnas dos tokens | `res/values-night/colors.xml` | Todos os tokens usados no tema claro possuem equivalente noturno. | Pendente | Aguardando execução. |
| F1-03 | Atualizar tema claro para Material 3 DayNight | `res/values/themes.xml` | `Theme.Appfinanceiro` usa `Theme.Material3.DayNight.NoActionBar` e tokens semânticos. | Pendente | Aguardando execução. |
| F1-04 | Criar tema noturno | `res/values-night/themes.xml` | Barras, ícones e superfícies corretos no modo escuro. | Pendente | Aguardando execução. |
| F1-05 | Criar estilos base modernos | `res/values/styles.xml` | `ModernCard`, `PrimaryCTAButton`, `BrandButton` e `ModernTextInput` definidos. | Pendente | Aguardando execução. |
| F1-06 | Rebasear estilos legados | `res/values/styles.xml` | `GlassCard`, `GlassButton`, `GlassTextInput` e `SettingsCard` usam tokens do tema. | Pendente | Aguardando execução. |
| F1-07 | Atualizar drawables compartilhados | `res/drawable/glass_*.xml`, `bg_outlined_rounded.xml`, `bg_dialog.xml` | Formas usam `?attr/colorSurface` e `?attr/colorOutline`; sem branco fixo. | Pendente | Aguardando execução. |
| F1-08 | Criar drawables modernos | `res/drawable/bg_modern_card.xml`, `res/drawable/bg_cta_button.xml` | Componentes podem usar superfície sólida e CTA esmeralda com ripple. | Pendente | Aguardando execução. |
| F1-09 | Atualizar navegação inferior | `layout/bottom_navigation.xml`, `res/color/navigation_item_tint.xml` | Fundo e tint seguem o tema claro/escuro. | Pendente | Aguardando execução. |
| F1-10 | Sincronizar tema Compose | `ui/theme/Color.kt`, `ui/theme/Theme.kt` | Tokens equivalentes, sem erro de import e sem dynamic color. | Pendente | Aguardando execução. |
| F1-11 | Consolidar documentação de design system | `docs/design-system.md` | Tokens, estilos, regras de uso e migração documentados. | Pendente | Aguardando execução. |
| F1-12 | Validar fundação | Build e revisão de recursos | `./gradlew testDebugUnitTest assembleDebug` e `git diff --check` sem erros. | Pendente | Aguardando execução. |

**Critérios de saída da Fase 1**

- Tokens e temas claro/escuro compilando.
- Componentes base usando atributos do tema.
- Nenhum drawable compartilhado com cor branca fixa.
- App atual funcionando no tema claro.
- Dark mode ainda pode estar parcial nas telas com cores fixas.

## Fase 2 — Migração global de cores e dark mode

Objetivo: remover cores fixas dos layouts para que o dark mode funcione em todas as telas, sem ainda redesenhar a estrutura.

| ID | Passo | Arquivos principais | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F2-01 | Migrar fundos raiz das telas | `res/layout/activity_*.xml` | `@android:color/white` substituído por `?attr/colorBackground`. | Pendente | Aguardando execução. |
| F2-02 | Migrar textos e ícones fixos | Layouts e `res/color/` | `@android:color/black` substituído por `?attr/colorOnSurface` ou `?attr/colorOnSurfaceVariant`. | Pendente | Aguardando execução. |
| F2-03 | Migrar superfícies, cards e toolbars | Layouts e estilos | Cards/toolbars usam `?attr/colorSurface` e `?attr/colorOutline`. | Pendente | Aguardando execução. |
| F2-04 | Migrar diálogos | `res/layout/dialog_*.xml` | Diálogos claros/escuros com superfície, texto e borda corretos. | Pendente | Aguardando execução. |
| F2-05 | Migrar itens de listas e adapters | `res/layout/item_*.xml` e adapters | Itens usam cores do tema; sem branco/preto fixo. | Pendente | Aguardando execução. |
| F2-06 | Migrar gráficos | `MonthlyEvolutionChartView`, `CategoryPieChartView` | Grade, rótulos e fundos legíveis em claro/escuro. | Pendente | Aguardando execução. |
| F2-07 | Revisar estados de carregamento/vazio/erro | Layouts e componentes | Estados usam superfícies e textos do tema. | Pendente | Aguardando execução. |
| F2-08 | Validar dark mode global | App em claro/escuro | Nenhuma tela com fundo claro e texto claro ou superfícies inconsistentes. | Pendente | Aguardando execução. |

**Critérios de saída da Fase 2**

- Nenhuma cor fixa crítica restante nos layouts.
- Dark mode automático funcionando em todas as telas e diálogos.
- Contraste mínimo legível em textos e ícones.
- Build e testes passando.

## Fase 3 — Redesenho estrutural das telas

Objetivo: aplicar a identidade Modern Tech & Sofisticado em cada tela, mantendo funcionalidades existentes.

| ID | Tela | Arquivos principais | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F3-01 | Login | `activity_login.xml`, `LoginActivity.java` | Estrutura moderna, CTA esmeralda, dark mode e estados de erro preservados. | Pendente | Aguardando execução. |
| F3-02 | Dashboard | `activity_dashboard.xml`, `DashboardActivity.java` | Cards de saldo/patrimônio/resumo modernos e legíveis nos dois temas. | Pendente | Aguardando execução. |
| F3-03 | Transações | `activity_transactions.xml`, `TransactionsActivity.java` | Filtros, resumo, lista e FAB alinhados ao novo design. | Pendente | Aguardando execução. |
| F3-04 | Criar/editar lançamento | `dialog_create_transaction.xml`, `CreateTransactionDialog.java` | Formulário moderno, teclado, máscara de valor e privacidade preservados. | Pendente | Aguardando execução. |
| F3-05 | Contas | `activity_accounts.xml`, `AccountsActivity.java` | Cards de conta e cartão modernizados sem perder ações. | Pendente | Aguardando execução. |
| F3-06 | Importação de extrato | `activity_statement_import.xml`, `StatementImportActivity.java` | Linhas, seleção e resumo legíveis em ambos os temas. | Pendente | Aguardando execução. |
| F3-07 | Cartões | `activity_cards.xml`, `CardsActivity.java` | Visão do cartão, fatura e resumo modernizados. | Pendente | Aguardando execução. |
| F3-08 | Relatórios | `activity_reports.xml`, `ReportsActivity.java` | Filtros, cards de resumo e categorias no novo padrão. | Pendente | Aguardando execução. |
| F3-09 | Configurações | `activity_settings.xml`, `SettingsActivity.java` | Seções, campos e metas alinhados ao design system. | Pendente | Aguardando execução. |
| F3-10 | Menu, categorias, perfil e notificações | Layouts e Activities correspondentes | Telas secundárias consistentes com o restante do app. | Pendente | Aguardando execução. |
| F3-11 | Barra inferior e toolbars | `bottom_navigation.xml`, layouts | Navegação e cabeçalhos consistentes em claro/escuro. | Pendente | Aguardando execução. |

**Critérios de saída da Fase 3**

- Todas as telas principais redesenhadas.
- Funcionalidades existentes preservadas.
- Navegação e estados de erro/carregamento consistentes.
- Nenhuma regressão visual evidente em telas pequenas.

## Fase 4 — Validação final e QA

| ID | Passo | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- |
| F4-01 | Executar testes e build | `./gradlew testDebugUnitTest assembleDebug` sem falhas. | Pendente | Aguardando execução. |
| F4-02 | Validar contraste e acessibilidade | Contraste adequado e `contentDescription` nos ícones interativos. | Pendente | Aguardando execução. |
| F4-03 | Validar dark mode automático | Troca do tema do sistema reflete corretamente no app. | Pendente | Aguardando execução. |
| F4-04 | Validar privacidade de valores | Botão de máscara continua funcionando em todas as telas previstas. | Pendente | Aguardando execução. |
| F4-05 | Validar exportações | CSV/PDF continuam com valores reais. | Pendente | Aguardando execução. |
| F4-06 | Atualizar documentação final | Plano e design system refletem o estado entregue. | Pendente | Aguardando execução. |
| F4-07 | Revisar regressões | Fluxos de login, transações, contas, cartões e relatórios funcionando. | Pendente | Aguardando execução. |

## Registro de execução

| Data | ID | Ação realizada | Conclusão / Evidência |
| :--- | :--- | :--- | :--- |
| 2026-10-24 | F0 | Diagnóstico do projeto e elaboração do plano | Estrutura atual mapeada; fases e critérios definidos. Nenhuma alteração de código realizada. |

## Comandos de validação

```bash
./gradlew testDebugUnitTest assembleDebug
git diff --check
```

## Arquivos-chave da refatoração

- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values-night/colors.xml` *(novo)*
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/themes.xml` *(novo)*
- `app/src/main/res/values/styles.xml`
- `app/src/main/res/drawable/glass_card_background.xml`
- `app/src/main/res/drawable/glass_button_background.xml`
- `app/src/main/res/drawable/bg_outlined_rounded.xml`
- `app/src/main/res/drawable/bg_dialog.xml`
- `app/src/main/res/color/navigation_item_tint.xml`
- `app/src/main/res/layout/bottom_navigation.xml`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/ui/theme/Color.kt`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/ui/theme/Theme.kt`
- Layouts `activity_*.xml`, `dialog_*.xml` e `item_*.xml`

## Riscos e pontos de atenção

| Risco | Impacto | Mitigação |
| :--- | :--- | :--- |
| Cores fixas em muitos layouts | Dark mode incompleto | Migração sistemática na Fase 2 com checklist por tela. |
| Alias `glass_white_translucent` usado em 47 locais | Mudança visual ampla | Validar cards, toolbars e loading antes de concluir F1. |
| `EdgeToEdge` altera barras do sistema | Ícones de status bar com contraste errado | Configurar `windowLightStatusBar` e `windowLightNavigationBar` por tema. |
| Tema Compose não afeta XML Views | Expectativa de modernização não atendida | Deixar claro que o Compose é apenas consistência futura. |
| Componentes compartilhados com estilos sobrepostos | Resultado inconsistente | Revisar precedência entre style e atributos diretos. |
| Regressões em gráficos | Baixa legibilidade | Migrar cores de grade/rótulo e validar nos dois temas. |

## Definition of Done da refatoração

- Build e testes passando.
- Tema claro e escuro funcionando automaticamente.
- Nenhuma cor fixa crítica nos componentes compartilhados.
- Todas as telas principais revisadas.
- Estados de carregamento, vazio e erro consistentes.
- Funcionalidades existentes preservadas, incluindo máscara de valores e exportações.
- Documentação atualizada com o estado final.
