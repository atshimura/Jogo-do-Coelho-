# Coelhitos 0.4.0

## Poderes

As chances ao abrir um baú agora são: Espinho 25%, Fogo 20%, Pena 25%, Raio 13% e Supersalto 17%.

- Espinho faz crescer um espinho do chão à frente do coelho ao disparar.
- Fogo continua disparando bolas de fogo que explodem.
- Pena continua disparando uma rajada de penas.
- Raio continua acelerando o coelho e permitindo atropelar inimigos, agora por 8 segundos.
- Supersalto é novo: aumenta altura e alcance dos saltos por 20 segundos. Enquanto ativo, o botão AÇÃO continua disparando cenouras.

## Salto e evasão

O salto direcional ganhou mais impulso vertical e alcance horizontal. Acionado no tempo certo enquanto o coelho anda para frente, permite passar acima de espinhos, fogo e inimigos sem perder vida. O pulo neutro permanece vertical.

## Validação

Nove testes instrumentados passaram no GitHub Actions #36, em emulador Android API 35. Eles verificam os percentuais nos 100 resultados possíveis, duração dos poderes, impulso do Supersalto, evasão cronometrada de espinhos/fogo/inimigos e os controles de toque da versão anterior. O Logcat não apresentou `FATAL EXCEPTION` nem `ConcurrentModificationException`.

- APK debug: `Coelhitos-v0.4.0-debug.apk` (`versionCode 8`, `versionName 0.4.0`).
- SHA-256: `f7b8613cd527558c3561e000c683f9ac028fc21c8d55eb471105e1d43d58f012`.
- Artefato APK `Coelhitos-debug-apk` (`12388728282`, retenção de 30 dias); evidência `Coelhitos-emulator-evidence` (`12388703335`, retenção de 14 dias).
- Execução: [GitHub Actions #36](https://github.com/atshimura/Jogo-do-Coelho-/actions/runs/39691425834).
- O emulador não substitui a avaliação de duração completa da fase, ergonomia e visual em aparelho físico.
