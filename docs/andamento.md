# Coelhitos — plano e andamento

Este arquivo acompanha a implementação em etapas, conforme solicitado. Cada etapa deve ser documentada antes de passar à seguinte.

## Etapas

| Etapa | Entrega | Estado |
|---|---|---|
| 1 | Estrutura Android, orientação horizontal, tela de jogo, desenho original e controles por toque | Concluída |
| 2 | Percurso da planície dimensionado para cerca de 3 minutos, movimento, câmera, obstáculos, corações e reinício | Concluída |
| 3 | Cinco baús e poderes, cinco cenouras iniciais, projéteis, inimigos com resistências e portal | Concluída |
| 4 | Trilha original em loop e efeitos sonoros de ações e eventos | Concluída |
| 5 | Compilação, verificações, APK de depuração e publicação do resultado | Em andamento |

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
