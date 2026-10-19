# Coelhitos — especificação funcional

**Versão:** 1.0 — 19/10/2026  
**Escopo inicial:** protótipo Android com a primeira fase completa.

## Conceito

Jogo de plataforma 2D lateral para celular Android, sempre em orientação horizontal. O personagem é um coelho que atravessa um portal para um mundo diferente, explora uma planície gramada e precisa chegar ao portal no fim da fase. O visual deve ser original, colorido, expressivo e arredondado, inspirado no clima de jogos de plataforma Kirby no Nintendo Switch, sem reproduzir personagens ou recursos protegidos.

## Primeira fase

- A fase inicial é uma planície com grama, poucas árvores e elementos de fantasia.
- O jogador deve levar aproximadamente 3 minutos para chegar ao portal no ritmo normal.
- A fase contém exatamente 5 baús. Tocar em um baú concede um poder aleatório.
- Há 5 cenouras disponíveis para coleta no começo da fase; cada cenoura coletada dá um disparo.
- Obstáculos: espinhos, buracos e fogo.
- O coelho tem aproximadamente um quarto da altura útil da tela.

## Movimento e controles

- O coelho anda, corre, pula e se desloca para a esquerda e para a direita.
- O joystick virtual fica à esquerda. Direção para cima pula; diagonal para cima salta para frente.
- À direita há dois botões: salto e ação.
- Segurar o botão de ação enquanto o joystick indica uma direção faz o coelho correr.
- Tocar e soltar rapidamente o botão de ação dispara. Sem poder, o disparo consome uma cenoura.
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

## Decisões de implementação assumidas

- O botão de ação escolhe automaticamente entre corrida (mantido pressionado com direção) e disparo (toque curto e soltura).
- A ação de Espinho, Pena e Fogo ocorre ao disparar; Raio ativa no momento da coleta.
- A primeira fase terá cinco cenouras colocadas perto do começo e outras recompensas de coração ao longo do percurso, sem alterar a quantidade inicial.
- Os desenhos serão feitos no Canvas, sem imagens externas, para manter o APK leve e a direção visual original.
