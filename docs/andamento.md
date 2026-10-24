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
| 10 | Refinar identidade visual, animação do coelho, projéteis, terreno e ícone Android | Concluída (0.2.0) |
| 11 | Unificar o mascote do jogo com o coelho do ícone, mantendo poses de repouso e salto | Concluída (0.2.1) |
| 12 | Corrigir multitoque e integrar arte aprovada dos inimigos/obstáculos | Concluída (0.3.0) |
| 13 | Ajustar poderes, adicionar Supersalto e garantir evasão com salto cronometrado | Concluída (0.4.0) |
| 14 | Tornar natural a aproximação e entrada do coelho no portal | Concluída (0.4.1) |
| 15 | Redesenhar o cenário, diversificar inimigos, enriquecer o relevo e corrigir colisões | Concluída (0.5.0) |
| 16 | Ajustar altura do salto, diferenciar inimigos, animar a caminhada e ampliar a complexidade da fase | Concluída (0.5.2) |
| 17 | Consolidar a especificação da fase 2 e o pacote de handoff técnico | Concluída (planejamento; sem implementação) |
| 18 | Desenhar e implementar a caverna de cristais e o percurso de cerca de 3 minutos | Concluída (0.6.0) |
| 19 | Implementar buracos, animação de queda, perda de coração e retorno seguro | Concluída (0.6.0) |
| 20 | Integrar três inimigos aprovados, animações naturais e projétil de semente evitável | Concluída (0.6.0) |
| 21 | Integrar progressão, poderes, coletáveis e portal da fase 2 | Concluída (0.6.0) |
| 22 | Testes de instrumentação, estabilidade, tempo da fase e ajustes visuais | Concluída no CI; tempo alvo requer confirmação em aparelho físico |
| 23 | Atualizar documentação de release e gerar APK da fase 2 | Concluída (0.6.0) |

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
- Ajustado o layout após inspecionar as capturas do emulador: os controles não aparecem mais por cima do menu, as três dicas cabem no painel e a cenoura do HUD tem escala legível.
- GitHub Actions execução [#22](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39516173225), tentativa 2, concluída com sucesso: Gradle `assembleDebug`, instalação em Android Emulator API 35, comandos de salto neutro/para frente/para trás, disparo com movimento, coelho ativo após a travessia e Logcat sem `FATAL EXCEPTION` nem `ConcurrentModificationException`.
- Capturas e Logcat: artefato `Coelhitos-emulator-evidence` (`12308053193`, retenção de 14 dias). A execução automatizada também confirma as entradas; a tela de aviso do modo imersivo do Android pode aparecer na captura durante a inicialização do emulador.
- APK debug `Coelhitos-v0.2.0-debug.apk`, `versionCode 5`, `versionName 0.2.0`. SHA-256: `33dc6cec9cf5149323b130c52ec17d2c0fc2361de5a9be42931b91b9823f358d`.
- Artefato APK `Coelhitos-debug-apk` (`12308003118`, retenção de 30 dias), anexado à [execução #22](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39516173225).
- A checagem continua limitada a um trecho curto automatizado; o tempo de três minutos e a ergonomia em aparelho físico precisam de avaliação manual.

### 21/10/2026 — etapa 11: mascote igual ao ícone (0.2.1)

- Criadas poses transparentes de repouso e salto a partir do coelho do ícone, preservando pelagem branca, olhos castanhos, orelhas rosadas e cenoura.
- Substituído o desenho vetorial simples por sprites em alta definição; a animação alterna entre apoio e salto, com as patas traseiras juntas.
- O personagem continua lateral, vira para a esquerda/direita e mantém efeitos de invulnerabilidade, aura do Raio e sombra de contato.
- Atualizada a versão para `0.2.1` (`versionCode 6`). - GitHub Actions execução [#24](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39593141098) concluída com sucesso: compilação Gradle, instalação em Android Emulator API 35, navegação, saltos neutro/para frente/para trás, disparo com movimento, processo ativo após o percurso e Logcat sem `FATAL EXCEPTION` nem `ConcurrentModificationException`.
- Capturas e Logcat: artefato `Coelhitos-emulator-evidence` (`12342766264`, retenção de 14 dias). A captura final mostra o coelho novo em jogo, com escala próxima de um quarto da altura da tela.
- APK debug `Coelhitos-v0.2.1-debug.apk`, `versionCode 6`, `versionName 0.2.1`; SHA-256: `2b19058a62821209db73244c7bb39b24df050c74c2c7df68ecf1cea15ec43c5c`.
- Artefato APK `Coelhitos-debug-apk` (`12342629714`, retenção de 30 dias), anexado à [execução #24](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39593141098).
- Limite: o teste automatizado cobre um trecho curto; a duração integral da fase e a jogabilidade em aparelho físico ainda precisam de avaliação manual.


### 21/10/2026 — planejamento v0.3.0 (sem implementação)

- Diagnosticada a falha de associação de ponteiros no multitoque: `getX()`/`getY()` usam o ponteiro padrão em `ACTION_POINTER_DOWN`, mas o evento associa o identificador do ponteiro novo.
- Registrado plano de correção e critérios de teste para salto direcional e ação durante movimento em `docs/plano-controles-e-arte-v0.3.0.md`.
- Criada uma prancha de conceitos para os dois inimigos (azul pequeno e roxo grande) e os três obstáculos do código atual (espinhos, fosso e fogo). A integração espera a aprovação visual.
- A prancha inclui o fosso para representar o código atual; o plano preserva a decisão anterior de não incluir buracos na fase 1.
- Nenhum código, arte do jogo ou APK foi alterado nesta etapa.

### 21/10/2026 — implementação v0.3.0

- Corrigido o roteamento de ponteiros simultâneos para joystick, AÇÃO e PULO; o salto captura a direção ativa e o tiro não interrompe nem inverte o movimento.
- Integradas as artes aprovadas dos inimigos azul/roxo, espinhos e fogo. O fosso não aparece na primeira fase, conforme a especificação; o sprite permanece como recurso para fases futuras.
- Criados seis testes de instrumentação no Android Emulator para eventos de dois dedos, corrida, salto neutro/direcional e disparo mantendo movimento à esquerda e à direita.
- Build Gradle e seis testes instrumentados passaram no GitHub Actions #34, em emulador Android API 35. Os testes confirmam pulo direcional para os dois lados, pulo vertical sem direção, AÇÃO durante deslocamento para esquerda/direita e corrida ao manter AÇÃO pressionada.
- A execução registrou disparos para esquerda/direita com movimento ativo, saltos para frente/trás e pulo neutro; o jogo permaneceu ativo após o trecho automatizado e o Logcat não apresentou exceções fatais.
- As capturas de tela entregues pelo emulador headless vieram pretas, portanto não são evidência de avaliação visual. A validação desta versão confirma build, entrada multitoque automatizada e estabilidade no trecho testado.
- APK `Coelhitos-v0.3.0-debug.apk`, `versionCode 7`, `versionName 0.3.0`; SHA-256: `3321bd433faffb1f4016be8faab9ca43fb97ef79e3cb82bafd65a047c5513a92`.
- Artefato APK `Coelhitos-debug-apk` (`12370400105`, retenção de 30 dias) e evidência (`12369830437`, retenção de 14 dias) na [execução #34](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39649842891).

### 21/10/2026 — mudanças v0.4.0

- Probabilidades atualizadas: Espinho 25%, Fogo 20%, Pena 25%, Raio 13% e Supersalto 17%; a distribuição soma 100%.
- Espinho agora cresce do chão à frente do coelho. Raio dura 8 segundos. Supersalto é um novo poder que aumenta altura e alcance dos saltos por 20 segundos.
- O salto direcional recebeu velocidade horizontal e impulso vertical para permitir passar por cima de espinhos, fogo e inimigos. Novos testes instrumentados verificam a evasão dos dois tipos de perigo e dos dois tamanhos de inimigo, além das probabilidades e duração dos poderes.
- GitHub Actions [execução #36](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39691425834) concluiu com sucesso: build Gradle, instalação e testes instrumentados em emulador Android API 35.
- Os nove testes passaram, cobrindo a distribuição dos 100 resultados possíveis dos baús, durações dos poderes, impulso do Supersalto, evasão cronometrada de espinhos, fogo e inimigos de 1/3 golpes, além dos controles existentes.
- Logcat sem `FATAL EXCEPTION` ou `ConcurrentModificationException`; APK validado como arquivo ZIP íntegro.
- APK `Coelhitos-v0.4.0-debug.apk`, `versionCode 8`, `versionName 0.4.0`; SHA-256: `f7b8613cd527558c3561e000c683f9ac028fc21c8d55eb471105e1d43d58f012`.
- Artefato APK `Coelhitos-debug-apk` (`12388728282`, retenção de 30 dias) e evidência (`12388703335`, retenção de 14 dias) anexados à execução #36.
- A evasão foi confirmada nos cenários automatizados; tempo total da fase, ergonomia e visual devem ser avaliados também em aparelho físico.

### 21/10/2026 — mudança v0.4.1

- Ao chegar perto do portal enquanto avança, o coelho entra em uma animação curta de caminhada automática; os controles são bloqueados durante esse trecho, o sprite diminui ao atravessar a abertura e a conclusão só aparece depois da entrada.
- GitHub Actions [execução #40](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39695463171) concluiu com sucesso: build Gradle e 10 testes instrumentados em emulador Android API 35.
- O teste novo confirma caminhada até o portal e conclusão somente após a entrada; os outros nove testes de poderes, evasão e controles também passaram.
- Logcat sem `FATAL EXCEPTION` ou `ConcurrentModificationException`; APK conferido como arquivo ZIP íntegro.
- APK debug `Coelhitos-v0.4.1-debug.apk`, `versionCode 9`, `versionName 0.4.1`; SHA-256: `1b4520857f13fb54ec2057e6c8e0c4456ee4e17d0e6da8087c43eb0eb06c2c75`.
- Artefato APK `Coelhitos-debug-apk` (`12389869066`, retenção de 30 dias) e evidência (`12390008829`, retenção de 14 dias), anexados à execução #40.
- A duração completa da fase e a avaliação visual em aparelho físico permanecem para validação manual.

### 22/10/2026 — etapa 15: cenário, inimigos e colisões (0.5.0)

- Cenário redesenhado com morros em camadas, contornos e sombras suaves; decor ganhou volume e detalhes no mesmo estilo macio do coelho. A planície inclui subidas retas íngremes.
- Fase ampliada para 24 inimigos: 18 terrestres e 6 voadores. Os terrestres seguem o terreno com patas apoiadas e animação; alguns têm cauda que balança. Os voadores usam cachecol.
- Colisões agora bloqueiam a passagem e aplicam a perda prevista de poder/vida; saltos direcionais cronometrados permitem superar perigos e inimigos.
- GitHub Actions [execução #44](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39808475411) concluída com sucesso. Build Gradle e 12 testes instrumentados passaram em emulador Android API 35; incluídos testes de composição do nível, subida reta, colisões bloqueantes e evasão por salto.
- APK `Coelhitos-v0.5.0-debug.apk`, `versionCode 10`, `versionName 0.5.0`; SHA-256: `4040aa1b5381cd4c99b6e256423b10b039d5ad2c1eda42ffc5a6a532171c9e89`.
- Artefato `Coelhitos-debug-apk` (`12438762708`, retenção de 30 dias) e evidência `Coelhitos-emulator-evidence` (`12439206467`, retenção de 14 dias), anexados à execução #44.

### 22/10/2026 — revisão visual e de movimento (0.5.2)

- O salto direcional comum voltou à altura regular (`1,8 × altura útil` de impulso vertical); o salto sem direção mantém `1,47 ×`. Somente Supersalto usa `2 ×` e aumenta o alcance horizontal.
- Inimigos agora têm cinco tratamentos visuais: criatura azul pequena, criatura verde com casco, criatura roxa alongada com cauda, criatura dourada com chifres e ave verde-azulada de cachecol. As diferenças usam silhuetas e corpos próprios, não apenas adornos.
- Patas dos inimigos terrestres alternam passos visíveis com o corpo acompanhando o ritmo; aves batem as asas e ondulam o cachecol. Teste instrumentado compara quadros da marcha.
- Relevo ampliado com seis pares de rampas retas e íngremes e mais morros ao fundo. A fase tem 22 inimigos terrestres, 8 voadores e 14 obstáculos.
- Contato contínuo contra um inimigo ou obstáculo continua bloqueando o avanço sem reaplicar dano enquanto o coelho permanece pressionando o mesmo ponto.
- GitHub Actions [execução #48](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39828135547) concluiu com sucesso: Gradle e 13 testes instrumentados passaram no emulador Android API 35.
- APK debug `Coelhitos-v0.5.2-debug.apk`, `versionCode 12`, `versionName 0.5.2`; SHA-256: `8a1904be5ec82eea12111d2351cf07da080cda443f8bae3e2258dc3e9c3b6ed8`.
- Artefato APK `Coelhitos-debug-apk` (`12447866823`, retenção de 30 dias) e evidência `Coelhitos-emulator-evidence` (`12447122967`, retenção de 14 dias), anexados à execução #48.


### 24/10/2026 — fase 2: decisões, arte aprovada e plano (sem implementação)

- Duração aprovada: aproximadamente 3 minutos em ritmo normal.
- Cenário aprovado: caverna colorida com cristais luminosos; subidas e descidas, buracos e obstáculos da fase 1.
- Aprovados visualmente três inimigos: criatura vegetal terrestre que dispara sementes; besouro baixo de dorso rochoso e placas laranja; mariposa lilás voadora com quatro asas e marcas turquesa. Prancha em docs/assets/fase2-inimigos-conceito-aprovado.webp.
- O disparo de sementes deve ser evitável com pulo. Movimentos naturais, incluindo o ciclo de patas do coelho e a batida de asas da mariposa.
- Buracos animam a queda, causam perda de um coração e devolvem o coelho a piso seguro antes do buraco. Perder três corações reinicia a fase.
- Mantêm-se controles, poderes, vida e objetivo do portal definidos para a fase 1.
- Nenhum código ou APK foi alterado. Plano técnico em docs/fase-2-planejamento-e-handoff.md.
- Próxima etapa: implementar cenário e topologia da fase 2, seguindo as etapas 18 a 23.




### 24/10/2026 — implementação da fase 2 (0.6.0)

- Concluída a caverna colorida com cristais luminosos, piso com subidas/descidas e HUD identificado como fase 2. A fase tem seu próprio arranjo de pickups, perigos e inimigos.
- O portal da fase 1 preserva corações, poder e munição e leva à caverna; o portal da fase 2 conclui a jornada atual.
- Implementados 5 baús, cenouras e corações, espinhos/fogo e cinco buracos. Cair em um vão anima a queda, desconta exatamente um coração e retorna a piso seguro; a regra é direta mesmo com poder equipado.
- Integrados os conceitos aprovados: planta terrestre que antecipa e dispara sementes, besouro rochoso baixo com ciclo de patas e mariposa lilás de quatro asas animadas. O pulo evita a semente.
- Adicionada trilha sintetizada própria para a caverna, além de efeitos de disparo de semente e queda.
- Testes de instrumentação adicionados para composição da fase, rampas/pits, continuidade entre fases, queda com cobrança única e retorno seguro, projétil evitável, ciclo de asas e controles. O relatório do CI mostra **18/18 testes passando**. A primeira execução do workflow ficou vermelha apenas porque a etapa de validação ainda procurava o texto `OK (13 tests)`; a contagem passou para uma verificação genérica de resultado antes de gerar o release.
- O alvo de aproximadamente 3 minutos orientou o comprimento/velocidade do percurso, mas o CI não simula uma jogada normal inteira. A duração final ainda deve ser medida em aparelho físico e ajustada se necessário.
- `versionCode 13`, `versionName 0.6.0`. GitHub Actions [execução #54](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/40100495114) concluiu com sucesso: Gradle, 18 testes instrumentados e smoke test no Android Emulator API 35. Nenhuma exceção fatal ou ConcurrentModificationException no Logcat.
- APK debug `Coelhitos-v0.6.0-debug.apk`, `versionCode 13`, `versionName 0.6.0`; SHA-256: `8f9ebe72579aae83afbbb469c8dc80274b1ee10323b33f1ea616fb420e4dcf70`.
- Artefato `Coelhitos-debug-apk` (`12563220098`, retenção de 30 dias) e evidência `Coelhitos-emulator-evidence` (`12562946094`, retenção de 14 dias) anexados à execução #54.
