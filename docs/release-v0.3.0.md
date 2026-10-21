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

Seis testes de instrumentação no emulador cobrem pulo para a esquerda/direita/vertical, AÇÃO durante movimento nos dois sentidos e pressão longa para correr.

## Validação e APK

- GitHub Actions [execução #34](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39649842891): build Gradle e execução em emulador Android API 35 concluídos com sucesso.
- Resultado: `OK (6 tests)`. O Logcat confirma saltos para esquerda/direita/vertical e disparos para ambos os sentidos enquanto o movimento continua; não houve exceção fatal no trecho automatizado.
- As capturas de tela do emulador headless vieram pretas e não servem para inspeção visual. A validação visual em aparelho físico continua pendente.
- APK: `Coelhitos-v0.3.0-debug.apk`, `versionCode 7`, `versionName 0.3.0`.
- SHA-256: `3321bd433faffb1f4016be8faab9ca43fb97ef79e3cb82bafd65a047c5513a92`.
- Artefatos no CI: APK `12370400105` (30 dias), evidência de emulador `12369830437` (14 dias).
