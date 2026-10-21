# Coelhitos 0.3.0

## Controles multitoque

- Corrigida a identificação do dedo em eventos `ACTION_POINTER_DOWN`. Joystick, AÇÃO e PULO não substituem mais o ponteiro dos outros controles.
- PULO captura a direção ativa: direita, esquerda ou vertical quando o joystick está neutro.
- AÇÃO curto dispara sem interromper o movimento; AÇÃO mantido com direção ativa a corrida.

## Inimigos e perigos

- Inimigo pequeno azul e inimigo grande roxo receberam sprites 3D fofos e texturizados no estilo do coelho; resistência continua em 1 e 3 golpes.
- Espinhos e fogo receberam sprites próprios com volume, luz e sombras para destacar do cenário.
- O sprite de fosso foi preparado para fases futuras. A fase 1 não contém buracos, conforme a especificação.
- Colisões, dano, vida dos inimigos, projéteis e controles de jogo foram preservados.

## Testes

Seis testes de instrumentação no emulador cobrem pulo para a esquerda/direita/vertical, AÇÃO durante movimento nos dois sentidos e pressão longa para correr. Os resultados da compilação, do teste e o SHA-256 do APK serão registrados após o CI.
