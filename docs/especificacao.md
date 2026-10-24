# Coelhitos — especificação funcional

**Versão:** 1.7 — 24/10/2026
**Estado:** fases 1 e 2 implementadas (v0.6.0); medir o tempo da fase 2 em aparelho físico permanece pendente.

## Conceito

Jogo de plataforma 2D lateral para celular Android, sempre em orientação horizontal. O personagem é um coelho que atravessa um portal para um mundo diferente, explora uma planície gramada e precisa chegar ao portal no fim da fase. O visual deve ser original, colorido, expressivo e arredondado, inspirado no clima de jogos de plataforma Kirby no Nintendo Switch, sem reproduzir personagens ou recursos protegidos.

## Visão do jogo completo

- Novos portais levam a mundos com plantas, inimigos e obstáculos próprios.
- O coelho pode escolher entre várias cores.
- O personagem poderá encontrar cabanas abandonadas e explorá-las para achar armaduras, poderes e itens.
- Cenouras representam a moeda do jogo para compras de armaduras e cores. Na primeira fase elas também funcionam como munição de uso único. A relação entre munição e saldo de compras fica para a especificação do sistema de loja; a primeira fase não inclui loja.
- O baseline publicado v0.5.2 implementa somente a primeira fase. A fase 2 está aprovada para planejamento e desenvolvimento, descrita nesta especificação e em docs/fase-2-planejamento-e-handoff.md. Mundos posteriores à fase 2, cabanas, armaduras, seleção de cores e loja permanecem no backlog.

## Primeira fase

- A fase inicial é uma planície gramada com morros arredondados e seis pares de subidas e descidas retas e íngremes; o relevo afeta a altura do personagem, inimigos, objetos e colisões. O cenário tem camadas, sombras, luz suave e vegetação volumétrica para combinar com o acabamento macio e tridimensional do coelho.
- A fase distribui ao longo do caminho 30 inimigos: criaturas terrestres de formatos diferentes, incluindo algumas com cauda móvel e outras com chifres, além de oito aves voadoras com cachecol e asas animadas. As criaturas no chão apoiam as patas no terreno e usam ciclos visíveis de caminhada; o corpo acompanha os passos.
- O jogador deve levar aproximadamente 3 minutos para chegar ao portal no ritmo normal.
- A fase contém exatamente 5 baús. Tocar em um baú concede um poder aleatório.
- Há 5 cenouras disponíveis para coleta no começo da fase; outras cenouras aparecem pelo caminho. Cada cenoura coletada dá um disparo.
- Obstáculos da primeira fase: espinhos e fogo. Buracos ficam reservados a fases futuras.
- O coelho tem aproximadamente um quarto da altura útil da tela.

## Segunda fase — implementada (v0.6.0)

A fase 2 foi aprovada em 24/10/2026. O roteiro técnico e os pontos ainda em aberto estão em [fase-2-planejamento-e-handoff.md](fase-2-planejamento-e-handoff.md).

- **Duração:** cerca de 3 minutos para terminar em ritmo normal, como na fase 1.
- **Ambiente:** caverna de fantasia, colorida, com cristais luminosos. O visual combina com o acabamento macio, arredondado e expressivo do coelho do ícone. A arte continua original.
- **Percurso:** alterna trechos de subida e descida e inclui buracos, além dos espinhos e do fogo já existentes na fase 1. O traçado exato e a quantidade/largura de buracos serão definidos ao desenhar o nível e ajustados para o tempo-alvo.
- **Queda em buraco:** tem animação própria, tira um coração e retorna o coelho para um ponto seguro antes daquele buraco. Esse custo não se converte apenas em perda do poder equipado. Com 3 corações perdidos, vale a regra existente de reiniciar a fase.
- **Inimigos novos aprovados visualmente:** criatura vegetal terrestre que dispara sementes; besouro baixo com dorso rochoso e placas laranja; mariposa voadora lilás, com quatro asas visíveis e marcas turquesa. A prancha aprovada está em [docs/assets/fase2-inimigos-conceito-aprovado.webp](assets/fase2-inimigos-conceito-aprovado.webp).
- **Ataque à distância:** o inimigo vegetal dispara sementes; o jogador consegue evitar o projétil pulando.
- **Animação natural:** movimentos do coelho, dos inimigos e de partes móveis devem ser contínuos e coerentes com a anatomia. A marcha quadrúpede do coelho mantém pequenos impulsos das patas traseiras juntas; inimigos terrestres apoiam e alternam as patas no terreno; a mariposa bate as asas em ciclo visível.
- **Regras herdadas:** manter controles, poderes e sistema de corações da fase 1; a conclusão continua sendo entrar calmamente no portal ao final. Conservar probabilidades e efeitos dos poderes existentes, salvo problema demonstrado no balanceamento.

### Parâmetros a definir durante o desenho do nível

Não são bloqueios para começar. Documentar as escolhas de detalhe: quantidade e posição de inimigos, baús e coletáveis; espaçamento/largura de buracos; comportamento de patrulha; tempo de aviso do disparo; e música/ambiente da caverna. Começar com os valores da fase 1 quando forem compatíveis e calibrar para 3 minutos sem alterar as decisões aprovadas.

## Movimento e controles

- O coelho é visto de lado, voltado para a direita quando avança; não fica de frente para a câmera. Sua locomoção é quadrúpede, como a de um coelho comum, com impulso das patas traseiras.
- O coelho anda, corre, pula e se desloca para a esquerda e para a direita. Ao mudar de direção, a silhueta vira junto.
- O joystick virtual fica à esquerda. Pulo sem direção horizontal sobe no mesmo lugar. Direita + pulo salta para a direita; esquerda + pulo salta para a esquerda. O coelho vira para o lado da direcional, inclusive quando anda para trás.
- Ao caminhar ou correr, as patas traseiras trabalham juntas e impulsionam pequenos pulos; as patas dianteiras acompanham o movimento. A animação não deve imitar a passada de um gato.
- À direita há dois botões: salto e ação.
- Segurar o botão de ação enquanto o joystick indica uma direção faz o coelho correr.
- Tocar e soltar o botão de ação dispara, inclusive enquanto o joystick continua apontado para a frente. O toque curto permanece responsivo durante o movimento; manter pressionado com direção aciona a corrida. Sem poder, o disparo consome uma cenoura.
- O botão de pulo dispara um salto uma vez por toque, inclusive com o joystick ativo. Direita + pulo salta para a direita; esquerda + pulo salta para a esquerda; sem direção horizontal, o salto sobe no mesmo lugar. Um toque pouco antes de aterrissar fica brevemente em buffer para não se perder.
- Joystick, AÇÃO e PULO podem ser operados ao mesmo tempo em dedos separados; soltar um botão não cancela nem troca o controle dos outros dedos.
- Com Pena ou Fogo, o disparo usa o poder equipado. Espinho faz crescer um espinho no chão, um pouco à frente do coelho. Supersalto é passivo e, enquanto ativo, AÇÃO continua disparando cenouras coletadas.
- Raio ativa automaticamente ao ser encontrado: por 8 segundos, o coelho corre mais rápido e atropela inimigos à frente.
- Supersalto aumenta a altura e a distância do pulo por 20 segundos.
- Com a direcional para a esquerda ou direita, o botão de pulo dá um salto frontal de altura normal, suficiente para superar espinhos, fogo e inimigos quando acionado no momento certo. Somente o poder Supersalto aumenta a altura do pulo. A colisão acontece ao nível do chão; o salto evita o dano enquanto o coelho passa acima do perigo.
- O corpo do coelho colide com a área visível de inimigos e obstáculos: um contato bloqueia seu avanço e aplica a regra de dano já definida. Saltar acima da área de colisão continua permitindo passar sem dano.
- Manter a direção empurrada contra o mesmo inimigo ou obstáculo bloqueia o coelho sem reaplicar dano durante o contato contínuo.

## Poderes

Um único poder pode estar equipado. Pegar outro substitui o anterior.

| Poder | Chance | Efeito |
|---|---:|---|
| Espinho | 25% | Faz crescer um espinho no chão à frente do coelho ao disparar. |
| Fogo | 20% | Dispara bolas de fogo que explodem. |
| Pena | 25% | Dispara uma rajada de penas. |
| Raio | 13% | Por 8 segundos, aumenta a velocidade e permite atropelar inimigos à frente. |
| Supersalto | 17% | Por 20 segundos, aumenta a altura e a distância dos saltos. |

Se o coelho for atingido por um inimigo ou obstáculo enquanto estiver com um poder, perde o poder e não perde coração. Sem poder, uma colisão tira um coração. Após ser atingido, há breve intervalo de invulnerabilidade para evitar dano repetido instantâneo.

## Cenouras, inimigos, vida e conclusão

- Cada cenoura coletada rende um disparo de cenoura.
- Inimigos têm resistência variável: o inimigo mais fraco morre com um disparo e o mais resistente com três.
- O coelho começa com 3 corações. Também pode encontrar corações durante o caminho.
- Ao perder os 3 corações, a fase reinicia desde o começo.
- O objetivo é alcançar o portal no fim da fase; ele representa a passagem à fase seguinte. Ao se aproximar pelo lado direito, o coelho continua andando calmamente, entra no portal e só então a fase é concluída. Durante essa animação, os controles ficam temporariamente bloqueados.

## Som

O jogo terá trilha musical original em loop e efeitos originais para caminhada, corrida, salto, coleta de cenoura, abertura de baú, obtenção e uso de cada poder, disparos, inimigos, dano, perda de coração, reinício e chegada ao portal. Os sons são sintetizados no aplicativo.
- Ao perder um coração, a trilha faz uma pausa curta e toca um efeito descendente próprio de perda de vida. Perder apenas um poder usa o efeito de dano, sem a pausa da música.
- Coelho, inimigos e obstáculos têm sombras e luzes em gradiente para sugerir volume e melhorar a leitura contra o cenário.
- A interface usa painéis translúcidos, indicadores desenhados no próprio jogo, hierarquia tipográfica clara e controles circulares com símbolos e rótulos legíveis.
- O aplicativo inclui ícone próprio nos formatos adaptativo e legado do Android.
- O projétil de Fogo tem corpo laranja, núcleo amarelo, halo e rastro; a Pena tem haste e barbas desenhadas para que os dois poderes sejam fáceis de distinguir.
- O mascote em jogo deve manter a mesma identidade visual do coelho do ícone: pelagem branca fofa, olhos castanhos grandes, orelhas rosadas e cenoura. As poses de repouso e salto preservam o perfil lateral e o movimento em pares das patas traseiras.

## Backlog posterior à fase 2

- Criar mundos e portais posteriores, com ambientes, plantas, obstáculos e inimigos variados.
- Adicionar cabanas exploráveis, armaduras e seleção de cores.
- Definir a loja e como separar ou conciliar cenouras usadas como munição e como moeda.

## Decisões de implementação assumidas

- O botão de ação escolhe automaticamente entre corrida (mantido pressionado com direção) e disparo (toque curto e soltura).
- A ação de Espinho, Pena e Fogo ocorre ao disparar; Raio ativa no momento da coleta.
- A primeira fase terá cinco cenouras colocadas perto do começo e outras recompensas de coração ao longo do percurso, sem alterar a quantidade inicial.
- O mundo é renderizado em Canvas e pode combinar formas desenhadas no código com sprites originais incluídos no app; nenhum recurso visual é baixado da rede durante o jogo.


