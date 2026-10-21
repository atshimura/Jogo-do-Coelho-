# Plano — controles multitoque e identidade visual (v0.3.0)

**Estado:** implementado na versão 0.3.0, aprovada a arte conceitual. APK e validação em emulador em andamento.

## 1. Diagnóstico dos controles

Em `app/src/main/java/com/coelhitos/MainActivity.java`, `onTouchEvent` obtém `x` e `y` com `getX()` e `getY()` sem informar o índice do ponteiro. Em seguida, usa `getPointerId(getActionIndex())`, que identifica o novo dedo em eventos `ACTION_POINTER_DOWN`. Quando o joystick já está sendo usado, as coordenadas lidas pertencem ao primeiro dedo, mas podem ser associadas ao identificador do segundo. O código pode então substituir o ponteiro do joystick pelo dedo que tocou AÇÃO ou PULO.

Isso explica os dois relatos: PULO deixa de receber o toque ou perde a direção ativa; AÇÃO pode descartar a direção de caminhada, interromper o movimento ou fazê-lo apontar para a direita. A lógica de salto e de disparo usa `leftHeld` e `rightHeld`, então depende de o joystick continuar associado ao dedo correto.

## 2. Solução implementada

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

## 4. Arte visual aprovada e integrada

A prancha aprovada foi convertida em sprites transparentes para o jogo. Ela contém:

- Inimigo pequeno azul (1 golpe).
- Inimigo grande roxo (3 golpes).
- Espinhos de chão.
- Fosso/buraco.
- Fogo de chão.

Os sprites usam volume, textura macia, iluminação e contornos compatíveis com o coelho. As silhuetas e o contraste com a grama permanecem claros. Vida, resistência e colisões dos inimigos e os danos dos perigos foram preservados.

### Divergência a preservar até decisão

O código atual contém fossos/buracos, e a prancha os inclui para retratar o conteúdo existente. Há uma decisão funcional anterior de que a primeira fase não deve ter buracos. Portanto, mesmo que a aparência seja aprovada, o fosso deve ficar fora de qualquer implementação da fase 1 enquanto esse requisito prevalecer. A prancha é apenas uma proposta de estilo, não uma mudança nessa regra.

## 5. Verificação e sequência concluída

1. Conceitos dos dois inimigos e dos três obstáculos aprovados.
2. Corrigido o mapeamento de coordenadas e IDs em `ACTION_POINTER_DOWN`; os controles mantêm IDs e estados independentes.
3. Criados seis testes de emulador com eventos multitoque sintéticos para saltos direcionais, salto neutro, tiro mantendo movimento para os dois sentidos e corrida com AÇÃO pressionada.
4. Integrados sprites dos inimigos, espinhos e fogo; fosso reservado a fases futuras. Comportamentos e colisões foram mantidos.
5. Build, teste em emulador e checksum serão registrados após a conclusão do CI.
