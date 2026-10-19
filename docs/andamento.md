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
