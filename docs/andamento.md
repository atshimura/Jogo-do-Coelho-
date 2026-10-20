# Coelhitos — plano e andamento

Este arquivo acompanha a implementação em etapas, conforme solicitado. Cada etapa deve ser documentada antes de passar à seguinte.

## Etapas

| Etapa | Entrega | Estado |
|---|---|---|
| 1 | Estrutura Android, orientação horizontal, tela de jogo, desenho original e controles por toque | Concluída |
| 2 | Percurso da planície dimensionado para cerca de 3 minutos, movimento, câmera, obstáculos, corações e reinício | Concluída |
| 3 | Cinco baús e poderes, cinco cenouras iniciais, projéteis, inimigos com resistências e portal | Concluída |
| 4 | Trilha original em loop e efeitos sonoros de ações e eventos | Concluída |
| 5 | Compilação, verificações, APK de depuração e publicação do resultado | Concluída |
| 6 | Ajuste da silhueta lateral quadrúpede, salto para frente e disparo durante movimento | Concluída |
| 7 | Revisão dos comandos de salto, animação de coelho e estabilidade em colisões | Concluída |
| 8 | Instalar e exercitar o jogo em emulador Android no GitHub Actions | Concluída |
| 9 | Corrigir disparo/pulo, dar relevo aos elementos e sincronizar áudio na perda de vida | Concluída (0.1.3) |
| 10 | Refinar identidade visual, animação do coelho, projéteis, terreno e ícone Android | Em validação (0.2.0) |

## Registro

### 19/10/2026 — início

- Auditoria: o repositório continha apenas `LICENSE` e um commit inicial; não havia app Android pré-existente.
- Decisão técnica: criar app Android nativo em Java, com desenho 2D em `Canvas`, sem bibliotecas de terceiros.
- Decisão de visual: arte vetorial original, com formas arredondadas e cores vivas, tomando como referência geral jogos de plataforma Kirby para Switch.
- Especificação funcional consolidada em `docs/especificacao.md`.
- Ambiente inicial: JDK de execução presente; foi instalado um JDK 17 local e Android SDK 35 para compilar e validar o APK.

### 19/10/2026 — etapas 1 a 4

- Etapa 1: app Android nativo, tela horizontal, coelho desenhado em Canvas, menu inicial, joystick e botões de pulo/ação.
- Etapa 2: planície lateral extensa, câmera, animação de movimento, árvores, espinhos, buracos, fogo, corações, dano e reinício.
- Etapa 3: cinco baús em pontos distintos; cinco cenouras colocadas no começo e mais pelo percurso; inimigos pequenos (1 golpe) e grandes (3 golpes); poderes, projéteis e portal final.
- Etapa 4: trilha original em loop e efeitos sintetizados para passos, corrida, salto, coleta, baús, poderes, disparos, inimigos, dano, reinício e portal.
- Verificação técnica: fonte Java compilada com JDK 17; DEX gerado com D8; APK montado com ferramentas do Android SDK e assinatura de depuração validada.
- O tempo de três minutos é estimado pela velocidade de caminhada e extensão do percurso; será necessário confirmar em aparelho real.
- A validação final da interface por toque e reprodução do áudio em aparelho real permanece pendente.

### 19/10/2026 — etapa 5 e entrega 0.1.0

- APK debug 0.1.0 gerado para Android 6.0 ou superior (minSdk 23), em orientação horizontal.
- Pacote conferido: identificador `com.coelhitos`, versão 0.1.0, rótulo Coelhitos.
- Assinatura de depuração conferida pelo apksigner; DEX gerado pelo D8 e APK alinhado pelo zipalign.
- SHA-256: `43a0447789acfb9f6415b4b9e7f357b8968a77badb01035f05a5ffc82035b40f`.
- Foi incluído fluxo GitHub Actions para compilar e publicar o APK debug como artefato em futuras execuções.
- A execução GitHub Actions #2 concluiu com sucesso: Gradle executou `assembleDebug` e o upload do artefato também passou. O artefato de CI fica disponível na execução por 30 dias.
- Limite desta validação: não havia aparelho Android nem emulador conectado. O controle por toque, o ritmo de 3 minutos e o áudio precisam ser confirmados em aparelho real.
- O APK entregue nesta conversa foi montado com JDK 17 e ferramentas do Android SDK (javac, D8, aapt2, zipalign e apksigner). O Gradle não conseguiu acessar os repositórios de plugins a partir desta sessão local; a compilação Gradle padrão foi confirmada com sucesso no GitHub Actions.

### 19/10/2026 — etapa 6 e revisão 0.1.1

- O coelho agora é desenhado de perfil, olhando para o sentido do movimento, com corpo alongado e patas dianteiras e traseiras para uma corrida quadrúpede.
- A animação articula as patas no chão e as recolhe durante o salto.
- Um salto sem direção horizontal também avança no sentido em que o coelho está olhando. Com o joystick na diagonal, o jogador direciona o salto e segue avançando.
- Um toque curto no botão AÇÃO dispara mesmo com o joystick mantendo o deslocamento; segurar AÇÃO com direção continua servindo para correr.
- Atualizada a especificação funcional. Fonte compilada com JDK 17; DEX gerado com D8; APK verificado com apksigner e metadados conferidos com aapt.
- APK debug `Coelhitos-v0.1.1-debug.apk`, versionCode 2, versionName 0.1.1. SHA-256: `4816a8455a34da0347527db6fd05eb0913815285510c53f51dd96e13b1cc7844`.
- A compilação Gradle local continua indisponível nesta sessão porque o plugin Android não está no cache e o repositório não aceitou a conexão; a validação foi concluída diretamente com as ferramentas do Android SDK.
- Validação de toque, animação e áudio em aparelho/emulador permanece pendente.

### 19/10/2026 — etapa 7 e revisão 0.1.2

- Salto sem direção horizontal agora é vertical. Direita + pulo desloca o salto para a direita; esquerda + pulo desloca para a esquerda. Virar acompanha a direcional.
- A animação de movimento foi refeita com impulsos curtos das duas patas traseiras juntas, patas dianteiras de apoio, corpo mais detalhado e orelhas com movimento.
- Corrigida a causa do fechamento ao colidir: ao perder o último coração, a rotina de dano reiniciava e limpava listas enquanto a colisão ainda as percorria, situação que podia lançar `ConcurrentModificationException`. O reinício agora espera os iteradores terminarem.
- Revisados os demais pontos sensíveis: encerramento do áudio sintetizado protegido contra falhas da saída de áudio; ponteiros do joystick e estado do salto são limpos em cancelamento e reinício.
- Verificação local concluída: fonte compilada com JDK 17, DEX gerado com D8, metadados conferidos com aapt e assinatura confirmada com apksigner. APK `Coelhitos-v0.1.2-debug.apk`, versionCode 3. SHA-256: `224b0e59042aecfb323deb126fe9a46bd6c7029b33362e23e75a1e54acb3b6e9`.
- GitHub Actions execução #5 concluída com sucesso: `assembleDebug` e envio do artefato passaram. Não há dispositivo ou emulador conectado para reproduzir o toque real.

### 19/10/2026 — etapa 8: emulador Android

- Incluído no GitHub Actions o Android Emulator Runner (`API 35`, perfil Pixel 7 Pro, imagem x86_64), que cria um dispositivo Android virtual dentro do executor Linux.
- O fluxo instala e abre o APK, captura telas do menu e da fase, aciona gestos direcionais de salto e mantém o coelho avançando por 15 segundos para verificar estabilidade e registro de erros.
- GitHub Actions execução #12 concluída com sucesso em emulador API 35 (Pixel 7 Pro, x86_64): compilação, inicialização do emulador, instalação e abertura do jogo, comandos de toque, processo ativo após o percurso, checagem do Logcat e upload dos artefatos passaram.
- Não foram encontrados `FATAL EXCEPTION` nem `ConcurrentModificationException` no Logcat dessa execução. O APK permaneceu ativo ao fim do trecho testado.
- Capturas e Logcat da execução: artefato `Coelhitos-emulator-evidence`, disponível em https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39340709024/artifacts/12228548825 (retenção de 14 dias).
- Limite: esse smoke test automatizado confirma inicialização e estabilidade durante um trecho curto; não substitui a avaliação visual e de jogabilidade em aparelho real nem mede a duração total de 3 minutos.

### 20/10/2026 — etapa 9: ajustes de controle, relevo e áudio (0.1.3)

- AÇÃO agora distingue toque curto para disparar e pressão longa com direção para correr; o disparo foi exercitado com o joystick mantendo o movimento.
- O pulo usa uma janela curta de buffer, só é ativado uma vez por comando, e captura a direção do joystick: vertical, para frente ou para trás.
- Coelho, inimigos e obstáculos ganharam sombras, contornos e gradientes de luz. A aceleração padrão do Canvas foi mantida para evitar uma camada de desenho por software.
- Ao perder um coração, a trilha pausa brevemente e toca um efeito descendente de perda de vida; perder um poder não interrompe a música.
- GitHub Actions execução #17 concluída com sucesso em emulador Android API 35 (Pixel 7 Pro): build Gradle, instalação, entrada no jogo, pulo neutro, pulo para frente e para trás, disparo com movimento, processo ativo após 15 segundos e Logcat sem `FATAL EXCEPTION` nem `ConcurrentModificationException`.
- Evidência de tela e Logcat: [execução #17](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39345854869), artefato `Coelhitos-emulator-evidence` (`12230264401`, retenção de 14 dias). O emulador foi limitado a 1280×720 para a renderização virtual acompanhar o jogo. A captura mantém visível o aviso de tela cheia do Android; os registros de entrada no Logcat confirmam que os comandos chegaram ao jogo.
- APK debug `Coelhitos-v0.1.3-debug.apk`, `versionCode 4`, `versionName 0.1.3`; SHA-256: `2505f37d36c6aad88b586cfd8f84607659962f62455f6d1477ec2afa921e38a8`.
- Artefato APK `Coelhitos-debug-apk` (`12230369269`, retenção de 30 dias), disponível na [execução #17](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39345854869).
- Limite: a fase completa de três minutos, a mixagem de áudio e a ergonomia em aparelho físico ainda precisam de avaliação manual.

### 20/10/2026 — etapa 10: acabamento visual e ícone (0.2.0)

- Menu inicial refeito com painel de apresentação, título, resumo da aventura e hierarquia visual mais clara.
- HUD redesenhado com corações e cenouras vetoriais, barra de progresso, nome da fase e indicador de poder; removidos emojis dependentes da fonte do aparelho.
- Controles virtuais ganharam contornos, luz, sombras, setas direcionais e símbolos próprios de ação/pulo, mantendo as mesmas áreas de toque.
- A caminhada do coelho agora usa ciclos sincronizados: as duas patas traseiras impulsionam pequenos saltos e as dianteiras acompanham juntas, sem alternância felina.
- O terreno recebeu quatro elevações jogáveis. Altura do piso é aplicada ao coelho, baús, itens, inimigos, perigos, espinho que cai e portal.
- Fogo agora é uma bola laranja com núcleo luminoso e rastro; Pena ganhou haste e barbas para diferenciar melhor os disparos.
- Criado ícone próprio para o app e integrado como adaptive icon e PNGs em múltiplas densidades para launchers antigos.
- Atualizada a versão para `0.2.0` (`versionCode 5`). Build e validação de interface no emulador estão em andamento.

