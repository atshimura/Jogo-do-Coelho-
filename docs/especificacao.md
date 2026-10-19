# Coelhitos — especificação funcional

**Versão:** 1.2 — 19/10/2026
**Escopo inicial:** protótipo Android com a primeira fase completa.

## Conceito

Jogo de plataforma 2D lateral para celular Android, sempre em orientação horizontal. O personagem é um coelho que atravessa um portal para um mundo diferente, explora uma planície gramada e precisa chegar ao portal no fim da fase. O visual deve ser original, colorido, expressivo e arredondado, inspirado no clima de jogos de plataforma Kirby no Nintendo Switch, sem reproduzir personagens ou recursos protegidos.

## Visão do jogo completo

- Novos portais levam a mundos com plantas, inimigos e obstáculos próprios.
- O coelho pode escolher entre várias cores.
- O personagem poderá encontrar cabanas abandonadas e explorá-las para achar armaduras, poderes e itens.
- Cenouras representam a moeda do jogo para compras de armaduras e cores. Na primeira fase elas também funcionam como munição de uso único. A relação entre munição e saldo de compras fica para a especificação do sistema de loja; a primeira fase não inclui loja.
- A primeira versão implementa somente a fase inicial descrita abaixo. Os outros mundos, as cabanas, as armaduras, a seleção de cores e a loja ficam no backlog posterior.

## Primeira fase

- A fase inicial é uma planície com grama, poucas árvores e elementos de fantasia.
- O jogador deve levar aproximadamente 3 minutos para chegar ao portal no ritmo normal.
- A fase contém exatamente 5 baús. Tocar em um baú concede um poder aleatório.
- Há 5 cenouras disponíveis para coleta no começo da fase; outras cenouras aparecem pelo caminho. Cada cenoura coletada dá um disparo.
- Obstáculos: espinhos, buracos e fogo.
- O coelho tem aproximadamente um quarto da altura útil da tela.

## Movimento e controles

- O coelho é visto de lado, voltado para a direita quando avança; não fica de frente para a câmera. Sua locomoção é quadrúpede, como a de um coelho comum, com impulso das patas traseiras.
- O coelho anda, corre, pula e se desloca para a esquerda e para a direita. Ao mudar de direção, a silhueta vira junto.
- O joystick virtual fica à esquerda. Pulo sem direção horizontal sobe no mesmo lugar. Direita + pulo salta para a direita; esquerda + pulo salta para a esquerda. O coelho vira para o lado da direcional, inclusive quando anda para trás.
- Ao caminhar ou correr, as patas traseiras trabalham juntas e impulsionam pequenos pulos; as patas dianteiras acompanham o movimento. A animação não deve imitar a passada de um gato.
- À direita há dois botões: salto e ação.
- Segurar o botão de ação enquanto o joystick indica uma direção faz o coelho correr.
- Tocar e soltar o botão de ação dispara, inclusive enquanto o joystick continua apontado para a frente. O toque curto permanece responsivo durante o movimento; manter pressionado com direção aciona a corrida. Sem poder, o disparo consome uma cenoura.
- O botão de pulo dispara um salto uma vez por toque. Direita ou esquerda mantidas no joystick definem a direção do salto; sem direção horizontal, o salto sobe no mesmo lugar. Um toque pouco antes de aterrissar fica brevemente em buffer para não se perder.
- Com Pena ou Fogo, o disparo usa o poder equipado. Espinho cria um espinho que cai do céu um pouco à frente do coelho.
- Raio ativa automaticamente ao ser encontrado: por 5 segundos, o coelho corre mais rápido e atropela inimigos à frente.

## Poderes

Um único poder pode estar equipado. Pegar outro substitui o anterior.

| Poder | Chance | Efeito |
|---|---:|---|
| Espinho | 30% | Ao disparar, faz cair um espinho do céu um pouco à frente do coelho. |
| Raio | 15% | Por 5 segundos, aumenta a velocidade e permite atropelar inimigos à frente. |
| Pena | 30% | Dispara uma rajada de penas. |
| Fogo | 25% | Dispara bolas de fogo que explodem. |

Se o coelho for atingido por um inimigo ou obstáculo enquanto estiver com um poder, perde o poder e não perde coração. Sem poder, uma colisão tira um coração. Após ser atingido, há breve intervalo de invulnerabilidade para evitar dano repetido instantâneo.

## Cenouras, inimigos, vida e conclusão

- Cada cenoura coletada rende um disparo de cenoura.
- Inimigos têm resistência variável: o inimigo mais fraco morre com um disparo e o mais resistente com três.
- O coelho começa com 3 corações. Também pode encontrar corações durante o caminho.
- Ao perder os 3 corações, a fase reinicia desde o começo.
- O objetivo é alcançar o portal no fim da fase; ele representa a passagem à fase seguinte.

## Som

O jogo terá trilha musical original em loop e efeitos originais para caminhada, corrida, salto, coleta de cenoura, abertura de baú, obtenção e uso de cada poder, disparos, inimigos, dano, perda de coração, reinício e chegada ao portal. Os sons são sintetizados no aplicativo.
- Ao perder um coração, a trilha faz uma pausa curta e toca um efeito descendente próprio de perda de vida. Perder apenas um poder usa o efeito de dano, sem a pausa da música.
- Coelho, inimigos e obstáculos têm sombras e luzes em gradiente para sugerir volume e melhorar a leitura contra o cenário.

## Backlog após a primeira fase

- Criar os próximos mundos e portais, com ambientes, plantas, obstáculos e inimigos variados.
- Adicionar cabanas exploráveis, armaduras e seleção de cores.
- Definir a loja e como separar ou conciliar cenouras usadas como munição e como moeda.

## Decisões de implementação assumidas

- O botão de ação escolhe automaticamente entre corrida (mantido pressionado com direção) e disparo (toque curto e soltura).
- A ação de Espinho, Pena e Fogo ocorre ao disparar; Raio ativa no momento da coleta.
- A primeira fase terá cinco cenouras colocadas perto do começo e outras recompensas de coração ao longo do percurso, sem alterar a quantidade inicial.
- Os desenhos serão feitos no Canvas, sem imagens externas, para manter o APK leve e a direção visual original.
