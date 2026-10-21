# Plano — controles multitoque e identidade visual (v0.3.0)

**Estado:** planejamento e propostas visuais aguardando aprovação. Nenhuma alteração no comportamento ou nos recursos do jogo foi implementada nesta etapa.

## 1. Diagnóstico dos controles

Em `app/src/main/java/com/coelhitos/MainActivity.java`, `onTouchEvent` obtém `x` e `y` com `getX()` e `getY()` sem informar o índice do ponteiro. Em seguida, usa `getPointerId(getActionIndex())`, que identifica o novo dedo em eventos `ACTION_POINTER_DOWN`. Quando o joystick já está sendo usado, as coordenadas lidas pertencem ao primeiro dedo, mas podem ser associadas ao identificador do segundo. O código pode então substituir o ponteiro do joystick pelo dedo que tocou AÇÃO ou PULO.

Isso explica os dois relatos: PULO deixa de receber o toque ou perde a direção ativa; AÇÃO pode descartar a direção de caminhada, interromper o movimento ou fazê-lo apontar para a direita. A lógica de salto e de disparo usa `leftHeld` e `rightHeld`, então depende de o joystick continuar associado ao dedo correto.

## 2. Solução planejada — sem implementação

1. Em `ACTION_POINTER_DOWN`, identificar a posição do dedo que acabou de tocar usando o índice de ação do próprio evento. Associar esse dedo somente ao controle que foi tocado.
2. Manter IDs independentes para joystick, AÇÃO e PULO. Um dedo novo em um botão não deve substituir nem reclassificar o dedo que já controla o joystick.
3. Ao soltar um dedo, limpar somente o estado do controle associado àquele ID. Tratar troca da ordem dos toques, `ACTION_CANCEL` e movimentos para fora da área de cada botão.
4. No PULO, capturar a direção horizontal ativa no início do comando: direita inicia salto para a direita; esquerda inicia salto para a esquerda; joystick neutro inicia salto vertical. Manter a trajetória horizontal coerente durante o salto mesmo que o botão PULO seja solto.
5. Em AÇÃO, manter o contrato atual: pressionar junto com uma direção acelera; toque curto e soltura dispara. O disparo deve preservar o movimento e usar a direção atual do coelho, sem virar ou parar o personagem.
6. Instrumentar os testes com registros de direção e posição antes, durante e depois dos comandos. Testar gestos multitoque reais/injetados, em vez de inferir a continuidade do movimento apenas pelo registro de que houve um disparo.

## 3. Critérios de validação

- Direita + PULO: um salto com deslocamento para a direita.
- Esquerda + PULO: um salto com deslocamento para a esquerda.
- PULO sem direção: salto vertical.
- Manter movimento e tocar/soltar AÇÃO: um disparo na direção do movimento; posição e direção continuam avançando sem inversão.
- Manter movimento e segurar AÇÃO: corrida contínua; soltar depois do limite de toque curto não dispara acidentalmente.
- Repetir os casos começando pelo joystick ou pelo botão, soltando os dedos em ordens diferentes e cancelar os toques.
- Confirmar no emulador que o aplicativo permanece ativo e que o Logcat não mostra exceções.

## 4. Propostas visuais para aprovação

A prancha criada para revisão contém os tipos que existem no código atual:

- Inimigo pequeno azul (1 golpe).
- Inimigo grande roxo (3 golpes).
- Espinhos de chão.
- Fosso/buraco.
- Fogo de chão.

A proposta é desenhá-los como criaturas e objetos 3D fofos, com textura macia e volume compatíveis com o coelho, preservando silhuetas distintas, contraste forte com a grama e leitura imediata de perigo. O visual dos inimigos não altera vida, resistência, colisão ou comportamento. O visual dos obstáculos também preserva colisões e dano.

### Divergência a preservar até decisão

O código atual contém fossos/buracos, e a prancha os inclui para retratar o conteúdo existente. Há uma decisão funcional anterior de que a primeira fase não deve ter buracos. Portanto, mesmo que a aparência seja aprovada, o fosso deve ficar fora de qualquer implementação da fase 1 enquanto esse requisito prevalecer. A prancha é apenas uma proposta de estilo, não uma mudança nessa regra.

## 5. Ordem prevista após aprovação

1. Receber aprovação ou ajustes para os cinco conceitos visuais e para a composição de obstáculos.
2. Corrigir e testar os controles multitoque isoladamente, sem alterar poderes, física ou colisões.
3. Produzir sprites finais transparentes dos itens aprovados e integrá-los sem alterar suas regras de jogo.
4. Validar no emulador controles, leitura dos sprites contra o cenário, colisões e estabilidade; atualizar especificação, andamento e notas da versão.
5. Gerar um novo APK somente depois da validação.
