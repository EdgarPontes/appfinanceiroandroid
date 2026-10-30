# Plano de Compartilhamento Financeiro Familiar

Documento de estudo e implementação para permitir que duas pessoas controlem as mesmas contas, cartões e lançamentos, com convite e aceite, mantendo uma base preparada para compartilhamento com mais pessoas no futuro.

- **Status geral:** Em andamento — primeira fatia de backend e Android implementada; migration de banco e validação end-to-end pendentes.
- **Última atualização:** 2026-10-30.
- **Escopo desta versão:** dois membros ativos por espaço financeiro.
- **Responsáveis:** equipe Android e equipe do backend.

## Como atualizar este documento

1. Atualizar o status da fase antes de iniciar e ao concluir.
2. Registrar evidências objetivas: migrations, endpoints, telas, testes e validações.
3. Não marcar uma fase como concluída sem executar os testes de isolamento entre espaços.
4. Manter as decisões de produto e eventuais novas pendências no final do documento.
5. Atualizar o histórico de execução a cada entrega relevante.

## Legenda de status

| Status | Significado |
| :--- | :--- |
| Pendente | Ainda não iniciado. |
| Em andamento | Iniciado, sem validação concluída. |
| Concluído | Implementado e validado. |
| Bloqueado | Depende de decisão de produto ou de outro time. |
| Cancelado | Fora do escopo atual. |

## 1. Objetivo

Permitir que dois usuários autenticados, por exemplo marido e esposa, consultem e alterem o mesmo conjunto de dados financeiros.

O fluxo deve incluir:

- convite por e-mail ou link compartilhado pelo WhatsApp, sempre vinculado ao e-mail do convidado;
- aceite do convite pelo outro usuário;
- confirmação do e-mail por código de uso único, inclusive quando o link chega pelo WhatsApp;
- acesso compartilhado a contas, cartões, categorias, lançamentos, faturas, relatórios e importação de extrato;
- identificação de quem criou ou alterou cada lançamento;
- exclusão dos próprios lançamentos pelo autor e pedido de aprovação ao proprietário para excluir lançamentos de outra pessoa;
- migração dos dados pessoais do convidado somente após aprovação do proprietário;
- alertas de faturas e vencimentos para todos os membros ativos;
- remoção de membro, cancelamento de convite e transferência de propriedade;
- isolamento total entre espaços financeiros diferentes.

## 2. Escopo da primeira versão

### Incluído

- Um espaço financeiro pessoal automático para cada usuário.
- Compartilhamento do espaço pessoal existente.
- No máximo dois membros ativos por espaço.
- Papéis `OWNER` e `MEMBER`.
- Convite por e-mail e compartilhamento do link pelo WhatsApp, com vínculo ao e-mail convidado.
- Aceite, recusa, cancelamento e expiração do convite.
- Leitura e escrita dos dados financeiros por ambos os membros.
- Exclusão dos próprios lançamentos; pedido de aprovação ao proprietário para exclusão de lançamentos alheios.
- Solicitação e aprovação da migração dos dados pessoais do convidado.
- Alertas de faturas e vencimentos para todos os membros ativos.
- Transferência de propriedade antes da desativação da conta do proprietário.
- Auditoria básica de autoria.
- Funcionamento offline existente preservado, agora vinculado também ao espaço.

### Fora do escopo desta versão

- Três ou mais membros ativos no mesmo espaço.
- Compartilhamento por conta bancária individual.
- Mesclagem automática de dados sem aprovação do proprietário.
- Migração seletiva de itens individuais; a primeira versão migra o conjunto financeiro do espaço pessoal.
- Sincronização em tempo real entre os aparelhos.
- Permissões personalizadas além de `OWNER` e `MEMBER`.
- Edição colaborativa simultânea com resolução de conflitos por campo.
- Compartilhamento de perfil, senha, dispositivos ou preferências pessoais.

## 3. Situação atual

### 3.1 Autenticação e autorização

- O backend emite um JWT com `userId` após login por senha ou Google.
- O Android armazena o JWT no `TokenManager` e o envia pelo `JwtInterceptor`.
- As rotas financeiras usam `req.user!.id` como único critério de propriedade.
- Contas, cartões, categorias e lançamentos possuem `user_id` no backend.
- Faturas são vinculadas ao cartão; metas, configurações e notificações possuem vínculos próprios por usuário.

### 3.2 Android

- `TokenManager.getUserIdentity()` identifica o usuário localmente.
- O cache de opções e a fila offline são isolados por identidade do usuário.
- A fila `pending_transactions` usa `ownerEmail` para impedir que lançamentos de outro login sejam sincronizados.
- O Room está na versão 2 e possui migration explícita `1 → 2`.
- O menu possui destinos para Categorias, Relatórios, Importação, Perfil, Configurações e Notificações.

### 3.3 Conclusão do diagnóstico

O compartilhamento não pode ser resolvido apenas com um convite no app. O backend precisa de um novo escopo de autorização. Sem isso, o convidado não terá acesso aos dados ou, se o acesso for liberado de forma ampla, qualquer usuário poderá consultar dados de terceiros.

## 4. Decisões consolidadas

| ID | Decisão | Justificativa |
| :--- | :--- | :--- |
| D-01 | Criar o conceito de **espaço financeiro** (`finance_space`) | O espaço representa o conjunto de dados compartilhado. É mais seguro e extensível do que vincular cada registro a dois usuários. |
| D-02 | Usar relação de membros, não colunas `spouse_id` ou `second_user_id` | A primeira versão limita a dois membros por regra de produto, mas o modelo já suporta mais pessoas sem migração estrutural. |
| D-03 | Manter o JWT identificando a pessoa, não o espaço | O usuário pode alternar de espaço sem novo login e a remoção de membro pode ser validada em cada requisição. |
| D-04 | Enviar o espaço ativo no cabeçalho `X-Finance-Space-Id` | Evita alterar todas as rotas e mantém o contrato explícito. O backend sempre valida a associação antes de acessar os dados. |
| D-05 | Manter `user_id` nos registros financeiros como autor/criador | Preserva auditoria e histórico. A autorização e os filtros passam a usar `space_id`. |
| D-06 | Limitar a dois membros ativos na API e na interface | Atende o caso familiar atual sem criar telas e regras prematuras para grupos. O limite fica em um único ponto de configuração do servidor. |
| D-07 | Migrar dados pessoais somente com aprovação do proprietário | O convidado pode trazer o conteúdo do espaço pessoal, mas o proprietário precisa revisar e aprovar. Sem aprovação, os dados permanecem no espaço pessoal. |
| D-08 | Tratar o espaço pessoal como um espaço normal | Reduz casos especiais. Todo usuário novo recebe um espaço pessoal e pode compartilhá-lo ou criar outro. |
| D-09 | Convite com token de uso único, hash no banco e validade, vinculado ao e-mail | Evita reutilização, vazamento de token e entrada de terceiros que recebam um link encaminhado. |
| D-10 | Autorização validada no servidor em todas as operações | O app não é fonte confiável para informar permissões ou IDs de espaço. |
| D-11 | Primeira versão sem tempo real | O comportamento atual de sincronização por atualização continua válido e reduz o escopo. |
| D-12 | Configurações, perfil, dispositivos e logs de notificação permanecem pessoais | São dados individuais, não patrimônio financeiro do casal. |
| D-13 | O convite pode ser entregue por e-mail ou compartilhado pelo WhatsApp | Atende os dois canais escolhidos. O link é o mesmo nos dois casos e só funciona para o e-mail convidado. |
| D-14 | Membro exclui os próprios lançamentos diretamente | O autor pode corrigir o que criou sem burocracia. |
| D-15 | Exclusão de lançamento alheio exige aprovação do proprietário | Evita que um membro apague o histórico do outro sem consentimento. A aprovação gera notificação. |
| D-16 | Contas, cartões, categorias e metas ficam sob gestão do proprietário | Protege a estrutura financeira compartilhada. O membro pode consultar e usar esses itens nos lançamentos. |
| D-17 | Alertas de faturas e vencimentos são enviados a todos os membros ativos | A decisão de produto é que ninguém fique fora dos avisos financeiros do espaço. |
| D-18 | Lembretes personalizados permanecem pessoais | São preferências individuais e não devem ser editados pelo outro membro. |
| D-19 | Desativação do proprietário exige transferência quando há outro membro | Evita que o espaço fique sem administrador e impede perda de dados compartilhados. |
| D-20 | Compartilhar o espaço pessoal existente, não criar um espaço vazio | Preserva o histórico atual do casal. O convite e o aceite devem avisar que os dados existentes ficarão visíveis. |

## 5. Modelo de domínio

```text
Usuário 1 ── participa como OWNER ──┐
                                    ├── Espaço financeiro ── Contas, cartões,
Usuário 2 ── participa como MEMBER ─┘                         categorias, lançamentos,
                                                              faturas, metas
```

### 5.1 Entidades novas

#### `finance_spaces`

| Campo | Tipo | Regra |
| :--- | :--- | :--- |
| `id` | UUID | Chave primária. |
| `name` | VARCHAR(255) | Ex.: “Finanças da família”. |
| `owner_user_id` | UUID | Escopo legado (`user_id`) usado pelos dados financeiros nesta fase. FK restritiva. |
| `created_at` | TIMESTAMP | Data de criação. |
| `updated_at` | TIMESTAMP | Data da última alteração. |

#### `finance_space_members`

| Campo | Tipo | Regra |
| :--- | :--- | :--- |
| `id` | UUID | Chave primária. |
| `space_id` | UUID | Referência ao espaço, com exclusão em cascata. |
| `user_id` | UUID | Referência ao usuário, com exclusão em cascata. |
| `role` | VARCHAR(20) | `OWNER` ou `MEMBER`. |
| `status` | VARCHAR(20) | `ACTIVE`, `LEFT` ou `REMOVED`. |
| `joined_at` | TIMESTAMP | Data de entrada. |
| `created_at` | TIMESTAMP | Data do registro. |

Criar índice único para impedir duas participações do mesmo usuário no mesmo espaço. A contagem de membros ativos deve ser feita dentro de transação com bloqueio do espaço para evitar que dois aceites simultâneos ultrapassem o limite de dois.

#### `finance_space_invitations`

| Campo | Tipo | Regra |
| :--- | :--- | :--- |
| `id` | UUID | Chave primária. |
| `space_id` | UUID | Espaço de destino. |
| `email` | VARCHAR(255) | E-mail normalizado em minúsculas. |
| `token_hash` | VARCHAR(64) | SHA-256 do token; nunca armazenar o token puro. |
| `verification_code_hash` | VARCHAR(64) | Hash SHA-256 do código de seis dígitos enviado ao e-mail. |
| `verification_code_expires_at` | TIMESTAMP | Validade do código, sugerida de 15 minutos. |
| `verification_attempts` | INTEGER | Tentativas de confirmação; bloquear após cinco falhas. |
| `email_verified_at` | TIMESTAMP | Data de confirmação do e-mail convidado. |
| `status` | VARCHAR(20) | `PENDING`, `ACCEPTED`, `DECLINED`, `REVOKED` ou `EXPIRED`. |
| `invited_by_user_id` | UUID | Membro que enviou o convite. |
| `accepted_by_user_id` | UUID | Usuário que aceitou. |
| `expires_at` | TIMESTAMP | Validade, sugerida de sete dias. |
| `created_at` | TIMESTAMP | Data do convite. |
| `responded_at` | TIMESTAMP | Data de aceite, recusa ou revogação. |

Regras:

- Um e-mail não pode ter dois convites pendentes para o mesmo espaço.
- O convite só pode ser aceito pelo usuário cujo e-mail autenticado corresponde ao e-mail convidado.
- O e-mail deve ser confirmado com um código de seis dígitos enviado à caixa postal convidada; login com o mesmo texto de e-mail, sozinho, não prova propriedade da caixa postal.
- O código é armazenado como hash, expira em 15 minutos e aceita no máximo cinco tentativas.
- Convites expirados não podem ser reativados; é necessário um novo convite.
- O token completo aparece somente no link compartilhado por e-mail ou WhatsApp, nunca em logs.
- O link pode ser encaminhado, mas o aceite continua bloqueado para qualquer conta cujo e-mail seja diferente do convite.
- O mesmo link pode ser entregue por e-mail e por WhatsApp sem gerar dois convites.
- O e-mail de entrega deve ser normalizado; aliases de caixa postal são tratados como endereços distintos.

#### `finance_space_deletion_requests`

| Campo | Tipo | Regra |
| :--- | :--- | :--- |
| `id` | UUID | Chave primária. |
| `space_id` | UUID | Referência ao espaço. |
| `transaction_id` | UUID | Lançamento cuja exclusão foi solicitada. |
| `requested_by_user_id` | UUID | Membro que pediu a exclusão. |
| `reviewed_by_user_id` | UUID | Proprietário que aprovou ou recusou. |
| `status` | VARCHAR(20) | `PENDING`, `APPROVED`, `REJECTED` ou `CANCELLED`. |
| `reason` | VARCHAR(500) | Motivo opcional informado pelo solicitante. |
| `created_at` | TIMESTAMP | Data do pedido. |
| `reviewed_at` | TIMESTAMP | Data da decisão. |

Regras:

- Apenas lançamentos de autoria diferente do solicitante podem gerar pedido.
- O autor exclui o próprio lançamento diretamente, sem solicitação.
- Deve existir no máximo um pedido pendente por lançamento.
- A aprovação exclui o lançamento em transação e registra o proprietário como revisor.
- A recusa mantém o lançamento intacto.
- O autor original do lançamento não pode aprovar o próprio pedido, pois não deveria haver pedido para lançamento próprio.
- A solicitação é encerrada como `CANCELLED` se o lançamento for alterado ou excluído antes da decisão.

#### `finance_space_migration_requests`

| Campo | Tipo | Regra |
| :--- | :--- | :--- |
| `id` | UUID | Chave primária. |
| `target_space_id` | UUID | Espaço compartilhado que receberá os dados. |
| `source_space_id` | UUID | Espaço pessoal do membro solicitante. |
| `requested_by_user_id` | UUID | Membro que pediu a migração. |
| `reviewed_by_user_id` | UUID | Proprietário que aprovou ou recusou. |
| `status` | VARCHAR(20) | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` ou `FAILED`. |
| `rejection_reason` | VARCHAR(500) | Motivo opcional da recusa. |
| `created_at` | TIMESTAMP | Data do pedido. |
| `reviewed_at` | TIMESTAMP | Data da decisão. |
| `completed_at` | TIMESTAMP | Data da migração concluída. |

Regras:

- O solicitante precisa ser membro ativo do espaço de destino.
- O espaço de origem deve ser o espaço pessoal do solicitante, não um espaço de terceiro.
- Deve existir no máximo um pedido pendente por combinação de origem e destino.
- O proprietário do destino é o único que pode aprovar ou recusar.
- A recusa não apaga, move nem altera os dados de origem.
- A aprovação move o conjunto financeiro em uma única transação e só marca `APPROVED` após a conclusão.
- Se a transação falhar, o status fica `FAILED` e nenhum dado é parcialmente movido.
- A migração não copia perfil, senha, dispositivos, notificações ou preferências pessoais.
- A primeira versão migra o espaço pessoal inteiro, sem seleção individual de contas.

### 5.2 Alterações nas entidades existentes

#### Adaptador de compatibilidade implementado

Na primeira fatia, as tabelas financeiras existentes **ainda não receberam `space_id`**. O middleware valida a associação ao espaço e resolve `finance_spaces.owner_user_id` como escopo das consultas legadas por `user_id`. Assim, compartilhar o espaço atual permite acessar seus dados sem juntar os dados pessoais do convidado.

O identificador do usuário autenticado é preservado em `financeActorUserId`. Novos lançamentos usam `transactions.created_by_user_id` para autoria, enquanto `transactions.user_id` continua representando o escopo legado. A migração `0023_create_finance_spaces.sql` preenche a autoria dos lançamentos antigos com o proprietário atual.

**Limite dessa etapa:** como os registros financeiros não têm `space_id`, não é seguro transferir a propriedade nem mesclar dados pessoais para um espaço cujo proprietário já tenha dados próprios. Essas operações ficam pendentes até os registros usarem um escopo explícito por espaço (ou uma solução equivalente com isolamento validado).

#### Migração estrutural planejada

Adicionar `space_id` obrigatório, após backfill, em:

- `accounts`;
- `credit_cards`;
- `categories`;
- `transactions`;
- `category_goals`;
- `daily_balance`, se os snapshots forem consultados diretamente pelo espaço.

Faturas continuam vinculadas ao cartão, mas toda consulta deve validar o espaço do cartão. No modelo final, `user_id` será autoria e `space_id` será o escopo.

### 5.3 Auditoria

Na implementação inicial, `transactions.created_by_user_id` identifica quem criou o lançamento; a migration preenche os registros antigos com o proprietário atual. Nas demais entidades financeiras, `user_id` ainda representa o escopo legado, não necessariamente o autor. Com `space_id` explícito, recomenda-se:

- `created_by_user_id` e `updated_by_user_id` explícitos em lançamentos;
- histórico de alterações para edição e exclusão;
- exibição de “Criado por” e “Editado por” na tela de lançamento.

## 6. Papéis e permissões

| Ação | `OWNER` | `MEMBER` |
| :--- | :---: | :---: |
| Consultar dados do espaço | Sim | Sim |
| Criar e editar lançamentos | Sim | Sim |
| Excluir os próprios lançamentos | Sim | Sim |
| Excluir lançamento de outra pessoa | Sim, com exclusão direta | Não; precisa de aprovação do proprietário |
| Aprovar ou recusar exclusão de lançamento alheio | Sim | Não |
| Gerenciar contas, cartões, categorias e metas | Sim | Não |
| Importar extrato e conciliar | Sim | Sim |
| Solicitar migração dos dados pessoais para o espaço | Não se aplica ao proprietário | Sim |
| Aprovar ou recusar migração de dados | Sim | Não |
| Convidar pessoa | Sim | Não |
| Cancelar convite | Sim | Não |
| Remover membro | Sim | Não |
| Sair do espaço | Não, se for o único proprietário | Sim |
| Transferir propriedade | Sim | Não |
| Renomear o espaço | Sim | Não |
| Encerrar o espaço | Sim | Não |

Regras complementares:

- O membro pode selecionar contas, cartões e categorias existentes ao criar lançamentos, mas não pode criá-los, editá-los ou excluí-los.
- A exclusão de lançamento alheio sempre passa pela aprovação do proprietário. O proprietário pode excluir qualquer lançamento diretamente.
- O pedido de exclusão deve ser visível no app do proprietário e gerar notificação. A interface do membro deve mostrar “Aguardando aprovação”.
- Enquanto o pedido estiver pendente, o lançamento permanece visível e editável conforme as regras normais.
- A aprovação e a recusa precisam ser registradas com autor e data.

## 7. Fluxo de convite e aceite

### 7.1 Envio

1. O proprietário abre “Espaço financeiro” no menu.
2. Seleciona “Convidar pessoa”.
3. Informa o e-mail do convidado. O e-mail é obrigatório mesmo quando o envio será feito pelo WhatsApp.
4. O app mostra o aviso: “Ao aceitar, a pessoa verá todas as contas, cartões, categorias e lançamentos deste espaço.”
5. O backend valida que o solicitante é `OWNER`, que há vaga e que o e-mail não é de um membro ativo.
6. O backend cria o convite, gera um token aleatório, monta o link e o envia por e-mail.
7. O e-mail contém o link e o código de verificação de seis dígitos.
8. O app recebe também o `shareUrl` e oferece **“Compartilhar no WhatsApp”**, usando o compartilhamento nativo do Android. O mesmo link pode ser enviado por qualquer aplicativo de mensagem, mas o código continua sendo enviado somente ao e-mail convidado.
9. O app exibe o convite como “Pendente”, com opção de reenviar (rotaciona link e código), compartilhar novamente ou cancelar.

O envio por WhatsApp não cria um convite sem e-mail e não gera um token diferente. É apenas outro canal para entregar o mesmo link ao endereço convidado.
O e-mail é obrigatório também para receber o código de confirmação; se o envio falhar, o Android não deve compartilhar o link e deve orientar o proprietário a configurar/reparar o serviço de e-mail e reenviar.

### 7.2 Aceite

1. A pessoa abre o link recebido por e-mail ou WhatsApp.
2. O app abre a tela de aceite, mostrando o nome do espaço, quem convidou e o aviso de visibilidade dos dados.
3. Se não estiver autenticada, entra ou cria conta com o mesmo e-mail do convite.
4. Se o e-mail ainda não foi confirmado, o convidado informa o código de seis dígitos enviado à caixa postal.
5. O backend valida token, código, status, validade e correspondência do e-mail.
6. O usuário confirma o aceite.
7. O backend cria a participação, marca o convite como aceito e retorna o espaço.
8. O Android atualiza a lista de espaços e seleciona o espaço compartilhado.

Se o link for encaminhado para outra pessoa, ela pode visualizar a tela de convite, mas o aceite será recusado porque o e-mail autenticado não corresponde ao e-mail convidado. A mensagem deve orientar a pedir um convite próprio ao proprietário.

### 7.3 Estados do convite

```text
PENDING ── aceite ──> ACCEPTED
   │
   ├── recusa ─────> DECLINED
   ├── cancelamento > REVOKED
   └── prazo ──────> EXPIRED
```

### 7.4 Situações de erro

| Situação | Resposta esperada |
| :--- | :--- |
| E-mail diferente do convite | `403` com mensagem para entrar com o e-mail convidado. |
| Token inválido | `404` genérico, sem revelar dados do espaço. |
| Convite expirado | `410` e opção de solicitar novo convite. |
| Convite já aceito | `409`. |
| Limite de dois membros atingido | `409` com mensagem sobre o limite da versão. |
| Usuário já é membro | `409`. |
| Solicitante não é proprietário | `403`. |
| Conta do convidado desativada | `403`. |
| Link encaminhado para terceiro | `403`, com orientação para solicitar um convite próprio. |
| Código de e-mail inválido | `400`; após cinco tentativas o código é bloqueado. |
| Código de e-mail expirado | `410`; o proprietário precisa reenviar o convite. |

### 7.5 Exclusão de lançamentos

1. O app verifica o autor do lançamento.
2. Se o autor for o próprio usuário, a exclusão segue o fluxo atual e é imediata.
3. Se o autor for outro membro e o usuário for `MEMBER`, o app oferece “Solicitar exclusão”.
4. O membro informa um motivo opcional e confirma o pedido.
5. O backend cria um `finance_space_deletion_request` com status `PENDING`.
6. O proprietário recebe uma notificação e vê o pedido na tela do espaço financeiro.
7. O proprietário aprova ou recusa, com registro de autor e data.
8. Se aprovado, o lançamento é excluído e o saldo é recalculado dentro da mesma transação.
9. O solicitante recebe o resultado; enquanto pendente, o item aparece como “Aguardando aprovação”.

O proprietário pode excluir qualquer lançamento diretamente. O membro não pode excluir contas, cartões, categorias ou metas.

### 7.6 Migração dos dados pessoais do convidado

1. Depois de aceitar o convite, o membro vê a opção “Trazer meus dados”.
2. O app mostra uma prévia com quantidade de contas, cartões, categorias, lançamentos e metas que serão transferidos.
3. O membro confirma a solicitação.
4. O backend cria uma `finance_space_migration_request` com status `PENDING` e envia notificação ao proprietário.
5. O proprietário revisa a prévia e aprova ou recusa.
6. **Recusada:** os dados permanecem integralmente no espaço pessoal. O membro é avisado de que eles não aparecerão no espaço compartilhado e pode continuar usando o espaço compartilhado sem eles.
7. **Aprovada:** o backend move o conjunto financeiro em uma transação. Se algo falhar, nada é movido.
8. Após a conclusão, o membro vê o espaço compartilhado com os dados migrados. O espaço pessoal continua existente, agora sem os dados financeiros que foram movidos, e pode ser ocultado da seleção quando estiver vazio.

A recusa **não apaga dados**. Apagar o espaço pessoal ou o conteúdo financeiro é uma ação separada, com aviso explícito e confirmação do usuário. O sistema não deve apagar dados como consequência de uma recusa.

## 8. Contrato de API proposto

Todas as rotas exigem JWT, exceto quando indicado. A resposta de erro deve seguir o padrão atual do backend, com `message` e código HTTP adequado.

### 8.1 Espaços e membros

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/finance-spaces` | Lista espaços em que o usuário é membro ativo. |
| `POST` | `/api/finance-spaces` | Cria um espaço e torna o usuário `OWNER`. |
| `GET` | `/api/finance-spaces/{spaceId}` | Detalhe do espaço, incluindo papel do usuário. |
| `PATCH` | `/api/finance-spaces/{spaceId}` | Renomeia o espaço; somente `OWNER`. |
| `GET` | `/api/finance-spaces/{spaceId}/members` | Lista membros ativos. |
| `DELETE` | `/api/finance-spaces/{spaceId}/members/{memberId}` | Remove membro; somente `OWNER`. |
| `POST` | `/api/finance-spaces/{spaceId}/transfer-ownership` | Transfere a propriedade para outro membro ativo; somente `OWNER`. |
| `GET` | `/api/finance-spaces/{spaceId}/invitations` | Lista convites pendentes; somente `OWNER`. |
| `POST` | `/api/finance-spaces/{spaceId}/invitations` | Cria convite; somente `OWNER`. |
| `DELETE` | `/api/finance-spaces/{spaceId}/invitations/{invitationId}` | Cancela convite; somente `OWNER`. |

Exemplo de criação de convite:

```http
POST /api/finance-spaces/8f0c.../invitations
Authorization: Bearer <jwt>
Content-Type: application/json

{ "email": "esposa@exemplo.com" }
```

Resposta:

```json
{
  "id": "5c1d...",
  "email": "esposa@exemplo.com",
  "status": "PENDING",
  "expiresAt": "2026-11-06T12:00:00.000Z",
  "emailSent": true,
  "shareUrl": "https://app.exemplo.com/invite/eyJhbGciOi..."
}
```

O Android usa `shareUrl` em um `Intent.ACTION_SEND` para abrir o seletor do sistema, onde o WhatsApp aparece como opção. O backend não precisa integrar diretamente com a API do WhatsApp nesta primeira versão.

### 8.2 Aceite

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/finance-spaces/invitations/preview/{token}` | Prévia autenticada do convite. |
| `POST` | `/api/finance-spaces/invitations/verify-email` | Confirma o e-mail com o token do convite e o código de seis dígitos. |
| `POST` | `/api/finance-spaces/invitations/accept` | Aceita o convite depois da verificação do e-mail. |
| `POST` | `/api/finance-spaces/invitations/decline` | Recusa o convite. |

O token não deve ser enviado ao backend em query string depois da prévia, para evitar registros em logs de acesso. No Android, o link pode abrir uma tela que mantém o token em memória e o envia no corpo ou no caminho da requisição de aceite.

### 8.3 Solicitações de exclusão

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/finance-spaces/{spaceId}/deletion-requests` | Lista pedidos; `OWNER` vê todos e `MEMBER` vê os próprios. |
| `POST` | `/api/finance-spaces/{spaceId}/deletion-requests` | Cria pedido para lançamento de outra pessoa; somente `MEMBER`. |
| `POST` | `/api/finance-spaces/{spaceId}/deletion-requests/{requestId}/approve` | Aprova e exclui o lançamento; somente `OWNER`. |
| `POST` | `/api/finance-spaces/{spaceId}/deletion-requests/{requestId}/reject` | Recusa o pedido; somente `OWNER`. |
| `DELETE` | `/api/finance-spaces/{spaceId}/deletion-requests/{requestId}` | Cancela um pedido próprio ainda pendente. |

Corpo da solicitação:

```json
{
  "transactionId": "b2f1...",
  "reason": "Lançamento duplicado"
}
```

Regras:

- Lançamento do próprio usuário não gera pedido; usa o endpoint normal de exclusão.
- O backend deve verificar o autor real do lançamento, não apenas o que o app informa.
- A aprovação deve revalidar se o lançamento continua no mesmo espaço e com o mesmo autor.
- A exclusão e o recálculo de saldo devem ocorrer na mesma transação.
- Pedidos pendentes de lançamento já excluído devem ser encerrados automaticamente.

### 8.4 Solicitações de migração

| Método | Rota | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/finance-spaces/{spaceId}/migration-requests` | Lista pedidos; `OWNER` vê todos e `MEMBER` vê os próprios. |
| `POST` | `/api/finance-spaces/{spaceId}/migration-requests` | Solicita a migração do espaço pessoal do membro para o espaço compartilhado. |
| `POST` | `/api/finance-spaces/{spaceId}/migration-requests/{requestId}/approve` | Aprova e executa a migração; somente `OWNER`. |
| `POST` | `/api/finance-spaces/{spaceId}/migration-requests/{requestId}/reject` | Recusa a migração; somente `OWNER`. |
| `DELETE` | `/api/finance-spaces/{spaceId}/migration-requests/{requestId}` | Cancela um pedido próprio ainda pendente. |

Corpo da solicitação:

```json
{
  "sourceSpaceId": "a91c..."
}
```

Prévia retornada ao proprietário:

```json
{
  "id": "91f0...",
  "status": "PENDING",
  "requestedBy": { "id": "u-2", "name": "Maria" },
  "summary": {
    "accounts": 3,
    "cards": 1,
    "categories": 18,
    "transactions": 412,
    "categoryGoals": 6
  }
}
```

A aprovação executa:

1. Bloqueia o espaço de destino e o espaço de origem.
2. Revalida a participação ativa e a propriedade do espaço de origem.
3. Move contas, cartões, categorias, lançamentos e metas em transação.
4. Preserva os IDs e os vínculos entre os registros.
5. Recalcula saldos e faturas do destino.
6. Marca o pedido como `APPROVED` ou `FAILED`.

A recusa não altera o espaço de origem. Se o proprietário recusar, o membro pode continuar no espaço compartilhado com os dados pessoais separados.

### 8.5 Desativação com transferência de propriedade

O fluxo atual usa `DELETE /api/users/deactivate` com a senha. A proposta é aceitar um campo opcional:

```json
{
  "password": "senha-atual",
  "transferOwnershipToUserId": "u-2"
}
```

Regras:

- Se o usuário for `OWNER` de um espaço com outro membro ativo, a transferência é obrigatória para concluir a desativação.
- O backend transfere a propriedade e remove a participação do usuário na mesma transação.
- Se o usuário for `MEMBER`, a desativação encerra sua participação. Os dados do espaço compartilhado permanecem; os dados pessoais do próprio usuário seguem o fluxo atual de exclusão.
- Se a transferência não for informada ou o destinatário não for elegível em um espaço com outro membro ativo, a API retorna `409` com a lista de espaços que exigem decisão.
- A exclusão do usuário não pode apagar dados compartilhados por cascata.

### 8.6 Escopo das rotas financeiras

As rotas existentes continuam iguais, mas passam a exigir:

```http
X-Finance-Space-Id: 8f0c...
```

O backend deve:

1. Extrair o usuário do JWT.
2. Validar que existe uma participação `ACTIVE` entre o usuário e o espaço.
3. Rejeitar com `403` quando não houver participação.
4. Usar `space_id` em todas as consultas e validações de propriedade.
5. Nunca confiar em `userId`, `spaceId` ou IDs de recursos enviados no corpo sem validação.

Durante a transição, se o cabeçalho não for enviado por um app antigo, o backend pode resolver o espaço pessoal do usuário. Essa compatibilidade deve ser removida depois que as versões antigas deixarem de ser suportadas.

## 9. Implementação no backend

O backend fica no repositório separado do App Financeiro e usa Express, Drizzle e PostgreSQL.

### 9.1 Fases

| Fase | Objetivo | Status |
| :--- | :--- | :--- |
| B0 | Definir regras e contrato | Concluído |
| B1 | Criar schema, migration e provisionar espaços pessoais | Em andamento — código criado, migration ainda não aplicada |
| B2 | Implementar espaços, membros, convites e pedidos de exclusão | Em andamento — endpoints implementados; falta teste com banco migrado |
| B3 | Aplicar autorização por espaço às rotas financeiras | Em andamento — middleware e papéis aplicados; revisão integral ainda necessária |
| B4 | Ajustar tarefas agendadas, alertas e notificações de pedidos | Em andamento — scheduler envia alertas de vencimento a membros ativos; validação end-to-end pendente |
| B5 | Implementar transferência de propriedade e desativação segura | Pendente — hoje a desativação é bloqueada se um proprietário ainda tiver membro ativo |
| B6 | Implementar aprovação da migração dos dados pessoais | Pendente |
| B7 | Testes de isolamento, concorrência e migração | Pendente — testes unitários de token/link passam; integração com PostgreSQL pendente |

### 9.1.1 Primeira fatia implementada

- Schema Drizzle e migration `0023_create_finance_spaces.sql` para espaços, membros, convites, pedidos de exclusão e autoria de lançamentos.
- Provisionamento de espaço pessoal no cadastro e provisionamento sob demanda para usuários antigos.
- Middleware `X-Finance-Space-Id` com fallback para o espaço pessoal, limite de dois membros e validação de participação ativa.
- Endpoints de espaços/membros, criação, listagem, reenvio e cancelamento de convites, prévia, aceite e recusa.
- Convites enviados por e-mail e link compartilhável via seletor Android/WhatsApp, vinculados ao e-mail convidado. O aceite também exige o código enviado à caixa postal.
- OTP de e-mail com seis dígitos, armazenamento HMAC, validade de 15 minutos e máximo de cinco tentativas.
- Contas, cartões, categorias e metas reservados ao proprietário; membro cria/edita lançamentos.
- Exclusão de lançamento alheio gera pedido; proprietário aprova/recusa. Exclusão em lote por membro é bloqueada.
- Alertas de faturas/vencimentos considerados para cada membro ativo, com preferências pessoais de notificação.
- Android com tela de espaço, lista de membros/convites, compartilhamento de link, aceite, espaço ativo por usuário e header nas chamadas.
- Room versão 3, com `financeSpaceId` nas pendências offline e isolamento dos caches de opções.

**Ainda não está pronto para publicação:** não aplicar a migration em produção antes dos testes de integração; transferência de propriedade e migração dos dados pessoais ainda exigem o escopo explícito por espaço descrito na seção 5.2. A importação CSV fica temporariamente restrita ao proprietário porque pode criar contas/categorias; importação OFX e conciliação continuam disponíveis aos membros usando opções existentes.

### 9.2 Arquivos e áreas afetadas

| Área | Mudança esperada |
| :--- | :--- |
| `shared/schema.ts` | Tabelas de espaço, membros, convites e pedidos de exclusão; `created_by_user_id` em lançamentos. |
| `migrations/0023_create_finance_spaces.sql` | Tabelas, índices, provisionamento legado e backfill de autoria. Não adiciona ainda `space_id` às tabelas financeiras. |
| `server/api/auth.ts` | Criar espaço pessoal no cadastro por senha e no primeiro acesso Google. |
| `server/api/users.ts` | Tratar desativação com transferência obrigatória de propriedade. |
| `server/mailer.ts` | Reutilizar o envio de e-mail para convites e avisos de decisão. |
| `server/middleware/auth.ts` | Preservar usuário autenticado e autor da operação. |
| `server/finance-spaces/scope.ts` | Validar `X-Finance-Space-Id`, participação e papel; mapear o espaço ao escopo legado `owner_user_id`. |
| `server/api/finance-spaces.ts` | Espaços, membros, convites, prévia, aceite/recusa e pedidos de exclusão. Migração e transferência seguem pendentes. |
| `server/api/accounts.ts`, `cards.ts`, `categories.ts`, `transactions.ts` | Trocar filtros por `userId` por filtros por `spaceId`, mantendo autoria e regras de exclusão. |
| `server/api/dashboard.ts`, `balance-history.ts`, `reconciliation.ts`, `import.ts`, `maintenance.ts` | Consultar e recalcular apenas o espaço autorizado. |
| `server/api/invoices.ts` | Validar o espaço do cartão antes de fechar, pagar ou reabrir. |
| `server/api/category-goals.ts` | Vincular metas ao espaço e validar a categoria. |
| `server/storage.ts` | Refatorar consultas que hoje recebem `userId` como escopo financeiro. |
| `server/scheduler.ts`, `server/finance-spaces/notifications.ts` | Alertas de vencimento para membros ativos; solicitações de exclusão notificam proprietário e solicitante. Lembretes personalizados continuam por usuário. |
| `server/ws.ts` | Enviar eventos a todos os membros ativos do espaço. |
| Triggers de saldo e migrations antigas | Revisar qualquer rotina que dependa de `user_id` para calcular saldo. |

### 9.3 Cuidados obrigatórios

- `invoices` não possui `user_id`; a autorização deve passar pelo cartão.
- Transferências validam duas contas; ambas devem pertencer ao mesmo espaço autorizado.
- Importação OFX, conciliação e recálculo devem rejeitar contas de outro espaço.
- Categorias usadas por lançamentos compartilhados devem ser do mesmo espaço.
- O endpoint de criação por API key usa `user_id` no corpo. Deve continuar funcionando, resolvendo o espaço pessoal do usuário informado ou exigindo um `spaceId` explícito numa evolução futura.
- Rotinas de exclusão de usuário não podem apagar em cascata os dados financeiros de um espaço compartilhado.
- A exclusão de lançamento alheio deve ser bloqueada diretamente para `MEMBER`; apenas a aprovação do `OWNER` pode executá-la.
- A aprovação de exclusão deve revalidar autor, espaço e existência do lançamento na mesma transação.
- A migração deve bloquear origem e destino, mover todas as entidades dependentes e falhar por completo se qualquer etapa falhar.
- Convites compartilhados por WhatsApp continuam exigindo correspondência de e-mail no aceite.
- A desativação do proprietário deve transferir a titularidade na mesma transação, nunca deixar um espaço com outro membro sem `OWNER`.

## 10. Migração dos dados existentes

### 10.1 Estratégia

1. Criar um espaço pessoal para cada usuário existente, com nome padrão como “Finanças de {primeiro nome}”.
2. Inserir o usuário como `OWNER`.
3. Preencher `space_id` nos registros financeiros a partir do `user_id` atual.
4. Validar que nenhum registro ficou sem espaço.
5. Tornar `space_id` obrigatório.
6. Criar índices por `space_id` e combinações usadas nas consultas frequentes.

### 10.2 Regras

- A migration deve ser idempotente e executada em transação.
- Antes do backfill, registrar contagens por usuário de contas, cartões, categorias e lançamentos.
- Depois do backfill, comparar contagens e somatórios de saldo.
- Fazer backup antes da execução em produção.
- Ter plano de rollback documentado.
- Não alterar IDs existentes.
- O espaço pessoal deve ser o padrão para apps antigos que ainda não enviam o cabeçalho.

### 10.3 Convite para o espaço pessoal existente

Quando o proprietário compartilha o próprio espaço, todos os dados atuais passam a ser visíveis ao convidado. Esse aviso deve aparecer antes do envio do convite e no aceite.

O convidado mantém o espaço pessoal dele. Se houver dados nos dois espaços, não haverá soma automática: o membro deve solicitar a migração, que será aprovada ou recusada pelo proprietário.

### 10.4 Migração do espaço pessoal do convidado

A migração move o conjunto financeiro do espaço pessoal do membro para o espaço compartilhado:

1. O membro solicita a migração e o app mostra uma prévia.
2. O proprietário recebe a notificação e revisa a quantidade de registros.
3. O proprietário aprova ou recusa.
4. Se recusar, nada é movido nem apagado.
5. Se aprovar, o backend move os dados em uma única transação.
6. O espaço pessoal permanece existente, vazio ou com os registros que não fazem parte do escopo financeiro.

Dependências que precisam ser movidas juntas:

- contas;
- cartões e seus vínculos com contas de débito;
- faturas e pagamentos de fatura vinculados aos cartões;
- categorias e subcategorias;
- lançamentos, incluindo parcelamentos, despesas fixas, transferências e conciliação;
- metas por categoria;
- snapshots de saldo, se forem mantidos por espaço.

Não fazem parte da migração:

- perfil, senha e foto;
- tokens de dispositivo e preferências de notificação;
- lembretes personalizados;
- configurações pessoais;
- logs de notificação.

Após a migração, executar o recálculo de saldos e faturas do espaço de destino. A origem deve ser validada antes da transação e o pedido deve terminar em `FAILED` se houver qualquer inconsistência.

## 11. Implementação no Android

### 11.1 Novos componentes

| Componente | Responsabilidade |
| :--- | :--- |
| `FinanceSpaceDto` | Espaço, papel do usuário e nome. |
| `FinanceSpaceMemberDto` | Membro, e-mail, nome e papel. |
| `FinanceSpaceInvitationDto` | Convite, link compartilhável e status. |
| `FinanceSpaceDeletionRequestDto` | Pedido de exclusão e status. |
| `FinanceSpaceMigrationRequestDto` | Pedido de migração, prévia e status. |
| `FinanceSpaceRepository` | Listagem, seleção, convites, exclusões, migração e remoção. |
| `FinanceSpaceViewModel` | Estados de carregamento, erro e ações. |
| `FinanceSpaceActivity` | Tela do espaço ativo, membros, convites e aprovações. |
| `InvitationAcceptActivity` | Aceite de convite por link. |
| `InvitationShareHelper` | Compartilhamento do link via seletor nativo/WhatsApp. |
| `ActiveFinanceSpaceStore` | Persistência local do espaço selecionado. |

### 11.2 Rede

- Adicionar as rotas ao `ApiService`.
- Adicionar o cabeçalho `X-Finance-Space-Id` no `JwtInterceptor` somente quando houver espaço ativo.
- Não enviar o cabeçalho em login, cadastro, recuperação de senha, perfil e registro de push.
- Tratar `403` de espaço removido com mensagem clara e atualização da lista de espaços.
- Manter o tratamento de `401` existente para expirar a sessão.

### 11.3 Tela e navegação

Adicionar em `MenuActivity` a opção **“Espaço financeiro”**, com subtítulo “Compartilhe suas finanças”.

A tela deve conter:

- nome do espaço ativo;
- seletor de espaço, quando houver mais de um;
- lista de membros com papel;
- botão “Convidar pessoa”, visível apenas para `OWNER`;
- opções “Enviar por e-mail” e “Compartilhar no WhatsApp” após criar o convite;
- lista de convites pendentes com status, reenvio, compartilhamento e cancelamento;
- seção “Pedidos de exclusão” para o proprietário aprovar ou recusar;
- seção “Migração de dados” para o membro solicitar e para o proprietário aprovar ou recusar;
- opção “Criar novo espaço”;
- aviso de privacidade e explicação sobre visibilidade dos dados;
- opção de sair do espaço para `MEMBER`;
- confirmação antes de remover um membro;
- opção de transferir a propriedade quando o proprietário for desativar a conta.

### 11.4 Aceite por link

- Configurar um deep link ou Android App Link para o domínio do backend.
- Se o usuário não estiver autenticado, direcionar ao login preservando o token do convite.
- Se estiver autenticado com outro e-mail, informar que é necessário entrar com o e-mail convidado; o link não deve conceder acesso a terceiros.
- Após o aceite, abrir o seletor de espaço ou entrar diretamente no espaço compartilhado.
- Não registrar o token em logs, analytics ou mensagens de erro.
- Tratar o caso de link encaminhado com mensagem clara e opção de solicitar um novo convite ao proprietário.

### 11.5 Compartilhamento por WhatsApp

- Usar `Intent.ACTION_SEND` com `type = "text/plain"` para abrir o seletor nativo do Android.
- Incluir uma mensagem curta, o nome do espaço e o `shareUrl`.
- Não exigir que o WhatsApp esteja instalado; o seletor deve mostrar os aplicativos disponíveis.
- Não usar a API oficial do WhatsApp nesta primeira versão.
- O link continua válido somente para o e-mail informado no convite.

### 11.6 Room e cache local

O banco local está na versão 2. Será necessária uma nova versão do Room, por exemplo 3.

Alterações recomendadas:

- adicionar `spaceId` à fila `pending_transactions`;
- manter `ownerEmail` como identidade do autor;
- adicionar `spaceId` ao cache de opções;
- incluir o espaço na chave do cache ou limpar o cache ao trocar de espaço;
- criar migration `2 → 3`;
- validar a migration com banco real de uma versão anterior.

Estratégia recomendada para a primeira versão: ao trocar de espaço, limpar caches de contas, transações e opções e recarregar da API. A fila offline **não pode** ser apagada, pois contém lançamentos ainda não sincronizados.

### 11.7 Fila offline

Hoje a fila usa `ownerEmail` para evitar sincronização sob outro login. Com espaços, ela precisa de:

- `ownerEmail` ou `authorUserId`: quem criou o lançamento;
- `spaceId`: onde o lançamento deve ser criado;
- `status`, `attempts` e `lastError` existentes.

O sincronizador só pode enviar itens cujo autor seja o usuário autenticado e cujo espaço esteja entre os espaços ativos desse usuário. Se o usuário tiver sido removido do espaço, o item deve ser marcado como erro permanente com mensagem clara, e não enviado a outro espaço.

## 12. Notificações e tarefas agendadas

Notificações são pessoais, mas os dados que as originam podem ser compartilhados.

Regras decididas:

- `message_configs`, `message_logs`, tokens de dispositivo e preferências continuam por usuário;
- alertas financeiros do sistema, como fatura próxima do vencimento e vencimentos, são gerados a partir dos dados do espaço;
- **todos os membros ativos do espaço recebem esses alertas**;
- a configuração pessoal pode controlar canal, horário de silêncio e forma de entrega, mas não excluir um membro do público-alvo dos alertas financeiros obrigatórios do espaço;
- não enviar duplicado para o mesmo usuário e dispositivo;
- respeitar membros removidos imediatamente;
- manter a deduplicação por usuário, configuração e data;
- lembretes personalizados criados por um membro são **pessoais**, não visíveis nem editáveis pelo outro;
- pedidos de exclusão de lançamento e de migração geram notificação para o proprietário;
- a decisão do proprietário gera notificação para o solicitante.

Esse ponto exige revisão do scheduler, que atualmente percorre dados por usuário. Ele deve resolver os membros ativos do espaço, gerar uma entrega por membro e manter os lembretes personalizados no escopo individual.

## 13. Segurança e privacidade

### 13.1 Controles obrigatórios

- Toda rota financeira valida a participação ativa no espaço.
- IDs de recursos de outro espaço retornam `403` ou `404`, sem dados adicionais.
- Convites usam token aleatório com pelo menos 256 bits e hash SHA-256 no banco.
- Convites expiram e são de uso único.
- E-mail é normalizado e comparado com a conta autenticada, inclusive quando o link é enviado por WhatsApp.
- O aceite exige código OTP enviado ao e-mail convidado: hash HMAC no banco, expiração de 15 minutos e bloqueio após cinco tentativas.
- Aplicar rate limit na criação, compartilhamento e aceite de convites.
- Não expor existência de e-mail, espaço ou convite em mensagens de erro públicas.
- Não registrar tokens de convite em logs, analytics ou notificações.
- Registrar eventos de criar, aceitar, revogar, remover, transferir propriedade e sair do espaço.
- A aprovação de exclusão deve ser feita apenas por `OWNER` e revalidar o autor real do lançamento.
- A aprovação de migração deve ser feita apenas por `OWNER`, com prévia e transação atômica.
- A recusa de migração nunca pode apagar dados de origem.
- A transferência de propriedade deve invalidar a possibilidade de o antigo proprietário administrar o espaço.
- Toda operação de aprovação deve ser idempotente, para que toques duplicados não excluam ou movam dados duas vezes.

### 13.2 Testes de isolamento

Criar testes que tentem, com um usuário sem associação:

- listar contas do espaço alheio;
- consultar transações por ID;
- editar e excluir lançamento;
- pagar fatura;
- importar extrato;
- conciliar transações;
- recalcular saldos;
- consultar dashboard e relatórios;
- acessar metas e categorias.

Todos devem ser rejeitados.

### 13.3 Exclusão e desativação de conta

O fluxo atual de desativação remove o usuário. Com dados compartilhados, isso não pode apagar o espaço nem os lançamentos da família.

Regras decididas:

- `MEMBER` que desativa a conta sai do espaço sem apagar dados financeiros.
- `OWNER` que possui outro membro ativo deve receber a opção de **transferir a propriedade** para esse membro antes de desativar a conta.
- A transferência deve ocorrer na mesma transação da desativação.
- Se houver outro membro ativo e o proprietário recusar a transferência, ou não houver destinatário elegível, a desativação é bloqueada com explicação; o espaço não pode ficar sem `OWNER`.
- Se o espaço não tiver outro membro ativo, o proprietário pode desativar a conta normalmente, com confirmação de que o espaço pessoal e seus dados serão removidos conforme o fluxo atual.
- Antes da desativação, o app deve mostrar o nome do membro que receberá a propriedade e pedir confirmação explícita.
- `created_by_user_id` deve usar `SET NULL` ou manter o identificador sem exclusão em cascata.
- Revisar todas as foreign keys para evitar `ON DELETE CASCADE` nos dados compartilhados.

**Implementação atual:** o endpoint já impede a exclusão destrutiva do proprietário quando há outro membro ativo e retorna `409 OWNERSHIP_TRANSFER_REQUIRED`. A transferência real e a confirmação Android ainda não foram implementadas; o bloqueio existe para evitar perda do espaço.

### 13.4 LGPD e transparência

- Exibir quais dados serão compartilhados antes do convite.
- Exigir consentimento explícito no aceite.
- Informar que o link pode ser encaminhado, mas só o e-mail convidado consegue aceitar.
- Permitir sair do espaço, revogar convites e cancelar pedidos pendentes.
- Documentar que a remoção de um membro não apaga lançamentos já criados por ele.
- Explicar que a recusa da migração mantém os dados no espaço pessoal e que apagar dados é uma ação separada.
- Definir política de retenção e exportação de dados compartilhados.

## 14. Experiência do usuário

### 14.1 Textos sugeridos

- Menu: **Espaço financeiro** — “Compartilhe suas finanças”.
- Convite: **Convidar pessoa** — “Informe o e-mail. Você poderá enviar o convite por e-mail ou compartilhar o link pelo WhatsApp.”
- Aviso do convite: “Ao aceitar, a pessoa verá todas as contas, cartões, categorias e lançamentos deste espaço.”
- Aceite: **Aceitar convite** — “Ao aceitar, você terá acesso às finanças de {nome do espaço}.”
- Link encaminhado: “Este convite foi enviado para outro e-mail. Peça um novo convite ao proprietário.”
- Exclusão alheia: **Solicitar exclusão** — “Este lançamento foi criado por {nome}. O proprietário precisa aprovar a exclusão.”
- Pedido pendente: **Aguardando aprovação** — “O proprietário foi notificado.”
- Aprovação de exclusão: **Aprovar exclusão?** — “O lançamento será removido para todos os membros.”
- Migração: **Trazer meus dados** — “Seus dados pessoais serão movidos para este espaço somente após a aprovação do proprietário.”
- Migração recusada: “Seus dados continuam no seu espaço pessoal e não foram apagados.”
- Transferência: **Transferir propriedade** — “Escolha quem será o novo proprietário antes de desativar sua conta.”
- Remoção: **Remover {nome}?** — “A pessoa perderá o acesso imediatamente. Os lançamentos já feitos serão mantidos.”

### 14.2 Estados necessários

- carregando espaços;
- nenhum espaço;
- um espaço;
- múltiplos espaços;
- convite pendente;
- convite expirado;
- convite aceito;
- link encaminhado para outro e-mail;
- pedido de exclusão pendente, aprovado ou recusado;
- migração pendente, aprovada, recusada ou com falha;
- espaço aguardando transferência de propriedade;
- erro de rede;
- acesso removido;
- limite de membros atingido.

Seguir os componentes e tokens definidos em `docs/design-system.md`, com `ModernCard`, `ModernTextInput`, badges de status e CTA esmeralda.

## 15. Critérios de aceite

### Espaços e convites

- [ ] Todo usuário existente possui um espaço pessoal após a migration.
- [ ] O proprietário consegue convidar um e-mail e vê o convite como pendente.
- [ ] O convite pode ser enviado por e-mail e compartilhado pelo WhatsApp.
- [ ] O convidado recebe o link e consegue visualizar a prévia do convite.
- [ ] O convite só é aceito pelo e-mail correto.
- [ ] Um link encaminhado para outro e-mail não permite o aceite.
- [ ] Convite expirado, revogado ou já aceito é recusado.
- [ ] Dois aceites simultâneos não ultrapassam dois membros ativos.
- [ ] O convite não pode ser reutilizado.
- [ ] O proprietário consegue cancelar um convite pendente.
- [ ] O proprietário consegue remover um membro, que perde o acesso imediatamente.
- [ ] O proprietário consegue transferir a propriedade para o outro membro.

### Dados financeiros

- [ ] Ambos os membros veem contas, cartões, categorias, lançamentos, faturas, dashboard e relatórios do espaço.
- [ ] Ambos conseguem criar lançamentos e o autor é registrado.
- [ ] Um lançamento criado por um membro aparece para o outro após atualizar.
- [ ] O membro consegue excluir os próprios lançamentos diretamente.
- [ ] A exclusão de lançamento alheio por um membro cria um pedido pendente.
- [ ] O proprietário recebe notificação do pedido e consegue aprovar ou recusar.
- [ ] A aprovação do pedido exclui o lançamento e recalcula o saldo para todos.
- [ ] A recusa mantém o lançamento intacto.
- [ ] O membro não consegue alterar contas, cartões, categorias ou metas.
- [ ] Transferências só aceitam contas do mesmo espaço.
- [ ] Importação e conciliação operam apenas no espaço ativo.
- [ ] Um usuário sem participação recebe erro ao tentar acessar qualquer recurso do espaço.
- [ ] Nenhum dado de um espaço aparece em outro.

### Migração

- [ ] O membro consegue solicitar a migração do próprio espaço pessoal.
- [ ] O proprietário vê a prévia e pode aprovar ou recusar.
- [ ] A recusa mantém todos os dados no espaço pessoal.
- [ ] A aprovação move os dados e relacionamentos em uma única transação.
- [ ] Uma falha não deixa migração parcial.
- [ ] Perfil, notificações e preferências não são migrados.
- [ ] O app informa claramente quando a migração foi recusada.

### Desativação do proprietário

- [ ] Ao desativar a conta com outro membro ativo, o proprietário recebe a opção de transferir a propriedade.
- [ ] A transferência e a desativação são concluídas juntas.
- [ ] Sem transferência válida, a desativação é bloqueada.
- [ ] Os dados do espaço permanecem após a saída do antigo proprietário.

### Offline e local

- [ ] A fila offline sincroniza no espaço correto.
- [ ] Trocar de espaço não envia pendências do espaço anterior.
- [ ] Remoção do espaço gera erro permanente e recuperável para o usuário.
- [ ] Caches de espaços diferentes não se misturam.
- [ ] A migration do Room preserva lançamentos pendentes.

### Notificações

- [ ] Faturas e vencimentos do espaço são enviados a todos os membros ativos.
- [ ] Preferências pessoais de canal e horário de silêncio são respeitadas sem excluir o membro do público-alvo.
- [ ] Lembretes personalizados continuam pessoais.
- [ ] Pedidos de exclusão e migração notificam o proprietário.
- [ ] A decisão do proprietário notifica o solicitante.
- [ ] Membro removido deixa de receber alertas daquele espaço.
- [ ] Não há envio duplicado para o mesmo usuário e dispositivo.

## 16. Plano de testes

### Unitários

- Validação de token, validade, status e e-mail do convite.
- Limite de membros em condição de corrida.
- Permissões de `OWNER` e `MEMBER`.
- Normalização de e-mail.
- Link encaminhado para outro e-mail.
- Autorização de exclusão própria e de lançamento alheio.
- Aprovação e recusa de exclusão.
- Aprovação, recusa, falha e atomicidade da migração.
- Regras de saída e transferência de propriedade.

### Integração do backend

- Migration e backfill com dados de dois usuários.
- Criação de espaço pessoal no cadastro e no login Google.
- Listagem de espaços apenas para membros ativos.
- Todas as rotas financeiras com e sem `X-Finance-Space-Id`.
- Tentativa de acesso cruzado a IDs de outro espaço.
- Convite, aceite, expiração, recusa, cancelamento e reuso.
- Envio por e-mail e geração de `shareUrl` para WhatsApp.
- Aceite com e-mail diferente do convite.
- Pedido de exclusão, aprovação, recusa, cancelamento e concorrência de toques.
- Migração aprovada, recusada, cancelada e com falha simulada.
- Remoção de membro e invalidação imediata de acesso.
- Transferência de propriedade e desativação transacional.
- Recálculo de saldo e faturas no escopo correto.
- Scheduler e envio de push para todos os membros ativos.

### Android

- Room `2 → 3` com banco real.
- Login, seleção de espaço e troca de espaço.
- Convite por e-mail e compartilhamento pelo seletor nativo/WhatsApp.
- Aceite por deep link e bloqueio de link encaminhado.
- Pedido de exclusão e visualização do status.
- Solicitação de migração, prévia e estados de aprovação/recusa.
- Transferência de propriedade antes da desativação.
- Fila offline no espaço correto.
- Logout e login com outro usuário.
- Tratamento de `403` após remoção.
- Testes de interface em modo claro e escuro.

### Homologação manual

1. Criar usuário A e usuário B.
2. Enviar convite de A para B por e-mail.
3. Compartilhar o mesmo link pelo WhatsApp e aceitar com a conta correta.
4. Encaminhar o link para outro e-mail e confirmar o bloqueio.
5. Criar lançamento em A e verificar em B.
6. Criar lançamento em B e verificar em A.
7. B tentar excluir lançamento criado por A e confirmar o pedido pendente.
8. A aprovar a exclusão e verificar o recálculo em ambos.
9. A recusar outro pedido e confirmar que o lançamento permanece.
10. B solicitar a migração dos dados pessoais e A recusar; confirmar preservação.
11. Repetir a solicitação e aprovar; confirmar movimentação completa.
12. Testar transferência, importação, fatura e notificações para ambos.
13. A tentar desativar a conta sem transferir e confirmar o bloqueio.
14. A transferir a propriedade para B e concluir a desativação.
15. Remover B e confirmar bloqueio.
16. Reenviar convite e testar expiração.
17. Testar com app antigo, sem cabeçalho de espaço.
18. Testar atualização de versão com banco local preenchido.

## 17. Publicação e compatibilidade

- Usar feature flag para liberar a tela e os endpoints de convite.
- Publicar primeiro o backend com suporte ao cabeçalho e fallback para o espaço pessoal.
- Publicar depois o Android com seleção de espaço e envio do cabeçalho.
- Manter o fallback por pelo menos uma versão do app.
- Monitorar erros `403`, falhas de convite e filas offline presas.
- Ter rollback do backend sem perda de dados; a migration de `space_id` deve ser compatível com a versão anterior durante a janela de transição.

## 18. Riscos e mitigação

| Risco | Impacto | Mitigação |
| :--- | :--- | :--- |
| Alguma consulta continuar filtrando por `userId` | Vazamento ou dados ausentes | Auditoria de todas as queries e testes de isolamento. |
| Exclusão de usuário apagar dados compartilhados | Perda irreversível | Revisar foreign keys e bloquear desativação do proprietário único. |
| Dois aceites simultâneos passarem do limite | Espaço com três membros | Transação com bloqueio e teste de concorrência. |
| Fila offline enviar para o espaço errado | Lançamento no lugar errado | `spaceId` e autor na fila; testes de troca de espaço. |
| Notificações duplicadas | Ruído e perda de confiança | Deduplicação e revisão do scheduler. |
| Convite vazar por e-mail ou log | Acesso indevido | Token de uso único, hash, validade, vínculo ao e-mail e sigilo em logs. |
| Link encaminhado no WhatsApp ser aceito por terceiro | Acesso indevido | Correspondência obrigatória entre e-mail do convite e conta autenticada. |
| Membro excluir lançamento alheio sem aprovação | Perda de histórico | Bloqueio no backend e pedido formal com aprovação do proprietário. |
| Aprovação de exclusão executada duas vezes | Erro ou inconsistência | Operação idempotente e revalidação na transação. |
| Migração falhar no meio | Dados duplicados ou perdidos | Transação atômica, bloqueio de origem/destino e status `FAILED` sem alteração parcial. |
| Recusa de migração apagar dados | Perda irreversível | Recusa somente de leitura; exclusão exige ação separada e confirmação explícita. |
| Proprietário desativar conta e deixar espaço sem dono | Espaço inadministrável | Transferência obrigatória na mesma transação da desativação. |
| App antigo quebrar após a migration | Usuários sem acesso | Fallback para espaço pessoal na API. |
| Cache local misturar espaços | Exibição incorreta | Chave por espaço ou limpeza ao trocar. |

## 19. Estimativa relativa

| Frente | Complexidade |
| :--- | :--- |
| Schema, migrations e backfill | Média |
| Espaços, membros e convites | Média |
| Aprovação de exclusões e notificações de decisão | Média |
| Migração de dados entre espaços | Alta |
| Autorização de todas as rotas financeiras | Alta |
| Saldos, triggers, faturas e scheduler | Alta |
| Transferência de propriedade e desativação segura | Média |
| Testes de isolamento e concorrência | Alta |
| Android: rede, seleção de espaço, convites e aprovações | Média |
| Android: Room, cache e fila offline | Média |
| Homologação e publicação gradual | Média |

A maior parte do esforço está no backend, porque o isolamento por `userId` está distribuído por várias consultas e rotinas financeiras.

## 20. Compartilhamento com mais de duas pessoas

A estrutura proposta já foi desenhada para vários membros. Para liberar mais de duas pessoas, será necessário:

1. Remover ou configurar o limite de membros ativos no backend.
2. Definir se o espaço aceitará convites ilimitados ou um limite por plano.
3. Revisar a condição de corrida do aceite para o novo limite.
4. Implementar papéis adicionais, como administrador, editor e somente leitura.
5. Criar interface de gestão de vários membros e convites.
6. Definir regras de transferência de propriedade e saída do proprietário.
7. Revisar quem aprova exclusões e migrações quando houver vários administradores.
8. Implementar auditoria detalhada de alterações por membro.
9. Revisar notificações para muitos destinatários e deduplicação.
10. Revisar privacidade, consentimento e retenção de dados.
11. Considerar limites comerciais e cobrança por espaço ou por membro.
12. Ampliar os testes de concorrência, isolamento e desempenho.

**O modelo de dados e o cabeçalho de espaço não precisam ser redesenhados.** O que muda é a regra de negócio, a interface de administração e a profundidade das permissões.

## 21. Decisões de produto

| ID | Pergunta | Decisão | Consequência técnica |
| :--- | :--- | :--- | :--- |
| P-01 | O convite será enviado apenas por e-mail ou também por link compartilhável? | E-mail e WhatsApp. O link é vinculado ao e-mail do convidado, que também confirma um código enviado à caixa postal. | O backend retorna `shareUrl`, envia um código de seis dígitos e o Android usa o compartilhamento nativo. |
| P-02 | O membro pode excluir lançamentos e contas ou apenas o proprietário? | O membro exclui os próprios lançamentos. Lançamentos alheios exigem aprovação do proprietário. Contas, cartões, categorias e metas ficam sob gestão do proprietário. | Tabela de pedidos de exclusão, notificação e aprovação transacional. |
| P-03 | Quem recebe alertas de faturas e vencimentos do espaço? | Todos os membros ativos. | Scheduler gera entrega para cada membro, com deduplicação por usuário e dispositivo. |
| P-04 | Lembretes personalizados são pessoais ou compartilhados? | Pessoais. | Permanecem vinculados ao usuário; não são exibidos nem editáveis pelo outro membro. |
| P-05 | O que acontece ao desativar a conta do proprietário? | O app pergunta se ele deseja transferir a propriedade para o outro membro. Com outro membro ativo, a desativação só ocorre após transferência válida; sem outro membro, segue o fluxo normal de exclusão. | Transferência e desativação na mesma transação; validação de espaço com `OWNER`. |
| P-06 | O espaço pessoal existente será compartilhado ou haverá um novo espaço vazio? | O espaço existente será compartilhado. | O convite e o aceite devem avisar que os dados atuais ficarão visíveis. |
| P-07 | O convidado poderá migrar dados pessoais para o espaço compartilhado? | Sim, com solicitação e aprovação do proprietário. Se recusada, os dados permanecem no espaço pessoal; apagar é uma ação separada. | Tabela de pedidos de migração, prévia, transação atômica e notificações de decisão. |

**Situação:** todas as pendências P-01 a P-07 foram respondidas em 2026-10-30. Não há decisões de produto bloqueando o início da implementação.

## 22. Referências no código atual

### Android

- `app/src/main/java/br/com/tecpontes/appfinanceiro/network/ApiService.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/utils/TokenManager.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/data/repository/AuthRepository.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/data/local/AppDatabase.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/di/DatabaseModule.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/data/local/entity/PendingTransaction.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/data/repository/PendingTransactionQueue.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/data/local/LocalOptionsCache.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/data/repository/ActiveFinanceSpaceStore.kt`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/data/repository/FinanceSpaceRepository.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/ui/menu/MenuActivity.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/ui/finance/FinanceSpaceActivity.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/ui/finance/FinanceSpaceInvitationActivity.java`
- `app/src/main/java/br/com/tecpontes/appfinanceiro/ui/profile/ProfileActivity.java`

### Backend

- `shared/schema.ts`
- `server/api/auth.ts`
- `server/api/accounts.ts`
- `server/api/cards.ts`
- `server/api/categories.ts`
- `server/api/transactions.ts`
- `server/api/invoices.ts`
- `server/api/dashboard.ts`
- `server/api/balance-history.ts`
- `server/api/reconciliation.ts`
- `server/api/maintenance.ts`
- `server/api/category-goals.ts`
- `server/api/users.ts`
- `server/mailer.ts`
- `server/middleware/auth.ts`
- `server/storage.ts`
- `server/scheduler.ts`
- `server/routes.ts`

## 23. Histórico de execução

| Data | Etapa | Resultado |
| :--- | :--- | :--- |
| 2026-10-30 | Estudo inicial | Documento criado a partir da análise do Android e do backend. Nenhuma implementação realizada. |
| 2026-10-30 | Decisões de produto | Resolvidas P-01 a P-07: convite por e-mail/WhatsApp vinculado ao e-mail, exclusão com aprovação, alertas para todos, lembretes pessoais, transferência de propriedade, compartilhamento do espaço existente e migração com aprovação. |
| 2026-10-30 | Primeira fatia e validação | API e Android iniciados; `npx tsc` passou, 8 testes server-side passaram e `./gradlew testDebugUnitTest` passou. Testes PostgreSQL end-to-end pendentes; migration ainda não aplicada. |
| 2026-10-30 | Início da implementação | Adicionados schema/migration de espaços e convites, middleware de escopo compatível com `user_id`, endpoints de convite/aceite, pedidos de exclusão, alertas por membro, seleção de espaço no Android, deep link, fila offline com espaço e cache de opções isolado. A migration não foi aplicada ao banco. |
