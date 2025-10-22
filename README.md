# App Financeiro Nativo - Java/Android

Um aplicativo financeiro moderno desenvolvido em Java com arquitetura MVVM, utilizando as melhores práticas do desenvolvimento Android moderno.

## 🚀 Características Principais

- **Arquitetura MVVM** com separação clara de responsabilidades
- **Injeção de Dependência** com Hilt
- **Banco de Dados Local** com Room
- **Comunicação API** com Retrofit2 + OkHttp
- **Design Glassmorphism** com Material 3
- **Autenticação JWT** com interceptor automático
- **Suporte Offline-First** com sincronização
- **Importação OFX** para transações bancárias
- **Temas Claro/Escuro** automáticos

## 🛠 Stack Tecnológica

- **Java 17** - Linguagem de programação
- **Android Jetpack** - Componentes modernos
- **Hilt** - Injeção de dependência
- **Room** - Persistência local
- **Retrofit2** - Comunicação HTTP
- **ViewBinding** - Type-safe view access
- **Material 3** - Design system
- **OkHttp** - Cliente HTTP
- **Gson** - Serialização JSON

## 📱 Telas Implementadas

### 1. Login
- Autenticação com JWT
- Validação de formulário
- Design glassmorphism
- Navegação automática baseada no estado de login

### 2. Dashboard
- Visualização de saldo total
- Transações recentes
- Sincronização de dados
- Pull-to-refresh
- Navegação rápida para transações

### 3. Transações
- Lista completa de transações
- CRUD de transações
- Importação de arquivos OFX
- Filtros por categoria e tipo
- Design responsivo

## 🏗 Arquitetura

```
app/src/main/java/br/com/tecpontes/appfinanceiro/
├── data/
│   ├── local/           # Room Database
│   │   ├── dao/         # Data Access Objects
│   │   ├── entity/      # Entidades Room
│   │   └── AppDatabase.java
│   └── repository/      # Repositórios
├── di/                  # Dependency Injection
├── model/               # DTOs e modelos
├── network/             # API e interceptors
├── ui/                  # Activities e Adapters
├── utils/               # Utilitários
└── viewmodel/           # ViewModels
```

## 🔧 Configuração e Instalação

### 1. Pré-requisitos

- **Android Studio Arctic Fox** ou superior
- **JDK 17**
- **Android SDK API 31+**

### 2. Configuração da API

1. Abra o arquivo `NetworkModule.java`
2. Configure a URL base da API:
```java
private static final String BASE_URL = "https://sua-api.com/";
```

### 3. Endpoints Esperados da API

O backend deve expor os seguintes endpoints:

#### Autenticação
- `POST /auth/login` - Login de usuário
- `POST /auth/register` - Registro de usuário

#### Contas
- `GET /accounts` - Lista de contas
- `POST /accounts` - Criar nova conta

#### Transações
- `GET /transactions` - Lista de transações
- `POST /transactions` - Criar transação

#### Dashboard
- `GET /dashboard` - Dados do dashboard

#### Importação
- `POST /import/ofx` - Upload e processamento de arquivo OFX

### 4. Build e Execução

```bash
# Build do projeto
./gradlew build

# Instalação em dispositivo/emulador
./gradlew installDebug

# Execução via Android Studio
# Run > Run 'app'
```

## 🔐 Autenticação JWT

O aplicativo implementa autenticação automática via JWT:

1. **Interceptação Automática**: Todas as requisições incluem o token JWT
2. **Tratamento de 401**: Token inválido/expirado limpa sessão automaticamente
3. **Persistência Segura**: Token armazenado em SharedPreferences (recomenda-se migrar para EncryptedSharedPreferences em produção)

## 🎨 Design Glassmorphism

O aplicativo utiliza design glassmorphism com:

- **Cores principais**: Roxo-azulado (#6C63FF / #3A8DFF)
- **Translucidez**: Cards com fundo translúcido
- **Bordas sutis**: Contornos com opacidade
- **Sombras suaves**: Efeito de profundidade
- **Temas adaptativos**: Claro/escuro automático

## 💾 Persistência Local

### Room Database

- **Entidades**: Account, Transaction
- **Queries otimizadas**: Índices e foreign keys
- **Sincronização**: Controle de timestamp para sync
- **Offline-first**: Funciona sem conexão

### Funcionalidades

- Cache de dados da API
- Sincronização incremental
- Tratamento de conflitos
- Fallback para dados locais

## 🔄 Sincronização

O aplicativo implementa sincronização inteligente:

1. **Pull-to-refresh** em todas as telas
2. **Sincronização automática** ao abrir o app
3. **Cache inteligente** com timestamp
4. **Tratamento de erros** com retry automático

## 📊 Importação OFX

Funcionalidade completa para importação de extratos bancários:

1. **File Picker**: Seleção de arquivo via sistema
2. **Upload Multipart**: Envio para API
3. **Processamento**: Parse e criação de transações
4. **Persistência**: Salva no banco local
5. **Feedback visual**: Progresso e resultado

## 🧪 Logs e Debug

Logs prefixados implementados em todos os componentes:

```java
Log.d("ComponentName", "Mensagem de debug");
```

### Locais de Log

- **Network**: JwtInterceptor, ApiService
- **Database**: DAOs, Repositories
- **UI**: Activities, ViewModels
- **Auth**: TokenManager, AuthRepository

## 🚀 Melhorias Futuras

### Produção
- [ ] EncryptedSharedPreferences para tokens
- [ ] Migrações de banco de dados
- [ ] Compressão de imagens
- [ ] Analytics e crash reporting

### Funcionalidades
- [ ] Notificações push
- [ ] Widget para saldo
- [ ] Backup/restore
- [ ] Múltiplas contas
- [ ] Relatórios avançados

## 📝 Critérios de Sucesso

✅ Login funcional com JWT armazenado e injeção automática nas chamadas
✅ Dashboard mostra saldo total e últimas 5 transações
✅ CRUD básico de transações persistindo localmente e sincronizando
✅ Importação OFX funcionando (upload + persistência)
✅ Theme Glassmorphism aplicável em modos claro/escuro
✅ Loading, error states e retry implementados
✅ ViewBinding usado em todas as Activities
✅ Hilt injetando Retrofit, DAOs e Repositories
✅ Sem erros no console durante operações básicas
✅ Responsividade mobile-first (375px+)
✅ Console logs prefixados em todos os componentes

## 🐛 Tratamento de Erros

### Estados Tratados

- **Rede**: Timeout, conexão lenta, servidor indisponível
- **Autenticação**: Token inválido, sessão expirada
- **Validação**: Campos obrigatórios, formato inválido
- **Banco**: Constraints, migrações, corrupção
- **Arquivos**: Permissões, formato inválido, arquivo grande

### UX de Erro

- **Snackbar** para erros não críticos
- **Toast** para ações rápidas
- **Retry automático** para falhas de rede
- **Fallback** para dados locais quando API indisponível

## 📱 Responsividade

- **Mobile-first**: Otimizado para 375px (iPhone SE)
- **Breakpoints**: Layouts adaptativos para tablets
- **Orientation**: Suporte landscape e portrait
- **Touch targets**: Botões com tamanho mínimo 48dp

## 🔧 Desenvolvimento

### Estrutura de Branches

```
main          # Produção
develop       # Desenvolvimento
feature/*     # Novas funcionalidades
hotfix/*      # Correções críticas
```

### Commits

```
feat: nova funcionalidade
fix: correção de bug
docs: documentação
style: formatação
refactor: refatoração
test: testes
```

## 📞 Suporte

Para dúvidas ou problemas:

1. Verifique os logs no Logcat
2. Consulte a documentação da API
3. Abra issue no repositório
4. Entre em contato com a equipe

## 📄 Licença

Este projeto é propriedade da TecPontes Sistemas. Todos os direitos reservados.

---

**Desenvolvido com ❤️ pela equipe TecPontes**
