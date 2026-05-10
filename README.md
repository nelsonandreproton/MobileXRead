# MobileXRead

App Android que recebe partilhas do X/Twitter, extrai o conteúdo completo (tweets normais e X Articles), gera um resumo de 10 pontos com um modelo de IA a correr localmente no telemóvel, e guarda tudo para consulta posterior.

O processamento é feito em **fila assíncrona**: partilhas o link, a app fecha imediatamente, e o resumo aparece na lista assim que ficar pronto — podes enviar vários links seguidos sem esperar.

---

## Funcionalidades

- Partilha direta do X/Twitter para a app via share sheet
- Suporte a tweets normais **e X Articles** (`x.com/i/status/...`)
- Fila de processamento assíncrona com **WorkManager** (sobrevive a force-close e reboot)
- Resumo de 10 pontos gerado por **Gemma 3 1B INT4** — modelo local, sem dados enviados para a internet
- Detecção automática de idioma (PT / ES / EN)
- Deduplicação: o mesmo URL não é processado duas vezes
- Items com falha aparecem na lista com mensagem de erro e botão de retry
- Recuperação automática no arranque: items interrompidos por crash voltam ao estado FAILED para poderem ser re-tentados
- Ecrã de **Logs de processamento** com stack traces completos (long-press numa linha copia para a área de transferência)
- Badge na barra com contagem de items em fila
- Partilha do resumo para Raindrop ou qualquer outra app
- Interface Material 3 Dark
- Base de dados local (Room/SQLite)

---

## Pré-requisitos

| Ferramenta | Versão mínima | Download |
|---|---|---|
| Android Studio | Hedgehog 2023.1.1+ | [developer.android.com/studio](https://developer.android.com/studio) |
| JDK | 11+ | incluído no Android Studio |
| Conta Kaggle | gratuita | [kaggle.com](https://www.kaggle.com) |

---

## 1. Clonar o repositório

```bash
git clone https://github.com/nelsonandreproton/MobileXRead.git
cd MobileXRead
```

---

## 2. Abrir no Android Studio

1. Abre o **Android Studio**
2. Clica em **File → Open** e selecciona a pasta `MobileXRead`
3. Aguarda o Android Studio sincronizar o Gradle (pode demorar 2–5 min na primeira vez)

---

## 3. Descarregar o modelo Gemma 3 1B

O modelo de IA (~540 MB) não está incluído no repositório e tem de ser descarregado separadamente.

### 3.1 Criar conta Kaggle e aceitar a licença Gemma

1. Vai a [kaggle.com](https://www.kaggle.com) e cria uma conta gratuita (ou faz login)
2. Abre a página do modelo e aceita os termos:
   - **Opção recomendada (.task):** [Gemma3-1B-IT multi-prefill q4 ekv2048](https://www.kaggle.com/models/google/gemma3/frameworks/litert/variations/gemma3-1b-it-multi-prefill-seq-q4-ekv2048)  
     Ficheiro: `Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task` (~540 MB)
   - **Alternativa (.bin):** [gemma3-1b-it-gpu-int4](https://www.kaggle.com/models/google/gemma/frameworks/tfLite/variations/gemma3-1b-it-gpu-int4)

### 3.2 Transferir para o telemóvel

**Via USB:**
```
1. Liga o telemóvel com cabo USB → "Transferência de ficheiros"
2. Copia o ficheiro .task (ou .bin) para a pasta Downloads do telemóvel
```

**Via Google Drive / OneDrive:**
```
1. Faz upload para o Drive
2. Descarrega no telemóvel
```

---

## 4. Ativar Modo de Programador (OPPO Reno 10 5G / ColorOS)

```
Definições → Sobre o telefone → Informações da versão
  → Toca 7× em "Número de compilação" → introduz PIN
```

Depois:

```
Definições → Definições adicionais → Opções de programador
  → Ativa "Depuração USB" → confirma no diálogo
```

---

## 5. Compilar e instalar

### Via Android Studio (recomendado)

1. Selecciona `OPPO Reno 10 5G` na barra de ferramentas
2. Clica em **▶ Run** (`Shift+F10`)

### Via linha de comandos

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 6. Configurar o modelo na app (primeira execução)

1. Abre a app → ícone ⚙️ **Definições**
2. Toca em **Configurar** junto a "Modelo não configurado"
3. Selecciona o ficheiro `.task` (ou `.bin`) na pasta Downloads
4. Aguarda a cópia (~1–2 min)
5. "Modelo instalado com sucesso!" — pronto

> O modelo ocupa ~540 MB no armazenamento da app. Só precisas de fazer este passo uma vez.

---

## 7. Utilizar a app

### Resumir um tweet ou X Article

1. No X/Twitter, abre o tweet/artigo que queres resumir
2. Toca em **Partilhar** → selecciona **MobileXRead**
3. A app mostra um toast "Adicionado à fila" e fecha imediatamente
4. O processamento corre em segundo plano (notificação de progresso)
5. Quando terminar, aparece uma notificação e o resumo surge na lista principal

Podes partilhar vários links seguidos — todos entram na fila e são processados por ordem.

### Gerir resumos

- **Lista principal:** todos os resumos concluídos, mais recentes primeiro
- **Items com erro:** aparecem no topo com fundo vermelho, mensagem de erro, e botão "Tentar novamente"
- **Badge na barra:** mostra quantos items estão em fila ou a processar
- **Long-press num resumo:** eliminar

### Logs de processamento

- Ícone `⌨` (Terminal) na barra superior → ecrã de logs
- Mostra todos os passos de processamento com timestamps
- **Long-press numa linha de log** → copia para a área de transferência (útil para debug)
- Botão `🗑` para limpar todos os logs

---

## Estrutura do projeto

```
MobileXRead/
├── app/src/main/java/com/mobilexread/
│   ├── data/
│   │   ├── local/          # Room: ArticleEntity, ArticleStatus, ProcessingLog, DAOs
│   │   ├── model/          # Article (domain model)
│   │   └── repository/     # ArticleRepository, LogRepository
│   ├── di/                 # Hilt modules (AppModule, DatabaseModule, WorkManagerModule)
│   ├── llm/                # MediaPipe + Gemma (GemmaInference, ModelManager)
│   ├── scraper/            # FxTwitterScraper, TweetData
│   ├── worker/             # ProcessingWorker (WorkManager)
│   └── ui/
│       ├── screens/        # ArticleListScreen, ArticleDetailScreen, LogsScreen, …
│       ├── viewmodel/      # ArticleListViewModel, ShareViewModel, LogsViewModel, …
│       ├── theme/          # Material 3 Dark
│       └── navigation/     # AppNavigation
└── gradle/
```

---

## Resolução de problemas

| Problema | Solução |
|---|---|
| "Modelo não encontrado" | Definições → Configurar Modelo → selecciona o ficheiro |
| Item fica em PROCESSING após crash | Reinicia a app — os items interrompidos passam automaticamente a FAILED com botão de retry |
| Item falhou — botão "Tentar novamente" não funciona | Vai aos Logs (ícone Terminal) para ver o erro completo |
| Telemóvel não aparece no Android Studio | Verifica "Depuração USB" ativa e confirma o diálogo no telemóvel |
| Gradle sync falha | File → Invalidate Caches / Restart |
| App fecha durante processamento | Pouca RAM — fecha outras apps |

---

## Stack técnica

| Componente | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Base de dados | Room / SQLite |
| Processamento assíncrono | WorkManager (expedited, foreground service) |
| Modelo de IA | Gemma 3 1B INT4 via MediaPipe LLM Inference API |
| Extração de conteúdo | FxTwitter API (`api.fxtwitter.com`) — sem autenticação |
| HTTP | OkHttp |
| Android mínimo | API 29 (Android 10) |
| Testado em | OPPO Reno 10 5G (Snapdragon 778G, 8 GB RAM, ColorOS 13) |

---

## Notas de privacidade

- O único tráfego de rede é para `api.fxtwitter.com` (extração do tweet) — API pública sem autenticação
- O modelo de IA corre **100% local** — o conteúdo dos tweets nunca sai do telemóvel
- Sem telemetria, sem analytics, sem contas
