# MobileXRead

App Android que recebe partilhas do X/Twitter, extrai o conteúdo completo (incluindo threads e tweets ligados), gera um resumo de 10 pontos com um modelo de IA a correr localmente no telemóvel, e guarda tudo para consulta posterior.

---

## Funcionalidades

- Partilha directa do X/Twitter para a app via share sheet
- Extracção de threads do autor e tweets incorporados (até 2 níveis)
- Resumo de 10 pontos gerado por **Gemma 3 1B INT4** — modelo local, sem dados enviados para a internet
- Detecção automática de idioma (PT / ES / EN)
- Deduplicação: se já processaste o mesmo tweet, abre o resumo existente
- Partilha do resumo para **Raindrop** (ou qualquer outra app) via share sheet
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
git checkout claude/plan-twitter-summary-app-DBblv
```

---

## 2. Abrir no Android Studio

1. Abre o **Android Studio**
2. Clica em **File → Open** e selecciona a pasta `MobileXRead`
3. Aguarda o Android Studio sincronizar o Gradle (pode demorar 2–5 min na primeira vez — vai descarregar as dependências)
4. Se aparecer um aviso sobre o `gradle-wrapper.jar` em falta, clica em **OK** para o Android Studio o gerar automaticamente

---

## 3. Descarregar o modelo Gemma 3 1B

O modelo de IA (~700 MB) não está incluído no repositório e tem de ser descarregado separadamente.

### 3.1 Criar conta Kaggle e aceitar a licença Gemma

1. Vai a [kaggle.com](https://www.kaggle.com) e cria uma conta gratuita (ou faz login)
2. Abre a página do modelo:  
   [kaggle.com/models/google/gemma/frameworks/tfLite/variations/gemma3-1b-it-gpu-int4](https://www.kaggle.com/models/google/gemma/frameworks/tfLite/variations/gemma3-1b-it-gpu-int4)
3. Clica em **Request Access** e aceita os termos de uso da Google/Gemma

### 3.2 Descarregar o ficheiro

1. Na mesma página, clica em **Download** (o ficheiro chama-se `gemma3-1b-it-gpu-int4.bin`, ~700 MB)
2. Guarda o ficheiro num local acessível no computador

### 3.3 Transferir para o OPPO Reno 10 5G

Escolhe um dos métodos:

**Via USB:**
```
1. Liga o telemóvel ao computador com cabo USB
2. No telemóvel: arrasta o painel de notificações → toca em "USB para carregar"
   → selecciona "Transferência de ficheiros"
3. No computador: abre o explorador de ficheiros → OPPO Reno 10 5G
4. Copia o ficheiro .bin para a pasta Downloads do telemóvel
```

**Via Google Drive / OneDrive:**
```
1. Faz upload do ficheiro .bin para o teu Drive
2. No telemóvel, abre o Drive e descarrega o ficheiro
   (fica em Android/data/... ou na pasta Downloads)
```

---

## 4. Activar o Modo de Programador no OPPO Reno 10 5G

O ColorOS tem um caminho ligeiramente diferente do Android padrão.

```
Definições
  → Sobre o telefone
    → Informações da versão
      → Toca 7 vezes em "Número de compilação"
        → Introduz o PIN/padrão quando pedido
          → Aparece "Modo de programador activado"
```

Depois:

```
Definições
  → Definições adicionais
    → Opções de programador
      → Activa "Depuração USB"
        → Confirma "Permitir" no diálogo que aparece no telemóvel
```

---

## 5. Ligar o telemóvel ao Android Studio

1. Liga o OPPO ao computador com cabo USB
2. No telemóvel: aparece um diálogo "Permitir depuração USB neste computador?" → toca em **Permitir**
3. No Android Studio: na barra de ferramentas superior, deverá aparecer `OPPO Reno 10 5G` no selector de dispositivos
   - Se não aparecer: **Tools → Device Manager** → verifica se o dispositivo está listado

> **Nota:** se o dispositivo não for reconhecido, instala o driver ADB:  
> [developer.android.com/studio/run/oem-usb](https://developer.android.com/studio/run/oem-usb)

---

## 6. Compilar e instalar a app

### Opção A — via Android Studio (recomendado)

1. No Android Studio, selecciona o dispositivo `OPPO Reno 10 5G` na barra de ferramentas
2. Clica no botão **▶ Run** (ou `Shift+F10`)
3. O Android Studio compila a app e instala-a directamente no telemóvel
4. A app abre automaticamente

### Opção B — via linha de comandos

```bash
# Compilar o APK de debug
./gradlew assembleDebug

# O APK fica em:
# app/build/outputs/apk/debug/app-debug.apk

# Instalar directamente (com o telemóvel ligado e ADB activo)
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 7. Configurar o modelo na app (primeira execução)

1. Abre a app **MobileXRead** no telemóvel
2. Toca no ícone ⚙️ (Definições) no canto superior direito
3. Toca em **Configurar** junto a "Modelo não configurado"
4. Na ecrã de configuração, toca em **Seleccionar ficheiro .bin**
5. Navega até à pasta Downloads e selecciona o ficheiro `gemma3-1b-it-gpu-int4.bin`
6. Aguarda a cópia (~700 MB, pode demorar 1–2 minutos)
7. Quando aparecer "Modelo instalado com sucesso!", o setup está completo

> **Nota:** o modelo ocupa ~700 MB no armazenamento interno da app. Só precisas de fazer este passo uma vez.

---

## 8. Utilizar a app

### Resumir um tweet

1. Abre o X/Twitter no telemóvel
2. Abre o tweet (ou thread) que queres resumir
3. Toca em **Partilhar** → selecciona **MobileXRead**
4. A app abre e mostra o progresso:
   - *A extrair tweet…*
   - *A ler thread (N respostas)…* (se houver thread)
   - *A carregar modelo de IA…* (apenas na primeira vez após instalar)
   - *A gerar resumo…* (30–90 segundos — vês o texto a surgir em tempo real)
5. Quando terminar, o resumo abre automaticamente

### Partilhar para o Raindrop

1. No ecrã de detalhe do resumo, toca no ícone de partilha (canto superior direito)
2. Selecciona **Raindrop** no share sheet
3. O Raindrop recebe o URL + título + os 10 pontos como nota

### Navegar pelos resumos guardados

- Abre a app directamente pelo ícone → vês todos os resumos guardados
- Toca num resumo para ver o detalhe
- Mantém premido para eliminar

---

## Estrutura do projecto

```
MobileXRead/
├── app/src/main/java/com/mobilexread/
│   ├── data/           # Room database, entidades, repositório
│   ├── di/             # Hilt modules
│   ├── llm/            # MediaPipe + Gemma (GemmaInference, ModelManager)
│   ├── scraper/        # Nitter scraper (NitterScraper, TweetData)
│   └── ui/
│       ├── screens/    # Ecrãs Compose
│       ├── theme/      # Material 3 Dark
│       ├── viewmodel/  # ViewModels
│       └── navigation/ # Navegação
└── gradle/
    └── wrapper/        # Gradle 8.9
```

---

## Resolução de problemas

| Problema | Solução |
|---|---|
| "Modelo não encontrado" | Vai às Definições → Configurar Modelo e selecciona o ficheiro .bin |
| "Todas as instâncias Nitter falharam" | Verifica a ligação à internet; as instâncias Nitter podem estar temporariamente em baixo — tenta novamente em alguns minutos |
| Telemóvel não aparece no Android Studio | Verifica se a "Depuração USB" está activa e confirma o diálogo no telemóvel |
| Gradle sync falha | File → Invalidate Caches / Restart no Android Studio |
| App fecha durante a geração do resumo | O telemóvel ficou sem RAM — fecha outras apps antes de processar |

---

## Notas técnicas

- **Modelo:** Gemma 3 1B INT4 via [MediaPipe LLM Inference API](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)
- **Extracção:** Nitter (espelho não-oficial do X/Twitter) via Jsoup — sujeito a disponibilidade das instâncias públicas
- **Privacidade:** todo o processamento é local; o único tráfego de rede é para o Nitter (extracção de conteúdo)
- **Android mínimo:** Android 10 (API 29)
- **Testado em:** OPPO Reno 10 5G (Snapdragon 778G, 8 GB RAM, Android 13/ColorOS 13)
